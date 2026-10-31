package com.maty.rocola.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.maty.rocola.art.Artwork

@Immutable
data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val albumArtist: String?,
    val durationMs: Long,
    val track: Int,
    val disc: Int,
    val year: Int,
    val dateAdded: Long,
    val folderPath: String,
    val folderName: String,
    val mimeType: String?,
) {
    val artwork: Artwork get() = Artwork(albumId, uri)
}

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val songs: List<Song>,
) {
    val durationMs: Long = songs.sumOf { it.durationMs }
    val artwork: Artwork get() = Artwork(id, songs.firstOrNull()?.uri)
}

@Immutable
data class Artist(
    val name: String,
    val albums: List<Album>,
    val songs: List<Song>,
) {
    val artwork: Artwork? get() = albums.firstOrNull()?.artwork
}

@Immutable
data class Folder(
    val path: String,
    val name: String,
    val songs: List<Song>,
)

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
)

enum class SongSort(val label: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    RECENT("Agregadas recientemente"),
    DURATION("Duración"),
}

@Immutable
data class LibraryData(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
    val byId: Map<Long, Song>,
    val albumsById: Map<Long, Album>,
    val hiddenCount: Int,
) {
    fun album(id: Long): Album? = albumsById[id]
    fun artist(name: String): Artist? = artists.firstOrNull { it.name.equals(name, ignoreCase = true) }
    fun folder(path: String): Folder? = folders.firstOrNull { it.path == path }
    fun songsOf(playlist: Playlist): List<Song> = playlist.songIds.mapNotNull { byId[it] }
}

@Immutable
data class SearchResults(
    val query: String,
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()

    companion object {
        val Empty = SearchResults("", emptyList(), emptyList(), emptyList())
    }
}
