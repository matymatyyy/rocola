package com.maty.rocola.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Barra para recorrer listas largas: aparece al desplazarse y se puede arrastrar.
 * Mientras se arrastra muestra la inicial de la zona en la que estás.
 */
@Composable
fun FastScroller(
    listState: LazyListState,
    itemCount: Int,
    firstItemOffset: Int,
    labelFor: (Int) -> String?,
    modifier: Modifier = Modifier,
) {
    if (itemCount < 40) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { 48.dp.toPx() }
    var trackHeight by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(0) }
    var recentlyScrolled by remember { mutableStateOf(false) }
    val currentLabelFor by rememberUpdatedState(labelFor)

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) recentlyScrolled = true
        else {
            delay(1400)
            recentlyScrolled = false
        }
    }
    val visible = dragging || recentlyScrolled
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 120 else 600), label = "scroller")
    val progress by remember(itemCount) {
        derivedStateOf {
            val index = (listState.firstVisibleItemIndex - firstItemOffset).coerceAtLeast(0)
            (index.toFloat() / (itemCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    val shownProgress = if (dragging) dragIndex.toFloat() / (itemCount - 1).coerceAtLeast(1) else progress

    fun jumpTo(y: Float) {
        val fraction = ((y - thumbHeightPx / 2) / (trackHeight - thumbHeightPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val index = (fraction * (itemCount - 1)).roundToInt()
        dragIndex = index
        scope.launch { listState.scrollToItem(index + firstItemOffset) }
    }

    Box(
        modifier
            .fillMaxHeight()
            .width(if (visible || alpha > 0.01f) 40.dp else 0.dp)
            .onSizeChanged { trackHeight = it.height }
            .graphicsLayer { this.alpha = alpha }
            .pointerInput(itemCount, firstItemOffset) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        jumpTo(offset.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        jumpTo(change.position.y)
                    },
                )
            },
    ) {
        val thumbY = (shownProgress * (trackHeight - thumbHeightPx).coerceAtLeast(0f)).roundToInt()
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbY) }
                .padding(end = 6.dp)
                .size(width = 5.dp, height = 48.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (dragging) Rocola.Marfil else Rocola.Marfil.copy(alpha = 0.55f)),
        )
        val label = if (dragging) currentLabelFor(dragIndex) else null
        if (label != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-with(density) { 26.dp.roundToPx() }, thumbY - with(density) { 4.dp.roundToPx() }) }
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Rocola.VinoClaro),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = RocolaType.headline.copy(fontSize = 26.sp, lineHeight = 26.sp), color = Rocola.Marfil)
            }
        }
    }
}
