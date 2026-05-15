package app.olauncher.helper

import android.app.Activity
import android.app.AppOpsManager
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import app.olauncher.BuildConfig
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import java.util.Calendar

fun View.hideKeyboard() {
    this.clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(show: Boolean = true) {
    if (show.not()) return
    if (this.requestFocus())
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }, 100)
}


@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.showLauncherSelector(requestCode: Int) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        startActivityForResult(intent, requestCode)
    } else
        resetDefaultLauncher()
}

fun Context.resetDefaultLauncher() {
    try {
        val componentName = ComponentName(this, FakeHomeActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        startActivity(selector)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.isDefaultLauncher(): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(this)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun Context.resetLauncherViaFakeActivity() {
    resetDefaultLauncher()
    if (getDefaultLauncherPackage(this).contains("."))
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}

fun Context.isEinkDisplay(): Boolean {
    return try {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.refreshRate <= Constants.MIN_ANIM_REFRESH_RATE
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.searchOnPlayStore(query: String? = null): Boolean {
    return try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/search?q=$query&c=apps")
            ).addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.isPackageInstalled(packageName: String, userHandle: UserHandle = android.os.Process.myUserHandle()): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.isNotEmpty()
}

fun Long.hasBeenHours(hours: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_HOUR_IN_MILLIS) >= hours

fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

fun Context.getAppTypeface(fontId: Int = Prefs(this).appFont): Typeface? {
    return when (fontId) {
        Constants.Font.MONO -> Typeface.create("monospace", Typeface.NORMAL)
        Constants.Font.JETBRAINS -> ResourcesCompat.getFont(this, R.font.jetbrains_mono)
        Constants.Font.SPACE_MONO -> ResourcesCompat.getFont(this, R.font.space_mono)
        Constants.Font.ROBOTO_MONO -> ResourcesCompat.getFont(this, R.font.roboto_mono)
        Constants.Font.INCONSOLATA -> ResourcesCompat.getFont(this, R.font.inconsolata)
        Constants.Font.ORBITRON -> ResourcesCompat.getFont(this, R.font.orbitron)
        Constants.Font.VT323 -> ResourcesCompat.getFont(this, R.font.vt323)
        Constants.Font.PRESS_START -> ResourcesCompat.getFont(this, R.font.press_start_2p)
        Constants.Font.MAJOR_MONO -> ResourcesCompat.getFont(this, R.font.major_mono)
        Constants.Font.MONOTON -> ResourcesCompat.getFont(this, R.font.monoton)
        else -> Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
}

fun View.applyAppFont(typeface: Typeface?) {
    if (typeface == null) return
    if (this is TextView) {
        val style = this.typeface?.style ?: Typeface.NORMAL
        this.typeface = Typeface.create(typeface, style)
    }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).applyAppFont(typeface)
        }
    }
}

fun View.scaleTextSizes(ratio: Float) {
    if (ratio == 1f) return
    if (this is TextView) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize * ratio)
    }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).scaleTextSizes(ratio)
        }
    }
}

fun View.applyAppTextSize(context: Context) {
    val prefScale = Prefs(context).textSizeScale
    if (prefScale == 1f) return
    scaleTextSizes(prefScale)
}
