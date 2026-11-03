package com.maty.rocola.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.maty.rocola.data.Song

data class SongMenuRequest(val song: Song, val playlistId: Long? = null)

/** Canciones elegidas con un toque largo en cualquier lista. */
@Stable
class SelectionController {
    var ids by mutableStateOf<Set<Long>>(emptySet())
        private set

    /** La lista donde se empezó a seleccionar: sirve para "Seleccionar todo" y para mantener el orden. */
    var source by mutableStateOf<List<Song>>(emptyList())
        private set

    val active: Boolean get() = ids.isNotEmpty()
    val count: Int get() = ids.size

    fun start(song: Song, list: List<Song>) {
        source = list
        ids = setOf(song.id)
    }

    fun toggle(song: Song) {
        ids = if (song.id in ids) ids - song.id else ids + song.id
        if (ids.isEmpty()) source = emptyList()
    }

    fun selectAll() {
        ids = source.map { it.id }.toSet()
    }

    fun clear() {
        if (ids.isNotEmpty()) ids = emptySet()
        if (source.isNotEmpty()) source = emptyList()
    }

    fun selectedSongs(): List<Song> = source.filter { it.id in ids }
}

/** Reúne las acciones que disparan las pantallas y el estado de las hojas emergentes. */
@Stable
class AppController(
    val library: LibraryViewModel,
    val player: PlayerViewModel,
    val nav: NavViewModel,
    private val context: Context,
) {
    val selection = SelectionController()

    var songMenu by mutableStateOf<SongMenuRequest?>(null)
    var addToPlaylist by mutableStateOf<List<Long>?>(null)
    var createPlaylistWith by mutableStateOf<List<Long>?>(null)
    var renamePlaylistId by mutableStateOf<Long?>(null)
    var showQueue by mutableStateOf(false)
    var showSleepTimer by mutableStateOf(false)
    var showSpeed by mutableStateOf(false)

    /** Canciones a borrar del teléfono, esperando la confirmación del sistema. */
    var deleteRequest by mutableStateOf<List<Song>?>(null)

    fun openAlbum(albumId: Long) = nav.push(Screen.AlbumDetail(albumId))
    fun openArtist(name: String) = nav.push(Screen.ArtistDetail(name))
    fun openFolder(path: String) = nav.push(Screen.FolderDetail(path))
    fun openPlaylist(id: Long) = nav.push(Screen.PlaylistDetail(id))
    fun openFavorites() = nav.push(Screen.Favorites)
    fun openSearch() = nav.push(Screen.Search)
    fun openSettings() = nav.push(Screen.Settings)
    fun openEqualizer() = nav.push(Screen.Equalizer)
    fun back() = nav.pop()

    fun play(songs: List<Song>, index: Int, label: String?) = player.play(songs, index, shuffle = null, label = label)
    fun playInOrder(songs: List<Song>, label: String?) = player.play(songs, 0, shuffle = false, label = label)
    fun shuffle(songs: List<Song>, label: String?) = player.play(songs, 0, shuffle = true, label = label)

    fun playNext(song: Song) {
        player.playNext(listOf(song))
        toast("Va a sonar a continuación")
    }

    fun addToQueue(song: Song) {
        player.addToQueue(listOf(song))
        toast("Agregada a la cola")
    }

    // region Selección

    fun playSelection() {
        val songs = selection.selectedSongs()
        if (songs.isEmpty()) return
        player.play(songs, 0, shuffle = false, label = "Tu selección")
        selection.clear()
    }

    fun queueSelection() {
        val songs = selection.selectedSongs()
        if (songs.isEmpty()) return
        player.addToQueue(songs)
        toast(if (songs.size == 1) "Agregada a la cola" else "Se agregaron ${songs.size} canciones a la cola")
        selection.clear()
    }

    fun addSelectionToPlaylist() {
        val ids = selection.selectedSongs().map { it.id }
        if (ids.isEmpty()) return
        addToPlaylist = ids
        selection.clear()
    }

    fun deleteSelection() {
        val songs = selection.selectedSongs()
        if (songs.isNotEmpty()) deleteRequest = songs
    }

    fun deleteSong(song: Song) {
        deleteRequest = listOf(song)
    }

    /** El sistema ya borró los archivos: los sacamos de la cola, favoritas y listas. */
    fun onSongsDeleted(songs: List<Song>) {
        deleteRequest = null
        if (songs.isEmpty()) return
        val ids = songs.map { it.id }.toSet()
        player.removeSongsFromQueue(ids)
        library.forgetSongs(ids)
        selection.clear()
        if (songMenu?.song?.id in ids) songMenu = null
        toast(if (songs.size == 1) "Se borró 1 canción" else "Se borraron ${songs.size} canciones")
    }

    fun onDeleteCancelled() {
        deleteRequest = null
    }

    // endregion

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val added = library.addToPlaylist(playlistId, songIds)
        val name = library.playlists.value.firstOrNull { it.id == playlistId }?.name.orEmpty()
        toast(
            when {
                added == 0 && songIds.size == 1 -> "Ya estaba en $name"
                added == 0 -> "Ya estaban todas en $name"
                added == 1 -> "Agregada a $name"
                else -> "Se agregaron $added canciones a $name"
            },
        )
    }

    fun createPlaylist(name: String, songIds: List<Long>) {
        library.createPlaylist(name, songIds)
        toast(
            when (songIds.size) {
                0 -> "Lista creada"
                1 -> "Lista creada con la canción"
                else -> "Lista creada con ${songIds.size} canciones"
            },
        )
    }

    fun share(song: Song) {
        val send = Intent(Intent.ACTION_SEND)
            .setType(song.mimeType ?: "audio/*")
            .putExtra(Intent.EXTRA_STREAM, song.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, "Compartir canción").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
