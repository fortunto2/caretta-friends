package com.carettafriends.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonPrimitive
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.CircleManager
import org.maplibre.android.plugins.annotation.CircleOptions

// Markers are drawn with CircleManager (GL fills) rather than SymbolManager (SDF icon layer) —
// circles render reliably on the emulator's software GL (SwiftShader), where symbol icons do not.
private const val NEST_COLOR = "#E0533D" // coral
private const val BEACH_COLOR = "#178C9E" // teal

@Composable
actual fun OsmMap(modifier: Modifier, points: List<MapMarker>, onClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(ctx)
        // textureMode(true) → TextureView-backed GL, the most emulator-friendly rendering path.
        val opts = MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)
        MapView(ctx, opts).apply { onCreate(null) }
    }
    var circleManager by remember { mutableStateOf<CircleManager?>(null) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(osmRasterStyle())) { style ->
                    val cm = CircleManager(mapView, map, style)
                    cm.addClickListener { circle ->
                        val data = circle.data?.asString
                        // "n:<id>" = nest (navigate); "b:<id>" = beach (ignore).
                        if (data != null && data.startsWith("n:")) onClick(data.removePrefix("n:"))
                        true
                    }
                    circleManager = cm
                    centerCamera(map, points)
                }
            }
            mapView
        },
        modifier = modifier,
    )

    // Read circleManager in a composable scope so it (and points) drive rendering reactively.
    val cm = circleManager
    LaunchedEffect(cm, points) {
        cm?.let { renderCircles(it, points) }
    }
}

private fun renderCircles(cm: CircleManager, points: List<MapMarker>) {
    cm.deleteAll()
    points.forEach { m ->
        cm.create(
            CircleOptions()
                .withLatLng(LatLng(m.lat, m.lng))
                .withCircleRadius(if (m.isBeach) 7f else 8f)
                .withCircleColor(if (m.isBeach) BEACH_COLOR else NEST_COLOR)
                .withCircleStrokeColor("#FFFFFF")
                .withCircleStrokeWidth(2.5f)
                .withData(JsonPrimitive(if (m.isBeach) "b:${m.id}" else "n:${m.id}")),
        )
    }
}

private fun centerCamera(map: MapLibreMap, points: List<MapMarker>) {
    val target = if (points.isEmpty()) {
        LatLng(36.27, 32.31)
    } else {
        LatLng(points.map { it.lat }.average(), points.map { it.lng }.average())
    }
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 11.5))
}

/** OSM raster style (no API key). tile.openstreetmap.org — OK for low-volume/dev use. */
private fun osmRasterStyle(): String = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "bg", "type": "background", "paint": { "background-color": "#e0e0e0" } },
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
""".trimIndent()
