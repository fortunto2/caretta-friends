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
    /// Beach dot colour: true = protected (green), false = unprotected (amber), nil = nest (red pin).
    var protectedBeach: Bool? = nil
    /// A community beach (tap → beach card) vs a baked overview area (name callout only).
    var openable: Bool = false

    static func == (l: MapPoint, r: MapPoint) -> Bool {
        l.id == r.id &&
        l.coordinate.latitude == r.coordinate.latitude &&
        l.coordinate.longitude == r.coordinate.longitude &&
        l.title == r.title &&
        l.protectedBeach == r.protectedBeach &&
        l.openable == r.openable
    }
}

/// A beach sand outline for the map, carrying its id (for tap → detail) and protected flag (for colour).
struct BeachPolygon: Equatable {
    let id: String
    let coords: [CLLocationCoordinate2D]
    let isProtected: Bool

    static func == (l: BeachPolygon, r: BeachPolygon) -> Bool {
        l.id == r.id && l.isProtected == r.isProtected && l.coords.count == r.coords.count
    }
}

/// MLNPointAnnotation subclass that carries our stable id through delegate callbacks
/// (MLNPointAnnotation itself has no id field, only coordinate/title/subtitle).
final class IdentifiedAnnotation: MLNPointAnnotation {
    var pointID: String = ""
    var isBeach: Bool = false
    /// true = protected (green dot), false = unprotected (amber dot); ignored for nests.
    var protectedBeach: Bool = true
    /// Community beach (tap opens the beach card) vs baked overview area (callout only).
    var openable: Bool = false
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
    /// Fires with the tapped nest's id.
    var onSelect: (String) -> Void = { _ in }
    /// Fires with the tapped community beach's id (dot or polygon) → opens the beach card.
    var onSelectBeach: (String) -> Void = { _ in }
    /// Live patrol track (breadcrumb coordinates) drawn as a polyline.
    var track: [CLLocationCoordinate2D] = []
    /// Beach sand outlines (OSM polygons), coloured green (protected) / amber (unprotected).
    var beachPolygons: [BeachPolygon] = []

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: Self.osmRasterStyleURL())
        mapView.delegate = context.coordinator
        mapView.setCenter(center, zoomLevel: zoomLevel, animated: false)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // Keep OSM attribution reachable (required by the tile usage policy).
        mapView.attributionButton.isHidden = false
        // Tap on a beach polygon → open its card. Wait for the map's own gestures (annotation
        // selection, double-tap zoom) so pin taps keep going to didSelect.
        let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleMapTap(_:)))
        for r in mapView.gestureRecognizers ?? [] where r is UITapGestureRecognizer {
            tap.require(toFail: r)
        }
        mapView.addGestureRecognizer(tap)
        context.coordinator.sync(nests: points, beaches: beaches, on: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.parent = self          // keep closure/props fresh across SwiftUI updates
        context.coordinator.sync(nests: points, beaches: beaches, on: mapView)
        context.coordinator.syncTrack(track, on: mapView)
        context.coordinator.applyBeachPolygons(beachPolygons)
    }

    // MARK: Coordinator = MLNMapViewDelegate

    final class Coordinator: NSObject, MLNMapViewDelegate {
        var parent: MapLibreView
        private var currentIDs: Set<String> = []
        private var polyline: MLNPolyline?
        private var trackCount = -1
        private var beachSource: MLNShapeSource?

        init(_ parent: MapLibreView) { self.parent = parent }

        // Add a beach-highlight source + fill/line style layers once the style is ready. Style layers
        // render reliably (unlike MLNPolygon annotations, which didn't show); then push the polygons.
        // Colour is data-driven per feature: green when protected == YES, amber otherwise.
        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            let src = MLNShapeSource(identifier: "cf-beaches", shape: nil, options: nil)
            style.addSource(src)
            let protectedPred = NSPredicate(format: "protected == YES")
            let fill = MLNFillStyleLayer(identifier: "cf-beaches-fill", source: src)
            fill.fillColor = NSExpression(
                forConditional: protectedPred,
                trueExpression: NSExpression(forConstantValue: green),
                falseExpression: NSExpression(forConstantValue: amber))
            fill.fillOpacity = NSExpression(forConstantValue: 0.22)
            style.addLayer(fill)
            let line = MLNLineStyleLayer(identifier: "cf-beaches-line", source: src)
            line.lineColor = NSExpression(
                forConditional: protectedPred,
                trueExpression: NSExpression(forConstantValue: green),
                falseExpression: NSExpression(forConstantValue: amber))
            line.lineWidth = NSExpression(forConstantValue: 2.5)
            style.addLayer(line)
            beachSource = src
            applyBeachPolygons(parent.beachPolygons)
        }

        /// Push the current beach outlines into the shape source. Each feature carries `id` (for tap →
        /// detail) and `protected` (for the green/amber colour expression above).
        func applyBeachPolygons(_ polys: [BeachPolygon]) {
            guard let src = beachSource else { return }
            let features: [MLNPolygonFeature] = polys.filter { $0.coords.count >= 3 }.map { poly in
                var c = poly.coords
                let f = MLNPolygonFeature(coordinates: &c, count: UInt(c.count))
                f.attributes = ["id": poly.id, "protected": poly.isProtected]
                return f
            }
            src.shape = MLNShapeCollectionFeature(shapes: features)
        }

        /// Tap outside any pin → hit-test beach polygons; open the beach card for the top one.
        @objc func handleMapTap(_ gr: UITapGestureRecognizer) {
            guard gr.state == .ended, let mapView = gr.view as? MLNMapView else { return }
            let pt = gr.location(in: mapView)
            let feats = mapView.visibleFeatures(at: pt, styleLayerIdentifiers: ["cf-beaches-fill"])
            if let id = feats.compactMap({ $0.attribute(forKey: "id") as? String }).first {
                parent.onSelectBeach(id)
            }
        }

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

        private let coral = UIColor(red: 0.98, green: 0.45, blue: 0.36, alpha: 1.0)
        // Match Android: protected #2E9E5B (green), unprotected #E0A82E (amber).
        fileprivate static let green = UIColor(red: 0.18, green: 0.62, blue: 0.357, alpha: 1.0)
        fileprivate static let amber = UIColor(red: 0.878, green: 0.659, blue: 0.18, alpha: 1.0)
        private var green: UIColor { Coordinator.green }
        private var amber: UIColor { Coordinator.amber }

        // Patrol track (polyline) styling.
        func mapView(_ mapView: MLNMapView, strokeColorForShapeAnnotation annotation: MLNShape) -> UIColor { coral }

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
                a.protectedBeach = b.protectedBeach ?? true
                a.openable = b.openable
                a.coordinate = b.coordinate; a.title = b.title
                anns.append(a)
            }
            mapView.addAnnotations(anns)
        }

        // Beaches = a coloured dot (green protected / amber unprotected); nests = built-in red pin (nil).
        func mapView(_ mapView: MLNMapView, imageFor annotation: MLNAnnotation) -> MLNAnnotationImage? {
            guard let a = annotation as? IdentifiedAnnotation, a.isBeach else { return nil }
            let id = a.protectedBeach ? "beach-dot-green" : "beach-dot-amber"
            if let img = mapView.dequeueReusableAnnotationImage(withIdentifier: id) { return img }
            return MLNAnnotationImage(image: Coordinator.beachDot(a.protectedBeach ? green : amber), reuseIdentifier: id)
        }

        // Overview area dots show their name callout; community beaches open the card on tap (no callout).
        func mapView(_ mapView: MLNMapView, annotationCanShowCallout annotation: MLNAnnotation) -> Bool {
            if let a = annotation as? IdentifiedAnnotation, a.isBeach { return !a.openable }
            return parent.showsCallout
        }

        // THE TAP CALLBACK — nest → detail; community beach dot → beach card; area dot → name callout.
        func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
            guard let a = annotation as? IdentifiedAnnotation else { return }
            if a.isBeach {
                if a.openable {
                    parent.onSelectBeach(a.pointID)
                    mapView.deselectAnnotation(annotation, animated: false)
                }
                return
            }
            parent.onSelect(a.pointID)
            if !parent.showsCallout {
                mapView.deselectAnnotation(annotation, animated: false)
            }
        }

        /// A small filled dot for beach markers in [color] (distinct from red nest pins).
        static func beachDot(_ color: UIColor) -> UIImage {
            let size = CGSize(width: 22, height: 22)
            return UIGraphicsImageRenderer(size: size).image { ctx in
                let rect = CGRect(x: 2, y: 2, width: 18, height: 18)
                color.setFill()
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