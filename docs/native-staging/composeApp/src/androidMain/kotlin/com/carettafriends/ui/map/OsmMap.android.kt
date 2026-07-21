package com.carettafriends.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

private const val GAZIPASA_LAT = 36.27
private const val GAZIPASA_LNG = 32.31
private const val DEFAULT_ZOOM = 12.0
private const val PIN_ICON_ID = "caretta-pin"

@Composable
actual fun OsmMap(
    modifier: Modifier,
    points: List<MapMarker>,
    onClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Latest data/lambda without rebuilding the (expensive, single-use) MapView.
    val currentPoints by rememberUpdatedState(points)
    val currentOnClick by rememberUpdatedState(onClick)

    // symbol.id (Long, assigned by SymbolManager) -> our stable MapMarker.id.
    val idLookup = remember { mutableMapOf<Long, String>() }
    // Holder so AndroidView.update{} can re-sync markers after the async style load.
    val managerHolder = remember { arrayOfNulls<SymbolManager>(1) }
    // Saved/restored map state (task: onSaveInstanceState).
    val stateBundle = remember { Bundle() }

    // MapLibre.getInstance MUST run before the first MapView is constructed.
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    // Full MapView lifecycle. addObserver() synchronously REPLAYS events up to the
    // current state, so ON_CREATE/START/RESUME fire even when OsmMap first composes on
    // an already-RESUMED screen (otherwise the map would stay blank).
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE ->
                    mapView.onCreate(if (stateBundle.isEmpty) null else stateBundle)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onSaveInstanceState(stateBundle) // task: onSaveInstanceState
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                // Load key-less OSM raster style; SymbolManager/addImage only AFTER it is fully loaded.
                map.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE)) { style ->
                    style.addImage(PIN_ICON_ID, buildPinBitmap())

                    val symbolManager = SymbolManager(mapView, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                    }
                    symbolManager.addClickListener { symbol ->
                        idLookup[symbol.id]?.let(currentOnClick)
                        true // consume
                    }
                    managerHolder[0] = symbolManager

                    syncMarkers(symbolManager, idLookup, currentPoints)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(cameraTarget(currentPoints))
                        .zoom(DEFAULT_ZOOM)
                        .build()
                }
            }
            mapView
        },
        update = {
            // Re-sync when `points` change after the manager exists.
            managerHolder[0]?.let { syncMarkers(it, idLookup, currentPoints) }
        },
    )
}

private fun syncMarkers(
    manager: SymbolManager,
    idLookup: MutableMap<Long, String>,
    points: List<MapMarker>,
) {
    manager.deleteAll()
    idLookup.clear()
    points.forEach { m ->
        val symbol = manager.create(
            SymbolOptions()
                .withLatLng(LatLng(m.lat, m.lng))
                .withIconImage(PIN_ICON_ID)
                .withIconSize(1.0f),
        )
        idLookup[symbol.id] = m.id
    }
}

private fun cameraTarget(points: List<MapMarker>): LatLng =
    if (points.isEmpty()) {
        LatLng(GAZIPASA_LAT, GAZIPASA_LNG)
    } else {
        LatLng(points.map { it.lat }.average(), points.map { it.lng }.average())
    }

/** Self-contained coral teardrop pin (no drawable resource required in KMP). */
private fun buildPinBitmap(): Bitmap {
    val w = 72
    val h = 96
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E8663C") }
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val cx = w / 2f
    val cy = w / 2f
    val r = w / 2f - 4f
    canvas.drawCircle(cx, cy, r + 3f, ring)
    canvas.drawCircle(cx, cy, r, body)
    val pointer = Path().apply {
        moveTo(cx - r * 0.55f, cy + r * 0.55f)
        lineTo(cx + r * 0.55f, cy + r * 0.55f)
        lineTo(cx, h.toFloat() - 4f)
        close()
    }
    canvas.drawPath(pointer, body)
    canvas.drawCircle(cx, cy, r * 0.42f, ring)
    return bmp
}