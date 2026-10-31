package com.maty.rocola.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

sealed interface Screen {
    data object Library : Screen
    data class AlbumDetail(val albumId: Long) : Screen
    data class ArtistDetail(val name: String) : Screen
    data class FolderDetail(val path: String) : Screen
    data class PlaylistDetail(val playlistId: Long) : Screen
    data object Favorites : Screen
    data object Search : Screen
    data object Settings : Screen
    data object Equalizer : Screen
}

data class NavEntry(val screen: Screen, val depth: Int)

/** Navegación simple con pila propia: sobrevive a la rotación y no necesita librerías extra. */
class NavViewModel : ViewModel() {
    val stack = mutableStateListOf<Screen>(Screen.Library)
    var playerExpanded by mutableStateOf(false)
    var libraryPage by mutableStateOf(0)

    val top: NavEntry get() = NavEntry(stack.last(), stack.size)

    fun push(screen: Screen) {
        playerExpanded = false
        if (stack.last() != screen) stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}
