package com.maty.rocola.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maty.rocola.playback.EqualizerState
import com.maty.rocola.playback.SleepTimer
import com.maty.rocola.ui.AppController
import com.maty.rocola.ui.components.DetailTopBar
import com.maty.rocola.ui.components.IconTileRow
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import java.util.Locale

@Composable
private fun ScreenTitle(text: String) {
    Text(text, style = RocolaType.headline, color = Rocola.Marfil, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp))
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = RocolaType.tab, color = Rocola.Arena, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp))
}

private fun ignoresBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
private fun askBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching { context.startActivity(direct) }.onFailure { runCatching { context.startActivity(fallback) } }
}

@Composable
fun SettingsScreen(app: AppController, bottomPadding: Dp) {
    val context = LocalContext.current
    val hideShort by app.library.hideShort.collectAsStateWithLifecycle()
    val eq by EqualizerState.settings.collectAsStateWithLifecycle()
    val timer by SleepTimer.state.collectAsStateWithLifecycle()
    var batteryFree by remember { mutableStateOf(ignoresBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        batteryFree = ignoresBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item { DetailTopBar(onBack = { app.back() }) }
        item { ScreenTitle("Ajustes") }

        item { GroupLabel("Reproducción") }
        item {
            IconTileRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Ecualizador",
                subtitle = if (eq.enabled) "Activado" else "Desactivado",
                onClick = app::openEqualizer,
            )
        }
        item {
            IconTileRow(
                icon = Icons.Rounded.Bedtime,
                title = "Temporizador para dormir",
                subtitle = when (timer) {
                    SleepTimer.State.Off -> "Pausa la música después de un rato"
                    SleepTimer.State.EndOfTrack -> "Se detiene al terminar la canción actual"
                    is SleepTimer.State.Countdown -> "Activado"
                },
                onClick = { app.showSleepTimer = true },
            )
        }

        item { GroupLabel("Segundo plano") }
        item {
            IconTileRow(
                icon = Icons.Rounded.BatteryChargingFull,
                title = if (batteryFree) "Batería sin restricciones" else "Quitar restricción de batería",
                subtitle = if (batteryFree) {
                    "Listo: el sistema no va a cortar la música con la pantalla apagada."
                } else {
                    "Algunos teléfonos (Xiaomi, Samsung, Huawei) cortan las apps en segundo plano. Tocá acá y elegí Permitir."
                },
                onClick = { askBatteryExemption(context) },
                iconTint = if (batteryFree) Rocola.Arena else Rocola.Marfil,
            )
        }

        item { GroupLabel("Biblioteca") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { app.library.setHideShort(!hideShort) }
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Ocultar audios cortos", style = RocolaType.itemTitle, color = Rocola.Marfil)
                    Spacer(Modifier.height(2.dp))
                    Text("No mostrar archivos de menos de 30 segundos, como notas de voz.", style = RocolaType.itemSub, color = Rocola.Arena)
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = hideShort,
                    onCheckedChange = { app.library.setHideShort(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Rocola.Vino,
                        checkedTrackColor = Rocola.Marfil,
                        uncheckedThumbColor = Rocola.Ceniza,
                        uncheckedTrackColor = Rocola.VinoAlto,
                        uncheckedBorderColor = Rocola.VinoClaro,
                    ),
                )
            }
        }
        item {
            IconTileRow(
                icon = Icons.Rounded.Refresh,
                title = "Buscar música de nuevo",
                subtitle = "Actualiza la biblioteca si agregaste o borraste archivos.",
                onClick = {
                    app.library.refresh()
                    app.toast("Biblioteca actualizada")
                },
            )
        }
        item {
            Text(
                "Rocola 1.0",
                style = RocolaType.small,
                color = Rocola.Ceniza,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            )
        }
    }
}

@Composable
fun EqualizerScreen(app: AppController, bottomPadding: Dp) {
    val info by EqualizerState.info.collectAsStateWithLifecycle()
    val settings by EqualizerState.settings.collectAsStateWithLifecycle()
    val sliderColors = SliderDefaults.colors(
        thumbColor = Rocola.Marfil,
        activeTrackColor = Rocola.Marfil,
        inactiveTrackColor = Rocola.VinoClaro,
        disabledThumbColor = Rocola.Ceniza,
        disabledActiveTrackColor = Rocola.VinoClaro,
        disabledInactiveTrackColor = Rocola.VinoAlto,
        activeTickColor = Rocola.Marfil,
        inactiveTickColor = Rocola.VinoClaro,
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item { DetailTopBar(onBack = { app.back() }) }
        item { ScreenTitle("Ecualizador") }
        val eqInfo = info
        if (eqInfo == null) {
            item {
                Text(
                    "El ecualizador se ajusta mientras suena música. Reproducí una canción y volvé a esta pantalla. Si sigue sin aparecer, este teléfono no permite ecualizar desde otras apps.",
                    style = RocolaType.body,
                    color = Rocola.Arena,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            return@LazyColumn
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { EqualizerState.update { it.copy(enabled = !it.enabled) } }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Activado", style = RocolaType.itemTitle, color = Rocola.Marfil, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { checked -> EqualizerState.update { it.copy(enabled = checked) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Rocola.Vino,
                        checkedTrackColor = Rocola.Marfil,
                        uncheckedThumbColor = Rocola.Ceniza,
                        uncheckedTrackColor = Rocola.VinoAlto,
                        uncheckedBorderColor = Rocola.VinoClaro,
                    ),
                )
            }
        }
        item { GroupLabel("Estilo") }
        item {
            val names = listOf("Personalizado") + eqInfo.presets
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                itemsIndexed(names) { i, name ->
                    val presetIndex = i - 1
                    FilterChip(
                        selected = settings.preset == presetIndex,
                        enabled = settings.enabled,
                        onClick = {
                            EqualizerState.update { s ->
                                if (presetIndex < 0) s.copy(preset = -1)
                                else s.copy(preset = presetIndex, levels = eqInfo.presetLevels.getOrElse(presetIndex) { s.levels })
                            }
                        },
                        label = { Text(name, style = RocolaType.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Rocola.Vino,
                            labelColor = Rocola.Arena,
                            selectedContainerColor = Rocola.Marfil,
                            selectedLabelColor = Rocola.Vino,
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        item { GroupLabel("Bandas") }
        eqInfo.bands.forEach { band ->
            item(key = "band-${band.index}") {
                val level = settings.levels.getOrElse(band.index) { 0 }.coerceIn(band.minLevel, band.maxLevel)
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatHz(band.centerHz), style = RocolaType.numeral, color = Rocola.Arena, modifier = Modifier.width(64.dp))
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { v ->
                            EqualizerState.update { s ->
                                val levels = MutableList(eqInfo.bands.size) { i -> s.levels.getOrElse(i) { 0 } }
                                levels[band.index] = v.toInt()
                                s.copy(levels = levels, preset = -1)
                            }
                        },
                        valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                        enabled = settings.enabled,
                        colors = sliderColors,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        String.format(Locale.ROOT, "%+.0f dB", level / 100f),
                        style = RocolaType.numeral,
                        color = Rocola.Arena,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(60.dp),
                    )
                }
            }
        }
        if (eqInfo.bassSupported) {
            item { GroupLabel("Refuerzo de graves") }
            item {
                Slider(
                    value = settings.bass.toFloat(),
                    onValueChange = { v -> EqualizerState.update { it.copy(bass = v.toInt()) } },
                    valueRange = 0f..1000f,
                    enabled = settings.enabled,
                    colors = sliderColors,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

private fun formatHz(hz: Int): String =
    if (hz >= 1000) String.format(Locale.ROOT, "%.1f kHz", hz / 1000f).replace(".0 ", " ") else "$hz Hz"
