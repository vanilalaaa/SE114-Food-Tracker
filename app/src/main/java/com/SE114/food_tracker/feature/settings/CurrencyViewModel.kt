package com.SE114.food_tracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.datastore.UserPreferences
import com.SE114.food_tracker.core.util.AppCurrency
import com.SE114.food_tracker.data.repository.CurrencyRatesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrencyUiState(
    val displayCurrency: AppCurrency = AppCurrency.DEFAULT,
    val rates: Map<String, Double> = emptyMap(),
    val ratesStale: Boolean = false,
    val currencies: List<AppCurrency> = AppCurrency.entries
)

/**
 * Owns the display-currency preference and the cached exchange rates. Used both by the
 * Settings currency picker and (via the app-root [com.SE114.food_tracker.core.util.LocalCurrencyDisplay])
 * to format every price. Selecting a currency only updates the preference — stored amounts
 * are never touched and there is deliberately no batch-convert.
 */
@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val ratesRepository: CurrencyRatesRepository
) : ViewModel() {

    private val _rates = MutableStateFlow(RatesState())

    val uiState: StateFlow<CurrencyUiState> =
        combine(userPreferences.displayCurrency, _rates) { code, rates ->
            CurrencyUiState(
                displayCurrency = AppCurrency.fromCode(code),
                rates = rates.rates,
                ratesStale = rates.stale
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyUiState())

    init { refreshRates() }

    fun selectCurrency(currency: AppCurrency) {
        viewModelScope.launch { userPreferences.setDisplayCurrency(currency.code) }
    }

    /** Refreshes rates (cache-backed: a real fetch happens at most every 24h). */
    fun refreshRates() {
        viewModelScope.launch {
            val result = ratesRepository.getRates()
            _rates.update { RatesState(result.rates, result.stale) }
        }
    }

    private data class RatesState(val rates: Map<String, Double> = emptyMap(), val stale: Boolean = false)
}
