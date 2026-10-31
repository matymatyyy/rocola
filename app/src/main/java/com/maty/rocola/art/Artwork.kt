package com.maty.rocola.art

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.runtime.Immutable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/** Tapa de un álbum. songUri sirve de respaldo cuando el álbum no tiene tapa propia. */
@Immutable
data class Artwork(val albumId: Long, val songUri: Uri? = null)

object ArtworkLoader {

    private val albumsWithoutArt: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun albumUri(albumId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)

    fun load(context: Context, albumId: Long, songUri: Uri?, sizePx: Int): Bitmap? {
        val size = sizePx.coerceIn(64, 1600)
        val cr = context.contentResolver
        if (albumId >= 0 && albumId !in albumsWithoutArt) {
            val fromAlbum: Bitmap? = if (Build.VERSION.SDK_INT >= 29) {
                runCatching { cr.loadThumbnail(albumUri(albumId), Size(size, size), null) }.getOrNull()
            } else {
                runCatching {
                    val legacy = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                    cr.openInputStream(legacy)?.use { decodeSampled(it.readBytes(), size) }
                }.getOrNull()
            }
            if (fromAlbum != null) return fromAlbum.software()
            albumsWithoutArt += albumId
        }
        if (songUri != null) {
            if (Build.VERSION.SDK_INT >= 29) {
                runCatching { cr.loadThumbnail(songUri, Size(size, size), null) }.getOrNull()?.let { return it.software() }
            }
            embedded(context, songUri, size)?.let { return it }
        }
        return null
    }

    private fun embedded(context: Context, uri: Uri, size: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { decodeSampled(it, size) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, size: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun Bitmap.software(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
}

class ArtworkFetcher(
    private val context: Context,
    private val data: Artwork,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val width = options.size.width.pxOrElse { 512 }
        val height = options.size.height.pxOrElse { 512 }
        val bitmap = withContext(Dispatchers.IO) {
            ArtworkLoader.load(context, data.albumId, data.songUri, maxOf(width, height))
        } ?: throw FileNotFoundException("Sin tapa para el álbum ${data.albumId}")
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Artwork> {
        override fun create(data: Artwork, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(context.applicationContext, data, options)
    }
}

class ArtworkKeyer : Keyer<Artwork> {
    override fun key(data: Artwork, options: Options): String =
        "art:${data.albumId}:${data.songUri?.lastPathSegment.orEmpty()}"
}
