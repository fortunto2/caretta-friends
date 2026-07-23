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

/// A beach sand outline for the map, carrying its id (for tap → detail), name and protected flag.
struct BeachPolygon: Equatable {
    let id: String
    let name: String
    let coords: [CLLocationCoordinate2D]
    let isProtected: Bool

    static func == (l: BeachPolygon, r: BeachPolygon) -> Bool {
        l.id == r.id && l.name == r.name && l.isProtected == r.isProtected && l.coords.count == r.coords.count
    }
}

/// A beach/area the user tapped on the map → drives the tooltip badge (name + status) and, for
/// community beaches, the "Open ›" action into the full beach card. `openable` is false for the
/// baked overview areas (which have no detail page — badge only).
struct TappedBeach: Identifiable, Equatable {
    let id: String
    let name: String
    let isProtected: Bool
    let openable: Bool
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
    /// Fires with the tapped beach/area (dot or polygon) → the caller shows a tooltip badge first.
    var onTapBeach: (TappedBeach) -> Void = { _ in }
    /// Community hubs (registered city) — orange dots; tap fires onSelectCommunity.
    var communities: [MapPoint] = []
    /// Fires with the tapped community's id → opens the community screen.
    var onSelectCommunity: (String) -> Void = { _ in }
    /// Live patrol track (breadcrumb coordinates) drawn as a polyline.
    var track: [CLLocationCoordinate2D] = []
    /// Beach sand outlines (OSM polygons), coloured green (protected) / amber (unprotected).
    var beachPolygons: [BeachPolygon] = []
    /// Recent rule-violation reports — red dots (shown only under the Violations filter).
    var violations: [MapPoint] = []

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: Self.osmRasterStyleURL())
        mapView.delegate = context.coordinator
        mapView.setCenter(center, zoomLevel: zoomLevel, animated: false)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        // Keep OSM attribution reachable (required by the tile usage policy).
        mapView.attributionButton.isHidden = false
        // Tap on a beach dot/polygon (style layers, not annotations) → open its card. Recognise
        // SIMULTANEOUSLY with the map's own tap (its recognizer succeeds on every tap, so require-to-fail
        // would suppress ours forever). Nest PINS are annotations → still handled by didSelect; our
        // handler just hit-tests the beach layers and no-ops elsewhere.
        let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleMapTap(_:)))
        tap.delegate = context.coordinator
        mapView.addGestureRecognizer(tap)
        context.coordinator.sync(nests: points, on: mapView)
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.parent = self          // keep closure/props fresh across SwiftUI updates
        context.coordinator.sync(nests: points, on: mapView)
        context.coordinator.syncTrack(track, on: mapView)
        context.coordinator.applyBeachPolygons(beachPolygons)
        context.coordinator.applyBeachDots(beaches)
        context.coordinator.applyCommunities(communities)
        context.coordinator.applyViolations(violations)
    }

    // MARK: Coordinator = MLNMapViewDelegate

    final class Coordinator: NSObject, MLNMapViewDelegate, UIGestureRecognizerDelegate {
        var parent: MapLibreView
        private var currentIDs: Set<String> = []
        private var polyline: MLNPolyline?
        private var trackCount = -1
        private var beachSource: MLNShapeSource?
        private var beachDotSource: MLNShapeSource?
        private var communitySource: MLNShapeSource?
        private var violationSource: MLNShapeSource?

        init(_ parent: MapLibreView) { self.parent = parent }

        // Build beach style layers once the style is ready: sand-outline fill/line (polygons) + a dot
        // layer for beaches without an outline and the baked overview areas. Style layers render + hit-test
        // reliably (unlike MLNPolygon/point annotations); colour is data-driven (green protected / amber).
        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            let protectedPred = NSPredicate(format: "protected == YES")
            let colourExpr = NSExpression(
                forConditional: protectedPred,
                trueExpression: NSExpression(forConstantValue: green),
                falseExpression: NSExpression(forConstantValue: amber))

            // Polygons (sand outlines).
            let src = MLNShapeSource(identifier: "cf-beaches", shape: nil, options: nil)
            style.addSource(src)
            let fill = MLNFillStyleLayer(identifier: "cf-beaches-fill", source: src)
            fill.fillColor = colourExpr
            fill.fillOpacity = NSExpression(forConstantValue: 0.22)
            style.addLayer(fill)
            let line = MLNLineStyleLayer(identifier: "cf-beaches-line", source: src)
            line.lineColor = colourExpr
            line.lineWidth = NSExpression(forConstantValue: 2.5)
            style.addLayer(line)
            beachSource = src

            // Dots (outline-less beaches + baked overview areas), drawn on top.
            let dotSrc = MLNShapeSource(identifier: "cf-beach-dots", shape: nil, options: nil)
            style.addSource(dotSrc)
            let dots = MLNCircleStyleLayer(identifier: "cf-beach-dots", source: dotSrc)
            dots.circleRadius = NSExpression(forConstantValue: 7)
            dots.circleColor = colourExpr
            dots.circleStrokeColor = NSExpression(forConstantValue: UIColor.white)
            dots.circleStrokeWidth = NSExpression(forConstantValue: 2.5)
            style.addLayer(dots)
            beachDotSource = dotSrc

            // Community hubs (registered city) — orange SQUARE icons (distinct from round dots/pins),
            // drawn on top of everything.
            let cSrc = MLNShapeSource(identifier: "cf-community-dots", shape: nil, options: nil)
            style.addSource(cSrc)
            style.setImage(Coordinator.communityIcon(), forName: "cf-community-icon")
            let cSym = MLNSymbolStyleLayer(identifier: "cf-community-dots", source: cSrc)
            cSym.iconImageName = NSExpression(forConstantValue: "cf-community-icon")
            cSym.iconAllowsOverlap = NSExpression(forConstantValue: true)
            cSym.iconIgnoresPlacement = NSExpression(forConstantValue: true)
            style.addLayer(cSym)
            communitySource = cSrc

            // Violation reports — red circles on top (shown only under the Violations filter).
            let vSrc = MLNShapeSource(identifier: "cf-violation-dots", shape: nil, options: nil)
            style.addSource(vSrc)
            let vDots = MLNCircleStyleLayer(identifier: "cf-violation-dots", source: vSrc)
            vDots.circleRadius = NSExpression(forConstantValue: 8)
            vDots.circleColor = NSExpression(forConstantValue: UIColor(red: 0.69, green: 0.0, blue: 0.13, alpha: 1))
            vDots.circleStrokeColor = NSExpression(forConstantValue: UIColor.white)
            vDots.circleStrokeWidth = NSExpression(forConstantValue: 2.5)
            style.addLayer(vDots)
            violationSource = vSrc

            applyBeachPolygons(parent.beachPolygons)
            applyBeachDots(parent.beaches)
            applyCommunities(parent.communities)
            applyViolations(parent.violations)
        }

        /// Push recent violation dots (red). Filter-gating (only under the Violations filter) is done
        /// by the caller passing an empty array when the filter is off.
        func applyViolations(_ dots: [MapPoint]) {
            guard let src = violationSource else { return }
            let features: [MLNPointFeature] = dots.map { d in
                let f = MLNPointFeature()
                f.coordinate = d.coordinate
                f.attributes = ["id": d.id]
                return f
            }
            src.shape = MLNShapeCollectionFeature(shapes: features)
        }

        /// Push the current beach outlines into the shape source. Each feature carries `id` (for tap →
        /// detail) and `protected` (for the green/amber colour expression above).
        func applyBeachPolygons(_ polys: [BeachPolygon]) {
            guard let src = beachSource else { return }
            let features: [MLNPolygonFeature] = polys.filter { $0.coords.count >= 3 }.map { poly in
                var c = poly.coords
                let f = MLNPolygonFeature(coordinates: &c, count: UInt(c.count))
                f.attributes = ["id": poly.id, "name": poly.name, "protected": poly.isProtected, "openable": true]
                return f
            }
            src.shape = MLNShapeCollectionFeature(shapes: features)
        }

        /// Push beach dots. Each feature carries `id`, `protected` (colour) and `openable` (community
        /// beach → tap opens the card; baked area → tap does nothing).
        func applyBeachDots(_ dots: [MapPoint]) {
            guard let src = beachDotSource else { return }
            let features: [MLNPointFeature] = dots.map { d in
                let f = MLNPointFeature()
                f.coordinate = d.coordinate
                f.attributes = [
                    "id": d.id,
                    "name": d.title ?? "",
                    "protected": d.protectedBeach ?? true,
                    "openable": d.openable,
                ]
                return f
            }
            src.shape = MLNShapeCollectionFeature(shapes: features)
        }

        // Fire alongside MapLibre's built-in tap (it always recognises, so we must not wait for it).
        func gestureRecognizer(_ g: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool {
            true
        }

        /// Push community hub dots (orange) — each feature carries `id` for tap → community screen.
        func applyCommunities(_ points: [MapPoint]) {
            guard let src = communitySource else { return }
            let features: [MLNPointFeature] = points.map { p in
                let f = MLNPointFeature()
                f.coordinate = p.coordinate
                f.attributes = ["id": p.id]
                return f
            }
            src.shape = MLNShapeCollectionFeature(shapes: features)
        }

        /// Tap on a community hub, beach dot or polygon → open the matching screen. Uses a padded rect
        /// so small dots are easy to hit; baked overview areas (openable == NO) don't navigate.
        @objc func handleMapTap(_ gr: UITapGestureRecognizer) {
            guard gr.state == .ended, let mapView = gr.view as? MLNMapView else { return }
            let pt = gr.location(in: mapView)
            let rect = CGRect(x: pt.x - 22, y: pt.y - 22, width: 44, height: 44)
            // Community hubs first (top layer), then beach dots, then polygons.
            let cFeats = mapView.visibleFeatures(in: rect, styleLayerIdentifiers: ["cf-community-dots"])
            if let id = cFeats.compactMap({ $0.attribute(forKey: "id") as? String }).first {
                parent.onSelectCommunity(id)
                return
            }
            let feats = mapView.visibleFeatures(in: rect, styleLayerIdentifiers: ["cf-beach-dots", "cf-beaches-fill"])
            for f in feats {
                guard let id = f.attribute(forKey: "id") as? String else { continue }
                let name = (f.attribute(forKey: "name") as? String) ?? ""
                let prot = (f.attribute(forKey: "protected") as? NSNumber)?.boolValue ?? false
                let openable = (f.attribute(forKey: "openable") as? NSNumber)?.boolValue ?? true
                parent.onTapBeach(TappedBeach(id: id, name: name, isProtected: prot, openable: openable))
                return
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
        // Match Android: protected #2E9E5B (green), unprotected #E0A82E (amber), community #F97316 (orange).
        fileprivate static let green = UIColor(red: 0.18, green: 0.62, blue: 0.357, alpha: 1.0)
        fileprivate static let amber = UIColor(red: 0.878, green: 0.659, blue: 0.18, alpha: 1.0)
        fileprivate static let orange = UIColor(red: 0.976, green: 0.451, blue: 0.086, alpha: 1.0)
        private var green: UIColor { Coordinator.green }
        private var amber: UIColor { Coordinator.amber }
        private var orange: UIColor { Coordinator.orange }

        /// A rounded orange SQUARE marking a community hub (distinct from round beach dots / nest pins).
        static func communityIcon() -> UIImage {
            let size = CGSize(width: 26, height: 26)
            return UIGraphicsImageRenderer(size: size).image { _ in
                let rect = CGRect(x: 3, y: 3, width: 20, height: 20)
                let path = UIBezierPath(roundedRect: rect, cornerRadius: 5)
                orange.setFill(); path.fill()
                UIColor.white.setStroke(); path.lineWidth = 3; path.stroke()
            }
        }

        // Patrol track (polyline) styling.
        func mapView(_ mapView: MLNMapView, strokeColorForShapeAnnotation annotation: MLNShape) -> UIColor { coral }

        func mapView(_ mapView: MLNMapView, lineWidthForPolylineAnnotation annotation: MLNPolyline) -> CGFloat { 4 }

        /// Rebuild nest PIN annotations when the id set changes; keeps the polyline. Beaches are NOT
        /// annotations — they're style layers (see applyBeachDots / applyBeachPolygons).
        func sync(nests: [MapPoint], on mapView: MLNMapView) {
            let ids = Set(nests.map(\.id))
            guard ids != currentIDs else { return }
            currentIDs = ids
            let toRemove = (mapView.annotations ?? []).filter { $0 is IdentifiedAnnotation }
            mapView.removeAnnotations(toRemove)
            let anns: [IdentifiedAnnotation] = nests.map { p in
                let a = IdentifiedAnnotation()
                a.pointID = p.id; a.isBeach = false
                a.coordinate = p.coordinate; a.title = p.title; a.subtitle = p.subtitle
                return a
            }
            mapView.addAnnotations(anns)
        }

        // Nests = built-in red pin (return nil → default). (Beaches are style-layer dots, not annotations.)
        func mapView(_ mapView: MLNMapView, imageFor annotation: MLNAnnotation) -> MLNAnnotationImage? {
            return nil
        }

        func mapView(_ mapView: MLNMapView, annotationCanShowCallout annotation: MLNAnnotation) -> Bool {
            return parent.showsCallout
        }

        // Nest pin tap → open its detail. (Beach taps are handled by handleMapTap on the style layers.)
        func mapView(_ mapView: MLNMapView, didSelect annotation: MLNAnnotation) {
            guard let a = annotation as? IdentifiedAnnotation else { return }
            parent.onSelect(a.pointID)
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