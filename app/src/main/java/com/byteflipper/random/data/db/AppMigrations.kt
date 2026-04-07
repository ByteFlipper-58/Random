package com.byteflipper.random.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE list_presets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE list_presets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE list_presets ADD COLUMN lastUsedAt INTEGER")
            db.execSQL("ALTER TABLE list_presets ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE list_presets ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE list_presets
                SET createdAt = CAST(strftime('%s','now') AS INTEGER) * 1000,
                    updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
                """
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
