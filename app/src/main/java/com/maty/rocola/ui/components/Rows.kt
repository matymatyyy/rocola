package com.maty.rocola.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.data.Song
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType

sealed interface SongLeading {
    data object Art : SongLeading
    data class Number(val value: Int) : SongLeading
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    leading: SongLeading = SongLeading.Art,
    subtitle: String? = song.artist,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val rowBackground by animateColorAsState(
        if (selected) Rocola.VinoAlto else Color.Transparent,
        tween(150),
        label = "rowSelection",
    )
    Row(
        modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = if (onLongClick != null && !selectionMode) "Seleccionar" else null,
            )
            .semantics {
                if (selectionMode) {
                    this.selected = selected
                    stateDescription = if (selected) "Seleccionada" else "Sin seleccionar"
                }
            }
            .heightIn(min = 68.dp)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (leading) {
            SongLeading.Art -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                ArtworkImage(song.artwork, song.album, Modifier.size(48.dp), RoundedCornerShape(7.dp), letterSize = 20.sp)
                if (selectionMode) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.Black.copy(alpha = if (selected) 0.55f else 0.35f)),
                    )
                    SelectionMark(selected)
                } else if (isCurrent) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                    )
                    PlayingBars(isPlaying)
                }
            }
            is SongLeading.Number -> Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    selectionMode -> SelectionMark(selected)
                    isCurrent -> PlayingBars(isPlaying)
                    else -> Text(leading.value.toString(), style = RocolaType.numeral, color = Rocola.Ceniza)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                song.title,
                style = RocolaType.itemTitle,
                color = if (isCurrent) Rocola.Ambar else Rocola.Marfil,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            formatDuration(song.durationMs),
            style = RocolaType.numeral,
            color = Rocola.Ceniza,
            modifier = Modifier.padding(start = 12.dp),
        )
        if (selectionMode) {
            Spacer(Modifier.width(48.dp))
        } else {
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones de ${song.title}", tint = Rocola.Arena)
            }
        }
    }
}

/** Círculo de selección: lleno con tilde si está elegida, solo el borde si no. */
@Composable
fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Rocola.Marfil),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Rocola.Vino, modifier = Modifier.size(17.dp))
        }
    } else {
        Box(
            modifier
                .size(24.dp)
                .border(2.dp, Rocola.Marfil.copy(alpha = 0.85f), CircleShape),
        )
    }
}

/** true si el usuario desactivó las animaciones del sistema (accesibilidad). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Tres barritas que marcan lo que está sonando. Quietas en pausa o con las animaciones desactivadas. */
@Composable
fun PlayingBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = Rocola.Ambar) {
    if (playing && !LocalReduceMotion.current) {
        val transition = rememberInfiniteTransition(label = "bars")
        val a = transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
        val b = transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(430, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(180)), label = "b")
        val c = transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(610, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(320)), label = "c")
        Bars(listOf(a.value, b.value, c.value), color, modifier)
    } else {
        Bars(listOf(0.45f, 0.8f, 0.6f), color, modifier)
    }
}

@Composable
private fun Bars(levels: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val w = size.width / 5f
        levels.forEachIndexed { i, level ->
            val h = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * 2 * w, size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2, w / 2),
            )
        }
    }
}

@Composable
fun DetailTopBar(onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Rocola.Marfil)
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** Botones "Reproducir" (lleno) y "Mezclar" (contorno) de las pantallas de detalle. */
@Composable
fun PlayShuffleRow(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("Reproducir", Icons.Rounded.PlayArrow, filled = true, onClick = onPlay, enabled = enabled, modifier = Modifier.weight(1f))
        PillButton("Mezclar", Icons.Rounded.Shuffle, filled = false, onClick = onShuffle, enabled = enabled, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector?,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val base = modifier
        .height(48.dp)
        .clip(shape)
        .then(
            if (filled) Modifier.background(if (enabled) Rocola.Marfil else Rocola.VinoClaro)
            else Modifier.border(1.5.dp, Rocola.VinoClaro, shape),
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 18.dp)
    val content = if (filled) Rocola.Vino else Rocola.Marfil
    Row(base, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = RocolaType.label.copy(fontSize = 15.sp), color = content, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = RocolaType.section, color = Rocola.Marfil, modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp))
}

/** Fila genérica con ícono en un cuadrado, título, subtítulo y algo a la derecha. */
@Composable
fun IconTileRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Rocola.Arena,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 68.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Rocola.VinoAlto),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun CircleArtRow(
    title: String,
    subtitle: String,
    artworkContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape)) { artworkContent() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun EmptyMessage(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier.padding(horizontal = 28.dp, vertical = 40.dp)) {
        Text(title, style = RocolaType.section, color = Rocola.Marfil)
        Spacer(Modifier.height(8.dp))
        Text(body, style = RocolaType.body, color = Rocola.Arena)
        Spacer(Modifier.height(20.dp))
        action()
    }
}
