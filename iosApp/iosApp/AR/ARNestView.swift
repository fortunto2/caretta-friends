import SwiftUI
import ARKit
import RealityKit
import CoreLocation
import ComposeApp

// MARK: - AR nest finder
// Point the phone around; nests are pinned in AR where they physically are. We take one good GPS
// fix as the world origin, place each nest at its East/North offset (ENU) in the ARKit world
// (worldAlignment = .gravityAndHeading), then let world tracking hold them as the volunteer moves.
// Markers are SwiftUI labels positioned by projecting each world point to the screen every frame —
// crisp text + tap + perspective, instead of 3D text meshes.
//
// NB: ARGeoAnchor (Apple's true geo-anchors) only works in select cities — NOT Gazipaşa — so we use
// the world-tracking + heading + GPS approach, which works anywhere. Accuracy is bounded by the
// compass + GPS; good enough for "which way, how far". ARKit does not run in the Simulator.

private func statusColor(_ s: String) -> Color {
    switch s {
    case "soon": return Color(red: 0.88, green: 0.66, blue: 0.18)      // amber
    case "emerging": return Color(red: 0.13, green: 0.77, blue: 0.37)  // vivid green
    case "excavated": return Color(red: 0.06, green: 0.48, blue: 0.51) // sea
    case "removed": return Color(red: 0.60, green: 0.65, blue: 0.64)   // grey
    default: return Color(red: 0.88, green: 0.33, blue: 0.24)          // coral (incubating)
    }
}

struct ARNest: Identifiable {
    let id: String
    let code: String
    let status: String
    let coord: CLLocationCoordinate2D
    var world: SIMD3<Float> = .zero
    var screen: CGPoint = .zero
    var distance: Double = 0        // real metres (label), independent of capped render distance
    var visible: Bool = false
}

final class ARNestModel: NSObject, ObservableObject, ARSessionDelegate, CLLocationManagerDelegate {
    @Published var nests: [ARNest] = []
    @Published var located = false
    @Published var supported = ARWorldTrackingConfiguration.isSupported

    weak var arView: ARView?
    private let loc = CLLocationManager()
    private var origin: CLLocation?
    private var placed = false
    private let renderCap = 60.0    // metres — far nests are drawn at 60 m along their true bearing

    override init() {
        super.init()
        let pts = IosEntryKt.mapPoints()
        nests = pts.map {
            ARNest(id: $0.id, code: $0.title, status: $0.status,
                   coord: CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng))
        }
        loc.delegate = self
        loc.desiredAccuracy = kCLLocationAccuracyBest
        loc.requestWhenInUseAuthorization()
        loc.startUpdatingLocation()
    }

    func attach(_ view: ARView) {
        arView = view
        guard supported else { return }
        let config = ARWorldTrackingConfiguration()
        config.worldAlignment = .gravityAndHeading
        // LiDAR (Pro devices): reconstruct the scene so markers get occluded by real geometry.
        if ARWorldTrackingConfiguration.supportsSceneReconstruction(.mesh) {
            config.sceneReconstruction = .mesh
            view.environment.sceneUnderstanding.options.insert(.occlusion)
        }
        view.session.delegate = self
        view.session.run(config, options: [.resetTracking, .removeExistingAnchors])
    }

    // First good fix → world origin; place every nest at its ENU offset.
    func locationManager(_ m: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let l = locations.last, l.horizontalAccuracy >= 0, l.horizontalAccuracy < 45 else { return }
        if origin == nil {
            origin = l
            placeAll(from: l)
            DispatchQueue.main.async { self.located = true }
        }
    }

    private func placeAll(from o: CLLocation) {
        let R = 6_371_000.0
        let lat0 = o.coordinate.latitude * .pi / 180
        for i in nests.indices {
            let dLat = (nests[i].coord.latitude - o.coordinate.latitude) * .pi / 180
            let dLng = (nests[i].coord.longitude - o.coordinate.longitude) * .pi / 180
            let north = dLat * R
            let east = dLng * R * cos(lat0)
            let dist = (north * north + east * east).squareRoot()
            nests[i].distance = dist
            let scale = dist > 0.5 ? min(dist, renderCap) / dist : 0
            // .gravityAndHeading world: +X = east, +Z = south → north is -Z.
            nests[i].world = SIMD3<Float>(Float(east * scale), 0, Float(-north * scale))
        }
        placed = true
    }

    // Project each nest to the screen every frame; only keep those in front of the camera.
    func session(_ session: ARSession, didUpdate frame: ARFrame) {
        guard placed, let arView = arView else { return }
        let t = frame.camera.transform
        let camPos = SIMD3<Float>(t.columns.3.x, t.columns.3.y, t.columns.3.z)
        let forward = -SIMD3<Float>(t.columns.2.x, t.columns.2.y, t.columns.2.z)
        var updated = nests
        for i in updated.indices {
            let dir = updated[i].world - camPos
            let inFront = simd_length(dir) > 0.001 && simd_dot(simd_normalize(dir), simd_normalize(forward)) > 0.2
            if inFront, let p = arView.project(updated[i].world) {
                updated[i].screen = p
                updated[i].visible = true
            } else {
                updated[i].visible = false
            }
        }
        DispatchQueue.main.async { self.nests = updated }
    }
}

// MARK: - ARView host

private struct ARContainer: UIViewRepresentable {
    let model: ARNestModel
    func makeUIView(context: Context) -> ARView {
        let v = ARView(frame: .zero, cameraMode: .ar, automaticallyConfigureSession: false)
        model.attach(v)
        return v
    }
    func updateUIView(_ uiView: ARView, context: Context) {}
}

// MARK: - Overlay

struct ARNestView: View {
    @StateObject private var model = ARNestModel()
    let onClose: () -> Void
    let onOpenNest: (String) -> Void

    /// The nest closest to screen centre (and near) → shown as an expanded card at the bottom.
    private func centred(_ size: CGSize) -> ARNest? {
        let c = CGPoint(x: size.width / 2, y: size.height / 2)
        return model.nests.filter { $0.visible }
            .filter { hypot($0.screen.x - c.x, $0.screen.y - c.y) < 140 }
            .min { $0.distance < $1.distance }
    }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                if model.supported {
                    ARContainer(model: model).ignoresSafeArea()
                } else {
                    Color.black.ignoresSafeArea()
                    VStack(spacing: 10) {
                        Image(systemName: "arkit").font(.system(size: 44)).foregroundColor(.white.opacity(0.6))
                        Text("AR не поддерживается на этом устройстве")
                            .foregroundColor(.white.opacity(0.8)).multilineTextAlignment(.center).padding(.horizontal, 40)
                    }
                }

                // floating nest markers
                ForEach(model.nests.filter { $0.visible }) { n in
                    marker(n, centred: centred(geo.size)?.id == n.id)
                        .position(x: n.screen.x, y: n.screen.y)
                        .onTapGesture { onOpenNest(n.id) }
                }

                // chrome: close + count + hint / centred card
                VStack {
                    HStack {
                        Button(action: onClose) {
                            Image(systemName: "xmark")
                                .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                                .frame(width: 44, height: 44).background(.ultraThinMaterial, in: Circle())
                        }
                        Spacer()
                        Text("\(model.nests.count) гнёзд рядом")
                            .font(.caption.weight(.bold)).foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 8)
                            .background(.ultraThinMaterial, in: Capsule())
                    }
                    .padding(.horizontal, 16).padding(.top, 8)

                    Spacer()

                    if model.supported && !model.located {
                        Text("Ищу спутники… держи телефон и медленно поводи им")
                            .font(.subheadline.weight(.medium)).foregroundColor(.white)
                            .padding(14).background(.ultraThinMaterial, in: Capsule())
                            .padding(.bottom, 40)
                    } else if let c = centred(geo.size) {
                        centredCard(c).padding(.horizontal, 16).padding(.bottom, 36)
                    }
                }
            }
        }
    }

    // A floating pin: dot + code + distance, scaled by proximity (nearer = bigger).
    @ViewBuilder
    private func marker(_ n: ARNest, centred: Bool) -> some View {
        let scale = CGFloat(max(0.7, min(1.25, 1.3 - n.distance / 120))) * (centred ? 1.12 : 1.0)
        HStack(spacing: 6) {
            Circle().fill(statusColor(n.status)).frame(width: 12, height: 12)
                .overlay(Circle().stroke(.white, lineWidth: 2))
            Text(n.code).font(.footnote.weight(.heavy)).foregroundColor(.white)
            Text(distLabel(n.distance)).font(.caption2.weight(.semibold)).foregroundColor(.white.opacity(0.85))
        }
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(Color.black.opacity(0.55), in: Capsule())
        .overlay(Capsule().stroke(statusColor(n.status), lineWidth: centred ? 2.5 : 1.5))
        .scaleEffect(scale)
        .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
    }

    @ViewBuilder
    private func centredCard(_ n: ARNest) -> some View {
        HStack(spacing: 12) {
            Circle().fill(statusColor(n.status)).frame(width: 16, height: 16)
            VStack(alignment: .leading, spacing: 2) {
                Text(n.code).font(.headline.weight(.heavy)).foregroundColor(.white)
                Text(distLabel(n.distance)).font(.caption).foregroundColor(.white.opacity(0.8))
            }
            Spacer()
            Button { onOpenNest(n.id) } label: {
                Text("Открыть ›").font(.subheadline.weight(.bold)).foregroundColor(.white)
                    .padding(.horizontal, 14).padding(.vertical, 9)
                    .background(Color(red: 1.0, green: 0.478, blue: 0.349), in: Capsule())
            }
        }
        .padding(14).background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func distLabel(_ m: Double) -> String {
        m < 1000 ? "\(Int(m.rounded())) м" : String(format: "%.1f км", m / 1000)
    }
}
