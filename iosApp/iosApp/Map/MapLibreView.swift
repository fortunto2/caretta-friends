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
    var isBeach: Bool = false
}

// MARK: - UIViewRepresentable

struct MapLibreView: UIViewRepresentable {
    /// Gazipaşa, Antalya, Türkiye.
    static let gazipasa = CLLocationCoordinate2D(latitude: 36.2700, longitude: 32.3100)

    var points: [MapPoint]
    var beaches: [MapPoint] = []
    var center: CLLocationCoordinate2D = MapLibreView.gazipasa
    var zoomLevel: Double = 11
    /// When false, tapping a pin fires onSelect but suppresses the title callout bubble.
    var showsCallout: Bool = true
    /// Fires with the tapped point's id.
    var onSelect: (String) -> Void = { _ in }
    /// Live patrol track (breadcrumb coordinates) drawn as a polyline.
    var track: [CLLocationCoordinate2D] = []
    /// Beach sand outlines (OSM polygons) drawn as translucent teal highlights.
    var beachPolygons: [[CLLocationCoordinate2D]] = []

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: Self.osmRasterStyleURL())
        mapView.delegate = context.coordinator
        mapView.setCenter(center, zoomLevel: zoomLevel, animated: false)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // Keep OSM attribution reachable (required by the tile usage policy).
        mapView.attributionButton.isHidden = false
        context.coordinator.sync(nests: points, beaches: beaches, on: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.parent = self          // keep closure/props fresh across SwiftUI updates
        context.coordinator.sync(nests: points, beaches: beaches, on: mapView)
        context.coordinator.syncTrack(track, on: mapView)
        context.coordinator.syncBeachPolygons(beachPolygons, on: mapView)
    }

    // MARK: Coordinator = MLNMapViewDelegate

    final class Coordinator: NSObject, MLNMapViewDelegate {
        var parent: MapLibreView
        private var currentIDs: Set<String> = []
        private var polyline: MLNPolyline?
        private var trackCount = -1
        private var beachPolys: [MLNPolygon] = []
        private var beachPolyCount = -1

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

        // Beach sand outlines (OSM polygons) drawn as translucent teal highlights.
        func syncBeachPolygons(_ polys: [[CLLocationCoordinate2D]], on mapView: MLNMapView) {
            guard polys.count != beachPolyCount else { return }
            beachPolyCount = polys.count
            if !beachPolys.isEmpty { mapView.removeAnnotations(beachPolys); beachPolys = [] }
            for coords in polys where coords.count >= 3 {
                var c = coords
                let poly = MLNPolygon(coordinates: &c, count: UInt(c.count))
                mapView.addAnnotation(poly)
                beachPolys.append(poly)
            }
        }

        private let teal = UIColor(red: 0.09, green: 0.55, blue: 0.62, alpha: 1.0)
        private let coral = UIColor(red: 0.98, green: 0.45, blue: 0.36, alpha: 1.0)

        // Beach polygons = teal, patrol track = coral.
        func mapView(_ mapView: MLNMapView, strokeColorForShapeAnnotation annotation: MLNShape) -> UIColor {
            annotation is MLNPolygon ? teal : coral
        }

        func mapView(_ mapView: MLNMapView, fillColorForPolygonAnnotation annotation: MLNPolygon) -> UIColor { teal }

        func mapView(_ mapView: MLNMapView, alphaForShapeAnnotation annotation: MLNShape) -> CGFloat {
            annotation is MLNPolygon ? 0.28 : 0.9
        }

        func mapView(_ mapView: MLNMapView, lineWidthForPolylineAnnotation annotation: MLNPolyline) -> CGFloat { 4 }

        /// Rebuild point annotations (nests + beaches) when the id set changes; keeps the polyline.
        func sync(nests: [MapPoint], beaches: [MapPoint], on mapView: MLNMapView) {
            let ids = Set(nests.map(\.id) + beaches.map(\.id))
            guard ids != currentIDs else { return }
            currentIDs = ids
            let toRemove = (mapView.annotations ?? []).filter { $0 is IdentifiedAnnotation }
            mapView.removeAnnotations(toRemove)
            var anns: [IdentifiedAnnotation] = []
            for p in nests {
                let a = IdentifiedAnnotation()
                a.pointID = p.id; a.isBeach = false
                a.coordinate = p.coordinate; a.title = p.title; a.subtitle = p.subtitle
                anns.append(a)
            }
            for b in beaches {
                let a = IdentifiedAnnotation()
                a.pointID = b.id; a.isBeach = true
                a.coordinate = b.coordinate; a.title = b.title
                anns.append(a)
            }
            mapView.addAnnotations(anns)
        }

        // Beaches = a teal dot; nests = MapLibre's built-in red pin (return nil).
        func mapView(_ mapView: MLNMapView, imageFor annotation: MLNAnnotation) -> MLNAnnotationImage? {
            guard let a = annotation as? IdentifiedAnnotation, a.isBeach else { return nil }
            let id = "beach-dot"
            if let img = mapView.dequeueReusableAnnotationImage(withIdentifier: id) { return img }
            return MLNAnnotationImage(image: Coordinator.beachDot(), reuseIdentifier: id)
        }

        // Beaches always show their name; nests follow showsCallout.
        func mapView(_ mapView: MLNMapView, annotationCanShowCallout annotation: MLNAnnotation) -> Bool {
            if let a = annotation as? IdentifiedAnnotation, a.isBeach { return true }
            return parent.showsCallout
        }

        // THE TAP CALLBACK — nest tap opens its detail; beach tap just shows the name callout.
        func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
            guard let a = annotation as? IdentifiedAnnotation else { return }
            if a.isBeach { return }
            parent.onSelect(a.pointID)
            if !parent.showsCallout {
                mapView.deselectAnnotation(annotation, animated: false)
            }
        }

        /// A small teal dot for beach markers (distinct from red nest pins).
        static func beachDot() -> UIImage {
            let size = CGSize(width: 22, height: 22)
            return UIGraphicsImageRenderer(size: size).image { ctx in
                let rect = CGRect(x: 2, y: 2, width: 18, height: 18)
                UIColor(red: 0.09, green: 0.55, blue: 0.62, alpha: 1.0).setFill()
                ctx.cgContext.fillEllipse(in: rect)
                UIColor.white.setStroke()
                ctx.cgContext.setLineWidth(2.5)
                ctx.cgContext.strokeEllipse(in: rect)
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