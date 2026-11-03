@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.maty.rocola.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import com.maty.rocola.ui.screens.DeleteHost
import com.maty.rocola.ui.screens.SelectionActionBar
import com.maty.rocola.ui.screens.SelectionTopBar
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maty.rocola.playback.SleepTimer
import com.maty.rocola.ui.components.LocalReduceMotion
import com.maty.rocola.ui.components.rememberReduceMotion
import com.maty.rocola.ui.screens.AlbumScreen
import com.maty.rocola.ui.screens.ArtistScreen
import com.maty.rocola.ui.screens.EqualizerScreen
import com.maty.rocola.ui.screens.FavoritesScreen
import com.maty.rocola.ui.screens.FolderScreen
import com.maty.rocola.ui.screens.LibraryScreen
import com.maty.rocola.ui.screens.MiniPlayer
import com.maty.rocola.ui.screens.NowPlayingActions
import com.maty.rocola.ui.screens.NowPlayingScreen
import com.maty.rocola.ui.screens.PermissionScreen
import com.maty.rocola.ui.screens.PlaylistNameDialog
import com.maty.rocola.ui.screens.PlaylistPickerSheet
import com.maty.rocola.ui.screens.PlaylistScreen
import com.maty.rocola.ui.screens.QueueSheet
import com.maty.rocola.ui.screens.SearchScreen
import com.maty.rocola.ui.screens.SettingsScreen
import com.maty.rocola.ui.screens.SleepTimerSheet
import com.maty.rocola.ui.screens.SongActionsSheet
import com.maty.rocola.ui.screens.SpeedSheet
import com.maty.rocola.ui.theme.Rocola

@Composable
fun RocolaRoot(libraryVm: LibraryViewModel, playerVm: PlayerViewModel, nav: NavViewModel) {
    val context = LocalContext.current
    val app = remember(libraryVm, playerVm, nav) { AppController(libraryVm, playerVm, nav, context.applicationContext) }
    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        RocolaContent(app, libraryVm, playerVm, nav)
    }
}

@Composable
private fun RocolaContent(app: AppController, libraryVm: LibraryViewModel, playerVm: PlayerViewModel, nav: NavViewModel) {

    LifecycleStartEffect(Unit) {
        playerVm.setVisible(true)
        libraryVm.recheckPermission()
        onStopOrDispose { playerVm.setVisible(false) }
    }

    val permission by libraryVm.permissionGranted.collectAsStateWithLifecycle()
    if (!permission) {
        PermissionScreen(onResult = libraryVm::recheckPermission)
        return
    }

    val library by libraryVm.library.collectAsStateWithLifecycle()
    val sort by libraryVm.sort.collectAsStateWithLifecycle()
    val favorites by libraryVm.favorites.collectAsStateWithLifecycle()
    val playlists by libraryVm.playlists.collectAsStateWithLifecycle()
    val player by playerVm.state.collectAsStateWithLifecycle()
    val queue by playerVm.queue.collectAsStateWithLifecycle()
    val palette by playerVm.palette.collectAsStateWithLifecycle()
    val sleepTimer by SleepTimer.state.collectAsStateWithLifecycle()

    val hasTrack = player.current != null
    val expanded = nav.playerExpanded && hasTrack
    val selecting = app.selection.active
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = navBar + if (hasTrack || selecting) 92.dp else 16.dp

    BackHandler(enabled = expanded) { nav.playerExpanded = false }
    BackHandler(enabled = !expanded && nav.stack.size > 1) { nav.pop() }
    // Registrado último para que el "atrás" primero cancele la selección.
    BackHandler(enabled = selecting && !expanded) { app.selection.clear() }

    // Cambiar de pantalla o abrir el reproductor cancela la selección.
    LaunchedEffect(nav.top, expanded) { app.selection.clear() }

    SharedTransitionLayout(Modifier.fillMaxSize().background(Rocola.Vino)) {
        val holder = rememberSaveableStateHolder()
        AnimatedContent(
            targetState = nav.top,
            transitionSpec = {
                val forward = targetState.depth >= initialState.depth
                val direction = if (forward) 1 else -1
                (slideInHorizontally(tween(280)) { w -> direction * w / 10 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(220)) { w -> -direction * w / 10 } + fadeOut(tween(160)))
            },
            label = "screens",
            modifier = Modifier.fillMaxSize(),
        ) { entry ->
            holder.SaveableStateProvider(entry.screen.toString()) {
                val lib = library
                when (val screen = entry.screen) {
                    Screen.Library -> LibraryScreen(app, lib, player, sort, favorites, playlists, bottomPadding)
                    Screen.Search -> SearchScreen(app, player, bottomPadding)
                    Screen.Settings -> SettingsScreen(app, bottomPadding)
                    Screen.Equalizer -> EqualizerScreen(app, bottomPadding)
                    else -> if (lib == null) Box(Modifier.fillMaxSize()) else when (screen) {
                        is Screen.AlbumDetail -> AlbumScreen(app, lib, screen.albumId, player, bottomPadding)
                        is Screen.ArtistDetail -> ArtistScreen(app, lib, screen.name, player, bottomPadding)
                        is Screen.FolderDetail -> FolderScreen(app, lib, screen.path, player, bottomPadding)
                        is Screen.PlaylistDetail -> PlaylistScreen(app, lib, playlists, screen.playlistId, player, bottomPadding)
                        Screen.Favorites -> FavoritesScreen(app, lib, favorites, player, bottomPadding)
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(260)) },
            label = "player",
            modifier = Modifier.fillMaxSize(),
        ) { isExpanded ->
            if (isExpanded) {
                val current = player.current
                NowPlayingScreen(
                    state = player,
                    queue = queue,
                    palette = palette,
                    position = playerVm.position,
                    isFavorite = current != null && current.songId in favorites,
                    sleepTimer = sleepTimer,
                    actions = remember(app) { nowPlayingActions(app) },
                    sharedScope = this@SharedTransitionLayout,
                    visibilityScope = this@AnimatedContent,
                )
            } else if (hasTrack || selecting) {
                Box(Modifier.fillMaxSize()) {
                    // Funde la lista hacia el fondo detrás de la barra, así no se asoma por debajo.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(navBar + 104.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Rocola.Vino.copy(alpha = 0f),
                                    0.5f to Rocola.Vino.copy(alpha = 0.88f),
                                    1f to Rocola.Vino,
                                ),
                            ),
                    )
                    if (selecting) {
                        SelectionActionBar(
                            onPlay = app::playSelection,
                            onQueue = app::queueSelection,
                            onAddToPlaylist = app::addSelectionToPlaylist,
                            onDelete = app::deleteSelection,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    } else {
                        MiniPlayer(
                            state = player,
                            palette = palette,
                            position = playerVm.position,
                            onExpand = { nav.playerExpanded = true },
                            onTogglePlay = playerVm::togglePlayPause,
                            onNext = playerVm::next,
                            sharedScope = this@SharedTransitionLayout,
                            visibilityScope = this@AnimatedContent,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = selecting && !expanded,
                enter = slideInVertically(tween(200)) { -it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(160)) { -it } + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                SelectionTopBar(
                    count = app.selection.count,
                    total = app.selection.source.size,
                    onClose = app.selection::clear,
                    onSelectAll = app.selection::selectAll,
                )
            }
        }
    }

    Overlays(app, favorites, playlists, player, queue, sleepTimer)
}

private fun nowPlayingActions(app: AppController): NowPlayingActions {
    val player = app.player
    fun currentSong() = player.state.value.current?.songId?.let { app.library.library.value?.byId?.get(it) }
    return NowPlayingActions(
        collapse = { app.nav.playerExpanded = false },
        togglePlay = player::togglePlayPause,
        next = player::next,
        previous = player::previous,
        skipNextItem = player::skipToNextItem,
        skipPreviousItem = player::skipToPreviousItem,
        seekTo = player::seekTo,
        toggleShuffle = player::toggleShuffle,
        cycleRepeat = player::cycleRepeat,
        toggleFavorite = { player.state.value.current?.songId?.let { app.library.toggleFavorite(it) } },
        openQueue = { app.showQueue = true },
        openSleepTimer = { app.showSleepTimer = true },
        openSpeed = { app.showSpeed = true },
        openEqualizer = { app.openEqualizer() },
        addToPlaylist = { currentSong()?.let { app.addToPlaylist = listOf(it.id) } },
        goToAlbum = { player.state.value.current?.albumId?.let { app.openAlbum(it) } },
        goToArtist = { player.state.value.current?.artist?.let { app.openArtist(it) } },
        share = { currentSong()?.let { app.share(it) } },
    )
}

@Composable
private fun Overlays(
    app: AppController,
    favorites: Set<Long>,
    playlists: List<com.maty.rocola.data.Playlist>,
    player: PlayerUiState,
    queue: QueueState,
    sleepTimer: SleepTimer.State,
) {
    app.songMenu?.let { request ->
        val song = request.song
        SongActionsSheet(
            song = song,
            isFavorite = song.id in favorites,
            inPlaylist = request.playlistId != null,
            onDismiss = { app.songMenu = null },
            onPlayNext = { app.playNext(song) },
            onAddToQueue = { app.addToQueue(song) },
            onAddToPlaylist = { app.addToPlaylist = listOf(song.id) },
            onGoToAlbum = { app.openAlbum(song.albumId) },
            onGoToArtist = { app.openArtist(song.artist) },
            onToggleFavorite = { app.library.toggleFavorite(song.id) },
            onRemoveFromPlaylist = { request.playlistId?.let { app.library.removeFromPlaylist(it, song.id) } },
            onShare = { app.share(song) },
            onDelete = { app.deleteSong(song) },
        )
    }

    app.addToPlaylist?.let { songIds ->
        PlaylistPickerSheet(
            playlists = playlists,
            onDismiss = { app.addToPlaylist = null },
            onPick = { playlist -> app.addSongsToPlaylist(playlist.id, songIds) },
            onCreate = { app.createPlaylistWith = songIds },
        )
    }

    app.createPlaylistWith?.let { songIds ->
        PlaylistNameDialog(
            title = "Nueva lista",
            initial = "",
            confirmLabel = "Crear lista",
            onDismiss = { app.createPlaylistWith = null },
            onConfirm = { name ->
                app.createPlaylist(name, songIds)
                app.createPlaylistWith = null
            },
        )
    }

    app.renamePlaylistId?.let { id ->
        val playlist = playlists.firstOrNull { it.id == id }
        if (playlist != null) {
            PlaylistNameDialog(
                title = "Cambiar nombre",
                initial = playlist.name,
                confirmLabel = "Guardar",
                onDismiss = { app.renamePlaylistId = null },
                onConfirm = { name ->
                    app.library.renamePlaylist(id, name)
                    app.renamePlaylistId = null
                },
            )
        }
    }

    if (app.showQueue) {
        QueueSheet(
            queue = queue,
            isPlaying = player.isPlaying,
            onDismiss = { app.showQueue = false },
            onPlay = { app.player.playQueueItem(it.windowIndex) },
            onRemove = { app.player.removeQueueItem(it.windowIndex) },
            // Solo se puede reordenar sin aleatorio, y ahí la posición en la lista es el índice en la cola.
            onMove = { from, to -> app.player.moveQueueItem(from, to) },
            onClearUpcoming = app.player::clearUpcoming,
        )
    }

    if (app.showSleepTimer) {
        SleepTimerSheet(state = sleepTimer, onDismiss = { app.showSleepTimer = false })
    }

    if (app.showSpeed) {
        SpeedSheet(current = player.speed, onPick = app.player::setSpeed, onDismiss = { app.showSpeed = false })
    }

    DeleteHost(app)

}
