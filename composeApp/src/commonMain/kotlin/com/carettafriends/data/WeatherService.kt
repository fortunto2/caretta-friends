package com.carettafriends.data

import com.carettafriends.domain.TemperatureReading
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A real weather reading for a point: current daily mean air temp + 7-day rain + the daily series. */
data class WeatherSnapshot(
    val airTempC: Double?,
    val rainMm7d: Double?,
    val daily: List<TemperatureReading>,
)

/**
 * Pulls REAL weather from Open-Meteo (free, no API key) for a lat/lng — daily mean air temperature and
 * precipitation. Cached by storing the result on the nest (`temps[]` daily series + airTempC/rainMm7d),
 * so it's fetched once per nest, not re-pulled per view. Best-effort / offline-safe.
 *
 * NOTE (sea-turtle science): sex is set by the mean SAND temp in the middle third of incubation (the
 * thermosensitive period), for which we accumulate the daily series here; air temp is a lagged proxy
 * until in-nest loggers (V2) or the user's own sensor layer are wired.
 */
class WeatherService {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    suspend fun fetch(lat: Double, lng: Double): WeatherSnapshot? = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
            "&daily=temperature_2m_mean,precipitation_sum&past_days=7&forecast_days=1&timezone=auto"
        val body = http.get(url).bodyAsText()
        val resp = json.decodeFromString<OpenMeteoResp>(body)
        val d = resp.daily ?: return@runCatching null
        val means = d.temperature_2m_mean
        val rains = d.precipitation_sum
        val series = d.time.mapIndexedNotNull { i, date ->
            means.getOrNull(i)?.let { TemperatureReading(date, "open-meteo", it) }
        }
        val latestMean = means.lastOrNull { it != null }
        val rain7d = rains.filterNotNull().takeLast(7).sum()
        WeatherSnapshot(airTempC = latestMean, rainMm7d = rain7d, daily = series)
    }.getOrNull()
}

@Serializable
private data class OpenMeteoResp(val daily: OpenMeteoDaily? = null)

@Serializable
private data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    val temperature_2m_mean: List<Double?> = emptyList(),
    val precipitation_sum: List<Double?> = emptyList(),
)
