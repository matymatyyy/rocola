package com.maty.rocola.playback

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Temporizador para dormir. Lo ejecuta el servicio, así funciona con la pantalla apagada. */
object SleepTimer {

    sealed interface State {
        data object Off : State
        data object EndOfTrack : State
        data class Countdown(val endsAtElapsed: Long) : State
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(minutes: Int) {
        _state.value = State.Countdown(SystemClock.elapsedRealtime() + minutes * 60_000L)
    }

    fun endOfTrack() {
        _state.value = State.EndOfTrack
    }

    fun cancel() {
        _state.value = State.Off
    }
}
