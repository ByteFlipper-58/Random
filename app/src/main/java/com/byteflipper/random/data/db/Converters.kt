package com.byteflipper.random.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? = when (value) {
        null -> null
        "" -> emptyList()
        else -> value.split("\u0001").map { it }
    }

    @TypeConverter
    fun listToString(list: List<String>?): String? = when (list) {
        null -> null
        emptyList<String>() -> ""
        else -> list.joinToString("\u0001")
    }
}


