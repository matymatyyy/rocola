package com.maty.rocola.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.maty.rocola.art.Artwork
import com.maty.rocola.ui.theme.RocolaType
import androidx.compose.material3.Text

private val placeholderTones = listOf(
    Color(0xFF5B2C3A),
    Color(0xFF2F4858),
    Color(0xFF6B4E2E),
    Color(0xFF3D3A6B),
    Color(0xFF4E5B31),
    Color(0xFF6B2F2F),
    Color(0xFF2E5A55),
    Color(0xFF5A4A6B),
)

/** Tapa del álbum con una "etiqueta de disco" de respaldo: color propio y la inicial. */
@Composable
fun ArtworkImage(
    artwork: Artwork?,
    seed: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    letterSize: TextUnit = 20.sp,
    requestSizePx: Int? = null,
) {
    val tone = remember(seed) { placeholderTones[(seed.hashCode() and Int.MAX_VALUE) % placeholderTones.size] }
    val initial = remember(seed) { seed.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "♪" }
    Box(modifier.clip(shape).background(tone), contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            style = RocolaType.headline.copy(fontSize = letterSize, lineHeight = letterSize),
            color = Color(0xD9F7EDE2),
            textAlign = TextAlign.Center,
        )
        if (artwork != null) {
            val context = LocalPlatformContext.current
            val model = remember(artwork, requestSizePx) {
                ImageRequest.Builder(context)
                    .data(artwork)
                    .apply { if (requestSizePx != null) size(Size(requestSizePx, requestSizePx)) }
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
