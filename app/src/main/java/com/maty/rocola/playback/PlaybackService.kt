package com.maty.rocola.playback

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.maty.rocola.MainActivity
import com.maty.rocola.R
import com.maty.rocola.rocola
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock
import kotlin.random.Random

/**
 * Servicio en primer plano que reproduce la música. Media3 se encarga de la notificación
 * multimedia, la pantalla de bloqueo, los auriculares y el foco de audio; acá sumamos el
 * wake lock (para que siga con la pantalla apagada), la cola persistente, el temporizador
 * y el ecualizador.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var queueStore: QueueStore
    private lateinit var effects: AudioEffects
    private var positionSaver: Job? = null
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        queueStore = QueueStore(this)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pausa si se desconectan los auriculares
            .setWakeMode(C.WAKE_MODE_LOCAL) // mantiene la CPU despierta con la pantalla apagada
            .build()
        player.addListener(PlayerEvents())

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.playback_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_rocola) },
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openPlayerIntent())
            .setBitmapLoader(CacheBitmapLoader(ArtworkBitmapLoader(this)))
            .setCallback(SessionCallback())
            .setMediaButtonPreferences(favoriteButtons(isFavorite = false))
            .build()

        effects = AudioEffects(scope)
        effects.attach(player.audioSessionId)

        scope.launch { rocola.userStore.favorites.collect { updateFavoriteButton() } }
        scope.launch { runSleepTimer() }
        restoreQueueIfEmpty()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Si la música está sonando, seguimos aunque cierren la app desde recientes.
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            saveQueue()
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveQueue()
        effects.release()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun openPlayerIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_PLAYER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    // region Favorita desde la notificación

    private fun favoriteButtons(isFavorite: Boolean): ImmutableList<CommandButton> = ImmutableList.of(
        CommandButton.Builder(if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setDisplayName(if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas")
            .setSessionCommand(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
            .build(),
    )

    private fun updateFavoriteButton() {
        val session = mediaSession ?: return
        val songId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val favorite = songId != null && rocola.userStore.isFavorite(songId)
        session.setMediaButtonPreferences(favoriteButtons(favorite))
    }

    // endregion

    // region Cola

    private fun resolve(item: MediaItem): MediaItem {
        if (item.localConfiguration != null) return item
        val uri = item.requestMetadata.mediaUri
            ?: item.mediaId.toLongOrNull()?.let {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it)
            }
        return item.buildUpon().setUri(uri).build()
    }

    private data class Restored(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeatMode: Int,
    )

    private suspend fun loadSavedQueue(): Restored? {
        val saved = queueStore.load() ?: return null
        val repository = rocola.repository
        if (!repository.hasPermission()) return null
        val songs = withTimeoutOrNull(8_000) { repository.awaitSongs() } ?: return null
        val byId = songs.associateBy { it.id }
        val items = ArrayList<MediaItem>(saved.ids.size)
        var index = 0
        var positionMs = 0L
        saved.ids.forEachIndexed { i, id ->
            val song = byId[id] ?: return@forEachIndexed
            if (i == saved.index) {
                index = items.size
                positionMs = saved.positionMs
            }
            items += song.toMediaItem()
        }
        if (items.isEmpty()) return null
        return Restored(items, index, positionMs, saved.shuffle, saved.repeatMode)
    }

    private fun restoreQueueIfEmpty() {
        scope.launch {
            val restored = loadSavedQueue() ?: return@launch
            if (player.mediaItemCount > 0) return@launch
            player.repeatMode = restored.repeatMode
            player.setMediaItems(restored.items, restored.index, restored.positionMs)
            player.shuffleModeEnabled = restored.shuffle
            // Sin prepare(): no aparece la notificación hasta que se toque reproducir.
        }
    }

    private fun saveQueue() {
        if (!::player.isInitialized) return
        val count = player.mediaItemCount
        if (count == 0) {
            queueStore.clear()
            return
        }
        val ids = (0 until count).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        queueStore.save(
            ids = ids,
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    /** Orden aleatorio que empieza por la canción actual, así no se saltea ninguna. */
    private fun shuffleFromCurrent() {
        val count = player.mediaItemCount
        if (!player.shuffleModeEnabled || count < 2) return
        val current = player.currentMediaItemIndex
        val rest = (0 until count).filter { it != current }.shuffled()
        val order = IntArray(count)
        order[0] = current
        rest.forEachIndexed { i, index -> order[i + 1] = index }
        player.setShuffleOrder(DefaultShuffleOrder(order, Random.nextLong()))
    }

    /** "Reproducir a continuación" con aleatorio activado: ubica lo agregado justo después de la actual. */
    private fun placeNextInShuffle(start: Int, count: Int) {
        if (!player.shuffleModeEnabled || count <= 0) return
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        val inserted = (start until start + count).toSet()
        val order = ArrayList<Int>(timeline.windowCount)
        var i = timeline.getFirstWindowIndex(true)
        while (i != C.INDEX_UNSET) {
            if (i !in inserted) order += i
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true)
        }
        val currentPos = order.indexOf(player.currentMediaItemIndex)
        order.addAll(currentPos + 1, (start until start + count).filter { it < timeline.windowCount })
        player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), Random.nextLong()))
    }

    private fun startPositionSaver() {
        positionSaver?.cancel()
        positionSaver = scope.launch {
            while (isActive) {
                delay(10_000)
                queueStore.savePosition(player.currentMediaItemIndex, player.currentPosition)
            }
        }
    }

    // endregion

    // region Temporizador

    private suspend fun runSleepTimer() {
        SleepTimer.state.collectLatest { state ->
            when (state) {
                SleepTimer.State.Off -> player.pauseAtEndOfMediaItems = false
                SleepTimer.State.EndOfTrack -> player.pauseAtEndOfMediaItems = true
                is SleepTimer.State.Countdown -> {
                    player.pauseAtEndOfMediaItems = false
                    val fadeMs = 8_000L
                    val remaining = state.endsAtElapsed - SystemClock.elapsedRealtime()
                    if (remaining > fadeMs) delay(remaining - fadeMs)
                    val startVolume = player.volume
                    try {
                        val steps = 32
                        repeat(steps) { step ->
                            player.volume = startVolume * (1f - (step + 1f) / steps)
                            delay(fadeMs / steps)
                        }
                        player.pause()
                    } finally {
                        player.volume = startVolume
                    }
                    SleepTimer.cancel()
                }
            }
        }
    }

    // endregion

    private inner class PlayerEvents : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateFavoriteButton()
            saveQueue()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) shuffleFromCurrent()
            saveQueue()
        }

        override fun onRepeatModeChanged(repeatMode: Int) = saveQueue()

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) saveQueue()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                consecutiveErrors = 0
                startPositionSaver()
            } else {
                positionSaver?.cancel()
                saveQueue()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady &&
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                SleepTimer.state.value == SleepTimer.State.EndOfTrack
            ) {
                SleepTimer.cancel()
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            effects.attach(audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            // Archivo borrado o dañado: pasamos a la siguiente en vez de frenar todo.
            consecutiveErrors++
            if (consecutiveErrors <= 5 && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(CMD_SHUFFLE_FROM_CURRENT, Bundle.EMPTY))
                .add(SessionCommand(CMD_PLACE_NEXT, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.map { resolve(it) }.toMutableList())

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_TOGGLE_FAVORITE -> {
                    player.currentMediaItem?.mediaId?.toLongOrNull()?.let { rocola.userStore.toggleFavorite(it) }
                }
                CMD_SHUFFLE_FROM_CURRENT -> shuffleFromCurrent()
                CMD_PLACE_NEXT -> {
                    val items = args.getLongArray(ARG_IDS)
                        ?.mapNotNull { id -> rocola.repository.song(id)?.toMediaItem() }
                        .orEmpty()
                    if (items.isNotEmpty()) {
                        val at = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
                        player.addMediaItems(at, items)
                        placeNextInShuffle(at, items.size)
                    }
                }
                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val restored = loadSavedQueue() ?: throw UnsupportedOperationException("No hay una cola guardada")
            player.repeatMode = restored.repeatMode
            player.shuffleModeEnabled = restored.shuffle
            MediaSession.MediaItemsWithStartPosition(restored.items, restored.index, restored.positionMs)
        }
    }
}
