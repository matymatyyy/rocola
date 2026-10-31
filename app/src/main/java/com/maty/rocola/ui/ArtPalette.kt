package com.maty.rocola.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.maty.rocola.art.ArtworkLoader
import com.maty.rocola.ui.theme.Rocola

/**
 * Colores del reproductor sacados de la tapa: un fondo profundo con el tono del disco
 * y un acento luminoso para la aguja del dial y el botón de reproducir.
 */
@Immutable
data class ArtPalette(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val onAccent: Color,
) {
    companion object {
        val Default = ArtPalette(
            background = Color(0xFF3B2330),
            surface = Color(0xFF4A2E3C),
            accent = Rocola.Ambar,
            onAccent = Rocola.AmbarTinta,
        )

        fun from(context: Context, albumId: Long, songUri: Uri?): ArtPalette {
            val bitmap = ArtworkLoader.load(context, albumId, songUri, 160) ?: return Default
            val palette = Palette.from(bitmap).maximumColorCount(24).generate()
            val baseSwatch = palette.darkMutedSwatch
                ?: palette.mutedSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.dominantSwatch
                ?: return Default
            val background = tone(baseSwatch.rgb, maxLightness = 0.19f, maxSaturation = 0.55f)
            val surface = tone(baseSwatch.rgb, maxLightness = 0.26f, maxSaturation = 0.5f, minLightness = 0.22f)

            val accentSource = palette.lightVibrantSwatch?.rgb
                ?: palette.vibrantSwatch?.rgb
                ?: palette.lightMutedSwatch?.rgb
            var accent = accentSource?.let { brighten(it, background) } ?: Rocola.Ambar.toArgb()
            if (ColorUtils.calculateContrast(accent, background) < 4.5) accent = Rocola.Ambar.toArgb()
            val onAccent = tone(accent, maxLightness = 0.13f, maxSaturation = 0.6f)

            return ArtPalette(
                background = Color(background),
                surface = Color(surface),
                accent = Color(accent),
                onAccent = Color(onAccent),
            )
        }

        private fun tone(rgb: Int, maxLightness: Float, maxSaturation: Float, minLightness: Float = 0.08f): Int {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rgb, hsl)
            hsl[1] = hsl[1].coerceAtMost(maxSaturation)
            hsl[2] = hsl[2].coerceIn(minLightness, maxLightness)
            return ColorUtils.HSLToColor(hsl)
        }

        private fun brighten(rgb: Int, background: Int): Int {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rgb, hsl)
            hsl[1] = hsl[1].coerceAtLeast(0.45f)
            hsl[2] = hsl[2].coerceAtLeast(0.62f)
            var color = ColorUtils.HSLToColor(hsl)
            while (ColorUtils.calculateContrast(color, background) < 4.5 && hsl[2] < 0.92f) {
                hsl[2] += 0.04f
                color = ColorUtils.HSLToColor(hsl)
            }
            return color
        }
    }
}
