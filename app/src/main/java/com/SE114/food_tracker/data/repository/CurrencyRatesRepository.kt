package com.SE114.food_tracker.data.repository

import com.SE114.food_tracker.core.datastore.UserPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Exchange rates (base USD) plus whether the data is likely outdated. */
data class RatesResult(val rates: Map<String, Double>, val stale: Boolean)

/**
 * Fetches and caches exchange rates from the free open.er-api.com endpoint (no API key).
 * Rates are cached in DataStore with a timestamp: a fetch is attempted when the cache is
 * older than 24h or missing; on network failure the last cache is returned, flagged [stale]
 * once it is older than 7 days.
 */
@Singleton
class CurrencyRatesRepository @Inject constructor(
    private val userPreferences: UserPreferences
) {
    private val http by lazy { HttpClient(OkHttp) }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRates(forceRefresh: Boolean = false): RatesResult = withContext(Dispatchers.IO) {
        val cached = userPreferences.cachedRates.first()
        val now = System.currentTimeMillis()
        val cacheFresh = cached != null && (now - cached.fetchedAt) < CACHE_TTL_MS

        if (!forceRefresh && cacheFresh && cached != null) {
            return@withContext RatesResult(decode(cached.ratesJson), stale = false)
        }

        runCatching { fetch() }.fold(
            onSuccess = { ratesJson ->
                userPreferences.setCachedRates(ratesJson, now)
                RatesResult(decode(ratesJson), stale = false)
            },
            onFailure = { err ->
                Timber.tag("Currency").w(err, "rate fetch failed; falling back to cache")
                if (cached != null) {
                    RatesResult(decode(cached.ratesJson), stale = (now - cached.fetchedAt) > STALE_THRESHOLD_MS)
                } else {
                    RatesResult(emptyMap(), stale = true)
                }
            }
        )
    }

    private suspend fun fetch(): String {
        val body = http.get(API_URL).bodyAsText()
        val parsed = json.decodeFromString<ErApiResponse>(body)
        if (parsed.result != "success" || parsed.rates.isEmpty()) error("er-api result=${parsed.result}")
        return json.encodeToString(parsed.rates)
    }

    private fun decode(ratesJson: String): Map<String, Double> =
        runCatching { json.decodeFromString<Map<String, Double>>(ratesJson) }.getOrDefault(emptyMap())

    @Serializable
    private data class ErApiResponse(
        val result: String = "",
        val rates: Map<String, Double> = emptyMap(),
        @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long = 0
    )

    companion object {
        private const val API_URL = "https://open.er-api.com/v6/latest/USD"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000           // refresh after 24h
        private const val STALE_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1000 // "tỷ giá có thể đã cũ" after 7 days
    }
}
