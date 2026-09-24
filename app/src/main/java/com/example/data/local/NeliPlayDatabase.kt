package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.MyListDao
import com.example.data.local.dao.PaymentOrderDao
import com.example.data.local.dao.WatchProgressDao
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.FavoriteEntity
import com.example.data.local.entities.MyListEntity
import com.example.data.local.entities.PaymentOrderEntity
import com.example.data.local.entities.WatchProgressEntity

@Database(
    entities = [
        WatchProgressEntity::class,
        FavoriteEntity::class,
        DownloadEntity::class,
        MyListEntity::class,
        PaymentOrderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NeliPlayDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun myListDao(): MyListDao
    abstract fun paymentOrderDao(): PaymentOrderDao

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
