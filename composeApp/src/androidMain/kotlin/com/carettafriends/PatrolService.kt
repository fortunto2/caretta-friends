package com.carettafriends

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.carettafriends.content.appStrings
import com.carettafriends.ui.PatrolTrack

/**
 * Records a patrol's GPS track while the phone is in the volunteer's pocket.
 *
 * A patrol is an hour of walking a dark beach — the screen is off for almost all of it. Android
 * stops delivering location to a backgrounded app that isn't running a foreground service, so the
 * recorder used to capture the first few metres of a walk and nothing else. The notification is not
 * decoration: it is the permission slip for keeping the GPS on, and it shows the distance so far.
 *
 * Started only while the app is on screen, so plain ACCESS_FINE_LOCATION is enough — the app never
 * asks for background location, which volunteers are right to refuse.
 */
class PatrolService : Service(), LocationListener {

    private val lm by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private var lang = "en"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }
        lang = intent?.getStringExtra(EXTRA_LANG) ?: "en"
        startForegroundNotice()
        if (!hasLocationPermission()) {
            stopRecording()
            return START_NOT_STICKY
        }
        PatrolTrack.begin()
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        runCatching {
            providers.forEach { p -> lm.requestLocationUpdates(p, UPDATE_INTERVAL_MS, 0f, this) }
        }.onFailure { stopRecording() }
        // START_STICKY would restart us with a null intent after a kill, mid-walk, with no track.
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        PatrolTrack.onFix(location.latitude, location.longitude, location.accuracy, location.hasAccuracy())
        notify(PatrolTrack.meters)
    }

    override fun onDestroy() {
        runCatching { lm.removeUpdates(this) }
        PatrolTrack.finish()
        super.onDestroy()
    }

    private fun stopRecording() {
        runCatching { lm.removeUpdates(this) }
        PatrolTrack.finish()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startForegroundNotice() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Patrol", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = buildNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notify(meters: Int) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(meters))
    }

    private fun buildNotification(meters: Int): Notification {
        val s = appStrings(lang)
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(s.startPatrol)
            .setContentText("$meters m")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "patrol"
        private const val NOTIFICATION_ID = 4201
        private const val UPDATE_INTERVAL_MS = 2_000L
        const val ACTION_STOP = "com.carettafriends.PATROL_STOP"
        const val EXTRA_LANG = "lang"
    }
}
