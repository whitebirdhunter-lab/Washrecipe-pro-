package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PhotoEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_archive.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed the default two categories
                            db.execSQL(
                                "INSERT OR IGNORE INTO categories (id, name, description, colorHex) " +
                                        "VALUES (1, 'Denim Recipes', 'Denim wash recipes, enzymatic treatments & stone wash formulas', '#2563EB')"
                            )
                            db.execSQL(
                                "INSERT OR IGNORE INTO categories (id, name, description, colorHex) " +
                                        "VALUES (2, 'Dyeing Recipes', 'Color dyeing, tinting, ozone & pigment dyeing recipes', '#7C3AED')"
                            )
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
