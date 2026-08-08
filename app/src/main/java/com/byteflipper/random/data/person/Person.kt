package com.byteflipper.random.data.person

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val gender: PersonGender = PersonGender.Unspecified,
    val birthYear: Int? = null,
    /** Epoch day of the full date of birth, when known. [birthYear] stays in sync with it. */
    val birthDateEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val isArchived: Boolean = false
)

enum class PersonGender {
    Unspecified,
    Male,
    Female
}
