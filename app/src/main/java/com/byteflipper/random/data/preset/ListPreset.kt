package com.byteflipper.random.data.preset

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "list_presets")
data class ListPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val items: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    val isPinned: Boolean = false
)


