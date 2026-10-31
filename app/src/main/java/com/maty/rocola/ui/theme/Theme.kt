package com.maty.rocola.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.R

/**
 * Paleta "Rocola": un vino profundo como la madera tapizada de una rocola,
 * marfil para el texto y ámbar reservado para lo que está sonando ahora.
 */
object Rocola {
    val Vino = Color(0xFF2A1A22)
    val VinoAlto = Color(0xFF38242E)
    val VinoClaro = Color(0xFF4A3340)
    val Marfil = Color(0xFFF7EDE2)
    val Arena = Color(0xFFC9B5A9)
    val Ceniza = Color(0xFFA08A82)
    val Ambar = Color(0xFFFFB238)
    val AmbarTinta = Color(0xFF3A2400)
    val Error = Color(0xFFFF8A75)
}

val ArchivoText = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
)

val ArchivoWide = FontFamily(
    Font(R.font.archivo_wide_bold, FontWeight.Bold),
    Font(R.font.archivo_wide_black, FontWeight.Black),
)

val ArchivoNarrow = FontFamily(
    Font(R.font.archivo_narrow_semibold, FontWeight.SemiBold),
)

object RocolaType {
    val wordmark = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp)
    val display = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = (-0.3).sp)
    val headline = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp)
    val section = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)
    val tab = TextStyle(fontFamily = ArchivoWide, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp)
    val itemTitle = TextStyle(fontFamily = ArchivoText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp)
    val itemSub = TextStyle(fontFamily = ArchivoText, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val body = TextStyle(fontFamily = ArchivoText, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val label = TextStyle(fontFamily = ArchivoText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val small = TextStyle(fontFamily = ArchivoText, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val numeral = TextStyle(fontFamily = ArchivoNarrow, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
    val numeralLarge = TextStyle(fontFamily = ArchivoNarrow, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum")
}

private val base = Typography()

private val RocolaTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Black),
    displayMedium = base.displayMedium.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Black),
    displaySmall = base.displaySmall.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Black),
    headlineLarge = base.headlineLarge.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Black),
    headlineSmall = base.headlineSmall.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = ArchivoText, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = ArchivoText, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = ArchivoText),
    bodyMedium = base.bodyMedium.copy(fontFamily = ArchivoText),
    bodySmall = base.bodySmall.copy(fontFamily = ArchivoText),
    labelLarge = base.labelLarge.copy(fontFamily = ArchivoText, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = ArchivoText, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = ArchivoText, fontWeight = FontWeight.Medium),
)

private val RocolaColors = darkColorScheme(
    primary = Rocola.Marfil,
    onPrimary = Rocola.Vino,
    primaryContainer = Rocola.VinoClaro,
    onPrimaryContainer = Rocola.Marfil,
    secondary = Rocola.Arena,
    onSecondary = Rocola.Vino,
    secondaryContainer = Rocola.VinoClaro,
    onSecondaryContainer = Rocola.Marfil,
    tertiary = Rocola.Ambar,
    onTertiary = Rocola.AmbarTinta,
    background = Rocola.Vino,
    onBackground = Rocola.Marfil,
    surface = Rocola.Vino,
    onSurface = Rocola.Marfil,
    surfaceVariant = Rocola.VinoAlto,
    onSurfaceVariant = Rocola.Arena,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Rocola.Vino,
    surfaceContainerLow = Color(0xFF31202A),
    surfaceContainer = Rocola.VinoAlto,
    surfaceContainerHigh = Color(0xFF412B36),
    surfaceContainerHighest = Rocola.VinoClaro,
    outline = Color(0xFF6E5560),
    outlineVariant = Rocola.VinoClaro,
    error = Rocola.Error,
    onError = Rocola.Vino,
    inverseSurface = Rocola.Marfil,
    inverseOnSurface = Rocola.Vino,
    scrim = Color(0xFF120A0E),
)

private val RocolaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RocolaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RocolaColors,
        typography = RocolaTypography,
        shapes = RocolaShapes,
        content = content,
    )
}
