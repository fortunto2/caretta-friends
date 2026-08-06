package com.carettafriends.ui

import androidx.compose.runtime.Composable
import com.carettafriends.domain.GeoPoint

/**
 * Records a walked patrol — the GPS breadcrumb behind "I walked this beach today".
 *
 * The properties are Compose state, so a screen that reads [meters] while [isRecording] redraws as
 * the walk grows. The track NEVER leaves the device until the volunteer publishes it
 * (`CarettaRepository.publishPatrol`): a live position feed would put volunteers, and the nests
 * they walk to, on a map for anyone who asks.
 */
interface PatrolRecorder {
    val isRecording: Boolean
    /** Distance walked so far, metres. */
    val meters: Int
    /** Seconds since the walk started. */
    val seconds: Int
    /** Breadcrumbs collected so far. */
    val track: List<GeoPoint>

    /** Begin recording (asks for location permission if it isn't granted yet). */
    fun start()

    /** Stop and hand back the finished track (empty if nothing usable was recorded). */
    fun stop(): List<GeoPoint>
}

/**
 * Remember the platform patrol recorder, or null where patrols are recorded outside Compose.
 *
 * iOS returns null — its SwiftUI shell owns the map and records with `PatrolRecorder.swift`.
 * Android returns a real recorder backed by a foreground service; before this the "start patrol"
 * pill on Android was decoration that recorded nothing. [lang] localizes the service's notification,
 * which is what keeps the GPS alive while the phone is in a pocket.
 */
@Composable
expect fun rememberPatrolRecorder(lang: String): PatrolRecorder?
