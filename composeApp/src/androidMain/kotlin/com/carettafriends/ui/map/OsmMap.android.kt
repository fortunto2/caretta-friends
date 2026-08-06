package com.carettafriends.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
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
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

// Protected beaches = green, unprotected = amber, nests = coral, community hubs = orange. Circles +
// polygon fills are basic GL fills → they render on the emulator's software GL (SwiftShader).
private const val GREEN = "#2E9E5B"
private const val AMBER = "#E0A82E"
private const val NEST = "#E0533D"
private const val COMMUNITY = "#F97316"
private const val VIOLATION = "#B00020"
// Nest lifecycle dot colours (see nestMapPhase): soon=amber, emerging=vivid green (act on it!),
// excavated=sea, removed(>2d / lost)=grey. incubating falls back to NEST.
private const val EMERGING = "#22C55E"
private const val EXCAVATED_COL = "#0F7A82"
private const val REMOVED_COL = "#9AA5A3"

private fun phaseColor(phase: String): String = when (phase) {
    "soon" -> AMBER
    "emerging" -> EMERGING
    "excavated" -> EXCAVATED_COL
    "removed" -> REMOVED_COL
    else -> NEST
}
private const val BEACH_SRC = "cf-beaches-src"
private const val BEACH_FILL = "cf-beaches-fill"
private const val BEACH_LINE = "cf-beaches-line"
private const val COMMUNITY_ICON = "cf-community-icon"

@Composable
actual fun OsmMap(
    modifier: Modifier,
    points: List<MapMarker>,
    onClick: (String) -> Unit,
    onBeachTap: (String) -> Unit,
    onCommunityTap: (String) -> Unit,
    focus: com.carettafriends.domain.GeoPoint?,
    onFocusConsumed: () -> Unit,
    recenterTick: Int,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(ctx)
        val opts = MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)
        MapView(ctx, opts).apply { onCreate(null) }
    }
    var circleManager by remember { mutableStateOf<CircleManager?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    // My-location: show the volunteer's position as a heading arrow (RenderMode.COMPASS), matching iOS.
    var locationGranted by remember {
        mutableStateOf(ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationGranted = granted
    }
    LaunchedEffect(Unit) {
        if (!locationGranted) permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LaunchedEffect(mapRef, mapStyle, locationGranted) {
        val map = mapRef
        val style = mapStyle
        if (map != null && style != null && locationGranted) {
            runCatching { enableUserLocation(ctx, map, style) }
        }
    }

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
                    style.addSource(GeoJsonSource(BEACH_SRC))
                    // Subtle fill so beaches read as "areas", not "selected"; colour by protection.
                    val colorByProtected = Expression.switchCase(
                        Expression.get("protected"), Expression.color(android.graphics.Color.parseColor(GREEN)),
                        Expression.color(android.graphics.Color.parseColor(AMBER)),
                    )
                    style.addLayer(
                        FillLayer(BEACH_FILL, BEACH_SRC).withProperties(
                            PropertyFactory.fillColor(colorByProtected),
                            PropertyFactory.fillOpacity(0.18f),
                        ),
                    )
                    style.addLayer(
                        LineLayer(BEACH_LINE, BEACH_SRC).withProperties(
                            PropertyFactory.lineColor(colorByProtected),
                            PropertyFactory.lineWidth(2.5f),
                        ),
                    )
                    val cm = CircleManager(mapView, map, style)
                    cm.addClickListener { circle ->
                        val data = circle.data?.asString ?: return@addClickListener false
                        when {
                            data.startsWith("n:") -> onClick(data.removePrefix("n:"))
                            data.startsWith("b:") -> onBeachTap(data.removePrefix("b:"))
                            data.startsWith("c:") -> onCommunityTap(data.removePrefix("c:"))
                        }
                        true
                    }
                    // Tap a beach polygon (not a circle) → open its tooltip.
                    map.addOnMapClickListener { latLng ->
                        val pt = map.projection.toScreenLocation(latLng)
                        val feats = map.queryRenderedFeatures(pt, BEACH_FILL)
                        val id = feats.firstOrNull()?.getStringProperty("id")
                        if (id != null) { onBeachTap(id); true } else false
                    }
                    // Community hubs = orange SQUARE icons (SymbolManager, distinct from round circles).
                    style.addImage(COMMUNITY_ICON, communityBitmap())
                    val sm = SymbolManager(mapView, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                    }
                    sm.addClickListener { sym ->
                        val data = sym.data?.asString
                        if (data != null && data.startsWith("c:")) { onCommunityTap(data.removePrefix("c:")); true } else false
                    }
                    circleManager = cm
                    symbolManager = sm
                    mapStyle = style
                    mapRef = map
                    centerCamera(map, points)
                }
            }
            mapView
        },
        modifier = modifier,
    )

    val cm = circleManager
    val sm = symbolManager
    val style = mapStyle
    LaunchedEffect(cm, sm, style, points) {
        cm?.let { renderCircles(it, points.filter { m -> (!m.isBeach || m.polygon.size < 3) && !m.isCommunity }) }
        style?.let { updateBeachPolygons(it, points.filter { m -> m.isBeach && m.polygon.size >= 3 }) }
        sm?.let { renderCommunitySymbols(it, points.filter { m -> m.isCommunity }) }
    }

    // One-shot: a nest's geo card requested this camera position → animate onto it, then clear.
    val map = mapRef
    LaunchedEffect(map, focus) {
        if (map != null && focus != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(focus.lat, focus.lng), 17.0))
            onFocusConsumed()
        }
    }

    // "Locate me" — one camera move to the puck's position. Deliberately NOT a camera mode that
    // follows the user: the volunteer is usually panning the beach while walking it, and a camera
    // that keeps snapping back fights them.
    LaunchedEffect(recenterTick) {
        if (recenterTick > 0) {
            val me = runCatching { map?.locationComponent?.lastKnownLocation }.getOrNull()
            if (me != null) {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(me.latitude, me.longitude), 17.0))
            }
        }
    }
}

private fun renderCommunitySymbols(sm: SymbolManager, points: List<MapMarker>) {
    sm.deleteAll()
    points.forEach { m ->
        sm.create(
            SymbolOptions()
                .withLatLng(LatLng(m.lat, m.lng))
                .withIconImage(COMMUNITY_ICON)
                .withIconSize(1.0f)
                .withData(JsonPrimitive("c:${m.id}")),
        )
    }
}

/** A rounded orange SQUARE marking a community hub — distinct from round beach/nest circles. */
private fun communityBitmap(): Bitmap {
    val size = 54
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rect = RectF(6f, 6f, size - 6f, size - 6f)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(COMMUNITY)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, 11f, 11f, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(rect, 11f, 11f, stroke)
    return bmp
}

private fun renderCircles(cm: CircleManager, points: List<MapMarker>) {
    cm.deleteAll()
    points.forEach { m ->
        // Community hubs are square SymbolManager icons, not circles (handled separately).
        val color = when {
            m.isViolation -> VIOLATION
            !m.isBeach -> phaseColor(m.phase)
            m.protected -> GREEN
            else -> AMBER
        }
        cm.create(
            CircleOptions()
                .withLatLng(LatLng(m.lat, m.lng))
                .withCircleRadius(if (m.isBeach) 6.5f else 8f)
                .withCircleColor(color)
                .withCircleStrokeColor("#FFFFFF")
                .withCircleStrokeWidth(2.5f)
                .withData(JsonPrimitive(if (m.isBeach) "b:${m.id}" else "n:${m.id}")),
        )
    }
}

private fun updateBeachPolygons(style: Style, beaches: List<MapMarker>) {
    val src = style.getSourceAs<GeoJsonSource>(BEACH_SRC) ?: return
    val features = beaches.map { m ->
        val ring = m.polygon.map { Point.fromLngLat(it.lng, it.lat) }.toMutableList()
        val f = ring.first()
        val l = ring.last()
        if (f.longitude() != l.longitude() || f.latitude() != l.latitude()) ring.add(f)
        Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
            addStringProperty("id", m.id)
            addBooleanProperty("protected", m.protected)
        }
    }
    src.setGeoJson(FeatureCollection.fromFeatures(features))
}

/** Turn on the MapLibre LocationComponent (a location puck) without hijacking the camera.
 *  Caller must have checked ACCESS_FINE_LOCATION (guarded), hence @SuppressLint.
 *  NB: RenderMode.NORMAL, not COMPASS — the compass renderer's async magnetometer handler calls
 *  getSourceAs during style transitions and crashes ("newer style is loading"), a MapLibre race.
 *  Same race also fires via the stale-state timer: a real GPS fix → updateLocation →
 *  StaleStateManager → refreshSource → getSourceAs while the style is (re)loading → crash. We
 *  don't grey out a "stale" puck anyway, so disable stale state entirely to kill that path. */
@SuppressLint("MissingPermission")
private fun enableUserLocation(ctx: Context, map: MapLibreMap, style: Style) {
    val lc = map.locationComponent
    val options = LocationComponentOptions.builder(ctx)
        .enableStaleState(false)
        .build()
    lc.activateLocationComponent(
        LocationComponentActivationOptions.builder(ctx, style)
            .locationComponentOptions(options)
            .useDefaultLocationEngine(true)
            .build(),
    )
    lc.isLocationComponentEnabled = true
    lc.renderMode = RenderMode.NORMAL    // stable location puck (COMPASS crashes on the style race)
    lc.cameraMode = CameraMode.NONE      // show me, but don't seize the camera
}

private fun centerCamera(map: MapLibreMap, points: List<MapMarker>) {
    val beaches = points.filter { it.isBeach && !it.id.startsWith("pa:") }
    val focus = beaches.ifEmpty { points }
    val target = if (focus.isEmpty()) {
        LatLng(36.27, 32.31)
    } else {
        LatLng(focus.map { it.lat }.average(), focus.map { it.lng }.average())
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
