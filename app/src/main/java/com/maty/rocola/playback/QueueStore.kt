package com.maty.rocola.playback

import android.content.Context

/** Guarda la cola para retomarla al volver a abrir la app o desde los auriculares. */
class QueueStore(context: Context) {

    data class Saved(
        val ids: List<Long>,
        val index: Int,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeatMode: Int,
    )

    private val prefs = context.getSharedPreferences("rocola_queue", Context.MODE_PRIVATE)

    fun save(ids: List<Long>, index: Int, positionMs: Long, shuffle: Boolean, repeatMode: Int) {
        prefs.edit()
            .putString(KEY_IDS, ids.joinToString(","))
            .putInt(KEY_INDEX, index)
            .putLong(KEY_POSITION, positionMs)
            .putBoolean(KEY_SHUFFLE, shuffle)
            .putInt(KEY_REPEAT, repeatMode)
            .apply()
    }

    fun savePosition(index: Int, positionMs: Long) {
        prefs.edit().putInt(KEY_INDEX, index).putLong(KEY_POSITION, positionMs).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun load(): Saved? {
        val ids = prefs.getString(KEY_IDS, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) return null
        return Saved(
            ids = ids,
            index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, ids.lastIndex),
            positionMs = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
            shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
            repeatMode = prefs.getInt(KEY_REPEAT, 0),
        )
    }

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
    }
}
