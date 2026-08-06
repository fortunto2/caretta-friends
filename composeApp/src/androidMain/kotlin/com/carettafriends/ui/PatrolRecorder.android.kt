package com.carettafriends.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.carettafriends.PatrolService
import com.carettafriends.domain.GeoPoint
import com.carettafriends.domain.distanceMeters

/**
 * The walk in progress, as Compose state.
 *
 * It lives here rather than inside the recorder because the thing doing the recording is a
 * [PatrolService] — a walk outlives the screen that started it, and has to outlive the screen lock:
 * a patrol is an hour on a dark beach with the phone in a pocket.
 */
internal object PatrolTrack : PatrolRecorder {

    override var isRecording by mutableStateOf(false)
        private set
    override var seconds by mutableStateOf(0)
        private set
    override var track by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    /** Metres as a Double: truncating each 3–4 m leg lost ~10% of a long walk. */
    private var walked by mutableStateOf(0.0)
    override val meters: Int get() = walked.toInt()

    private var startedAtMs = 0L

    /** Wired by the composable, which is the only place with a Context and a permission launcher. */
    internal var launcher: (() -> Unit)? = null
    internal var stopper: (() -> Unit)? = null

    override fun start() { launcher?.invoke() }

    override fun stop(): List<GeoPoint> {
        val finished = track
        stopper?.invoke()
        return finished
    }

    internal fun begin() {
        track = emptyList()
        walked = 0.0
        seconds = 0
        startedAtMs = SystemClock.elapsedRealtime()
        isRecording = true
    }

    internal fun finish() { isRecording = false }

    internal fun onFix(lat: Double, lng: Double, accuracy: Float, hasAccuracy: Boolean) {
        if (!isRecording) return
        // Drop the jitter a phone emits while standing still: a bad fix, or a "step" too small to
        // be a step, would inflate the walked distance by hundreds of metres over an hour.
        if (hasAccuracy && accuracy > 30f) return
        val point = GeoPoint(lat, lng)
        val last = track.lastOrNull()
        val step = last?.let { distanceMeters(it, point) } ?: 0.0
        if (last != null && step < 3.0) return
        track = track + point
        walked += step
        seconds = ((SystemClock.elapsedRealtime() - startedAtMs) / 1000).toInt()
    }
}

@Composable
actual fun rememberPatrolRecorder(lang: String): PatrolRecorder? {
    val context = LocalContext.current.applicationContext
    // Asking at the moment the volunteer taps "start patrol" is the only place the permissions make
    // sense — the answer comes back here and starts the walk without a second tap. Notifications are
    // asked for alongside location on purpose: the ongoing notice is how the volunteer can see that
    // we're holding their GPS open, and without it Android just hides that fact.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) context.startPatrol(lang)
    }
    remember(lang) {
        PatrolTrack.launcher = {
            if (context.hasLocationPermission()) {
                context.startPatrol(lang)
            } else {
                ask.launch(
                    buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray(),
                )
            }
        }
        PatrolTrack.stopper = {
            context.startService(
                Intent(context, PatrolService::class.java).setAction(PatrolService.ACTION_STOP),
            )
        }
        PatrolTrack
    }
    // Nothing is disposed here on purpose: the service owns the GPS, and a walk must survive
    // leaving the map — opening the nest you just found used to destroy the whole recording.
    return PatrolTrack
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.startPatrol(lang: String) {
    val intent = Intent(this, PatrolService::class.java).putExtra(PatrolService.EXTRA_LANG, lang)
    ContextCompat.startForegroundService(this, intent)
}
