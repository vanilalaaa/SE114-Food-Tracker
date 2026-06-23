package com.SE114.food_tracker.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.SE114.food_tracker.core.util.AppCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Typed wrapper over the single app DataStore. Repositories/ViewModels read this, not DataStore. */
@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** The chosen display currency code; defaults to VND. */
    val displayCurrency: Flow<String> =
        dataStore.data.map { it[DISPLAY_CURRENCY] ?: AppCurrency.DEFAULT.code }

    /** Cached exchange-rates blob (base USD JSON) plus the epoch-millis it was fetched. */
    val cachedRates: Flow<CachedRates?> = dataStore.data.map { prefs ->
        val json = prefs[RATES_JSON] ?: return@map null
        val ts = prefs[RATES_TIMESTAMP] ?: return@map null
        CachedRates(json, ts)
    }

    suspend fun setDisplayCurrency(code: String) {
        dataStore.edit { it[DISPLAY_CURRENCY] = code }
    }

    suspend fun setCachedRates(ratesJson: String, fetchedAt: Long) {
        dataStore.edit {
            it[RATES_JSON] = ratesJson
            it[RATES_TIMESTAMP] = fetchedAt
        }
    }

    companion object {
        private val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        private val RATES_JSON = stringPreferencesKey("rates_json")
        private val RATES_TIMESTAMP = longPreferencesKey("rates_timestamp")
    }
}

data class CachedRates(val ratesJson: String, val fetchedAt: Long)
