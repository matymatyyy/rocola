package com.maty.rocola.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.data.LibraryData
import com.maty.rocola.data.Playlist
import com.maty.rocola.data.Song
import com.maty.rocola.ui.AppController
import com.maty.rocola.ui.PlayerUiState
import com.maty.rocola.ui.SongMenuRequest
import com.maty.rocola.ui.components.ArtworkImage
import com.maty.rocola.ui.components.DetailTopBar
import com.maty.rocola.ui.components.EmptyMessage
import com.maty.rocola.ui.components.PlayShuffleRow
import com.maty.rocola.ui.components.SectionTitle
import com.maty.rocola.ui.components.SongLeading
import com.maty.rocola.ui.components.SongRow
import com.maty.rocola.ui.components.albumCount
import com.maty.rocola.ui.components.formatTotal
import com.maty.rocola.ui.components.songCount
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType

@Composable
private fun NotFound(app: AppController, what: String) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(onBack = { app.back() })
        EmptyMessage(title = "No encontramos $what", body = "Puede que el archivo se haya movido o borrado.")
    }
}

@Composable
private fun DetailHeader(
    title: String,
    meta: String,
    subtitle: String? = null,
    onSubtitle: (() -> Unit)? = null,
    art: (@Composable () -> Unit)? = null,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    enabled: Boolean,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (art != null) {
            art()
            Spacer(Modifier.height(22.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = RocolaType.headline, color = Rocola.Marfil)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = RocolaType.label.copy(fontSize = 16.sp),
                color = Rocola.Arena,
                modifier = if (onSubtitle != null) Modifier.clickable(onClick = onSubtitle) else Modifier,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(meta, style = RocolaType.itemSub, color = Rocola.Ceniza)
        Spacer(Modifier.height(20.dp))
        PlayShuffleRow(onPlay = onPlay, onShuffle = onShuffle, enabled = enabled)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun AlbumScreen(app: AppController, library: LibraryData, albumId: Long, player: PlayerUiState, bottomPadding: Dp) {
    val album = library.album(albumId) ?: return NotFound(app, "este álbum")
    val meta = listOfNotNull(
        album.year.takeIf { it > 0 }?.toString(),
        songCount(album.songs.size),
        formatTotal(album.durationMs),
    ).joinToString(", ")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item(key = "top") { DetailTopBar(onBack = { app.back() }) }
        item(key = "header") {
            DetailHeader(
                title = album.title,
                subtitle = album.artist,
                onSubtitle = { app.openArtist(album.artist) },
                meta = meta,
                art = {
                    ArtworkImage(album.artwork, album.title, Modifier.size(224.dp), RoundedCornerShape(14.dp), letterSize = 88.sp)
                },
                onPlay = { app.playInOrder(album.songs, album.title) },
                onShuffle = { app.shuffle(album.songs, album.title) },
                enabled = album.songs.isNotEmpty(),
            )
        }
        itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == player.current?.songId,
                isPlaying = player.isPlaying,
                onClick = { app.play(album.songs, index, album.title) },
                onMore = { app.songMenu = SongMenuRequest(song) },
                leading = SongLeading.Number(song.track.takeIf { it > 0 } ?: (index + 1)),
                subtitle = song.artist.takeIf { !it.equals(album.artist, ignoreCase = true) },
            )
        }
    }
}

@Composable
fun ArtistScreen(app: AppController, library: LibraryData, name: String, player: PlayerUiState, bottomPadding: Dp) {
    val artist = library.artist(name) ?: return NotFound(app, "este artista")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item(key = "top") { DetailTopBar(onBack = { app.back() }) }
        item(key = "header") {
            DetailHeader(
                title = artist.name,
                meta = "${albumCount(artist.albums.size)}, ${songCount(artist.songs.size)}",
                art = {
                    ArtworkImage(artist.artwork, artist.name, Modifier.size(132.dp), CircleShape, letterSize = 56.sp)
                },
                onPlay = { app.playInOrder(artist.songs, artist.name) },
                onShuffle = { app.shuffle(artist.songs, artist.name) },
                enabled = artist.songs.isNotEmpty(),
            )
        }
        if (artist.albums.isNotEmpty()) {
            item(key = "albums-title") { SectionTitle("Álbumes") }
            item(key = "albums") {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp)) {
                    items(artist.albums, key = { it.id }) { album ->
                        AlbumTile(album, onClick = { app.openAlbum(album.id) }, modifier = Modifier.width(148.dp))
                        Spacer(Modifier.width(14.dp))
                    }
                }
            }
            item(key = "songs-title") { SectionTitle("Canciones", Modifier.padding(top = 12.dp)) }
        }
        itemsIndexed(artist.songs, key = { _, s -> s.id }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == player.current?.songId,
                isPlaying = player.isPlaying,
                onClick = { app.play(artist.songs, index, artist.name) },
                onMore = { app.songMenu = SongMenuRequest(song) },
                subtitle = song.album,
            )
        }
    }
}

@Composable
fun FolderScreen(app: AppController, library: LibraryData, path: String, player: PlayerUiState, bottomPadding: Dp) {
    val folder = library.folder(path) ?: return NotFound(app, "esta carpeta")
    SongListScreen(
        app = app,
        title = folder.name,
        meta = "${songCount(folder.songs.size)} en ${folder.path.ifEmpty { "la raíz" }}",
        songs = folder.songs,
        player = player,
        bottomPadding = bottomPadding,
        label = folder.name,
    )
}

@Composable
fun FavoritesScreen(app: AppController, library: LibraryData, favorites: Set<Long>, player: PlayerUiState, bottomPadding: Dp) {
    val songs = library.songs.filter { it.id in favorites }
    SongListScreen(
        app = app,
        title = "Favoritas",
        meta = "${songCount(songs.size)}, ${formatTotal(songs.sumOf { it.durationMs })}",
        songs = songs,
        player = player,
        bottomPadding = bottomPadding,
        label = "Favoritas",
        empty = "Tocá el corazón en el reproductor o en el menú de una canción para guardarla acá.",
    )
}

@Composable
fun PlaylistScreen(
    app: AppController,
    library: LibraryData,
    playlists: List<Playlist>,
    playlistId: Long,
    player: PlayerUiState,
    bottomPadding: Dp,
) {
    val playlist = playlists.firstOrNull { it.id == playlistId } ?: return NotFound(app, "esta lista")
    val songs = library.songsOf(playlist)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    SongListScreen(
        app = app,
        title = playlist.name,
        meta = "${songCount(songs.size)}, ${formatTotal(songs.sumOf { it.durationMs })}",
        songs = songs,
        player = player,
        bottomPadding = bottomPadding,
        label = playlist.name,
        playlistId = playlist.id,
        empty = "Agregá canciones desde el menú de cualquier canción con \"Agregar a una lista\".",
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones de la lista", tint = Rocola.Marfil)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Cambiar nombre", style = RocolaType.itemTitle) },
                        onClick = {
                            menuOpen = false
                            app.renamePlaylistId = playlist.id
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar lista", style = RocolaType.itemTitle) },
                        onClick = {
                            menuOpen = false
                            confirmDelete = true
                        },
                    )
                }
            }
        },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar \"${playlist.name}\"", style = RocolaType.section) },
            text = { Text("Se borra la lista, no las canciones.", style = RocolaType.body) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    app.back()
                    app.library.deletePlaylist(playlist.id)
                }) { Text("Eliminar lista") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
            containerColor = Rocola.VinoAlto,
        )
    }
}

@Composable
private fun SongListScreen(
    app: AppController,
    title: String,
    meta: String,
    songs: List<Song>,
    player: PlayerUiState,
    bottomPadding: Dp,
    label: String,
    playlistId: Long? = null,
    empty: String? = null,
    actions: @Composable () -> Unit = {},
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item(key = "top") { DetailTopBar(onBack = { app.back() }, actions = actions) }
        item(key = "header") {
            DetailHeader(
                title = title,
                meta = meta,
                onPlay = { app.playInOrder(songs, label) },
                onShuffle = { app.shuffle(songs, label) },
                enabled = songs.isNotEmpty(),
            )
        }
        if (songs.isEmpty() && empty != null) {
            item(key = "empty") {
                Text(empty, style = RocolaType.body, color = Rocola.Arena, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            }
        }
        itemsIndexed(songs, key = { i, s -> "${s.id}-$i" }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == player.current?.songId,
                isPlaying = player.isPlaying,
                onClick = { app.play(songs, index, label) },
                onMore = { app.songMenu = SongMenuRequest(song, playlistId) },
            )
        }
    }
}
