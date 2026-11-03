package com.maty.rocola.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maty.rocola.data.LibraryData
import com.maty.rocola.data.LibraryIndex
import com.maty.rocola.data.Playlist
import com.maty.rocola.data.SearchResults
import com.maty.rocola.data.SongSort
import com.maty.rocola.rocola
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.rocola.repository
    private val store = app.rocola.userStore

    private val _permissionGranted = MutableStateFlow(repository.hasPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    val sort: StateFlow<SongSort> = store.sort
    val hideShort: StateFlow<Boolean> = store.hideShort
    val favorites: StateFlow<Set<Long>> = store.favorites
    val playlists: StateFlow<List<Playlist>> = store.playlists

    val library: StateFlow<LibraryData?> =
        combine(repository.songs, store.sort, store.hideShort) { songs, sort, hide ->
            songs?.let { LibraryIndex.build(it, sort, hide) }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val query = MutableStateFlow("")

    val searchResults: StateFlow<SearchResults> =
        combine(library.filterNotNull(), query.debounce(120)) { lib, q -> LibraryIndex.search(lib, q) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults.Empty)

    init {
        if (repository.hasPermission()) repository.start()
    }

    /** Se llama al volver a la app o después de pedir el permiso. */
    fun recheckPermission() {
        val granted = repository.hasPermission()
        _permissionGranted.value = granted
        if (granted) repository.start()
    }

    fun refresh() = repository.refresh()
    fun toggleFavorite(songId: Long) = store.toggleFavorite(songId)
    fun setSort(sort: SongSort) = store.setSort(sort)
    fun setHideShort(hide: Boolean) = store.setHideShort(hide)
    fun createPlaylist(name: String, songIds: List<Long>): Long = store.createPlaylist(name, songIds)
    fun renamePlaylist(id: Long, name: String) = store.renamePlaylist(id, name)
    fun deletePlaylist(id: Long) = store.deletePlaylist(id)
    fun addToPlaylist(id: Long, songIds: List<Long>): Int = store.addToPlaylist(id, songIds)
    fun removeFromPlaylist(id: Long, songId: Long) = store.removeFromPlaylist(id, songId)

    /** Después de borrar archivos: limpia favoritas y listas, y vuelve a leer la biblioteca. */
    fun forgetSongs(songIds: Set<Long>) {
        store.removeSongs(songIds)
        repository.refresh()
    }
}
