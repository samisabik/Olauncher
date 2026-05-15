package app.olauncher.helper

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
