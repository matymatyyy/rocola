@file:OptIn(ExperimentalMaterial3Api::class)

package com.maty.rocola.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maty.rocola.art.Artwork
import com.maty.rocola.data.Playlist
import com.maty.rocola.data.Song
import com.maty.rocola.playback.SleepTimer
import com.maty.rocola.ui.QueueItem
import com.maty.rocola.ui.QueueState
import com.maty.rocola.ui.components.ArtworkImage
import com.maty.rocola.ui.components.PlayingBars
import com.maty.rocola.ui.components.formatDuration
import com.maty.rocola.ui.components.songCount
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
private fun RocolaSheet(onDismiss: () -> Unit, skipPartial: Boolean = false, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartial),
        containerColor = Rocola.VinoAlto,
        contentColor = Rocola.Marfil,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Rocola.Ceniza) },
    ) {
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = Rocola.Arena,
    textColor: Color = Rocola.Marfil,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(18.dp))
        Text(text, style = RocolaType.itemTitle, color = textColor, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SongActionsSheet(
    song: Song,
    isFavorite: Boolean,
    inPlaylist: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    RocolaSheet(onDismiss) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(song.artwork, song.album, Modifier.size(56.dp), RoundedCornerShape(8.dp), letterSize = 24.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = RocolaType.section.copy(fontSize = 17.sp), color = Rocola.Marfil, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(song.artist, style = RocolaType.itemSub, color = Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = Rocola.VinoClaro, modifier = Modifier.padding(vertical = 8.dp))
        fun andClose(action: () -> Unit): () -> Unit = {
            action()
            onDismiss()
        }
        SheetAction(Icons.Rounded.QueuePlayNext, "Reproducir a continuación", andClose(onPlayNext))
        SheetAction(Icons.Rounded.AddToQueue, "Agregar a la cola", andClose(onAddToQueue))
        SheetAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Agregar a una lista", andClose(onAddToPlaylist))
        SheetAction(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
            andClose(onToggleFavorite),
            tint = if (isFavorite) Rocola.Marfil else Rocola.Arena,
        )
        if (inPlaylist) SheetAction(Icons.Rounded.RemoveCircleOutline, "Quitar de esta lista", andClose(onRemoveFromPlaylist))
        SheetAction(Icons.Rounded.Album, "Ir al álbum", andClose(onGoToAlbum))
        SheetAction(Icons.Rounded.Person, "Ir al artista", andClose(onGoToArtist))
        SheetAction(Icons.Rounded.Share, "Compartir", andClose(onShare))
        SheetAction(Icons.Rounded.Delete, "Borrar del teléfono", andClose(onDelete), tint = Rocola.Error, textColor = Rocola.Error)
    }
}

@Composable
fun PlaylistPickerSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    RocolaSheet(onDismiss) {
        Text("Agregar a una lista", style = RocolaType.section, color = Rocola.Marfil, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        SheetAction(Icons.Rounded.Add, "Nueva lista", {
            onCreate()
            onDismiss()
        }, tint = Rocola.Marfil)
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                SheetAction(Icons.AutoMirrored.Rounded.PlaylistAdd, playlist.name, {
                    onPick(playlist)
                    onDismiss()
                }) {
                    Text(songCount(playlist.songIds.size), style = RocolaType.small, color = Rocola.Ceniza)
                }
            }
        }
    }
}

@Composable
fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focus.requestFocus() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Rocola.VinoAlto,
        title = { Text(title, style = RocolaType.section, color = Rocola.Marfil) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Nombre de la lista", color = Rocola.Ceniza) },
                textStyle = RocolaType.itemTitle.copy(color = Rocola.Marfil),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Rocola.Marfil,
                    unfocusedBorderColor = Rocola.VinoClaro,
                    cursorColor = Rocola.Marfil,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun SleepTimerSheet(state: SleepTimer.State, onDismiss: () -> Unit) {
    RocolaSheet(onDismiss) {
        Text("Temporizador para dormir", style = RocolaType.section, color = Rocola.Marfil, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        val now by produceState(android.os.SystemClock.elapsedRealtime()) {
            while (true) {
                value = android.os.SystemClock.elapsedRealtime()
                delay(1000)
            }
        }
        val status = when (state) {
            is SleepTimer.State.Countdown -> "La música se va a pausar en ${formatDuration((state.endsAtElapsed - now).coerceAtLeast(0L))}, bajando el volumen de a poco."
            SleepTimer.State.EndOfTrack -> "La música se va a pausar cuando termine la canción actual."
            SleepTimer.State.Off -> "Elegí cuándo pausar la música. Funciona aunque apagues la pantalla."
        }
        Text(status, style = RocolaType.itemSub, color = Rocola.Arena, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        val options = listOf(15 to "15 minutos", 30 to "30 minutos", 45 to "45 minutos", 60 to "1 hora", 90 to "1 hora y media")
        options.forEach { (minutes, label) ->
            SheetAction(Icons.Rounded.Check, label, {
                SleepTimer.start(minutes)
                onDismiss()
            }, tint = Color.Transparent)
        }
        SheetAction(Icons.Rounded.Check, "Al terminar esta canción", {
            SleepTimer.endOfTrack()
            onDismiss()
        }, tint = if (state == SleepTimer.State.EndOfTrack) Rocola.Marfil else Color.Transparent)
        if (state != SleepTimer.State.Off) {
            SheetAction(Icons.Rounded.Delete, "Desactivar temporizador", {
                SleepTimer.cancel()
                onDismiss()
            }, tint = Rocola.Arena)
        }
    }
}

@Composable
fun SpeedSheet(current: Float, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    RocolaSheet(onDismiss) {
        Text("Velocidad", style = RocolaType.section, color = Rocola.Marfil, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
            val label = if (speed == 1f) "Normal" else "${formatSpeed(speed)}×"
            SheetAction(Icons.Rounded.Check, label, {
                onPick(speed)
                onDismiss()
            }, tint = if (speed == current) Rocola.Marfil else Color.Transparent)
        }
    }
}

@Composable
fun QueueSheet(
    queue: QueueState,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlay: (QueueItem) -> Unit,
    onRemove: (QueueItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onClearUpcoming: () -> Unit,
) {
    RocolaSheet(onDismiss, skipPartial = true) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cola", style = RocolaType.headline.copy(fontSize = 24.sp), color = Rocola.Marfil)
                val upcoming = (queue.items.size - queue.currentPos - 1).coerceAtLeast(0)
                Text(
                    if (upcoming == 0) "No hay más canciones después de esta" else "${songCount(upcoming)} después de esta",
                    style = RocolaType.itemSub,
                    color = Rocola.Arena,
                )
            }
            if (queue.currentPos < queue.items.lastIndex) {
                TextButton(onClick = onClearUpcoming) { Text("Vaciar lo que sigue", style = RocolaType.label, color = Rocola.Marfil) }
            }
        }
        if (queue.shuffle) {
            Text(
                "Con el modo aleatorio activado se ve el orden en que van a sonar; para reordenar, desactivalo.",
                style = RocolaType.small,
                color = Rocola.Ceniza,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        // Mientras se arrastra, el orden vive solo acá; al soltar se manda un único movimiento.
        var items by remember { mutableStateOf(queue.items) }
        var dragging by remember { mutableStateOf(false) }
        var dragFrom by remember { mutableStateOf(-1) }
        LaunchedEffect(queue.items) { if (!dragging) items = queue.items }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (queue.currentPos - 1).coerceAtLeast(0))
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            items = items.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            itemsIndexed(items, key = { _, item -> item.key }) { position, item ->
                ReorderableItem(reorderState, key = item.key, enabled = !queue.shuffle) { isDragging ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                onRemove(item)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Rocola.VinoClaro)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text("Quitar", style = RocolaType.label, color = Rocola.Marfil)
                            }
                        },
                    ) {
                        val background by animateColorAsState(
                            if (isDragging) Rocola.VinoClaro else Rocola.VinoAlto,
                            label = "dragBg",
                        )
                        val isCurrent = position == queue.currentPos
                        val isPast = position < queue.currentPos
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(background)
                                .clickable { onPlay(item) }
                                .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                ArtworkImage(Artwork(item.albumId, item.songUri), item.album.ifEmpty { item.title }, Modifier.size(44.dp), RoundedCornerShape(6.dp), letterSize = 18.sp)
                                if (isCurrent) {
                                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.5f)))
                                    PlayingBars(isPlaying)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = RocolaType.itemTitle,
                                    color = when {
                                        isCurrent -> Rocola.Ambar
                                        isPast -> Rocola.Ceniza
                                        else -> Rocola.Marfil
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(item.artist, style = RocolaType.itemSub, color = if (isPast) Rocola.Ceniza else Rocola.Arena, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!queue.shuffle) {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            dragging = true
                                            dragFrom = items.indexOfFirst { it.key == item.key }
                                        },
                                        onDragStopped = {
                                            dragging = false
                                            val to = items.indexOfFirst { it.key == item.key }
                                            if (dragFrom >= 0 && to >= 0 && to != dragFrom) onMove(dragFrom, to)
                                            dragFrom = -1
                                        },
                                    ),
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Arrastrar para reordenar", tint = Rocola.Ceniza)
                                }
                            } else {
                                Text(formatDuration(item.durationMs), style = RocolaType.numeral, color = Rocola.Ceniza, modifier = Modifier.padding(end = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
