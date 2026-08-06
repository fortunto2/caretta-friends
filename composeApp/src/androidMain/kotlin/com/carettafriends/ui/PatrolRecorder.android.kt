package com.carettafriends.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.distanceMeters

@Composable
actual fun rememberPatrolRecorder(): PatrolRecorder? {
    val context = LocalContext.current
    val recorder = remember { AndroidPatrolRecorder(context) }
    // Asking at the moment the volunteer taps "start patrol" is the only place the permission makes
    // sense — the answer comes back here and starts the walk without a second tap.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recorder.beginUpdates()
    }
    recorder.requestPermission = { ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    // A walk must not keep the GPS running after the map is gone.
    DisposableEffect(recorder) { onDispose { recorder.dispose() } }
    return recorder
}

/**
 * GPS track recorder over [LocationManager] (no Play Services dependency — the app must work on
 * phones without them, and MapLibre already runs on the plain platform provider).
 */
private class AndroidPatrolRecorder(private val context: Context) : PatrolRecorder, LocationListener {

    override var isRecording by mutableStateOf(false)
        private set
    override var meters by mutableStateOf(0)
        private set
    override var seconds by mutableStateOf(0)
        private set
    override var track by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    /** Set by the composable — asks for location permission and starts once it's granted. */
    var requestPermission: () -> Unit = {}

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var startedAtMs = 0L

    override fun start() {
        if (isRecording) return
        if (hasPermission()) beginUpdates() else requestPermission()
    }

    fun beginUpdates() {
        if (isRecording || !hasPermission()) return
        track = emptyList()
        meters = 0
        seconds = 0
        startedAtMs = SystemClock.elapsedRealtime()
        isRecording = true
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        runCatching {
            providers.forEach { p -> lm.requestLocationUpdates(p, 2_000L, 0f, this) }
        }.onFailure { isRecording = false }
    }

    override fun stop(): List<GeoPoint> {
        if (!isRecording) return emptyList()
        runCatching { lm.removeUpdates(this) }
        isRecording = false
        return track
    }

    fun dispose() {
        runCatching { lm.removeUpdates(this) }
        isRecording = false
    }

    override fun onLocationChanged(location: Location) {
        if (!isRecording) return
        // Drop the jitter a phone emits while standing still: a bad fix, or a "step" too small to
        // be a step, would inflate the walked distance by hundreds of metres over an hour.
        if (location.hasAccuracy() && location.accuracy > 30f) return
        val point = GeoPoint(location.latitude, location.longitude)
        val last = track.lastOrNull()
        val step = last?.let { distanceMeters(it, point) } ?: 0.0
        if (last != null && step < 3.0) return
        track = track + point
        meters += step.toInt()
        seconds = ((SystemClock.elapsedRealtime() - startedAtMs) / 1000).toInt()
    }

    @Deprecated("Required by LocationListener on API < 30; never called on newer Android.")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
