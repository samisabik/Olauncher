package app.olauncher.helper

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Mirrors the scratchpad note to a plain-text file in the public Documents
 * folder (Documents/Olauncher/notes.txt) so it survives uninstall / "Clear data"
 * and can be opened in any file manager. SharedPreferences stays the fast
 * source of truth; this is a durable backup.
 */
object NotesBackup {

    private const val SUBDIR = "Olauncher"
    private const val FILE_NAME = "notes.txt"
    private const val MIME = "text/plain"

    fun write(context: Context, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) writeMediaStore(context, content)
            else writeLegacy(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun read(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) readMediaStore(context)
            else readLegacy()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun relativePath() = Environment.DIRECTORY_DOCUMENTS + "/" + SUBDIR

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collection(): Uri =
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findUri(context: Context): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        val args = arrayOf(relativePath() + "/", FILE_NAME)
        context.contentResolver.query(collection(), projection, selection, args, null)?.use { c ->
            if (c.moveToFirst()) return ContentUris.withAppendedId(collection(), c.getLong(0))
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeMediaStore(context: Context, content: String) {
        val resolver = context.contentResolver
        val uri = findUri(context) ?: resolver.insert(
            collection(),
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath())
            }
        ) ?: return
        // "wt" truncates, so a shorter note fully overwrites a longer one.
        resolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readMediaStore(context: Context): String? {
        val uri = findUri(context) ?: return null
        return context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }

    private fun legacyFile(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            SUBDIR
        )
        return File(dir, FILE_NAME)
    }

    private fun writeLegacy(content: String) {
        val file = legacyFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun readLegacy(): String? {
        val file = legacyFile()
        return if (file.exists()) file.readText() else null
    }
}
