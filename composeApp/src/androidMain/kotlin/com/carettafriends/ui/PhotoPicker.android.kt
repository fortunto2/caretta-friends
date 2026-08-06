package com.carettafriends.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.carettafriends.data.photoFingerprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Calendar

@Composable
actual fun rememberGalleryPicker(onPicked: (PickedPhoto?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onPicked(null)
        } else {
            // Copying a multi-megabyte JPEG, re-reading it for EXIF and hashing it are three file
            // passes; the picker's callback runs on the main thread, so they go to IO.
            scope.launch {
                val picked = withContext(Dispatchers.IO) {
                    runCatching {
                // Copy to app storage — a content:// grant is transient, a file path survives.
                val dest = File(context.filesDir, "nest_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                // NOTE: the Android photo picker always hands back a location-REDACTED copy, and
                // MediaStore.setRequireOriginal can't reach its picker URIs — verified on API 36.
                // So a gallery import here never has GPS (unlike iOS, where PHPicker keeps it) and
                // the form says so out loud. On Android, capture is the path to real coordinates.
                        val meta = context.contentResolver.openInputStream(uri)?.use { readExif(it) }
                        PickedPhoto(
                            path = dest.absolutePath,
                            exifEpochMillis = meta?.millis,
                            lat = meta?.lat,
                            lng = meta?.lng,
                            hash = photoFingerprint(dest.absolutePath),
                        )
                    }.getOrNull()
                }
                onPicked(picked)
            }
        }
    }
    return { launcher.launch("image/*") }
}

/**
 * Take a photo with the system camera and hand back the file plus the device's location.
 *
 * Android's camera app does not reliably write GPS into the EXIF (it's an opt-in setting, off by
 * default), so we stamp our own last known fix into the file — a nest without coordinates is close
 * to useless in the field. Replaces the mock viewfinder screen, whose shutter produced no photo.
 */
@Composable
actual fun rememberCameraCapture(onCaptured: (PickedPhoto?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = target
        if (!ok || file == null || !file.exists() || file.length() == 0L) {
            onCaptured(null)
            return@rememberLauncherForActivityResult
        }
        // Writing EXIF, copying into the gallery and hashing are all file work — off the main thread.
        scope.launch {
            val shot = withContext(Dispatchers.IO) {
                val fix = lastKnownFix(context)
                if (fix != null) {
                    runCatching {
                        ExifInterface(file.absolutePath).apply { setLatLong(fix.first, fix.second); saveAttributes() }
                    }
                }
                saveToGallery(context, file)
                PickedPhoto(
                    path = file.absolutePath,
                    exifEpochMillis = System.currentTimeMillis(),
                    lat = fix?.first,
                    lng = fix?.second,
                    hash = photoFingerprint(file.absolutePath),
                )
            }
            onCaptured(shot)
        }
    }
    val open = {
        val file = File(context.filesDir, "nest_${System.currentTimeMillis()}.jpg")
        target = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        runCatching { launcher.launch(uri) }.onFailure { onCaptured(null) }
    }
    // The app DECLARES android.permission.CAMERA, and Android then refuses to start IMAGE_CAPTURE
    // for a caller that hasn't been granted it ("Permission Denial: … with revoked permission") —
    // even though the camera app does the capturing. So ask first, then open.
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) open() else onCaptured(null)
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            open()
        } else {
            askCamera.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Copy the finished photo into the device gallery, in a "Caretta Friends" album.
 *
 * The app's own copy lives in private storage and dies with an uninstall — a volunteer would lose
 * a season of nest photos to a reinstall. iOS already saves to a Photos album; this is the Android
 * half. Best-effort and silent: the nest record is what matters, the album is a courtesy.
 * MediaStore's relative-path album needs API 29; below that we'd need storage permission, which
 * isn't worth asking for.
 */
private fun saveToGallery(context: Context, file: File) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Caretta Friends")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }
}

/** Last known position from any enabled provider, or null without a fix / permission. */
private fun lastKnownFix(context: Context): Pair<Double, Double>? = runCatching {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    lm.getProviders(true)
        .mapNotNull { p -> lm.getLastKnownLocation(p) }
        .maxByOrNull { it.time }
        ?.let { it.latitude to it.longitude }
}.getOrNull()

/** What a photo's EXIF can tell us: when and where it was taken. Any field may be missing. */
private data class ExifMeta(val millis: Long?, val lat: Double?, val lng: Double?)

/** Read EXIF capture time + GPS fix. The GPS tags are what turn an imported photo into a real nest
 *  location instead of a pin dropped on the beach centre. */
private fun readExif(input: InputStream): ExifMeta? = runCatching {
    val exif = ExifInterface(input)
    val gps = exif.latLong          // [lat, lng] with the N/S · E/W sign already applied, or null
    ExifMeta(readExifMillis(exif), gps?.getOrNull(0), gps?.getOrNull(1))
}.getOrNull()

/** Parse EXIF DateTimeOriginal ("yyyy:MM:dd HH:mm:ss") → epoch millis, or null. */
private fun readExifMillis(exif: ExifInterface): Long? = runCatching {
    val dt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null
    val (dPart, tPart) = (dt.split(" ") + "00:00:00").let { it[0] to it[1] }
    val d = dPart.split(":").map { it.toInt() }
    val t = (tPart.split(":") + listOf("0", "0", "0")).map { it.toIntOrNull() ?: 0 }
    Calendar.getInstance().apply { set(d[0], d[1] - 1, d[2], t[0], t[1], t[2]) }.timeInMillis
}.getOrNull()
