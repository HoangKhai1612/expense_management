package com.finai.mobile.ui

import java.text.NumberFormat
import java.util.Locale

/** Vietnamese dong formatting, matching how the backend renders amounts. */
private val dongFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN")).apply {
    maximumFractionDigits = 0
}

fun formatMoney(value: Double?): String {
    if (value == null) return "—"
    return "${dongFormat.format(value)} ₫"
}

fun formatMoneySigned(value: Double?): String {
    if (value == null) return "—"
    val sign = if (value >= 0) "+" else "-"
    return "$sign${dongFormat.format(kotlin.math.abs(value))} ₫"
}

fun formatPercent(value: Double?): String {
    if (value == null) return "—"
    return "${String.format(Locale.US, "%.1f", value)}%"
}

/**
 * Renders a backend timestamp as `yyyy-MM-dd HH:mm`.
 *
 * The API emits an ISO instant such as `2026-09-15T10:30:45.123Z`. Rather than
 * trusting the string, this matches the expected shape: anything else (null, an
 * empty value, or an unexpected format) renders as a placeholder instead of being
 * passed through to the screen.
 */
private val instantPattern = Regex("""^(\d{4}-\d{2}-\d{2})(?:[T ](\d{2}:\d{2}))?""")

fun formatInstant(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val match = instantPattern.find(value.trim()) ?: return "—"
    val date = match.groupValues[1]
    val time = match.groupValues[2]
    return if (time.isEmpty()) date else "$date $time"
}

fun formatDayMonth(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val parts = value.split("-")
    if (parts.size < 3) return value
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}