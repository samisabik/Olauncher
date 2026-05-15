package app.olauncher.helper

import android.app.Notification
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.alpha
import app.olauncher.data.Prefs

class GlucoseNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!sbn.packageName.startsWith(CAMAPS_PREFIX)) return

        val notif = sbn.notification
        val extras = notif.extras
        val standardTexts = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
            notif.tickerText?.toString().orEmpty(),
        )

        val customTexts = extractRemoteViewTexts(notif)
        val all = (standardTexts + customTexts).filter { it.isNotBlank() }
        val iconInfo = extractIcons(notif)

        Log.i(TAG, "pkg=${sbn.packageName} ongoing=${sbn.isOngoing} texts=$all icons=${iconInfo.map { it.description }}")

        val candidate = all.firstOrNull { looksLikeBg(it) } ?: return

        val prefs = Prefs(applicationContext)
        prefs.cgmRawText = candidate.trim()
        prefs.cgmBgTime = sbn.postTime
        prefs.cgmPackage = sbn.packageName
        val isMmol = sbn.packageName.substringAfterLast('.') != "mgdl"
        prefs.cgmUnit = if (isMmol) "mmol/L" else "mg/dL"
        prefs.cgmTrend = extractTrend(all, candidate, iconInfo)

        BgUpdates.fire()
    }

    private data class IconInfo(val description: String, val drawable: Drawable?)

    private fun extractTrend(texts: List<String>, bgText: String, icons: List<IconInfo>): String {
        TREND_ARROW.find(bgText)?.value?.let { return it }
        for (t in texts) {
            if (t == bgText) continue
            TREND_ARROW.find(t)?.value?.let { return it }
        }
        for (t in texts + icons.map { it.description }) {
            trendFromWord(t)?.let { return it }
        }
        // Fallback: rasterise the "trend" icon and infer direction from pixel distribution.
        icons.firstOrNull { it.description.contains("trend", ignoreCase = true) && it.drawable != null }
            ?.drawable?.let { d ->
                inferTrendFromDrawable(d)?.let { return it }
            }
        return ""
    }

    private fun inferTrendFromDrawable(d: Drawable): String? {
        val w = d.intrinsicWidth.coerceAtLeast(24).coerceAtMost(64)
        val h = d.intrinsicHeight.coerceAtLeast(24).coerceAtMost(64)
        if (w <= 0 || h <= 0) return null
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            d.setBounds(0, 0, w, h)
            d.draw(canvas)
            // Count "opaque" pixels per third (top / middle / bottom) and (left / right)
            var topDark = 0; var midDark = 0; var botDark = 0
            var leftDark = 0; var rightDark = 0
            val band = h / 3
            for (y in 0 until h) for (x in 0 until w) {
                val a = bitmap.getPixel(x, y).alpha
                if (a < 64) continue
                when {
                    y < band -> topDark++
                    y < 2 * band -> midDark++
                    else -> botDark++
                }
                if (x < w / 2) leftDark++ else rightDark++
            }
            val total = topDark + midDark + botDark
            if (total < 20) return null
            val topPct = topDark * 100 / total
            val botPct = botDark * 100 / total
            val midPct = midDark * 100 / total
            val rightBias = (rightDark - leftDark) * 100 / total
            Log.i(TAG, "trend infer: top=$topPct mid=$midPct bot=$botPct rightBias=$rightBias")
            // Heuristics
            return when {
                topPct > botPct + 8 && rightBias > 6 -> "↗"
                topPct > botPct + 8 -> "↑"
                botPct > topPct + 8 && rightBias > 6 -> "↘"
                botPct > topPct + 8 -> "↓"
                midPct > topPct && midPct > botPct -> "→"
                else -> null
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun trendFromWord(text: String): String? {
        val lower = text.lowercase()
        return when {
            // Order matters: most specific first
            "rising fast" in lower || "rising quickly" in lower -> "↑↑"
            "falling fast" in lower || "falling quickly" in lower -> "↓↓"
            "rising slowly" in lower -> "↗"
            "falling slowly" in lower -> "↘"
            "rising" in lower || "increasing" in lower -> "↑"
            "falling" in lower || "decreasing" in lower -> "↓"
            "stable" in lower || "steady" in lower || "flat" in lower -> "→"
            else -> null
        }
    }

    private fun extractIcons(notif: Notification): List<IconInfo> {
        val views = notif.bigContentView ?: notif.contentView ?: return emptyList()
        return try {
            val inflated = views.apply(applicationContext, null)
            val out = mutableListOf<IconInfo>()
            collectIcons(inflated, out)
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun collectIcons(view: View, out: MutableList<IconInfo>) {
        if (view is ImageView) {
            val desc = view.contentDescription?.toString().orEmpty()
            if (desc.isNotBlank()) out.add(IconInfo(desc, view.drawable))
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectIcons(view.getChildAt(i), out)
            }
        }
    }

    private fun extractRemoteViewTexts(notif: Notification): List<String> {
        val views = notif.bigContentView ?: notif.contentView ?: return emptyList()
        return try {
            val inflated = views.apply(applicationContext, null)
            val out = mutableListOf<String>()
            collectTextViews(inflated, out)
            out
        } catch (e: Exception) {
            Log.w(TAG, "RemoteViews extract failed: ${e.message}")
            emptyList()
        }
    }

    private fun collectTextViews(view: View, out: MutableList<String>) {
        if (view is TextView) {
            view.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectTextViews(view.getChildAt(i), out)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "connected")
        try {
            activeNotifications?.forEach { onNotificationPosted(it) }
        } catch (e: Exception) {
            Log.w(TAG, "active fetch failed: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "disconnected, requesting rebind")
        requestRebind(ComponentName(this, javaClass))
        super.onListenerDisconnected()
    }

    private fun looksLikeBg(s: String): Boolean {
        if (s.isBlank()) return false
        return BG_PATTERN.containsMatchIn(s)
    }

    companion object {
        const val TAG = "BgNotif"
        const val CAMAPS_PREFIX = "com.camdiab.fx_alert."
        private val BG_PATTERN = Regex("""\d+([.,]\d+)?""")
        // Common CGM trend glyphs across CamAPS/xDrip/AAPS/Dexcom export styles
        private val TREND_ARROW = Regex("""[↑↗→↘↓⇈⇊⬆⬇⬈⬊⬉⬋⇧⇩⤴⤵▲▼⮝⮟⮜⮞↑↓→]+""")
    }
}
