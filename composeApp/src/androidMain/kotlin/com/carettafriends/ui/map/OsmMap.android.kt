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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
            !m.isBeach -> NEST
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
