package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["dateString"]),
        Index(value = ["createdAt"])
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Int, // 1 for Category 1, 2 for Category 2
    val name: String,
    val dateString: String, // e.g. "2026-10-20"
    val notes: String = "",
    val filePath: String, // Absolute path in app storage or relative
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
