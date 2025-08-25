package com.byteflipper.random.data.di

import android.content.Context
import androidx.room.Room
import com.byteflipper.random.data.db.AppDatabase
import com.byteflipper.random.data.db.Converters

object DatabaseModule {
    @Volatile private var db: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "random.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { db = it }
        }
}


