package com.maty.rocola.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.data.Album
import com.maty.rocola.data.LibraryData
import com.maty.rocola.data.LibraryIndex
import com.maty.rocola.data.Playlist
import com.maty.rocola.data.SongSort
import com.maty.rocola.ui.AppController
import com.maty.rocola.ui.PlayerUiState
import com.maty.rocola.ui.SongMenuRequest
import com.maty.rocola.ui.components.ArtworkImage
import com.maty.rocola.ui.components.FastScroller
import com.maty.rocola.ui.components.CircleArtRow
import com.maty.rocola.ui.components.EmptyMessage
import com.maty.rocola.ui.components.IconTileRow
import com.maty.rocola.ui.components.PillButton
import com.maty.rocola.ui.components.SongRow
import com.maty.rocola.ui.components.albumCount
import com.maty.rocola.ui.components.songCount
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import kotlinx.coroutines.launch

private val tabs = listOf("Canciones", "Álbumes", "Artistas", "Carpetas", "Listas")

@Composable
fun LibraryScreen(
    app: AppController,
    library: LibraryData?,
    player: PlayerUiState,
    sort: SongSort,
    favorites: Set<Long>,
    playlists: List<Playlist>,
    bottomPadding: Dp,
) {
    val pager = rememberPagerState(initialPage = app.nav.libraryPage) { tabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { app.nav.libraryPage = it }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 6.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Rocola", style = RocolaType.wordmark, color = Rocola.Marfil, modifier = Modifier.weight(1f))
            IconButton(onClick = app::openSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = Rocola.Marfil)
            }
            IconButton(onClick = app::openSettings) {
                Icon(Icons.Rounded.Tune, contentDescription = "Ajustes", tint = Rocola.Marfil)
            }
        }
        TabStrip(selected = pager.currentPage, onSelect = { scope.launch { pager.animateScrollToPage(it) } })
        when {
            library == null -> Box(Modifier.fillMaxSize())
            library.songs.isEmpty() && library.hiddenCount == 0 -> EmptyMessage(
                title = "Todavía no hay música",
                body = "Copiá archivos de audio (MP3, FLAC, M4A u OGG) a la carpeta Música del teléfono y tocá Buscar de nuevo.",
            ) {
                PillButton("Buscar de nuevo", null, filled = true, onClick = { app.library.refresh() })
            }
            else -> HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> SongsTab(app, library, player, sort, bottomPadding)
                    1 -> AlbumsTab(app, library.albums, bottomPadding)
                    2 -> ArtistsTab(app, library, bottomPadding)
                    3 -> FoldersTab(app, library, bottomPadding)
                    else -> PlaylistsTab(app, library, favorites, playlists, bottomPadding)
                }
            }
        }
    }
}

@Composable
private fun TabStrip(selected: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { listState.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        itemsIndexed(tabs) { index, label ->
            val active = index == selected
            val color by animateColorAsState(if (active) Rocola.Marfil else Rocola.Ceniza, tween(200), label = "tab")
            val bar by animateColorAsState(if (active) Rocola.Marfil else Rocola.Vino, tween(200), label = "bar")
            Column(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "Ver $label") { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 10.dp)
                    .width(IntrinsicSize.Max),
            ) {
                Text(label, style = RocolaType.tab, color = color)
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(bar),
                )
            }
        }
    }
}

@Composable
private fun SongsTab(app: AppController, library: LibraryData, player: PlayerUiState, sort: SongSort, bottomPadding: Dp) {
    val songs = library.songs
    var sortOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomPadding)) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    text = "Mezclar ${songCount(songs.size)}",
                    icon = Icons.Rounded.Shuffle,
                    filled = true,
                    enabled = songs.isNotEmpty(),
                    onClick = { app.shuffle(songs, "Toda tu música") },
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { sortOpen = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Ordenar por ${sort.label}", tint = Rocola.Marfil)
                    }
                    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                        SongSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, style = RocolaType.itemTitle) },
                                trailingIcon = {
                                    if (option == sort) Icon(Icons.Rounded.Check, contentDescription = "Seleccionado", tint = Rocola.Marfil)
                                },
                                onClick = {
                                    sortOpen = false
                                    app.library.setSort(option)
                                },
                            )
                        }
                    }
                }
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == player.current?.songId,
                isPlaying = player.isPlaying,
                onClick = { app.play(songs, index, "Toda tu música") },
                onMore = { app.songMenu = SongMenuRequest(song) },
            )
        }
        if (library.hiddenCount > 0) {
            item(key = "hidden") {
                Text(
                    if (library.hiddenCount == 1) "1 audio de menos de 30 segundos está oculto. Podés mostrarlo en Ajustes."
                    else "${library.hiddenCount} audios de menos de 30 segundos están ocultos. Podés mostrarlos en Ajustes.",
                    style = RocolaType.small,
                    color = Rocola.Ceniza,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }
    FastScroller(
        listState = listState,
        itemCount = songs.size,
        firstItemOffset = 1,
        labelFor = { index ->
            val song = songs.getOrNull(index)
            val source = when (sort) {
                SongSort.TITLE -> song?.title
                SongSort.ARTIST -> song?.artist
                SongSort.ALBUM -> song?.album
                else -> null
            }
            source?.let { LibraryIndex.normalize(it).firstOrNull { c -> c.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#" }
        },
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, bottom = bottomPadding),
    )
    }
}

@Composable
private fun AlbumsTab(app: AppController, albums: List<Album>, bottomPadding: Dp) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumTile(album, onClick = { app.openAlbum(album.id) })
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
fun AlbumTile(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        ArtworkImage(
            artwork = album.artwork,
            seed = album.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(12.dp),
            letterSize = 56.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(album.title, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArtistsTab(app: AppController, library: LibraryData, bottomPadding: Dp) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding)) {
        items(library.artists, key = { it.name }) { artist ->
            CircleArtRow(
                title = artist.name,
                subtitle = "${albumCount(artist.albums.size)}, ${songCount(artist.songs.size)}",
                artworkContent = {
                    ArtworkImage(artist.artwork, artist.name, Modifier.fillMaxSize(), letterSize = 22.sp)
                },
                onClick = { app.openArtist(artist.name) },
            )
        }
    }
}

@Composable
private fun FoldersTab(app: AppController, library: LibraryData, bottomPadding: Dp) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding)) {
        items(library.folders, key = { it.path }) { folder ->
            IconTileRow(
                icon = Icons.Rounded.Folder,
                title = folder.name,
                subtitle = "${songCount(folder.songs.size)} en ${folder.path.ifEmpty { "la raíz" }}",
                onClick = { app.openFolder(folder.path) },
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    app: AppController,
    library: LibraryData,
    favorites: Set<Long>,
    playlists: List<Playlist>,
    bottomPadding: Dp,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding)) {
        item(key = "new") {
            IconTileRow(
                icon = Icons.Rounded.Add,
                title = "Nueva lista",
                subtitle = null,
                iconTint = Rocola.Marfil,
                onClick = { app.createPlaylistWith = emptyList() },
            )
        }
        item(key = "favorites") {
            val count = favorites.count { it in library.byId }
            IconTileRow(
                icon = Icons.Rounded.Favorite,
                title = "Favoritas",
                subtitle = songCount(count),
                iconTint = Rocola.Marfil,
                onClick = app::openFavorites,
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            val songs = library.songsOf(playlist)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { app.openPlaylist(playlist.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaylistMosaic(songs.map { it.albumId }.distinct().take(4), playlist.name, Modifier.size(48.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, style = RocolaType.itemTitle, color = Rocola.Marfil, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(songCount(songs.size), style = RocolaType.itemSub, color = Rocola.Arena)
                }
            }
        }
    }
}

/** Mosaico de hasta cuatro tapas para identificar una lista. */
@Composable
fun PlaylistMosaic(albumIds: List<Long>, seed: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Rocola.VinoAlto)) {
        if (albumIds.size < 4) {
            ArtworkImage(
                artwork = albumIds.firstOrNull()?.let { com.maty.rocola.art.Artwork(it) },
                seed = seed,
                modifier = Modifier.fillMaxSize(),
                letterSize = 22.sp,
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    ArtworkImage(com.maty.rocola.art.Artwork(albumIds[0]), seed, Modifier.weight(1f).fillMaxSize(), letterSize = 10.sp)
                    ArtworkImage(com.maty.rocola.art.Artwork(albumIds[1]), seed + "1", Modifier.weight(1f).fillMaxSize(), letterSize = 10.sp)
                }
                Row(Modifier.weight(1f)) {
                    ArtworkImage(com.maty.rocola.art.Artwork(albumIds[2]), seed + "2", Modifier.weight(1f).fillMaxSize(), letterSize = 10.sp)
                    ArtworkImage(com.maty.rocola.art.Artwork(albumIds[3]), seed + "3", Modifier.weight(1f).fillMaxSize(), letterSize = 10.sp)
                }
            }
        }
    }
}
