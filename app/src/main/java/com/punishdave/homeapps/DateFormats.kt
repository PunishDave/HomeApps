package com.punishdave.homeapps

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DisplayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val IsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun LocalDate.toDisplayDate(): String = format(DisplayDateFormatter)

fun todayDisplayDate(): String = LocalDate.now().format(DisplayDateFormatter)

fun formatDateForDisplay(raw: String?): String? {
    val parsed = parseFlexibleDate(raw)
    return when {
        parsed != null -> parsed.format(DisplayDateFormatter)
        else -> raw?.trim()?.takeIf { it.isNotBlank() }
    }
}

fun normalizeDateToIso(raw: String?): String? {
    val parsed = parseFlexibleDate(raw)
    return parsed?.format(IsoDateFormatter)
}

fun parseFlexibleDate(raw: String?): LocalDate? {
    val cleaned = raw?.let(::dateOnly)?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { LocalDate.parse(cleaned, DisplayDateFormatter) }.getOrNull()
        ?: runCatching { LocalDate.parse(cleaned, IsoDateFormatter) }.getOrNull()
}

private fun dateOnly(raw: String): String {
    val trimmed = raw.trim()
    val datePortion = trimmed.substringBefore('T').substringBefore(' ')
    return if (datePortion.isNotBlank()) datePortion else trimmed
}
