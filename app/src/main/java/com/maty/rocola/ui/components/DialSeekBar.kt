package com.maty.rocola.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType

private val minorSteps = intArrayOf(1, 2, 5, 10, 15, 30, 60, 120, 300, 600)
private val majorSteps = intArrayOf(10, 15, 30, 60, 120, 300, 600, 900, 1800, 3600)

/**
 * El dial: una escala de radio con marcas por segundo y una aguja que la recorre.
 * Lo ya escuchado queda iluminado con el color de acento. Se toca o se arrastra para saltar.
 */
@Composable
fun DialSeekBar(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Rocola.Marfil,
) {
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val labelStyle = RocolaType.numeral.copy(fontSize = 12.sp, color = ink.copy(alpha = 0.6f))
    var scrub by remember { mutableStateOf<Float?>(null) }
    var lastMark by remember { mutableIntStateOf(-1) }
    val duration = durationMs.coerceAtLeast(0L)
    val currentDuration by rememberUpdatedState(duration)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val fraction = scrub ?: if (duration > 0) (positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownMs = (fraction * duration).toLong()

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val d = currentDuration
                        if (d > 0) currentOnSeek(((offset.x / size.width).coerceIn(0f, 1f) * d).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            scrub = (offset.x / size.width).coerceIn(0f, 1f)
                            lastMark = -1
                        },
                        onDragEnd = {
                            val d = currentDuration
                            scrub?.let { if (d > 0) currentOnSeek((it * d).toLong()) }
                            scrub = null
                        },
                        onDragCancel = { scrub = null },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val f = ((scrub ?: 0f) + dragAmount / size.width).coerceIn(0f, 1f)
                            scrub = f
                            val mark = ((f * currentDuration) / 10_000L).toInt()
                            if (mark != lastMark) {
                                if (lastMark != -1) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastMark = mark
                            }
                        },
                    )
                }
                .semantics {
                    contentDescription = "Posición de la canción"
                    stateDescription = "${formatDuration(shownMs)} de ${formatDuration(duration)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { target ->
                        val d = currentDuration
                        if (d > 0) {
                            currentOnSeek((target.coerceIn(0f, 1f) * d).toLong())
                            true
                        } else {
                            false
                        }
                    }
                },
        ) {
            val w = size.width
            val baseline = size.height - 2.dp.toPx()
            val needleX = fraction * w
            val totalSec = (duration / 1000L).toInt()
            val dim = ink.copy(alpha = 0.24f)
            val dimMajor = ink.copy(alpha = 0.45f)

            if (totalSec <= 0) {
                drawLine(dim, Offset(0f, baseline), Offset(w, baseline), 2.dp.toPx(), StrokeCap.Round)
            } else {
                val widthDp = w / density
                val minor = minorSteps.firstOrNull { totalSec / it <= widthDp / 6f } ?: minorSteps.last()
                val major = majorSteps.firstOrNull { it % minor == 0 && widthDp * it / totalSec >= 44f } ?: majorSteps.last()
                val half = if ((major / 2) % minor == 0) major / 2 else -1
                val minorH = 7.dp.toPx()
                val halfH = 12.dp.toPx()
                val majorH = 19.dp.toPx()
                var t = 0
                while (t <= totalSec) {
                    val x = (t.toFloat() / totalSec) * w
                    val isMajor = t % major == 0
                    val isHalf = !isMajor && half > 0 && t % half == 0
                    val h = when {
                        isMajor -> majorH
                        isHalf -> halfH
                        else -> minorH
                    }
                    val lit = x <= needleX
                    val color = when {
                        lit -> accent.copy(alpha = if (isMajor) 1f else 0.85f)
                        isMajor -> dimMajor
                        else -> dim
                    }
                    drawLine(
                        color = color,
                        start = Offset(x, baseline - h),
                        end = Offset(x, baseline),
                        strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    if (isMajor) {
                        val label = if (major < 60) formatDuration(t * 1000L) else (t / 60).toString()
                        val layout = measurer.measure(label, labelStyle)
                        val lx = (x + 4.dp.toPx()).coerceAtMost(w - layout.size.width)
                        drawText(layout, topLeft = Offset(lx, 0f))
                    }
                    t += minor
                }
            }

            // Aguja con resplandor
            val top = 16.dp.toPx()
            val scrubbing = scrub != null
            drawLine(accent.copy(alpha = 0.16f), Offset(needleX, top), Offset(needleX, baseline + 1.dp.toPx()), (if (scrubbing) 16 else 11).dp.toPx(), StrokeCap.Round)
            drawLine(accent.copy(alpha = 0.32f), Offset(needleX, top), Offset(needleX, baseline + 1.dp.toPx()), 6.dp.toPx(), StrokeCap.Round)
            drawLine(accent, Offset(needleX, top), Offset(needleX, baseline + 1.dp.toPx()), 2.6.dp.toPx(), StrokeCap.Round)
            drawCircle(accent, radius = (if (scrubbing) 6.5f else 4.5f).dp.toPx(), center = Offset(needleX, top))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(formatDuration(shownMs), style = RocolaType.numeralLarge, color = ink)
            Spacer(Modifier.weight(1f))
            Text("-" + formatDuration((duration - shownMs).coerceAtLeast(0L)), style = RocolaType.numeralLarge, color = ink.copy(alpha = 0.6f))
        }
    }
}
