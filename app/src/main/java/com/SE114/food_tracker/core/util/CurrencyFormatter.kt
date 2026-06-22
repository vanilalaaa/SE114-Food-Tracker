package com.SE114.food_tracker.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Converts a stored amount into the chosen display currency at display time and formats it.
 *
 * Conversion uses the USD cross-rate from [rates] (units per 1 USD, base USD, as returned by
 * open.er-api.com). Stored amounts are NEVER mutated; switching display currency only changes
 * how a value is shown. When a rate is missing (offline / unknown code) the raw amount is shown
 * so nothing is ever lost.
 */
object CurrencyFormatter {

    fun convert(
        amount: Double,
        storedCurrency: String,
        displayCurrency: String,
        rates: Map<String, Double>
    ): Double {
        if (storedCurrency.equals(displayCurrency, ignoreCase = true)) return amount
        val from = rates[storedCurrency.uppercase()]
        val to = rates[displayCurrency.uppercase()]
        if (from == null || to == null || from == 0.0) return amount
        return amount / from * to
    }

    fun format(
        amount: Double,
        storedCurrency: String,
        displayCurrency: String,
        rates: Map<String, Double>
    ): String = formatIn(convert(amount, storedCurrency, displayCurrency, rates), AppCurrency.fromCode(displayCurrency))

    /** Exact amount with grouping separators and the currency symbol, e.g. "25,000 ₫" / "$1.07". */
    fun formatIn(amount: Double, currency: AppCurrency): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = currency.fractionDigits
            minimumFractionDigits = currency.fractionDigits
        }
        return withSymbol(nf.format(amount), currency)
    }

    /** Compact form for dense chart/insight labels, e.g. "1.2M ₫" / "$1.1K". */
    fun formatShortIn(amount: Double, currency: AppCurrency): String {
        val n = when {
            amount >= 1_000_000 -> "%.1f".format(amount / 1_000_000) + "M"
            amount >= 1_000 -> (amount / 1_000).toInt().toString() + "K"
            amount > 0 -> amount.toInt().toString()
            else -> "0"
        }
        return withSymbol(n, currency)
    }

    private fun withSymbol(number: String, currency: AppCurrency): String =
        if (currency == AppCurrency.VND) "$number ${currency.symbol}" else "${currency.symbol}$number"
}
