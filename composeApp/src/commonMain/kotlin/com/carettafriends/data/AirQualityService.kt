package com.carettafriends.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Median PM2.5 / PM10 (µg/m³) for an area, plus how many sensors contributed. */
data class AirSample(val pm25: Double, val pm10: Double, val sensors: Int)

/**
 * Pulls open citizen air-quality data from **Sensor.Community** (the same free source the user's Air
 * Signal / airq project uses — Gazipaşa is sensor 77955). No API key. Supplementary layer: returns
 * null where no sensors exist nearby, so it never blocks the app.
 *
 * P2 / SDS_P2 = PM2.5, P1 / SDS_P1 = PM10. Values ≤0 or >500 are dropped; we take the median.
 */
class AirQualityService {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    /** Nearest sensors within [radiusKm] of (lat,lng), aggregated to a median reading. */
    suspend fun near(lat: Double, lng: Double, radiusKm: Int = 15): AirSample? = runCatching {
        val url = "https://data.sensor.community/airrohr/v1/filter/area=$lat,$lng,$radiusKm"
        val body = http.get(url) { header("User-Agent", "caretta-app/1.0") }.bodyAsText()
        val rows = json.decodeFromString<List<ScRow>>(body)
        val pm25 = mutableListOf<Double>()
        val pm10 = mutableListOf<Double>()
        val sensorIds = mutableSetOf<Long>()
        for (r in rows) {
            var got = false
            for (v in r.sensordatavalues) {
                val x = v.value.toDoubleOrNull() ?: continue
                if (x <= 0.0 || x > 500.0) continue
                when (v.value_type) {
                    "P2", "SDS_P2" -> { pm25.add(x); got = true }
                    "P1", "SDS_P1" -> { pm10.add(x); got = true }
                }
            }
            if (got) r.sensor?.id?.let { sensorIds.add(it) }
        }
        if (pm25.isEmpty() && pm10.isEmpty()) return@runCatching null
        AirSample(pm25 = median(pm25), pm10 = median(pm10), sensors = sensorIds.size)
    }.getOrNull()

    private fun median(xs: List<Double>): Double {
        if (xs.isEmpty()) return 0.0
        val s = xs.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
    }
}

@Serializable
private data class ScRow(
    val sensor: ScSensor? = null,
    val sensordatavalues: List<ScValue> = emptyList(),
)

@Serializable
private data class ScSensor(val id: Long = 0)

@Serializable
private data class ScValue(val value_type: String = "", val value: String = "")
