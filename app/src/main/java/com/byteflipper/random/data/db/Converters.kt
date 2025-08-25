package com.byteflipper.random.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? = value?.split("\u0001")?.map { it }

    @TypeConverter
    fun listToString(list: List<String>?): String? = list?.joinToString("\u0001")
}


