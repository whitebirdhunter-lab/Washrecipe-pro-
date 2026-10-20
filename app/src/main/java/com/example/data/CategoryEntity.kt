package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: Int, // 1 or 2
    val name: String, // e.g. "Category 1" or custom renamed title
    val description: String = "",
    val colorHex: String = "#2563EB"
)
