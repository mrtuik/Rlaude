package com.tuik.rlaude

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves generated or attached bytes into the device's normal Downloads
 * folder — the same place a browser download lands — so the file shows up
 * in the system file manager / "Downloads" app / notification shade instead
 * of being trapped inside app-private storage where nothing else can see it.
 */
object Downloads {
    fun save(context: Context, name: String, mime: String, bytes: ByteArray): JSONObject {
        val safeName = name.trim().ifBlank { "rlaude-file" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create a Downloads entry")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IOException("Could not write to Downloads")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            JSONObject().put("saved", true).put("name", safeName).put("location", "Downloads")
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, safeName)
            FileOutputStream(file).use { it.write(bytes) }
            JSONObject().put("saved", true).put("name", safeName).put("location", file.absolutePath)
        }
    }
}
