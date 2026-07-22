package com.carettafriends.domain

import kotlinx.serialization.Serializable

/** Air-quality level from particulate matter. DUST = sand/dust in the air (PM10 ≫ PM2.5). */
enum class AirLevel { GOOD, MODERATE, UNHEALTHY, DUST }

/**
 * A merged air-quality reading for a beach area, from open citizen sensors (Sensor.Community — the
 * same source the user's Air Signal project uses; Gazipaşa = sensor 77955). Supplementary layer:
 * present only where sensors exist. High dust → patrolling is not advised.
 */
@Serializable
data class AirStatus(
    val pm25: Double,
    val pm10: Double,
    val sensors: Int,
    val level: AirLevel,
    /** false when the air is bad enough that patrolling / lingering outside isn't advisable. */
    val patrolAdvisable: Boolean,
    val advice: String,
    val fetchedAtMillis: Long = 0,
)

/**
 * Classify a PM2.5 / PM10 reading. Dust is keyed on the PM10:PM2.5 ratio (sand/dust storms push PM10
 * far above PM2.5) — mirrors the user's airq `classify_source`. Thresholds follow common PM2.5 bands.
 */
fun classifyAir(pm25: Double, pm10: Double, sensors: Int, nowMillis: Long): AirStatus {
    val ratio = if (pm25 > 0.0) pm10 / pm25 else 0.0
    val (level, patrolOk, advice) = when {
        ratio > 4.0 && pm10 > 50.0 ->
            Triple(AirLevel.DUST, false, "Dust/sand in the air — patrolling not advised. Wear a mask if you go out.")
        pm25 > 55.0 ->
            Triple(AirLevel.UNHEALTHY, false, "Unhealthy air — better to postpone patrols today.")
        pm25 > 25.0 ->
            Triple(AirLevel.MODERATE, true, "Moderate air — sensitive people take it easy.")
        else ->
            Triple(AirLevel.GOOD, true, "Air is clean — good for patrolling.")
    }
    return AirStatus(
        pm25 = pm25, pm10 = pm10, sensors = sensors,
        level = level, patrolAdvisable = patrolOk, advice = advice, fetchedAtMillis = nowMillis,
    )
}
