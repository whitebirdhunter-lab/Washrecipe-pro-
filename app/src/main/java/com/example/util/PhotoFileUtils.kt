package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object PhotoFileUtils {

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    data class SavedImageInfo(
        val filePath: String,
        val fileName: String,
        val fileSizeBytes: Long,
        val mimeType: String,
        val width: Int,
        val height: Int
    )

    /**
     * Copies the image from the given content URI into app internal storage directory,
     * preserving full original bitstream quality.
     */
    suspend fun saveUriToInternalStorage(
        context: Context,
        uri: Uri,
        suggestedName: String?
    ): Result<SavedImageInfo> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val photosDir = File(context.filesDir, "photos").apply {
                if (!exists()) mkdirs()
            }

            // Determine mime type and extension
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: getExtensionFromUri(context, uri)
                ?: "jpg"

            val uniqueFileName = "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
            val destFile = File(photosDir, uniqueFileName)

            var bytesCopied: Long = 0
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(Exception("Could not read input stream from selected image."))

            // Decode image dimensions without loading entire bitmap into memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(destFile.absolutePath, options)

            // Original display name if not specified
            val displayName = if (!suggestedName.isNullOrBlank()) {
                suggestedName.trim()
            } else {
                queryDisplayName(context, uri) ?: "Photo_${System.currentTimeMillis()}"
            }

            Result.success(
                SavedImageInfo(
                    filePath = destFile.absolutePath,
                    fileName = displayName,
                    fileSizeBytes = bytesCopied,
                    mimeType = mimeType,
                    width = options.outWidth,
                    height = options.outHeight
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a photo to the device's public Pictures/MyPhotoArchive directory using MediaStore,
     * so it appears in Google Photos / Gallery and files.
     */
    suspend fun exportPhotoToDeviceGallery(
        context: Context,
        sourceFilePath: String,
        targetDisplayName: String,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source photo file not found on disk."))
            }

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val cleanName = targetDisplayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val fileNameWithExt = if (cleanName.endsWith(".$extension", ignoreCase = true)) {
                cleanName
            } else {
                "$cleanName.$extension"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileNameWithExt)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyPhotoArchive")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val targetUri = context.contentResolver.insert(collection, values)
                    ?: return@withContext Result.failure(Exception("Failed to allocate MediaStore entry."))

                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream)
                        outStream.flush()
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(targetUri, values, null, null)

                Result.success("Saved to Pictures/MyPhotoArchive/$fileNameWithExt")
            } else {
                // Pre-Android 10
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "MyPhotoArchive"
                ).apply { if (!exists()) mkdirs() }

                val targetFile = File(picturesDir, fileNameWithExt)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Result.success("Saved to ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteFileFromInternalStorage(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    fun decodeBitmapForPrinting(filePath: String, maxDimension: Int = 4096): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(filePath, decodeOpts)
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name?.substringBeforeLast('.')
    }

    private fun getExtensionFromUri(context: Context, uri: Uri): String? {
        val name = queryDisplayName(context, uri) ?: return null
        return if (name.contains(".")) name.substringAfterLast(".") else null
    }
}
