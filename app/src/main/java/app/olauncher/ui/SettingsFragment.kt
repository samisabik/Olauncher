package app.olauncher.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentSettingsBinding
import app.olauncher.helper.animateAlpha
import app.olauncher.helper.dpToPx
import app.olauncher.helper.applyAppFont
import app.olauncher.helper.getAppTypeface
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.scaleTextSizes
import app.olauncher.helper.isDarkThemeOn
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isOlauncherDefault
import app.olauncher.helper.isTablet
import app.olauncher.helper.openUrl
import app.olauncher.helper.showToast

class SettingsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        viewModel.isOlauncherDefault()


        binding.homeAppsNum.text = prefs.homeAppsNum.toString()
        populateKeyboardText()
        binding.sortByUsage.text =
            getString(if (prefs.appsSortByUsage) R.string.on else R.string.off)
        populateTextSize()
        populateAppFont()
        populateCgm()
        populateHomeColor()
        buildSwatches()
        populateAlignment()
        populateStatusBar()
        populateDateTime()
        populateSwipeApps()
        populateSwipeDownAction()
        initClickListeners()
        initObservers()

        view.applyAppFont(requireContext().getAppTypeface())
        applyFontPreviewToButtons()
    }

    override fun onClick(view: View) {
        binding.appsNumSelectLayout.visibility = View.GONE
        binding.dateTimeSelectLayout.visibility = View.GONE
        binding.swipeDownSelectLayout.visibility = View.GONE
        if (view.id != R.id.homeColorCurrent
            && view.id != R.id.homeColorAuto
        ) {
            binding.homeColorSelectLayout.visibility = View.GONE
        }
        if (view.id != R.id.appFontText
            && view.id != R.id.fontLight && view.id != R.id.fontMono
            && view.id != R.id.fontJetbrains && view.id != R.id.fontSpaceMono
            && view.id != R.id.fontRobotoMono && view.id != R.id.fontInconsolata
            && view.id != R.id.fontOrbitron && view.id != R.id.fontVt323
            && view.id != R.id.fontPressStart && view.id != R.id.fontMajorMono
            && view.id != R.id.fontMonoton
        ) {
            binding.appFontSelectLayout.visibility = View.GONE
        }
        if (view.id != R.id.textSizeMinus && view.id != R.id.textSizePlus) {
            if (binding.textSizesLayout.isVisible) {
                binding.textSizesLayout.visibility = View.GONE
                applyTextSizeScale()
            }
        }
        if (view.id != R.id.alignmentBottom)
            binding.alignmentSelectLayout.visibility = View.GONE
        if (view.id != R.id.cgmTrendCurrent
            && view.id != R.id.arrowDd && view.id != R.id.arrowD
            && view.id != R.id.arrowDr && view.id != R.id.arrowR
            && view.id != R.id.arrowUr && view.id != R.id.arrowU
            && view.id != R.id.arrowUu && view.id != R.id.arrowClear
        ) {
            binding.cgmTrendPicker.visibility = View.GONE
        }

        when (view.id) {
            R.id.autoShowKeyboard -> toggleKeyboardText()
            R.id.sortByUsage -> toggleSortByUsage()
            R.id.homeAppsNum -> binding.appsNumSelectLayout.visibility = View.VISIBLE
            R.id.alignment -> binding.alignmentSelectLayout.visibility = View.VISIBLE
            R.id.alignmentLeft -> viewModel.updateHomeAlignment(Gravity.START)
            R.id.alignmentCenter -> viewModel.updateHomeAlignment(Gravity.CENTER)
            R.id.alignmentRight -> viewModel.updateHomeAlignment(Gravity.END)
            R.id.alignmentBottom -> updateHomeBottomAlignment()
            R.id.statusBar -> toggleStatusBar()
            R.id.dateTime -> binding.dateTimeSelectLayout.visibility = View.VISIBLE
            R.id.dateTimeOn -> toggleDateTime(Constants.DateTime.ON)
            R.id.dateTimeOff -> toggleDateTime(Constants.DateTime.OFF)
            R.id.dateOnly -> toggleDateTime(Constants.DateTime.DATE_ONLY)
            R.id.textSizeValue -> binding.textSizesLayout.visibility = View.VISIBLE
            R.id.appFontText -> binding.appFontSelectLayout.visibility = View.VISIBLE
            R.id.fontLight -> updateAppFont(Constants.Font.LIGHT)
            R.id.fontMono -> updateAppFont(Constants.Font.MONO)
            R.id.fontJetbrains -> updateAppFont(Constants.Font.JETBRAINS)
            R.id.fontSpaceMono -> updateAppFont(Constants.Font.SPACE_MONO)
            R.id.fontRobotoMono -> updateAppFont(Constants.Font.ROBOTO_MONO)
            R.id.fontInconsolata -> updateAppFont(Constants.Font.INCONSOLATA)
            R.id.fontOrbitron -> updateAppFont(Constants.Font.ORBITRON)
            R.id.fontVt323 -> updateAppFont(Constants.Font.VT323)
            R.id.fontPressStart -> updateAppFont(Constants.Font.PRESS_START)
            R.id.fontMajorMono -> updateAppFont(Constants.Font.MAJOR_MONO)
            R.id.fontMonoton -> updateAppFont(Constants.Font.MONOTON)
            R.id.cgmToggle -> toggleCgm()
            R.id.cgmNotifAccess -> openNotificationAccessSettings()
            R.id.cgmTrendCurrent -> toggleTrendPicker()
            R.id.arrowDd -> assignTrend("↓↓")
            R.id.arrowD -> assignTrend("↓")
            R.id.arrowDr -> assignTrend("↘")
            R.id.arrowR -> assignTrend("→")
            R.id.arrowUr -> assignTrend("↗")
            R.id.arrowU -> assignTrend("↑")
            R.id.arrowUu -> assignTrend("↑↑")
            R.id.arrowClear -> assignTrend("")
            R.id.homeColorCurrent -> binding.homeColorSelectLayout.visibility = View.VISIBLE
            R.id.homeColorAuto -> updateHomeColor(0)

            R.id.tvGestures -> binding.flSwipeDown.visibility = View.VISIBLE

            R.id.maxApps0 -> updateHomeAppsNum(0)
            R.id.maxApps1 -> updateHomeAppsNum(1)
            R.id.maxApps2 -> updateHomeAppsNum(2)
            R.id.maxApps3 -> updateHomeAppsNum(3)
            R.id.maxApps4 -> updateHomeAppsNum(4)
            R.id.maxApps5 -> updateHomeAppsNum(5)
            R.id.maxApps6 -> updateHomeAppsNum(6)
            R.id.maxApps7 -> updateHomeAppsNum(7)
            R.id.maxApps8 -> updateHomeAppsNum(8)

            R.id.textSizeMinus -> adjustTextSizePreview(-0.1f)
            R.id.textSizePlus -> adjustTextSizePreview(0.1f)

            R.id.swipeLeftApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.swipeRightApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_RIGHT_APP)
            R.id.swipeDownAction -> binding.swipeDownSelectLayout.visibility = View.VISIBLE
            R.id.notifications -> updateSwipeDownAction(Constants.SwipeDownAction.NOTIFICATIONS)
            R.id.search -> updateSwipeDownAction(Constants.SwipeDownAction.SEARCH)
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.alignment -> {
                prefs.appLabelAlignment = prefs.homeAlignment
                findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
                requireContext().showToast(getString(R.string.alignment_changed))
            }

            R.id.swipeLeftApp -> toggleSwipeLeft()
            R.id.swipeRightApp -> toggleSwipeRight()
        }
        return true
    }

    private fun initClickListeners() {
        binding.scrollLayout.setOnClickListener(this)
        binding.autoShowKeyboard.setOnClickListener(this)
        binding.sortByUsage.setOnClickListener(this)
        binding.homeAppsNum.setOnClickListener(this)
        binding.alignment.setOnClickListener(this)
        binding.alignmentLeft.setOnClickListener(this)
        binding.alignmentCenter.setOnClickListener(this)
        binding.alignmentRight.setOnClickListener(this)
        binding.alignmentBottom.setOnClickListener(this)
        binding.statusBar.setOnClickListener(this)
        binding.dateTime.setOnClickListener(this)
        binding.dateTimeOn.setOnClickListener(this)
        binding.dateTimeOff.setOnClickListener(this)
        binding.dateOnly.setOnClickListener(this)
        binding.swipeLeftApp.setOnClickListener(this)
        binding.swipeRightApp.setOnClickListener(this)
        binding.swipeDownAction.setOnClickListener(this)
        binding.search.setOnClickListener(this)
        binding.notifications.setOnClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
        binding.appFontText.setOnClickListener(this)
        binding.fontLight.setOnClickListener(this)
        binding.fontMono.setOnClickListener(this)
        binding.fontJetbrains.setOnClickListener(this)
        binding.fontSpaceMono.setOnClickListener(this)
        binding.fontRobotoMono.setOnClickListener(this)
        binding.fontInconsolata.setOnClickListener(this)
        binding.fontOrbitron.setOnClickListener(this)
        binding.fontVt323.setOnClickListener(this)
        binding.fontPressStart.setOnClickListener(this)
        binding.fontMajorMono.setOnClickListener(this)
        binding.fontMonoton.setOnClickListener(this)
        binding.cgmToggle.setOnClickListener(this)
        binding.cgmNotifAccess.setOnClickListener(this)
        binding.cgmTrendCurrent.setOnClickListener(this)
        binding.arrowDd.setOnClickListener(this)
        binding.arrowD.setOnClickListener(this)
        binding.arrowDr.setOnClickListener(this)
        binding.arrowR.setOnClickListener(this)
        binding.arrowUr.setOnClickListener(this)
        binding.arrowU.setOnClickListener(this)
        binding.arrowUu.setOnClickListener(this)
        binding.arrowClear.setOnClickListener(this)
        binding.homeColorCurrent.setOnClickListener(this)
        binding.homeColorAuto.setOnClickListener(this)
        attachHueSlider()

        binding.maxApps0.setOnClickListener(this)
        binding.maxApps1.setOnClickListener(this)
        binding.maxApps2.setOnClickListener(this)
        binding.maxApps3.setOnClickListener(this)
        binding.maxApps4.setOnClickListener(this)
        binding.maxApps5.setOnClickListener(this)
        binding.maxApps6.setOnClickListener(this)
        binding.maxApps7.setOnClickListener(this)
        binding.maxApps8.setOnClickListener(this)

        binding.textSizeMinus.setOnClickListener(this)
        binding.textSizePlus.setOnClickListener(this)

        binding.alignment.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnLongClickListener(this)
        binding.swipeRightApp.setOnLongClickListener(this)
    }

    private fun initObservers() {
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) {
            populateSwipeApps()
        }
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_left_app_enabled))
        } else {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_left_app_disabled))
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_right_app_enabled))
        } else {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_right_app_disabled))
        }
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            showStatusBar()
            binding.statusBar.text = getString(R.string.on)
        } else {
            hideStatusBar()
            binding.statusBar.text = getString(R.string.off)
        }
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        populateDateTime()
        viewModel.toggleDateTime()
    }

    private fun populateDateTime() {
        binding.dateTime.text = getString(
            when (prefs.dateTimeVisibility) {
                Constants.DateTime.DATE_ONLY -> R.string.date
                Constants.DateTime.ON -> R.string.on
                else -> R.string.off
            }
        )
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun updateHomeAppsNum(num: Int) {
        binding.homeAppsNum.text = num.toString()
        binding.appsNumSelectLayout.visibility = View.GONE
        prefs.homeAppsNum = num
        viewModel.refreshHome(true)
    }

    private var pendingTextSizeScale: Float = -1f

    private fun adjustTextSizePreview(delta: Float) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale
        val newScale = Math.round((current + delta) * 10f) / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
        val formatted = String.format("%.1f", clamped)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
    }

    private fun applyTextSizeScale() {
        if (pendingTextSizeScale < 0 || prefs.textSizeScale == pendingTextSizeScale) {
            pendingTextSizeScale = -1f
            return
        }
        prefs.textSizeScale = pendingTextSizeScale
        pendingTextSizeScale = -1f
    }

    private fun toggleKeyboardText() {
        if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
            viewModel.showDialog.postValue(Constants.Dialog.KEYBOARD)
            prefs.keyboardMessageShown = true
        } else {
            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
            populateKeyboardText()
        }
    }

    private fun toggleSortByUsage() {
        prefs.appsSortByUsage = !prefs.appsSortByUsage
        binding.sortByUsage.text =
            getString(if (prefs.appsSortByUsage) R.string.on else R.string.off)
    }

    private fun populateTextSize() {
        val formatted = String.format("%.1f", prefs.textSizeScale)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
    }

    private fun applyFontPreviewToButtons() {
        val ctx = requireContext()
        binding.fontLight.typeface = ctx.getAppTypeface(Constants.Font.LIGHT)
        binding.fontMono.typeface = ctx.getAppTypeface(Constants.Font.MONO)
        binding.fontJetbrains.typeface = ctx.getAppTypeface(Constants.Font.JETBRAINS)
        binding.fontSpaceMono.typeface = ctx.getAppTypeface(Constants.Font.SPACE_MONO)
        binding.fontRobotoMono.typeface = ctx.getAppTypeface(Constants.Font.ROBOTO_MONO)
        binding.fontInconsolata.typeface = ctx.getAppTypeface(Constants.Font.INCONSOLATA)
        binding.fontOrbitron.typeface = ctx.getAppTypeface(Constants.Font.ORBITRON)
        binding.fontVt323.typeface = ctx.getAppTypeface(Constants.Font.VT323)
        binding.fontPressStart.typeface = ctx.getAppTypeface(Constants.Font.PRESS_START)
        binding.fontMajorMono.typeface = ctx.getAppTypeface(Constants.Font.MAJOR_MONO)
        binding.fontMonoton.typeface = ctx.getAppTypeface(Constants.Font.MONOTON)
    }

    private fun populateAppFont(fontId: Int = prefs.appFont) {
        binding.appFontText.text = getString(
            when (fontId) {
                Constants.Font.MONO -> R.string.font_mono
                Constants.Font.JETBRAINS -> R.string.font_jetbrains
                Constants.Font.SPACE_MONO -> R.string.font_space_mono
                Constants.Font.ROBOTO_MONO -> R.string.font_roboto_mono
                Constants.Font.INCONSOLATA -> R.string.font_inconsolata
                Constants.Font.ORBITRON -> R.string.font_orbitron
                Constants.Font.VT323 -> R.string.font_vt323
                Constants.Font.PRESS_START -> R.string.font_press_start
                Constants.Font.MAJOR_MONO -> R.string.font_major_mono
                Constants.Font.MONOTON -> R.string.font_monoton
                else -> R.string.font_light
            }
        )
    }

    // Curated Nord-style palette for one-tap picks.
    private val homeColorSwatches = intArrayOf(
        0xFFECEFF4.toInt(), // snow
        0xFF88C0D0.toInt(), // frost blue
        0xFF8FBCBB.toInt(), // teal
        0xFFA3BE8C.toInt(), // green
        0xFFEBCB8B.toInt(), // yellow
        0xFFD08770.toInt(), // orange
        0xFFBF616A.toInt(), // red
        0xFFB48EAD.toInt(), // purple
    )

    // Greyscale row — the hue slider can't reach neutral tones.
    private val homeColorGreys = intArrayOf(
        0xFFFFFFFF.toInt(),
        0xFFD8D8D8.toInt(),
        0xFFB0B0B0.toInt(),
        0xFF888888.toInt(),
        0xFF606060.toInt(),
        0xFF3C3C3C.toInt(),
        0xFF1C1C1C.toInt(),
        0xFF000000.toInt(),
    )

    private val swatchDots = mutableListOf<Pair<View, Int>>()
    private var suppressSliderListener = false

    // Saturation/value of the last picked colour — the hue slider nudges hue while keeping these.
    private var hueSat = 1f
    private var hueVal = 1f

    private fun populateHomeColor() {
        val color = prefs.homeTextColor
        val effective = color.takeIf { it != 0 }
            ?: requireContext().getColorFromAttr(R.attr.primaryColor)
        binding.homeColorCurrent.setTextColor(effective)
        applyPreviewColor(effective)
        syncHueFromColor(color.takeIf { it != 0 } ?: 0xFF88C0D0.toInt())
        refreshSwatchSelection()
    }

    private fun updateHomeColor(color: Int) {
        binding.homeColorSelectLayout.visibility = View.GONE
        prefs.homeTextColor = color
        populateHomeColor()
    }

    private fun applyPreviewColor(color: Int) {
        binding.homeColorPreview.text = "06:45  abc"
        binding.homeColorPreview.setTextColor(color)
        binding.homeColorHex.text = String.format("#%06X", 0xFFFFFF and color)
    }

    private fun onColorPicked(color: Int) {
        prefs.homeTextColor = color
        binding.homeColorCurrent.setTextColor(color)
        applyPreviewColor(color)
        refreshSwatchSelection()
    }

    private fun buildSwatches() {
        swatchDots.clear()
        fillSwatchRow(binding.homeColorSwatches, homeColorSwatches)
        fillSwatchRow(binding.homeColorGreys, homeColorGreys)
        refreshSwatchSelection()
    }

    private fun fillSwatchRow(container: LinearLayout, colors: IntArray) {
        container.removeAllViews()
        val dotSize = 26.dpToPx()
        val cellHeight = 40.dpToPx()
        colors.forEach { color ->
            val cell = FrameLayout(requireContext())
            val dot = View(requireContext())
            dot.layoutParams = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER)
            cell.addView(dot)
            cell.setOnClickListener {
                onColorPicked(color)
                syncHueFromColor(color)
            }
            container.addView(cell, LinearLayout.LayoutParams(0, cellHeight, 1f))
            swatchDots.add(dot to color)
        }
    }

    private fun refreshSwatchSelection() {
        if (swatchDots.isEmpty()) return
        val current = prefs.homeTextColor
        swatchDots.forEach { (dot, color) ->
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                if (color == current) {
                    // Contrast the ring against the dot so it shows on both white and black.
                    val luminance = 0.299 * Color.red(color) +
                            0.587 * Color.green(color) + 0.114 * Color.blue(color)
                    val ring = if (luminance > 140) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    setStroke(3.dpToPx(), ring)
                }
            }
        }
    }

    private fun syncHueFromColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hueSat = hsv[1]
        hueVal = hsv[2]
        suppressSliderListener = true
        binding.homeColorHue.progress = hsv[0].toInt()
        suppressSliderListener = false
    }

    private fun attachHueSlider() {
        applyHueTrack()
        binding.homeColorHue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (suppressSliderListener || !fromUser) return
                // White / greys carry almost no hue — give the slider something to act on.
                val sat = if (hueSat < 0.15f) 0.55f else hueSat
                val value = if (hueVal < 0.3f) 0.9f else hueVal
                val color = Color.HSVToColor(floatArrayOf(progress.toFloat(), sat, value))
                prefs.homeTextColor = color
                binding.homeColorCurrent.setTextColor(color)
                applyPreviewColor(color)
                refreshSwatchSelection()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun applyHueTrack() {
        val hueColors = intArrayOf(
            0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
            0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt(),
        )
        val track = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hueColors)
        track.cornerRadius = 100f
        val vInset = 5.dpToPx()
        binding.homeColorHue.progressDrawable = InsetDrawable(track, 0, vInset, 0, vInset)

        val thumbSize = 20.dpToPx()
        binding.homeColorHue.thumb = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
            setStroke(2.dpToPx(), 0x66000000)
            setSize(thumbSize, thumbSize)
        }
    }

    private fun populateCgm() {
        binding.cgmToggle.text = getString(if (prefs.cgmEnabled) R.string.on else R.string.off)
        populateTrend()
    }

    private fun toggleCgm() {
        prefs.cgmEnabled = !prefs.cgmEnabled
        populateCgm()
    }

    private fun populateTrend() {
        val id = prefs.cgmTrendIconId
        val glyph = if (id != 0) prefs.cgmTrendMap()[id].orEmpty() else ""
        binding.cgmTrendCurrent.text = when {
            id == 0 -> "—"
            glyph.isBlank() -> "?"
            else -> glyph
        }
    }

    private fun toggleTrendPicker() {
        if (prefs.cgmTrendIconId == 0) {
            requireContext().showToast(getString(R.string.cgm_trend_no_icon))
            return
        }
        binding.cgmTrendPicker.visibility =
            if (binding.cgmTrendPicker.isVisible) View.GONE else View.VISIBLE
    }

    private fun assignTrend(glyph: String) {
        val id = prefs.cgmTrendIconId
        if (id == 0) return
        prefs.cgmTrendMapPut(id, glyph)
        prefs.cgmTrend = glyph
        binding.cgmTrendPicker.visibility = View.GONE
        populateTrend()
        app.olauncher.helper.BgUpdates.fire()
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun updateAppFont(fontId: Int) {
        binding.appFontSelectLayout.visibility = View.GONE
        if (prefs.appFont == fontId) return
        prefs.appFont = fontId
        populateAppFont(fontId)
        binding.root.applyAppFont(requireContext().getAppTypeface(fontId))
        applyFontPreviewToButtons()
    }

    private fun populateKeyboardText() {
        if (prefs.autoShowKeyboard) binding.autoShowKeyboard.text = getString(R.string.on)
        else binding.autoShowKeyboard.text = getString(R.string.off)
    }

    private fun updateHomeBottomAlignment() {
        if (viewModel.isOlauncherDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_olauncher_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun populateAlignment() {
        when (prefs.homeAlignment) {
            Gravity.START -> binding.alignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.alignment.text = getString(R.string.center)
            Gravity.END -> binding.alignment.text = getString(R.string.right)
        }
        binding.alignmentBottom.text = if (prefs.homeBottomAlignment)
            getString(R.string.bottom_on)
        else getString(R.string.bottom_off)
    }

    private fun populateSwipeDownAction() {
        binding.swipeDownAction.text = when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.NOTIFICATIONS -> getString(R.string.notifications)
            else -> getString(R.string.search)
        }
    }

    private fun updateSwipeDownAction(swipeDownFor: Int) {
        if (prefs.swipeDownAction == swipeDownFor) return
        prefs.swipeDownAction = swipeDownFor
        populateSwipeDownAction()
    }

    private fun populateSwipeApps() {
        binding.swipeLeftApp.text = prefs.appNameSwipeLeft.lowercase()
        binding.swipeRightApp.text = prefs.appNameSwipeRight.lowercase()
        if (!prefs.swipeLeftEnabled)
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        if (!prefs.swipeRightEnabled)
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
    }

//    private fun populateDigitalWellbeing() {
//        binding.digitalWellbeing.isVisible = requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_PACKAGE_NAME).not()
//                && requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME).not()
//                && prefs.hideDigitalWellbeing.not()
//    }

    private fun showAppListIfEnabled(flag: Int) {
        if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to flag)
        )
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) populateTrend()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}