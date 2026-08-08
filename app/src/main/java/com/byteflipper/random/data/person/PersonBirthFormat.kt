package com.byteflipper.random.data.person

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Localized birth line for a person: the full date when one was picked, otherwise the bare year.
 * Returns null when neither is known.
 */
fun Person.formatBirth(locale: Locale): String? = when {
    birthDateEpochDay != null -> formatBirthDate(birthDateEpochDay, locale)
    birthYear != null -> birthYear.toString()
    else -> null
}

fun formatBirthDate(epochDay: Long, locale: Locale): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
