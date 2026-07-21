import SwiftUI
import MapLibre        // module name = product name; classes are MLN-prefixed
import CoreLocation

// MARK: - Model passed in from SwiftUI

/// Lightweight point the SwiftUI layer supplies. `id` is echoed back on tap.
struct MapPoint: Identifiable, Equatable {
    let id: String
    let coordinate: CLLocationCoordinate2D
    var title: String? = nil
    var subtitle: String? = nil

    static func == (l: MapPoint, r: MapPoint) -> Bool {
        l.id == r.id &&
        l.coordinate.latitude == r.coordinate.latitude &&
        l.coordinate.longitude == r.coordinate.longitude &&
        l.title == r.title
    }
}

/// MLNPointAnnotation subclass that carries our stable id through delegate callbacks
/// (MLNPointAnnotation itself has no id field, only coordinate/title/subtitle).
final class IdentifiedAnnotation: MLNPointAnnotation {
    var pointID: String = ""
}

// MARK: - UIViewRepresentable

struct MapLibreView: UIViewRepresentable {
    /// Gazipaşa, Antalya, Türkiye.
    static let gazipasa = CLLocationCoordinate2D(latitude: 36.2700, longitude: 32.3100)

    var points: [MapPoint]
    var center: CLLocationCoordinate2D = MapLibreView.gazipasa
    var zoomLevel: Double = 11
    /// When false, tapping a pin fires onSelect but suppresses the title callout bubble.
    var showsCallout: Bool = true
    /// Fires with the tapped point's id.
    var onSelect: (String) -> Void = { _ in }
    /// Live patrol track (breadcrumb coordinates) drawn as a polyline.
    var track: [CLLocationCoordinate2D] = []

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: Self.osmRasterStyleURL())
        mapView.delegate = context.coordinator
        mapView.setCenter(center, zoomLevel: zoomLevel, animated: false)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // Keep OSM attribution reachable (required by the tile usage policy).
        mapView.attributionButton.isHidden = false
        context.coordinator.sync(points, on: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.parent = self          // keep closure/props fresh across SwiftUI updates
        context.coordinator.sync(points, on: mapView)
        context.coordinator.syncTrack(track, on: mapView)
    }

    // MARK: Coordinator = MLNMapViewDelegate

    final class Coordinator: NSObject, MLNMapViewDelegate {
        var parent: MapLibreView
        private var currentIDs: Set<String> = []
        private var polyline: MLNPolyline?
        private var trackCount = -1

        init(_ parent: MapLibreView) { self.parent = parent }

        /// Rebuild the patrol polyline only when the breadcrumb count changes (grows while recording).
        func syncTrack(_ coords: [CLLocationCoordinate2D], on mapView: MLNMapView) {
            guard coords.count != trackCount else { return }
            trackCount = coords.count
            if let old = polyline { mapView.removeAnnotation(old); polyline = nil }
            guard coords.count >= 2 else { return }
            var pts = coords
            let line = MLNPolyline(coordinates: &pts, count: UInt(pts.count))
            mapView.addAnnotation(line)
            polyline = line
        }

        // Patrol track styling (coral, matches the FAB).
        func mapView(_ mapView: MLNMapView, strokeColorForShapeAnnotation annotation: MLNShape) -> UIColor {
            UIColor(red: 0.98, green: 0.45, blue: 0.36, alpha: 1.0)
        }

        func mapView(_ mapView: MLNMapView, lineWidthForPolylineAnnotation annotation: MLNPolyline) -> CGFloat { 4 }

        /// Rebuild annotations only when the id set changes (fine for small counts).
        func sync(_ points: [MapPoint], on mapView: MLNMapView) {
            let ids = Set(points.map(\.id))
            guard ids != currentIDs else { return }
            currentIDs = ids
            if let existing = mapView.annotations { mapView.removeAnnotations(existing) }
            let annotations = points.map { p -> IdentifiedAnnotation in
                let a = IdentifiedAnnotation()
                a.pointID = p.id
                a.coordinate = p.coordinate
                a.title = p.title
                a.subtitle = p.subtitle
                return a
            }
            mapView.addAnnotations(annotations)
        }

        // Return nil -> MapLibre draws its built-in default marker for MLNPointAnnotation.
        func mapView(_ mapView: MLNMapView, imageFor annotation: MLNAnnotation) -> MLNAnnotationImage? {
            nil
        }

        // Enables the title/subtitle callout bubble.
        func mapView(_ mapView: MLNMapView, annotationCanShowCallout annotation: MLNAnnotation) -> Bool {
            parent.showsCallout
        }

        // THE TAP CALLBACK — invoked when the user taps/selects an annotation.
        func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
            guard let a = annotation as? IdentifiedAnnotation else { return }
            parent.onSelect(a.pointID)
            // Without a callout, immediately deselect so the SAME pin can fire again next tap.
            if !parent.showsCallout {
                mapView.deselectAnnotation(annotation, animated: false)
            }
        }

    }
}

// MARK: - Style helpers (no API key)

extension MapLibreView {
    /// OSM raster style (no key). MLNMapView has no "load style from JSON string" API on iOS,
    /// so we write the style to a temp file and hand back a file URL.
    /// NOTE: tile.openstreetmap.org is bound by the OSMF tile usage policy — OK for
    /// low-volume/dev use; swap for a paid raster/vector provider (MapTiler, Stadia, Jawg…) in production.
    static func osmRasterStyleURL() -> URL {
        let styleJSON = """
        {
          "version": 8,
          "sources": {
            "osm": {
              "type": "raster",
              "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
              "tileSize": 256,
              "minzoom": 0,
              "maxzoom": 19,
              "attribution": "© OpenStreetMap contributors"
            }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#e0e0e0" } },
            { "id": "osm", "type": "raster", "source": "osm" }
          ]
        }
        """
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("osm-raster-style.json")
        try? styleJSON.data(using: .utf8)?.write(to: url)
        return url
    }

    /// Simplest official alternative: MapLibre-hosted global vector demo tiles (no key, lower street detail).
    /// Use instead of osmRasterStyleURL() in MLNMapView(frame:styleURL:).
    static var demotilesStyleURL: URL { URL(string: "https://demotiles.maplibre.org/style.json")! }
}