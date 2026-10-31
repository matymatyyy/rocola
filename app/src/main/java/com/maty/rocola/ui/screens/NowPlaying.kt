@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.maty.rocola.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.maty.rocola.art.Artwork
import com.maty.rocola.playback.SleepTimer
import com.maty.rocola.ui.ArtPalette
import com.maty.rocola.ui.NowPlaying
import com.maty.rocola.ui.PlayerUiState
import com.maty.rocola.ui.QueueState
import com.maty.rocola.ui.components.ArtworkImage
import com.maty.rocola.ui.components.DialSeekBar
import com.maty.rocola.ui.components.formatDuration
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

const val ART_REQUEST_PX = 720

class NowPlayingActions(
    val collapse: () -> Unit,
    val togglePlay: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val skipNextItem: () -> Unit,
    val skipPreviousItem: () -> Unit,
    val seekTo: (Long) -> Unit,
    val toggleShuffle: () -> Unit,
    val cycleRepeat: () -> Unit,
    val toggleFavorite: () -> Unit,
    val openQueue: () -> Unit,
    val openSleepTimer: () -> Unit,
    val openSpeed: () -> Unit,
    val openEqualizer: () -> Unit,
    val addToPlaylist: () -> Unit,
    val goToAlbum: () -> Unit,
    val goToArtist: () -> Unit,
    val share: () -> Unit,
)

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    queue: QueueState,
    palette: ArtPalette,
    position: StateFlow<Long>,
    isFavorite: Boolean,
    sleepTimer: SleepTimer.State,
    actions: NowPlayingActions,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
) {
    val current = state.current ?: return
    val background by animateColorAsState(palette.background, tween(650), label = "bg")
    val accent by animateColorAsState(palette.accent, tween(650), label = "accent")
    val onAccent by animateColorAsState(palette.onAccent, tween(650), label = "onAccent")
    var dragY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { 140.dp.toPx() }

    with(sharedScope) {
        Box(
            Modifier
                .fillMaxSize()
                .sharedBounds(rememberSharedContentState("player-surface"), visibilityScope)
                .graphicsLayer { translationY = dragY }
                .background(background)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY > dismissPx) actions.collapse()
                            else scope.launch { animate(dragY, 0f) { v, _ -> dragY = v } }
                        },
                        onDragCancel = { scope.launch { animate(dragY, 0f) { v, _ -> dragY = v } } },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragY = (dragY + amount).coerceAtLeast(0f)
                        },
                    )
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                TopRow(state, current, actions)
                Spacer(Modifier.weight(0.5f))
                ArtStage(
                    current = current,
                    hasNext = state.hasNext,
                    hasPrevious = state.hasPrevious,
                    onSwipeNext = actions.skipNextItem,
                    onSwipePrevious = actions.skipPreviousItem,
                    artModifier = Modifier.sharedElement(rememberSharedContentState("player-art"), visibilityScope),
                )
                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            current.title,
                            style = RocolaType.display,
                            color = Rocola.Marfil,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            current.artist,
                            style = RocolaType.body.copy(fontSize = 17.sp),
                            color = Rocola.Marfil.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = actions.goToArtist),
                        )
                    }
                    IconButton(onClick = actions.toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                            tint = if (isFavorite) accent else Rocola.Marfil.copy(alpha = 0.85f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                PositionDial(position, state.durationMs, accent, actions.seekTo)
                Spacer(Modifier.weight(0.35f))
                Transport(state, accent, onAccent, actions)
                Spacer(Modifier.height(22.dp))
                BottomRow(queue, state, sleepTimer, actions)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopRow(state: PlayerUiState, current: NowPlaying, actions: NowPlayingActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.collapse) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimizar", tint = Rocola.Marfil, modifier = Modifier.size(30.dp))
        }
        Text(
            state.contextLabel ?: current.album,
            style = RocolaType.label,
            color = Rocola.Marfil.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones", tint = Rocola.Marfil)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(
                    "Temporizador para dormir" to actions.openSleepTimer,
                    "Velocidad" to actions.openSpeed,
                    "Ecualizador" to actions.openEqualizer,
                    "Agregar a una lista" to actions.addToPlaylist,
                    "Ir al álbum" to actions.goToAlbum,
                    "Ir al artista" to actions.goToArtist,
                    "Compartir" to actions.share,
                ).forEach { (label, action) ->
                    DropdownMenuItem(
                        text = { Text(label, style = RocolaType.itemTitle) },
                        onClick = {
                            menuOpen = false
                            action()
                        },
                    )
                }
            }
        }
    }
}

/** La tapa: se arrastra a los costados para cambiar de tema y "cae" con un rebote al cambiar. */
@Composable
private fun ArtStage(
    current: NowPlaying,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    artModifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(current.mediaId) {
        offset.snapTo(0f)
        pop.snapTo(0.93f)
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 320f))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Box(
            artModifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value
                    scaleX = pop.value
                    scaleY = pop.value
                    rotationZ = offset.value / widthPx * 5f
                    alpha = 1f - (abs(offset.value) / widthPx).coerceIn(0f, 0.7f)
                }
                .shadow(28.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(current.mediaId, hasNext, hasPrevious) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val threshold = widthPx * 0.22f
                                when {
                                    offset.value < -threshold && hasNext -> {
                                        offset.animateTo(-widthPx, tween(170))
                                        onSwipeNext()
                                        delay(700)
                                        if (offset.value != 0f) offset.animateTo(0f)
                                    }
                                    offset.value > threshold && hasPrevious -> {
                                        offset.animateTo(widthPx, tween(170))
                                        onSwipePrevious()
                                        delay(700)
                                        if (offset.value != 0f) offset.animateTo(0f)
                                    }
                                    else -> offset.animateTo(0f, spring(dampingRatio = 0.7f))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            scope.launch { offset.snapTo(offset.value + amount) }
                        },
                    )
                },
        ) {
            ArtworkImage(
                artwork = Artwork(current.albumId, current.songUri),
                seed = current.album.ifEmpty { current.title },
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                letterSize = 120.sp,
                requestSizePx = ART_REQUEST_PX,
            )
        }
    }
}

@Composable
private fun PositionDial(position: StateFlow<Long>, durationMs: Long, accent: Color, onSeek: (Long) -> Unit) {
    val pos by position.collectAsStateWithLifecycle()
    DialSeekBar(positionMs = pos, durationMs = durationMs, accent = accent, onSeek = onSeek, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Transport(state: PlayerUiState, accent: Color, onAccent: Color, actions: NowPlayingActions) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleButton(
            icon = Icons.Rounded.Shuffle,
            description = if (state.shuffle) "Desactivar aleatorio" else "Activar aleatorio",
            active = state.shuffle,
            accent = accent,
            onClick = actions.toggleShuffle,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = actions.previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Rocola.Marfil, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.width(10.dp))
        PlayKey(state.playWhenReady, accent, onAccent, actions.togglePlay)
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = actions.next, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Siguiente",
                tint = if (state.hasNext) Rocola.Marfil else Rocola.Marfil.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        ToggleButton(
            icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            description = when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> "Repetir la cola (tocá para repetir esta canción)"
                Player.REPEAT_MODE_ONE -> "Repetir esta canción (tocá para desactivar)"
                else -> "Activar repetición"
            },
            active = state.repeatMode != Player.REPEAT_MODE_OFF,
            accent = accent,
            onClick = actions.cycleRepeat,
        )
    }
}

/** Tecla de reproducir: rectangular como los botones de selección de una rocola. */
@Composable
private fun PlayKey(playing: Boolean, accent: Color, onAccent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(stiffness = 600f), label = "press")
    Box(
        Modifier
            .size(width = 88.dp, height = 70.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(accent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = if (playing) "Pausar" else "Reproducir",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = { (fadeIn(tween(140)) + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut(tween(90)) + scaleOut(targetScale = 0.7f)) },
            label = "playIcon",
        ) { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = onAccent,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun ToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description, tint = if (active) accent else Rocola.Marfil.copy(alpha = 0.7f))
        }
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (active) accent else Color.Transparent),
        )
    }
}

@Composable
private fun BottomRow(queue: QueueState, state: PlayerUiState, sleepTimer: SleepTimer.State, actions: NowPlayingActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = actions.openQueue)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = Rocola.Marfil.copy(alpha = 0.85f))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("A continuación", style = RocolaType.small, color = Rocola.Marfil.copy(alpha = 0.6f))
                Text(
                    queue.next?.title ?: if (state.repeatMode == Player.REPEAT_MODE_ALL) "Vuelve a empezar la cola" else "Nada más en la cola",
                    style = RocolaType.label,
                    color = Rocola.Marfil,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SleepChip(sleepTimer, actions.openSleepTimer)
        if (state.speed != 1f) {
            Text(
                "${formatSpeed(state.speed)}×",
                style = RocolaType.numeral,
                color = Rocola.Marfil,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = actions.openSpeed)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SleepChip(timer: SleepTimer.State, onClick: () -> Unit) {
    if (timer == SleepTimer.State.Off) return
    val now by produceState(android.os.SystemClock.elapsedRealtime(), timer) {
        while (true) {
            value = android.os.SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val label = when (timer) {
        is SleepTimer.State.Countdown -> formatDuration((timer.endsAtElapsed - now).coerceAtLeast(0L))
        else -> "Fin del tema"
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Bedtime, contentDescription = "Temporizador activo", tint = Rocola.Marfil, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = RocolaType.numeral, color = Rocola.Marfil)
    }
}

fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.').replace('.', ',')

/** Barra compacta que queda abajo mientras se navega la biblioteca. */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    palette: ArtPalette,
    position: StateFlow<Long>,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val current = state.current ?: return
    val surface by animateColorAsState(palette.surface, tween(650), label = "miniSurface")
    val accent by animateColorAsState(palette.accent, tween(650), label = "miniAccent")
    var expandedByDrag by remember { mutableStateOf(false) }
    with(sharedScope) {
        Box(
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .sharedBounds(rememberSharedContentState("player-surface"), visibilityScope)
                .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(18.dp))
                .background(surface)
                .clickable(onClickLabel = "Abrir el reproductor", onClick = onExpand)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { expandedByDrag = false },
                        onVerticalDrag = { change, amount ->
                            if (amount < -8f && !expandedByDrag) {
                                expandedByDrag = true
                                change.consume()
                                onExpand()
                            }
                        },
                    )
                },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(start = 9.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .sharedElement(rememberSharedContentState("player-art"), visibilityScope)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    ArtworkImage(
                        artwork = Artwork(current.albumId, current.songUri),
                        seed = current.album.ifEmpty { current.title },
                        modifier = Modifier.fillMaxSize(),
                        letterSize = 20.sp,
                        requestSizePx = ART_REQUEST_PX,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(current.title, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist, style = RocolaType.itemSub, color = Rocola.Marfil.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (state.playWhenReady) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.playWhenReady) "Pausar" else "Reproducir",
                        tint = Rocola.Marfil,
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        tint = if (state.hasNext) Rocola.Marfil else Rocola.Marfil.copy(alpha = 0.35f),
                    )
                }
            }
            MiniProgress(position, state.durationMs, accent, Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun MiniProgress(position: StateFlow<Long>, durationMs: Long, accent: Color, modifier: Modifier) {
    val pos by position.collectAsStateWithLifecycle()
    val fraction = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .fillMaxWidth(fraction)
            .height(3.dp)
            .background(accent),
    )
}
