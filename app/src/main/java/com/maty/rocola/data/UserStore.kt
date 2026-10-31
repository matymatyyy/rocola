package com.maty.rocola.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Favoritas, listas y preferencias, guardadas en el teléfono. */
class UserStore(context: Context) {

    private val prefs = context.getSharedPreferences("rocola_user", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow(loadPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _sort = MutableStateFlow(SongSort.entries.getOrElse(prefs.getInt(KEY_SORT, 0)) { SongSort.TITLE })
    val sort: StateFlow<SongSort> = _sort.asStateFlow()

    private val _hideShort = MutableStateFlow(prefs.getBoolean(KEY_HIDE_SHORT, true))
    val hideShort: StateFlow<Boolean> = _hideShort.asStateFlow()

    fun isFavorite(songId: Long): Boolean = songId in _favorites.value

    fun toggleFavorite(songId: Long) {
        _favorites.update { current -> if (songId in current) current - songId else current + songId }
        prefs.edit().putString(KEY_FAVORITES, _favorites.value.joinToString(",")).apply()
    }

    fun setSort(sort: SongSort) {
        _sort.value = sort
        prefs.edit().putInt(KEY_SORT, sort.ordinal).apply()
    }

    fun setHideShort(hide: Boolean) {
        _hideShort.value = hide
        prefs.edit().putBoolean(KEY_HIDE_SHORT, hide).apply()
    }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList()): Long {
        val id = (_playlists.value.maxOfOrNull { it.id } ?: 0L) + 1
        _playlists.update { it + Playlist(id, name.trim().ifEmpty { "Lista $id" }, songIds.distinct()) }
        savePlaylists()
        return id
    }

    fun renamePlaylist(id: Long, name: String) {
        _playlists.update { list -> list.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it } }
        savePlaylists()
    }

    fun deletePlaylist(id: Long) {
        _playlists.update { list -> list.filterNot { it.id == id } }
        savePlaylists()
    }

    /** Agrega canciones al final, sin repetir las que ya están. Devuelve cuántas se agregaron. */
    fun addToPlaylist(id: Long, songIds: List<Long>): Int {
        var added = 0
        _playlists.update { list ->
            list.map { p ->
                if (p.id != id) p else {
                    val fresh = songIds.distinct().filterNot { it in p.songIds }
                    added = fresh.size
                    p.copy(songIds = p.songIds + fresh)
                }
            }
        }
        savePlaylists()
        return added
    }

    fun removeFromPlaylist(id: Long, songId: Long) {
        _playlists.update { list -> list.map { if (it.id == id) it.copy(songIds = it.songIds - songId) else it } }
        savePlaylists()
    }

    private fun loadFavorites(): Set<Long> =
        prefs.getString(KEY_FAVORITES, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    private fun loadPlaylists(): List<Playlist> = runCatching {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val array = JSONArray(raw)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val songs = o.getJSONArray("songs")
            Playlist(
                id = o.getLong("id"),
                name = o.getString("name"),
                songIds = List(songs.length()) { j -> songs.getLong(j) },
            )
        }
    }.getOrElse { emptyList() }

    private fun savePlaylists() {
        val array = JSONArray()
        _playlists.value.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("songs", JSONArray(p.songIds)),
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_PLAYLISTS = "playlists"
        const val KEY_SORT = "sort"
        const val KEY_HIDE_SHORT = "hide_short"
    }
}
