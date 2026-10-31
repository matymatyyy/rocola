package com.maty.rocola.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maty.rocola.ui.AppController
import com.maty.rocola.ui.PlayerUiState
import com.maty.rocola.ui.SongMenuRequest
import com.maty.rocola.ui.components.ArtworkImage
import com.maty.rocola.ui.components.CircleArtRow
import com.maty.rocola.ui.components.SectionTitle
import com.maty.rocola.ui.components.SongRow
import com.maty.rocola.ui.components.albumCount
import com.maty.rocola.ui.components.songCount
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType

@Composable
fun SearchScreen(app: AppController, player: PlayerUiState, bottomPadding: Dp) {
    val query by app.library.query.collectAsStateWithLifecycle()
    val results by app.library.searchResults.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { app.back() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Rocola.Marfil)
            }
            Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Canciones, álbumes o artistas", style = RocolaType.section.copy(fontSize = 17.sp), color = Rocola.Ceniza, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { app.library.query.value = it },
                    singleLine = true,
                    textStyle = RocolaType.section.copy(fontSize = 17.sp, color = Rocola.Marfil),
                    cursorBrush = SolidColor(Rocola.Marfil),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { app.library.query.value = "" }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Borrar búsqueda", tint = Rocola.Arena)
                }
            }
        }
        val searched = query.isNotBlank() && results.query == query
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
            if (searched && results.isEmpty) {
                item {
                    Text(
                        "No encontramos nada con \"$query\". Probá con otra palabra o revisá cómo está escrito.",
                        style = RocolaType.body,
                        color = Rocola.Arena,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            if (results.artists.isNotEmpty()) {
                item(key = "artists-title") { SectionTitle("Artistas") }
                items(results.artists, key = { "artist-" + it.name }) { artist ->
                    CircleArtRow(
                        title = artist.name,
                        subtitle = "${albumCount(artist.albums.size)}, ${songCount(artist.songs.size)}",
                        artworkContent = { ArtworkImage(artist.artwork, artist.name, Modifier.fillMaxSize(), letterSize = 22.sp) },
                        onClick = { app.openArtist(artist.name) },
                    )
                }
            }
            if (results.albums.isNotEmpty()) {
                item(key = "albums-title") { SectionTitle("Álbumes") }
                items(results.albums, key = { "album-" + it.id }) { album ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { app.openAlbum(album.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkImage(album.artwork, album.title, Modifier.size(52.dp), RoundedCornerShape(8.dp), letterSize = 22.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(album.title, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(album.artist, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (results.songs.isNotEmpty()) {
                item(key = "songs-title") { SectionTitle("Canciones") }
                itemsIndexed(results.songs, key = { _, s -> "song-" + s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == player.current?.songId,
                        isPlaying = player.isPlaying,
                        onClick = { app.play(results.songs, index, "Búsqueda: $query") },
                        onMore = { app.songMenu = SongMenuRequest(song) },
                    )
                }
            }
        }
    }
}
