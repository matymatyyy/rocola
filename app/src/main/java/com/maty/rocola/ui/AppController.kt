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

/** Reúne las acciones que disparan las pantallas y el estado de las hojas emergentes. */
@Stable
class AppController(
    val library: LibraryViewModel,
    val player: PlayerViewModel,
    val nav: NavViewModel,
    private val context: Context,
) {
    var songMenu by mutableStateOf<SongMenuRequest?>(null)
    var addToPlaylist by mutableStateOf<List<Long>?>(null)
    var createPlaylistWith by mutableStateOf<List<Long>?>(null)
    var renamePlaylistId by mutableStateOf<Long?>(null)
    var showQueue by mutableStateOf(false)
    var showSleepTimer by mutableStateOf(false)
    var showSpeed by mutableStateOf(false)

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

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val added = library.addToPlaylist(playlistId, songIds)
        val name = library.playlists.value.firstOrNull { it.id == playlistId }?.name.orEmpty()
        toast(if (added == 0) "Ya estaba en $name" else "Agregada a $name")
    }

    fun createPlaylist(name: String, songIds: List<Long>) {
        library.createPlaylist(name, songIds)
        toast(if (songIds.isEmpty()) "Lista creada" else "Lista creada con la canción")
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
