package com.carettafriends.ui.map

/**
 * Key-less MapLibre style: raster tiles straight from OpenStreetMap.
 *
 * DEV ONLY. This hits tile.openstreetmap.org directly, which is fine for local dev but
 * violates OSM's bulk/production tile-usage policy. For production either:
 *   - set an app-identifying User-Agent (MapLibre: HttpRequestUtil.setOkHttpClient(...))
 *     and stay within policy, OR
 *   - switch to your own / keyed tiles (MapTiler, Stadia, ...), OR
 *   - use MapLibre's key-less vector demo style: https://demotiles.maplibre.org/style.json
 */
internal const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "name": "OSM Raster",
  "sources": {
    "osm-raster": {
      "type": "raster",
      "tiles": [
        "https://a.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://b.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://c.tile.openstreetmap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "minzoom": 0,
      "maxzoom": 19,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": { "background-color": "#E9F2F6" }
    },
    {
      "id": "osm-raster",
      "type": "raster",
      "source": "osm-raster",
      "minzoom": 0,
      "maxzoom": 22
    }
  ]
}
"""