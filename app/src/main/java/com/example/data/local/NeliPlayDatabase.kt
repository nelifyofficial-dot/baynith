package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.WatchProgressDao
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.WatchProgressEntity

@Database(
    entities = [
        WatchProgressEntity::class,
        FavoriteEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NeliPlayDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: NeliPlayDatabase? = null

        fun getDatabase(context: Context): NeliPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NeliPlayDatabase::class.java,
                    "neliplay_local.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
