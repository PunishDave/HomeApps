package com.punishdave.homeapps

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

fun friendlyDateLabel(raw: String?, daysAhead: Long = 6): String? {
    val parsed = parseFlexibleDate(raw) ?: return raw?.trim()?.takeIf { it.isNotBlank() }
    val today = LocalDate.now()
    val windowEnd = today.plusDays(daysAhead)
    if (!parsed.isBefore(today) && !parsed.isAfter(windowEnd)) {
        return parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    return parsed.format(DisplayDateFormatter)
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
