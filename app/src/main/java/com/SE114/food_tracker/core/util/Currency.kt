package com.SE114.food_tracker.core.util

/**
 * The six display currencies supported by the app. Amounts are stored in the currency
 * they were entered in (default VND) and converted only at display time — see Task 5.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val fractionDigits: Int
) {
    VND("VND", "₫", "Việt Nam Đồng", 0),
    USD("USD", "$", "US Dollar", 2),
    EUR("EUR", "€", "Euro", 2),
    JPY("JPY", "¥", "Japanese Yen", 0),
    CNY("CNY", "CN¥", "Chinese Yuan", 0),
    KRW("KRW", "₩", "Korean Won", 0);

    companion object {
        val DEFAULT = VND

        fun fromCode(code: String?): AppCurrency =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
    }
}
