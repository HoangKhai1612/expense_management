package com.finai.mobile

import com.finai.mobile.ui.formatDayMonth
import com.finai.mobile.ui.formatInstant
import com.finai.mobile.ui.formatMoney
import com.finai.mobile.ui.formatMoneySigned
import com.finai.mobile.ui.formatPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Amount and timestamp rendering.
 *
 * These run on every screen, so a locale regression (a dot instead of a thousands
 * separator, or a currency shown for a null value) would be visible on every row
 * of the app.
 */
class FormatTest {

    @Test
    fun `formats whole dong amounts with thousands separators`() {
        assertEquals("0 ₫", formatMoney(0.0))
        assertEquals("1.000 ₫", formatMoney(1000.0))
        assertEquals("3.600.000 ₫", formatMoney(3_600_000.0))
    }

    @Test
    fun `shows a placeholder when an amount is absent`() {
        assertEquals("—", formatMoney(null))
    }

    @Test
    fun `keeps the minus sign out of the grouped digits when signing`() {
        val signed = formatMoneySigned(-2_400_000.0)
        assertTrue("expected an explicit minus sign in $signed", signed.startsWith("-"))
        assertTrue("expected grouped digits in $signed", signed.contains("2.400.000"))
    }

    @Test
    fun `signs positive amounts with a plus`() {
        assertTrue(formatMoneySigned(1500.0).startsWith("+"))
    }

    @Test
    fun `renders percentages to one decimal place`() {
        assertEquals("80.0%", formatPercent(80.0))
        assertEquals("119.9%", formatPercent(119.94))
        assertEquals("—", formatPercent(null))
    }

    @Test
    fun `strips the time part off an instant for compact display`() {
        assertEquals("2026-09-15 10:30", formatInstant("2026-09-15T10:30:45.123Z"))
    }

    @Test
    fun `renders a dash for missing or unparsable timestamps`() {
        assertEquals("—", formatInstant(null))
        assertEquals("—", formatInstant(""))
        assertEquals("—", formatInstant("not-a-timestamp"))
    }

    @Test
    fun `renders a date-only instant without a trailing time`() {
        assertEquals("2026-09-15", formatInstant("2026-09-15"))
    }

    @Test
    fun `renders iso dates day first`() {
        assertEquals("15/09/2026", formatDayMonth("2026-09-15"))
    }

    @Test
    fun `passes through a date it cannot decompose`() {
        assertEquals("2026-09", formatDayMonth("2026-09"))
    }
}