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

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `people` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `gender` TEXT NOT NULL,
                    `birthYear` INTEGER,
                    `birthDateEpochDay` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `isArchived` INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `team_presets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `splitMode` TEXT NOT NULL,
                    `teamCount` INTEGER,
                    `groupSize` INTEGER,
                    `equalTeamSizesOnly` INTEGER NOT NULL DEFAULT 0,
                    `balanceByGender` INTEGER NOT NULL DEFAULT 0,
                    `balanceByAge` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER,
                    `useCount` INTEGER NOT NULL DEFAULT 0,
                    `isPinned` INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `team_preset_members` (
                    `presetId` INTEGER NOT NULL,
                    `personId` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`presetId`, `personId`),
                    FOREIGN KEY(`presetId`) REFERENCES `team_presets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_preset_members_presetId` ON `team_preset_members` (`presetId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_preset_members_personId` ON `team_preset_members` (`personId`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3
    )
}
