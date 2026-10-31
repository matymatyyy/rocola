package com.maty.rocola.ui.components

import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** "48 min" o "1 h 12 min" */
fun formatTotal(ms: Long): String {
    val minutes = (ms / 60_000L).coerceAtLeast(if (ms > 0) 1 else 0)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

fun songCount(n: Int) = plural(n, "canción", "canciones")
fun albumCount(n: Int) = plural(n, "álbum", "álbumes")
