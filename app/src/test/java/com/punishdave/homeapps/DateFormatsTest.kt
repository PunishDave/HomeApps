package com.punishdave.homeapps

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateFormatsTest {
    @Test
    fun ukDatesConvertToApiDates() {
        assertEquals("2026-08-09", normalizeDateToIso("09-08-2026"))
    }

    @Test
    fun apiDatesRenderAsUkDates() {
        assertEquals("09-08-2026", formatDateForDisplay("2026-08-09"))
        assertEquals("09-08-2026", LocalDate.of(2026, 8, 9).toDisplayDate())
    }

    @Test
    fun invalidDatesAreRejected() {
        assertNull(normalizeDateToIso("31-02-2026"))
    }
}
