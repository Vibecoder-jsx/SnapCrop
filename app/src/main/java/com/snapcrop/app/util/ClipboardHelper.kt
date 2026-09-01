package com.snapcrop.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ClipboardHelper {
    private const val TAG = "ClipboardHelper"
    private const val AUTHORITY = "com.snapcrop.app.fileprovider"

    fun copyBitmapToClipboard(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "screenshots")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Immediately purge older cache files so disk cache never accumulates
            val existingFiles = cacheDir.listFiles()?.sortedByDescending { it.lastModified() }
            existingFiles?.drop(1)?.forEach { file ->
                try { file.delete() } catch (ignored: Exception) {}
            }

            val imageFile = File(cacheDir, "clip_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            val contentUri: Uri = FileProvider.getUriForFile(context, AUTHORITY, imageFile)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData(
                ClipDescription("Screenshot", arrayOf("image/png", "image/*")),
                ClipData.Item(contentUri)
            )

            context.grantUriPermission(
                "com.google.android.inputmethod.latin",
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "Copied bitmap to clipboard: $contentUri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy bitmap to clipboard", e)
            false
        }
    }
}
