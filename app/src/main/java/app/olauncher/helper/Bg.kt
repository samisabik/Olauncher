package app.olauncher.helper

import app.olauncher.data.Prefs

object BgUpdates {
    @Volatile
    private var listener: (() -> Unit)? = null

    fun set(l: (() -> Unit)?) {
        listener = l
    }

    fun fire() {
        listener?.invoke()
    }
}

object BgFormat {
    const val STALE_AFTER_MS = 10 * 60 * 1000L

    fun isStale(timestamp: Long): Boolean =
        timestamp <= 0 || (System.currentTimeMillis() - timestamp) > STALE_AFTER_MS
}

object BgHistory {
    private const val MAX_ENTRIES = 200
    private const val WINDOW_MS = 6L * 60 * 60 * 1000
    private val BLOCKS = "▁▂▃▄▅▆▇█"

    fun append(prefs: Prefs, mgdl: Float, timestamp: Long) {
        if (mgdl <= 0f || timestamp <= 0L) return
        val current = prefs.cgmHistory
        val lastTs = current.substringAfterLast('|', "").substringBefore(',').toLongOrNull() ?: 0L
        if (lastTs == timestamp) return
        val entry = "$timestamp,$mgdl"
        val combined = if (current.isBlank()) entry else "$current|$entry"
        val parts = combined.split('|')
        prefs.cgmHistory = if (parts.size > MAX_ENTRIES)
            parts.takeLast(MAX_ENTRIES).joinToString("|") else combined
    }

    private fun recentReadings(prefs: Prefs): List<Float> {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        return prefs.cgmHistory.split('|').mapNotNull {
            val parts = it.split(',')
            if (parts.size != 2) return@mapNotNull null
            val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
            val v = parts[1].toFloatOrNull() ?: return@mapNotNull null
            if (ts < cutoff) null else v
        }
    }

    fun sparkline(prefs: Prefs, width: Int = 20, useMmol: Boolean): String {
        val values = recentReadings(prefs)
        if (values.size < 2) return ""
        val sampled = if (values.size > width) {
            val step = values.size.toFloat() / width
            (0 until width).map { values[(it * step).toInt()] }
        } else values
        val min = sampled.min()
        val max = sampled.max()
        val range = (max - min).coerceAtLeast(0.001f)
        val chars = sampled.joinToString("") { v ->
            val idx = ((v - min) / range * 7).toInt().coerceIn(0, 7)
            BLOCKS[idx].toString()
        }
        val mn = if (useMmol) String.format("%.1f", min / 18.0182f) else min.toInt().toString()
        val mx = if (useMmol) String.format("%.1f", max / 18.0182f) else max.toInt().toString()
        return "$mn $chars $mx"
    }
}
