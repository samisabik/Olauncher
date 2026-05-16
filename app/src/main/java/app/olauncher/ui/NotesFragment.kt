package app.olauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentNotesBinding
import app.olauncher.helper.NotesBackup
import app.olauncher.helper.applyAppFont
import app.olauncher.helper.applyAppTextSize
import app.olauncher.helper.getAppTypeface
import app.olauncher.helper.getColorFromAttr

class NotesFragment : Fragment() {

    private lateinit var prefs: Prefs

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { persistNotes() }

    private var baseBottomPadding = 0
    private var savedSoftInputMode = -1

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) backupNotes()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        val color = if (prefs.homeTextColor != 0) prefs.homeTextColor
        else requireContext().getColorFromAttr(R.attr.primaryColor)

        binding.notes.apply {
            setText(withTrailingNewline(prefs.notes))
            setSelection(text?.length ?: 0)
            setTextColor(color)
            applyCursorColor(color)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    saveHandler.removeCallbacks(saveRunnable)
                    saveHandler.postDelayed(saveRunnable, SAVE_DEBOUNCE_MS)
                }
            })
        }

        view.applyAppFont(requireContext().getAppTypeface())
        view.applyAppTextSize(requireContext())
        applyClockAlignedPadding()
        followKeyboard()

        ensureStoragePermission()
        if (prefs.notes.isBlank()) recoverFromBackup()
    }

    /**
     * Grow the editor's bottom padding by the keyboard height so its scroll
     * viewport ends above the keyboard. As text reaches that edge the EditText
     * scrolls itself, rolling the top lines out of view.
     */
    private fun followKeyboard() {
        baseBottomPadding = binding.notes.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.notes) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = baseBottomPadding + imeBottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.notes)
    }

    /** Ensure the note ends on a fresh line, so the cursor lands below the last line — not at the end of it. */
    private fun withTrailingNewline(text: String): String =
        if (text.isNotEmpty() && !text.endsWith("\n")) "$text\n" else text

    /** Start the text at the clock's actual Y, captured by HomeFragment. */
    private fun applyClockAlignedPadding() {
        val topPadding = prefs.notesTopPadding
        if (topPadding <= 0) return
        binding.notes.setPadding(
            binding.notes.paddingLeft,
            topPadding,
            binding.notes.paddingRight,
            binding.notes.paddingBottom,
        )
    }

    private fun applyCursorColor(color: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            binding.notes.textCursorDrawable?.let { cursor ->
                cursor.setTint(color)
                binding.notes.textCursorDrawable = cursor
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    /** SharedPreferences was empty (fresh install / cleared data) — restore from the backup file. */
    private fun recoverFromBackup() {
        val appContext = requireContext().applicationContext
        Thread {
            val restored = NotesBackup.read(appContext)?.takeIf { it.isNotBlank() } ?: return@Thread
            view?.post {
                val current = _binding ?: return@post
                if (current.notes.text.isNullOrEmpty()) {
                    val display = withTrailingNewline(restored)
                    current.notes.setText(display)
                    current.notes.setSelection(display.length)
                    prefs.notes = restored
                }
            }
        }.start()
    }

    private fun persistNotes() {
        val current = _binding ?: return
        prefs.notes = current.notes.text?.toString().orEmpty()
        backupNotes()
    }

    private fun backupNotes() {
        val appContext = context?.applicationContext ?: return
        val content = prefs.notes
        Thread { NotesBackup.write(appContext, content) }.start()
    }

    override fun onResume() {
        super.onResume()
        // Keep the window fixed (translucent system bars break adjustResize);
        // followKeyboard() handles staying above the keyboard via padding.
        val window = requireActivity().window
        savedSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(
            (savedSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv())
                or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
    }

    override fun onPause() {
        saveHandler.removeCallbacks(saveRunnable)
        persistNotes()
        if (savedSoftInputMode != -1)
            requireActivity().window.setSoftInputMode(savedSoftInputMode)
        super.onPause()
    }

    override fun onDestroyView() {
        saveHandler.removeCallbacks(saveRunnable)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 400L
    }
}
