package com.maty.rocola.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Arma álbumes, artistas y carpetas a partir de la lista plana de canciones. */
object LibraryIndex {

    const val SHORT_AUDIO_MS = 30_000L
    private val locale: Locale = Locale.forLanguageTag("es-AR")
    private val diacritics = Regex("\\p{Mn}+")

    private fun newCollator(): Collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

    fun build(all: List<Song>, sort: SongSort, hideShort: Boolean): LibraryData {
        val collator = newCollator()
        val visible = if (hideShort) all.filter { it.durationMs >= SHORT_AUDIO_MS } else all

        val albums = visible.groupBy { it.albumId }.map { (albumId, songs) ->
            val ordered = songs.sortedWith(
                compareBy<Song>({ it.disc }, { if (it.track > 0) it.track else Int.MAX_VALUE })
                    .thenComparator { a, b -> collator.compare(a.title, b.title) },
            )
            val artist = songs.firstNotNullOfOrNull { it.albumArtist }
                ?: songs.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key
                ?: "Artista desconocido"
            Album(
                id = albumId,
                title = songs.first().album,
                artist = artist,
                year = songs.maxOf { it.year },
                songs = ordered,
            )
        }.sortedWith { a, b -> collator.compare(a.title, b.title) }
        val albumsById = albums.associateBy { it.id }

        val artists = visible.groupBy { it.artist.lowercase(locale) }.map { (_, songs) ->
            val name = songs.first().artist
            val artistAlbums = songs.map { it.albumId }.distinct()
                .mapNotNull { albumsById[it] }
                .sortedByDescending { it.year }
            Artist(
                name = name,
                albums = artistAlbums,
                songs = songs.sortedWith { a, b -> collator.compare(a.title, b.title) },
            )
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }

        val folders = visible.groupBy { it.folderPath }.map { (path, songs) ->
            Folder(
                path = path,
                name = songs.first().folderName,
                songs = songs.sortedWith { a, b -> collator.compare(a.title, b.title) },
            )
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }

        return LibraryData(
            songs = sortSongs(visible, sort, collator),
            albums = albums,
            artists = artists,
            folders = folders,
            byId = all.associateBy { it.id },
            albumsById = albumsById,
            hiddenCount = all.size - visible.size,
        )
    }

    private fun sortSongs(songs: List<Song>, sort: SongSort, collator: Collator): List<Song> = when (sort) {
        SongSort.TITLE -> songs.sortedWith { a, b -> collator.compare(a.title, b.title) }
        SongSort.ARTIST -> songs.sortedWith { a, b ->
            collator.compare(a.artist, b.artist).takeIf { it != 0 } ?: collator.compare(a.title, b.title)
        }
        SongSort.ALBUM -> songs.sortedWith { a, b ->
            collator.compare(a.album, b.album).takeIf { it != 0 }
                ?: (a.disc * 1000 + a.track).compareTo(b.disc * 1000 + b.track)
        }
        SongSort.RECENT -> songs.sortedByDescending { it.dateAdded }
        SongSort.DURATION -> songs.sortedByDescending { it.durationMs }
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(diacritics, "").lowercase(locale)

    fun search(lib: LibraryData, rawQuery: String): SearchResults {
        val query = normalize(rawQuery.trim())
        if (query.isEmpty()) return SearchResults.Empty
        val terms = query.split(' ').filter { it.isNotBlank() }
        fun matches(vararg fields: String): Boolean {
            val haystack = fields.joinToString(" ") { normalize(it) }
            return terms.all { haystack.contains(it) }
        }
        return SearchResults(
            query = rawQuery,
            songs = lib.songs.filter { matches(it.title, it.artist, it.album) }.take(60),
            albums = lib.albums.filter { matches(it.title, it.artist) }.take(20),
            artists = lib.artists.filter { matches(it.name) }.take(20),
        )
    }
}
