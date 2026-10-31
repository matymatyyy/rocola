package com.maty.rocola.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class EqBand(val index: Int, val centerHz: Int, val minLevel: Int, val maxLevel: Int)

@Immutable
data class EqInfo(
    val bands: List<EqBand>,
    val presets: List<String>,
    val presetLevels: List<List<Int>>,
    val bassSupported: Boolean,
)

@Immutable
data class EqSettings(
    val enabled: Boolean = false,
    val preset: Int = -1,
    val levels: List<Int> = emptyList(),
    val bass: Int = 0,
)

/** Estado del ecualizador compartido entre la pantalla y el servicio. */
object EqualizerState {
    private var prefs: SharedPreferences? = null

    private val _info = MutableStateFlow<EqInfo?>(null)
    val info: StateFlow<EqInfo?> = _info.asStateFlow()

    private val _settings = MutableStateFlow(EqSettings())
    val settings: StateFlow<EqSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("rocola_eq", Context.MODE_PRIVATE)
        prefs = p
        _settings.value = EqSettings(
            enabled = p.getBoolean("enabled", false),
            preset = p.getInt("preset", -1),
            levels = p.getString("levels", null)?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty(),
            bass = p.getInt("bass", 0),
        )
    }

    internal fun publishInfo(info: EqInfo?) {
        _info.value = info
    }

    fun update(transform: (EqSettings) -> EqSettings) {
        _settings.update(transform)
        val s = _settings.value
        prefs?.edit()
            ?.putBoolean("enabled", s.enabled)
            ?.putInt("preset", s.preset)
            ?.putString("levels", s.levels.joinToString(","))
            ?.putInt("bass", s.bass)
            ?.apply()
    }
}

/** Ecualizador y refuerzo de graves enganchados a la sesión de audio del reproductor. */
class AudioEffects(private val scope: CoroutineScope) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var job: Job? = null

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val range = eq.bandLevelRange
            val bandCount = eq.numberOfBands.toInt()
            val bands = (0 until bandCount).map { i ->
                EqBand(i, eq.getCenterFreq(i.toShort()) / 1000, range[0].toInt(), range[1].toInt())
            }
            val presetCount = eq.numberOfPresets.toInt()
            val presets = (0 until presetCount).map { eq.getPresetName(it.toShort()) }
            val presetLevels = (0 until presetCount).map { p ->
                eq.usePreset(p.toShort())
                (0 until bandCount).map { b -> eq.getBandLevel(b.toShort()).toInt() }
            }
            val bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            bassBoost = bass
            EqualizerState.publishInfo(EqInfo(bands, presets, presetLevels, bass?.strengthSupported == true))
        } catch (_: Throwable) {
            EqualizerState.publishInfo(null)
            release()
            return
        }
        job = scope.launch { EqualizerState.settings.collect { apply(it) } }
    }

    private fun apply(settings: EqSettings) {
        val eq = equalizer ?: return
        runCatching {
            if (settings.enabled) {
                val bandCount = eq.numberOfBands.toInt()
                settings.levels.take(bandCount).forEachIndexed { i, level ->
                    eq.setBandLevel(i.toShort(), level.toShort())
                }
            }
            eq.enabled = settings.enabled
        }
        bassBoost?.let { bass ->
            runCatching {
                if (bass.strengthSupported) bass.setStrength(settings.bass.coerceIn(0, 1000).toShort())
                bass.enabled = settings.enabled && settings.bass > 0
            }
        }
    }

    fun release() {
        job?.cancel()
        job = null
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }
}
