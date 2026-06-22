package com.SE114.food_tracker.core.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Display-time currency context provided at the app root from the live exchange rates and the
 * user's chosen display currency. Composables format stored (VND) amounts through this instead
 * of hardcoding a VND symbol, so changing the display currency re-renders every price without
 * touching any stored value.
 *
 * The default is identity-VND with no rates, so previews and any call made outside the provider
 * still render the original amount sensibly.
 */
class CurrencyDisplay(
    val displayCurrency: AppCurrency = AppCurrency.VND,
    val rates: Map<String, Double> = emptyMap()
) {
    fun format(amount: Double, storedCurrency: String = AppCurrency.VND.code): String =
        CurrencyFormatter.format(amount, storedCurrency, displayCurrency.code, rates)

    fun formatShort(amount: Double, storedCurrency: String = AppCurrency.VND.code): String {
        val converted = CurrencyFormatter.convert(amount, storedCurrency, displayCurrency.code, rates)
        return CurrencyFormatter.formatShortIn(converted, displayCurrency)
    }
}

val LocalCurrencyDisplay = staticCompositionLocalOf { CurrencyDisplay() }
