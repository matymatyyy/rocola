package com.maty.rocola.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.maty.rocola.data.Song
import com.maty.rocola.ui.AppController
import com.maty.rocola.ui.PlayerUiState
import com.maty.rocola.ui.SongMenuRequest
import com.maty.rocola.ui.components.SongLeading
import com.maty.rocola.ui.components.SongRow
import com.maty.rocola.ui.theme.Rocola
import com.maty.rocola.ui.theme.RocolaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fila de canción que entiende la selección: un toque largo empieza a elegir y,
 * mientras se elige, cada toque marca o desmarca en vez de reproducir.
 */
@Composable
fun SelectableSongRow(
    app: AppController,
    song: Song,
    list: List<Song>,
    index: Int,
    player: PlayerUiState,
    label: String?,
    modifier: Modifier = Modifier,
    leading: SongLeading = SongLeading.Art,
    subtitle: String? = song.artist,
    playlistId: Long? = null,
) {
    val selection = app.selection
    val haptics = LocalHapticFeedback.current
    SongRow(
        song = song,
        isCurrent = song.id == player.current?.songId,
        isPlaying = player.isPlaying,
        onClick = { if (selection.active) selection.toggle(song) else app.play(list, index, label) },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (selection.active) selection.toggle(song) else selection.start(song, list)
        },
        onMore = { app.songMenu = SongMenuRequest(song, playlistId) },
        modifier = modifier,
        leading = leading,
        subtitle = subtitle,
        selectionMode = selection.active,
        selected = song.id in selection.ids,
    )
}

/** Barra de arriba mientras se elige: cuántas hay, cancelar y elegir todas. */
@Composable
fun SelectionTopBar(count: Int, total: Int, onClose: () -> Unit, onSelectAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Rocola.Vino)
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancelar la selección", tint = Rocola.Marfil)
        }
        Text(
            if (count == 1) "1 seleccionada" else "$count seleccionadas",
            style = RocolaType.section,
            color = Rocola.Marfil,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        if (count < total) {
            TextButton(onClick = onSelectAll) {
                Text("Seleccionar todo", style = RocolaType.label, color = Rocola.Marfil)
            }
        }
    }
}

/** Barra de acciones que reemplaza al reproductor chico mientras se elige. */
@Composable
fun SelectionActionBar(
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(18.dp))
            .background(Rocola.VinoClaro)
            .height(66.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionAction(Icons.Rounded.PlayArrow, "Reproducir", onPlay, Modifier.weight(1f))
        SelectionAction(Icons.Rounded.AddToQueue, "A la cola", onQueue, Modifier.weight(1f))
        SelectionAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "A una lista", onAddToPlaylist, Modifier.weight(1f))
        SelectionAction(Icons.Rounded.Delete, "Borrar", onDelete, Modifier.weight(1f), tint = Rocola.Error)
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Rocola.Marfil,
) {
    Column(
        modifier
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = RocolaType.small.copy(fontSize = 12.sp), color = tint, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/**
 * Borra del teléfono las canciones pedidas en [AppController.deleteRequest].
 * Desde Android 11 confirma el sistema con su propio cartel; antes, la app pregunta
 * y pide el permiso de escritura.
 */
@Composable
fun DeleteHost(app: AppController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val systemConfirm = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val songs = app.deleteRequest.orEmpty()
        if (result.resultCode == Activity.RESULT_OK) app.onSongsDeleted(songs) else app.onDeleteCancelled()
    }

    fun deleteLegacy(songs: List<Song>) {
        scope.launch {
            val deleted = withContext(Dispatchers.IO) { deleteFiles(context, songs) }
            app.onSongsDeleted(deleted)
            if (deleted.size < songs.size) app.toast("No se pudieron borrar ${songs.size - deleted.size} canciones")
        }
    }

    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val songs = app.deleteRequest.orEmpty()
        if (granted) {
            deleteLegacy(songs)
        } else {
            app.onDeleteCancelled()
            app.toast("Sin el permiso de almacenamiento no se pueden borrar archivos")
        }
    }

    val request = app.deleteRequest ?: return
    if (Build.VERSION.SDK_INT >= 30) {
        LaunchedEffect(request) {
            runCatching {
                val pending = MediaStore.createDeleteRequest(context.contentResolver, request.map { it.uri })
                systemConfirm.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            }.onFailure {
                app.onDeleteCancelled()
                app.toast("No se pudo pedir el borrado")
            }
        }
    } else {
        val count = request.size
        AlertDialog(
            onDismissRequest = { app.onDeleteCancelled() },
            containerColor = Rocola.VinoAlto,
            title = {
                Text(
                    if (count == 1) "¿Borrar \"${request.first().title}\"?" else "¿Borrar $count canciones?",
                    style = RocolaType.section,
                    color = Rocola.Marfil,
                )
            },
            text = { Text("Se borran los archivos del teléfono. No se puede deshacer.", style = RocolaType.body, color = Rocola.Arena) },
            confirmButton = {
                TextButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) deleteLegacy(request) else writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }) { Text("Borrar", color = Rocola.Error) }
            },
            dismissButton = { TextButton(onClick = { app.onDeleteCancelled() }) { Text("Cancelar") } },
        )
    }
}

/** Android 10 o anterior: borra cada archivo y su registro en el MediaStore. Devuelve las que se borraron. */
@Suppress("DEPRECATION")
private fun deleteFiles(context: Context, songs: List<Song>): List<Song> = songs.filter { song ->
    runCatching {
        val path = context.contentResolver.query(song.uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        val rows = context.contentResolver.delete(song.uri, null, null)
        val file = path?.let { File(it) }
        if (file != null && file.exists()) file.delete()
        rows > 0 || (file != null && !file.exists())
    }.getOrDefault(false)
}
