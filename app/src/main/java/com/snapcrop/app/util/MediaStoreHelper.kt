package com.snapcrop.app.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.InputStream

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"

    data class ScreenshotInfo(
        val id: Long,
        val uri: Uri,
        val path: String?,
        val name: String,
        val dateAdded: Long
    )

    fun getLatestScreenshot(context: Context, maxAgeSeconds: Long = 10): ScreenshotInfo? {
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val currentTime = System.currentTimeMillis() / 1000

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: ""
                    val dateAdded = cursor.getLong(dateIndex)

                    var path: String? = if (dataIndex != -1) cursor.getString(dataIndex) else null

                    // If DATA column is null on Android 10+, construct from RELATIVE_PATH
                    if (path.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val relPathIndex = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                        if (relPathIndex != -1) {
                            val relPath = cursor.getString(relPathIndex) ?: ""
                            val file = File(Environment.getExternalStorageDirectory(), "$relPath/$name")
                            path = file.absolutePath
                        }
                    }

                    // Check if path is in Screenshots folder
                    val isScreenshot = (path != null && path.contains("Screenshots", ignoreCase = true)) ||
                            name.startsWith("Screenshot", ignoreCase = true) ||
                            name.contains("Screenshot", ignoreCase = true)

                    val age = currentTime - dateAdded
                    if (isScreenshot && (age in -2..maxAgeSeconds)) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        return ScreenshotInfo(id, contentUri, path, name, dateAdded)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        // Direct Filesystem Fallback for GrapheneOS
        try {
            val screenshotsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Screenshots"
            )
            if (screenshotsDir.exists() && screenshotsDir.isDirectory) {
                val latestFile = screenshotsDir.listFiles { file ->
                    file.isFile && (file.extension.equals("png", true) || file.extension.equals("jpg", true))
                }?.maxByOrNull { it.lastModified() }

                if (latestFile != null) {
                    val fileAge = (System.currentTimeMillis() - latestFile.lastModified()) / 1000
                    if (fileAge <= maxAgeSeconds) {
                        val uri = Uri.fromFile(latestFile)
                        return ScreenshotInfo(
                            id = latestFile.lastModified(),
                            uri = uri,
                            path = latestFile.absolutePath,
                            name = latestFile.name,
                            dateAdded = latestFile.lastModified() / 1000
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Filesystem fallback scan error", e)
        }

        return null
    }

    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from URI: $uri", e)
            null
        }
    }

    fun deleteScreenshot(context: Context, uri: Uri, path: String?): Boolean {
        var deleted = false

        // 1. Direct Linux Filesystem deletion (works with MANAGE_EXTERNAL_STORAGE)
        if (!path.isNullOrBlank()) {
            try {
                val file = File(path)
                if (file.exists()) {
                    deleted = file.delete()
                    Log.d(TAG, "Direct file deleted: $path (result=$deleted)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct file delete exception", e)
            }
        }

        // 2. Fallback: Search in Screenshots folder if path was not absolute
        if (!deleted) {
            try {
                val fileName = uri.lastPathSegment ?: ""
                val screenshotsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Screenshots"
                )
                val targetFile = screenshotsDir.listFiles()?.find {
                    it.name == fileName || it.absolutePath == path
                }
                if (targetFile != null && targetFile.exists()) {
                    deleted = targetFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Folder fallback delete error", e)
            }
        }

        // 3. Purge from MediaStore database and tell MediaScanner
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (ignored: Exception) {}

        if (!path.isNullOrBlank()) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    null
                ) { scannedPath, scannedUri ->
                    Log.d(TAG, "MediaScanner purged: $scannedPath")
                }
            } catch (ignored: Exception) {}
        }

        return deleted
    }

    fun saveCroppedImage(context: Context, bitmap: Bitmap, originalName: String?): Uri? {
        val fileName = if (!originalName.isNullOrBlank()) {
            if (originalName.contains(".")) {
                originalName.substringBeforeLast(".") + "_cropped.png"
            } else {
                "${originalName}_cropped.png"
            }
        } else {
            "Screenshot_cropped_${System.currentTimeMillis()}.png"
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Screenshots")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, values, null, null)
            }
            imageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cropped bitmap", e)
            resolver.delete(imageUri, null, null)
            null
        }
    }
}
