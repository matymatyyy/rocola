package com.maty.rocola.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lee las canciones del MediaStore y se actualiza sola cuando cambian los archivos. */
class MusicRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val _songs = MutableStateFlow<List<Song>?>(null)

    /** null mientras no se cargó; lista vacía si no hay música. */
    val songs: StateFlow<List<Song>?> = _songs.asStateFlow()

    @Volatile
    private var byId: Map<Long, Song> = emptyMap()
    @Volatile
    private var observerRegistered = false

    init {
        scope.launch {
            for (request in refreshRequests) {
                val result = runCatching { query() }.getOrElse { emptyList() }
                byId = result.associateBy { it.id }
                _songs.value = result
                delay(600) // agrupa las ráfagas de cambios que manda el escáner de medios
            }
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (!hasPermission()) return
        if (!observerRegistered) {
            observerRegistered = true
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) = refresh()
                },
            )
        }
        refresh()
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    fun song(id: Long): Song? = byId[id]

    suspend fun awaitSongs(): List<Song> {
        start()
        return songs.filterNotNull().first()
    }

    @Suppress("DEPRECATION") // DATA sigue siendo la única forma de obtener la carpeta antes de Android 10
    private fun query(): List<Song> {
        if (!hasPermission()) return emptyList()
        val columns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        if (Build.VERSION.SDK_INT >= 29) columns += MediaStore.Audio.Media.RELATIVE_PATH
        if (Build.VERSION.SDK_INT >= 30) columns += MediaStore.Audio.Media.ALBUM_ARTIST

        val result = ArrayList<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns.toTypedArray(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relCol = if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1
            val albumArtistCol = if (Build.VERSION.SDK_INT >= 30) c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val displayName = c.getString(nameCol).orEmpty()
                val folderPath = when {
                    relCol >= 0 && !c.isNull(relCol) -> c.getString(relCol).trimEnd('/')
                    dataCol >= 0 && !c.isNull(dataCol) -> c.getString(dataCol)
                        .substringBeforeLast('/')
                        .removePrefix("/storage/emulated/0/")
                        .removePrefix("/sdcard/")
                    else -> ""
                }
                val folderName = folderPath.substringAfterLast('/').ifBlank { "Almacenamiento" }
                val rawTrack = if (c.isNull(trackCol)) 0 else c.getInt(trackCol)
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.').ifBlank { "Sin título" }
                val artist = c.getString(artistCol).cleanTag() ?: "Artista desconocido"
                val album = c.getString(albumCol).cleanTag() ?: folderName
                val albumArtist = if (albumArtistCol >= 0) c.getString(albumArtistCol).cleanTag() else null

                result += Song(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = c.getLong(albumIdCol),
                    albumArtist = albumArtist,
                    durationMs = c.getLong(durationCol),
                    track = rawTrack % 1000,
                    disc = rawTrack / 1000,
                    year = if (c.isNull(yearCol)) 0 else c.getInt(yearCol),
                    dateAdded = c.getLong(addedCol),
                    folderPath = folderPath,
                    folderName = folderName,
                    mimeType = c.getString(mimeCol),
                )
            }
        }
        return result
    }

    private fun String?.cleanTag(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != "<unknown>" }

    companion object {
        fun audioPermission(): String =
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
