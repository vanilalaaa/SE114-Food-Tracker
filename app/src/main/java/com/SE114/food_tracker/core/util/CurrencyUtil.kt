package com.SE114.food_tracker.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a Double as an exact VND amount with thousand-group separators.
 * Examples:
 *   300000.0  → "300,000 đ"
 *   3000000.0 → "3,000,000 đ"
 *   500.0     → "500 đ"
 */
fun Double.formatVndExact(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return "${formatter.format(this)} đ"
}

fun Double.formatVndShort(): String = when {
    this >= 1_000_000 -> "${"%.1f".format(this / 1_000_000)}M"
    this >= 1_000     -> "${(this / 1_000).toInt()}K"
    this > 0          -> "${this.toInt()}"
    else              -> ""
}