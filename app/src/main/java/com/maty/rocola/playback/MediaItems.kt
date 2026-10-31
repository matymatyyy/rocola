package com.maty.rocola.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.maty.rocola.art.ArtworkLoader
import com.maty.rocola.data.Song

const val EXTRA_ALBUM_ID = "rocola.albumId"
const val EXTRA_DURATION = "rocola.duration"

/** Comandos propios entre la interfaz (o la notificación) y el servicio. */
const val CMD_TOGGLE_FAVORITE = "rocola.TOGGLE_FAVORITE"
const val CMD_SHUFFLE_FROM_CURRENT = "rocola.SHUFFLE_FROM_CURRENT"
const val CMD_PLACE_NEXT = "rocola.PLACE_NEXT"
const val ARG_IDS = "ids"

fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_ALBUM_ID, albumId)
        putLong(EXTRA_DURATION, durationMs)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(albumArtist ?: artist)
        .setTrackNumber(track.takeIf { it > 0 })
        .setReleaseYear(year.takeIf { it > 0 })
        .setArtworkUri(ArtworkLoader.albumUri(albumId))
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(metadata)
        .build()
}
