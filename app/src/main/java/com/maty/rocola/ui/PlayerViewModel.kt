package com.maty.rocola.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.maty.rocola.data.LibraryIndex
import com.maty.rocola.data.Song
import com.maty.rocola.playback.ARG_IDS
import com.maty.rocola.playback.CMD_PLACE_NEXT
import com.maty.rocola.playback.EXTRA_ALBUM_ID
import com.maty.rocola.playback.PlaybackService
import com.maty.rocola.playback.toMediaItem
import com.maty.rocola.rocola
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Immutable
data class NowPlaying(
    val songId: Long,
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val songUri: Uri?,
)

@Immutable
data class PlayerUiState(
    val connected: Boolean = false,
    val current: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val contextLabel: String? = null,
)

@Immutable
data class QueueItem(
    val key: String,
    val windowIndex: Int,
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val songUri: Uri?,
    val durationMs: Long,
)

@Immutable
data class QueueState(
    val items: List<QueueItem> = emptyList(),
    val currentPos: Int = -1,
    val shuffle: Boolean = false,
) {
    val next: QueueItem? get() = items.getOrNull(currentPos + 1)
}

/** Conecta la interfaz con el servicio de reproducción a través de un MediaController. */
class PlayerViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = app.rocola.repository
    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
    private var controller: MediaController? = null
    private val connected = CompletableDeferred<MediaController>()
    private var contextLabel: String? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val _palette = MutableStateFlow(ArtPalette.Default)
    val palette: StateFlow<ArtPalette> = _palette.asStateFlow()

    private val visible = MutableStateFlow(true)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                )
            ) {
                syncQueue(player)
            }
            syncState(player)
            _position.value = player.currentPosition
        }
    }

    init {
        viewModelScope.launch {
            val c = runCatching { controllerFuture.await() }.getOrNull() ?: return@launch
            controller = c
            c.addListener(listener)
            syncQueue(c)
            syncState(c)
            _position.value = c.currentPosition
            connected.complete(c)
        }
        // Avanza la posición solo mientras suena y la pantalla de la app está visible.
        viewModelScope.launch {
            combine(visible, _state.map { it.isPlaying }.distinctUntilChanged()) { v, playing -> v && playing }
                .distinctUntilChanged()
                .collectLatest { active ->
                    while (active) {
                        controller?.let { _position.value = it.currentPosition }
                        delay(200)
                    }
                }
        }
        // Cuando termina de cargar la biblioteca, completamos títulos y tapas de la cola.
        viewModelScope.launch {
            repository.songs.filterNotNull().collect {
                controller?.let { c ->
                    syncQueue(c)
                    syncState(c)
                }
            }
        }
        // Colores del reproductor según la tapa actual.
        viewModelScope.launch {
            _state.map { s -> s.current?.let { Triple(it.albumId, it.songUri, it.mediaId) } }
                .distinctUntilChanged { a, b -> a?.first == b?.first && a?.second == b?.second }
                .collectLatest { key ->
                    _palette.value = if (key == null) ArtPalette.Default else withContext(Dispatchers.Default) {
                        runCatching { ArtPalette.from(app, key.first, key.second) }.getOrDefault(ArtPalette.Default)
                    }
                }
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }

    fun setVisible(isVisible: Boolean) {
        visible.value = isVisible
        if (isVisible) controller?.let { _position.value = it.currentPosition }
    }

    // region Sincronización

    private fun nowPlayingOf(item: MediaItem): NowPlaying {
        val songId = item.mediaId.toLongOrNull() ?: -1L
        val song = repository.song(songId)
        val md = item.mediaMetadata
        return NowPlaying(
            songId = songId,
            mediaId = item.mediaId,
            title = song?.title ?: md.title?.toString() ?: "Sin título",
            artist = song?.artist ?: md.artist?.toString().orEmpty(),
            album = song?.album ?: md.albumTitle?.toString().orEmpty(),
            albumId = song?.albumId ?: md.extras?.getLong(EXTRA_ALBUM_ID, -1L) ?: -1L,
            songUri = song?.uri ?: item.requestMetadata.mediaUri,
        )
    }

    private fun syncState(p: Player) {
        val now = p.currentMediaItem?.let { nowPlayingOf(it) }
        val knownDuration = now?.songId?.let { repository.song(it)?.durationMs } ?: 0L
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: knownDuration
        _state.value = PlayerUiState(
            connected = true,
            current = now,
            isPlaying = p.isPlaying,
            playWhenReady = p.playWhenReady,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            durationMs = duration,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            speed = p.playbackParameters.speed,
            hasNext = p.hasNextMediaItem(),
            hasPrevious = p.hasPreviousMediaItem(),
            contextLabel = contextLabel,
        )
    }

    private fun syncQueue(p: Player) {
        val timeline = p.currentTimeline
        if (timeline.isEmpty) {
            _queue.value = QueueState()
            return
        }
        val shuffle = p.shuffleModeEnabled
        val order = ArrayList<Int>(timeline.windowCount)
        var i = timeline.getFirstWindowIndex(shuffle)
        while (i != C.INDEX_UNSET && order.size < timeline.windowCount) {
            order += i
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, shuffle)
        }
        val seen = HashMap<String, Int>()
        val items = order.map { window ->
            val item = p.getMediaItemAt(window)
            val now = nowPlayingOf(item)
            val occurrence = (seen[item.mediaId] ?: 0) + 1
            seen[item.mediaId] = occurrence
            QueueItem(
                key = "${item.mediaId}#$occurrence",
                windowIndex = window,
                songId = now.songId,
                title = now.title,
                artist = now.artist,
                album = now.album,
                albumId = now.albumId,
                songUri = now.songUri,
                durationMs = repository.song(now.songId)?.durationMs ?: 0L,
            )
        }
        _queue.value = QueueState(items, order.indexOf(p.currentMediaItemIndex), shuffle)
    }

    // endregion

    // region Acciones

    /**
     * Reproduce una lista. [shuffle]: true = mezclar, false = en orden, null = respetar el modo actual.
     */
    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean? = null, label: String? = null) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        contextLabel = label
        val wantShuffle = shuffle ?: c.shuffleModeEnabled
        val start = if (shuffle == true) songs.indices.random() else startIndex.coerceIn(0, songs.lastIndex)
        // Se carga la lista sin aleatorio y recién después se activa: los comandos del reproductor
        // llegan en orden, así el servicio arma el orden mezclado empezando por la canción elegida.
        if (c.shuffleModeEnabled) c.shuffleModeEnabled = false
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        if (wantShuffle) c.shuffleModeEnabled = true
        c.prepare()
        c.play()
        syncState(c)
    }

    /** Para el atajo "Mezclar todo": espera a que estén listos el servicio y la biblioteca. */
    fun shuffleAllWhenReady() {
        viewModelScope.launch {
            if (!repository.hasPermission()) return@launch
            withTimeoutOrNull(10_000) { connected.await() } ?: return@launch
            // Recién encendido, Android puede tardar unos segundos en terminar de indexar la música.
            repository.start()
            val songs = withTimeoutOrNull(20_000) {
                repository.songs.filterNotNull().first { it.isNotEmpty() }
            } ?: return@launch
            val hideShort = app.rocola.userStore.hideShort.value
            val pool = songs.filter { !hideShort || it.durationMs >= LibraryIndex.SHORT_AUDIO_MS }.ifEmpty { songs }
            play(pool, shuffle = true, label = "Toda tu música")
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
            return
        }
        when (c.playbackState) {
            Player.STATE_IDLE -> c.prepare()
            Player.STATE_ENDED -> c.seekToDefaultPosition(c.currentTimeline.getFirstWindowIndex(c.shuffleModeEnabled).coerceAtLeast(0))
        }
        c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun skipToNextItem() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun skipToPreviousItem() {
        controller?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        c.seekTo(positionMs)
        _position.value = positionMs
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun playQueueItem(windowIndex: Int) {
        val c = controller ?: return
        c.seekToDefaultPosition(windowIndex)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun removeQueueItem(windowIndex: Int) {
        controller?.let { if (windowIndex in 0 until it.mediaItemCount) it.removeMediaItem(windowIndex) }
    }

    fun moveQueueItem(from: Int, to: Int) {
        controller?.let {
            if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from, to)
        }
    }

    fun clearUpcoming() {
        val c = controller ?: return
        val q = _queue.value
        val upcoming = q.items.drop(q.currentPos + 1).map { it.windowIndex }.sortedDescending()
        upcoming.forEach { c.removeMediaItem(it) }
    }

    fun playNext(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) {
            play(songs, 0, shuffle = false)
            return
        }
        if (c.shuffleModeEnabled) {
            // Con aleatorio, el servicio inserta y ubica las canciones justo después de la actual.
            c.sendCustomCommand(
                SessionCommand(CMD_PLACE_NEXT, Bundle.EMPTY),
                bundleOf(ARG_IDS to songs.map { it.id }.toLongArray()),
            )
        } else {
            c.addMediaItems(c.currentMediaItemIndex + 1, songs.map { it.toMediaItem() })
        }
    }

    /** Quita de la cola las canciones borradas (si suena una, pasa a la siguiente). */
    fun removeSongsFromQueue(songIds: Set<Long>) {
        val c = controller ?: return
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId.toLongOrNull() in songIds) c.removeMediaItem(i)
        }
    }

    fun addToQueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) {
            play(songs, 0, shuffle = false)
            return
        }
        c.addMediaItems(songs.map { it.toMediaItem() })
    }

    // endregion
}
