package app.olauncher.helper

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        val icons = extractIcons(notif)
        val trendIcon = icons.firstOrNull { it.description.contains("trend", ignoreCase = true) }

        Log.i(TAG, "pkg=${sbn.packageName} texts=$all trendResId=${trendIcon?.resourceId}")

        val candidate = all.firstOrNull { looksLikeBg(it) } ?: return

        val prefs = Prefs(applicationContext)
        prefs.cgmRawText = candidate.trim()
        prefs.cgmBgTime = sbn.postTime
        prefs.cgmPackage = sbn.packageName
        val isMmol = sbn.packageName.substringAfterLast('.') != "mgdl"
        prefs.cgmUnit = if (isMmol) "mmol/L" else "mg/dL"

        val trendResId = trendIcon?.resourceId ?: 0
        prefs.cgmTrendIconId = trendResId
        prefs.cgmTrend = if (trendResId != 0) prefs.cgmTrendMap()[trendResId].orEmpty() else ""

        BgUpdates.fire()
    }

    private data class IconInfo(val description: String, val resourceId: Int)

    private fun extractIcons(notif: Notification): List<IconInfo> {
        val views = notif.bigContentView ?: notif.contentView ?: return emptyList()
        return try {
            val inflated = views.apply(applicationContext, null)
            val out = mutableListOf<IconInfo>()
            collectIcons(inflated, out)
            out
        } catch (e: Exception) {
            Log.w(TAG, "icon extract failed: ${e.message}")
            emptyList()
        }
    }

    private fun collectIcons(view: View, out: MutableList<IconInfo>) {
        if (view is ImageView) {
            val desc = view.contentDescription?.toString().orEmpty()
            out.add(IconInfo(desc, readImageViewResourceId(view)))
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) collectIcons(view.getChildAt(i), out)
        }
    }

    // ImageView doesn't expose the resource id set via setImageResource, but it
    // stores it internally as mResource. Reflectively read it — that's the only
    // way to recover the original drawable identifier from an inflated RemoteViews.
    private fun readImageViewResourceId(view: ImageView): Int = try {
        val f = ImageView::class.java.getDeclaredField("mResource")
        f.isAccessible = true
        f.getInt(view)
    } catch (e: Exception) {
        0
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
            for (i in 0 until view.childCount) collectTextViews(view.getChildAt(i), out)
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
    }
}
