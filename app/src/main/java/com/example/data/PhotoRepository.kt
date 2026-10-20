package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.example.util.PhotoFileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao
) {
    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()
    val allCategories: Flow<List<CategoryEntity>> = photoDao.getAllCategories()

    fun getPhotosForCategory(categoryId: Int): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByCategory(categoryId)
    }

    fun countInCategory(categoryId: Int): Flow<Int> {
        return photoDao.countPhotosInCategory(categoryId)
    }

    suspend fun getPhotoById(id: Long): PhotoEntity? {
        return photoDao.getPhotoById(id)
    }

    suspend fun updateCategoryName(id: Int, newName: String, description: String = "", colorHex: String = "#2563EB") {
        withContext(Dispatchers.IO) {
            photoDao.updateCategory(
                CategoryEntity(
                    id = id,
                    name = newName.trim(),
                    description = description.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    suspend fun importSinglePhoto(
        uri: Uri,
        name: String,
        dateString: String,
        notes: String,
        categoryId: Int
    ): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        val saveResult = PhotoFileUtils.saveUriToInternalStorage(context, uri, name)
        if (saveResult.isFailure) {
            return@withContext Result.failure(
                saveResult.exceptionOrNull() ?: Exception("Failed to save image file")
            )
        }

        val info = saveResult.getOrThrow()
        val entity = PhotoEntity(
            categoryId = categoryId,
            name = if (name.isNotBlank()) name.trim() else info.fileName,
            dateString = if (dateString.isNotBlank()) dateString.trim() else PhotoFileUtils.getTodayDateString(),
            notes = notes.trim(),
            filePath = info.filePath,
            fileSizeBytes = info.fileSizeBytes,
            mimeType = info.mimeType,
            width = info.width,
            height = info.height
        )

        val insertedId = photoDao.insertPhoto(entity)
        Result.success(entity.copy(id = insertedId))
    }

    suspend fun importMultiplePhotos(
        uris: List<Uri>,
        baseName: String,
        dateString: String,
        notes: String,
        categoryId: Int,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        val total = uris.size
        for ((index, uri) in uris.withIndex()) {
            val photoName = if (baseName.isNotBlank()) {
                if (total > 1) "$baseName (${index + 1})" else baseName
            } else ""

            val result = importSinglePhoto(uri, photoName, dateString, notes, categoryId)
            if (result.isSuccess) {
                count++
            }
            onProgress(index + 1, total)
        }
        Result.success(count)
    }

    suspend fun updatePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        photoDao.updatePhoto(photo)
    }

    suspend fun deletePhoto(photo: PhotoEntity): Boolean = withContext(Dispatchers.IO) {
        PhotoFileUtils.deleteFileFromInternalStorage(photo.filePath)
        photoDao.deletePhoto(photo)
        true
    }

    /**
     * Seeds initial high-resolution demo photos if archive is brand new,
     * ensuring immediate testing capability for users on emulators without camera photos.
     */
    suspend fun seedSampleArchiveIfEmpty() = withContext(Dispatchers.IO) {
        val cat1 = photoDao.getCategoryById(1)
        if (cat1 == null) {
            photoDao.insertCategory(
                CategoryEntity(1, "Denim Recipes", "Denim wash recipes, enzyme treatments & stone wash formulas", "#2563EB")
            )
        } else if (cat1.name == "Category 1") {
            photoDao.updateCategory(
                cat1.copy(name = "Denim Recipes", description = "Denim wash recipes, enzyme treatments & stone wash formulas")
            )
        }

        val cat2 = photoDao.getCategoryById(2)
        if (cat2 == null) {
            photoDao.insertCategory(
                CategoryEntity(2, "Dyeing Recipes", "Color dyeing, tinting, ozone & pigment dyeing recipes", "#7C3AED")
            )
        } else if (cat2.name == "Category 2") {
            photoDao.updateCategory(
                cat2.copy(name = "Dyeing Recipes", description = "Color dyeing, tinting, ozone & pigment dyeing recipes")
            )
        }
    }

    /**
     * Creates an illustrative sample photo file locally with high fidelity
     */
    suspend fun createSampleDemoPhoto(
        title: String,
        date: String,
        categoryId: Int,
        notes: String,
        primaryHex: Int,
        accentHex: Int
    ): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        try {
            val photosDir = File(context.filesDir, "photos").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "demo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val destFile = File(photosDir, fileName)

            // Draw a stylish sample photo card
            val width = 1200
            val height = 900
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint().apply { isAntiAlias = true }

            // Gradient / background
            paint.color = primaryHex
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Accent shape
            paint.color = accentHex
            canvas.drawCircle(width * 0.75f, height * 0.35f, 280f, paint)

            // Secondary shape
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawRoundRect(100f, 100f, (width - 100).toFloat(), (height - 100).toFloat(), 40f, 40f, paint)

            // Label text
            paint.color = Color.WHITE
            paint.textSize = 58f
            paint.isFakeBoldText = true
            canvas.drawText(title, 140f, height * 0.70f, paint)

            paint.textSize = 34f
            paint.isFakeBoldText = false
            paint.color = Color.argb(220, 255, 255, 255)
            canvas.drawText("Date: $date  •  Category $categoryId", 140f, height * 0.77f, paint)
            canvas.drawText(notes.take(45), 140f, height * 0.83f, paint)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val entity = PhotoEntity(
                categoryId = categoryId,
                name = title,
                dateString = date,
                notes = notes,
                filePath = destFile.absolutePath,
                fileSizeBytes = destFile.length(),
                mimeType = "image/jpeg",
                width = width,
                height = height
            )

            val id = photoDao.insertPhoto(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
