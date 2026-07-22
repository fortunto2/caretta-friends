import SwiftUI
import ComposeApp
import CoreLocation

/// Hosts a Kotlin-provided Compose UIViewController inside SwiftUI.
struct ComposeHost: UIViewControllerRepresentable {
    let make: () -> UIViewController
    func makeUIViewController(context: Context) -> UIViewController { make() }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

/// Push destinations shared by every tab's NavigationStack.
enum Route: Hashable {
    case nest(String)
    case excavation(String)
    case addNest
    case camera
    case community(String)
    case beach(String)
    case stats
}

/// A pushed Compose screen: native swipe-back, no tab bar, hidden nav bar.
private struct DetailScreen<Content: View>: View {
    let fullBleed: Bool
    @ViewBuilder let content: () -> Content
    var body: some View {
        content()
            .modifier(FullBleed(on: fullBleed))
            .toolbar(.hidden, for: .navigationBar)
            // Keep the bottom tab bar visible on detail cards (per request).
    }
}

private struct FullBleed: ViewModifier {
    let on: Bool
    func body(content: Content) -> some View {
        if on { content.ignoresSafeArea() } else { content }
    }
}

@ViewBuilder
private func destinationView(_ route: Route, path: Binding<NavigationPath>) -> some View {
    switch route {
    case .nest(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.NestDetailVC(
                    nestId: id,
                    onBack: { path.wrappedValue.removeLast() },
                    onExcavate: { nid in path.wrappedValue.append(Route.excavation(nid)) }
                )
            }
        }
    case .excavation(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.ExcavationVC(nestId: id, onBack: { path.wrappedValue.removeLast() }) }
        }
    case .addNest:
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.AddNestVC(
                    onDone: { path.wrappedValue.removeLast() },
                    onCamera: { path.wrappedValue.append(Route.camera) }
                )
            }
        }
    case .camera:
        DetailScreen(fullBleed: true) {
            ComposeHost {
                IosEntryKt.CameraVC(
                    onBack: { path.wrappedValue.removeLast() },
                    onCaptured: {
                        path.wrappedValue.removeLast()
                        path.wrappedValue.append(Route.addNest)
                    }
                )
            }
        }
    case .community(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.CommunityVC(communityId: id, onBack: { path.wrappedValue.removeLast() }) }
        }
    case .beach(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.BeachDetailVC(
                    beachId: id,
                    onBack: { path.wrappedValue.removeLast() },
                    onOpenNest: { nid in path.wrappedValue.append(Route.nest(nid)) }
                )
            }
        }
    case .stats:
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.StatsVC(onBack: { path.wrappedValue.removeLast() }) }
        }
    }
}

/// A tab owning its navigation stack of Compose screens. Root content is inset above the
/// native (Liquid Glass) tab bar; detail pushes hide the tab bar for a full sub-flow.
struct TabStack<Root: View>: View {
    @State private var path = NavigationPath()
    @ViewBuilder let root: (Binding<NavigationPath>) -> Root

    var body: some View {
        NavigationStack(path: $path) {
            root($path)
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Route.self) { route in
                    destinationView(route, path: $path)
                }
        }
    }
}

extension Color {
    static let cfCoral = Color(red: 1.0, green: 0.478, blue: 0.349)
    static let cfSea = Color(red: 0.059, green: 0.478, blue: 0.510)
    static let cfGood = Color(red: 0.31, green: 0.66, blue: 0.42)
    static let cfDeep = Color(red: 0.043, green: 0.231, blue: 0.247)
    static let cfAmber = Color(red: 0.878, green: 0.659, blue: 0.18)
}

/// Map tab: a NATIVE MapLibre + OSM map with SwiftUI overlays. The "+" opens the native camera;
/// after capture the photo + GPS go to the shared layer and the add-nest form is pushed; tapping a
/// nest pin opens its detail.
struct MapTab: View {
    @State private var path = NavigationPath()
    @State private var showCamera = false
    @State private var points: [MapPoint] = []
    @State private var beaches: [MapPoint] = []
    @State private var communities: [MapPoint] = []
    @State private var beachPolygons: [BeachPolygon] = []
    @State private var filter = "all"
    @StateObject private var patrol = PatrolRecorder()
    @StateObject private var deviceLoc = DeviceLocation()
    @State private var savedPatrolId: String? = nil
    @State private var showPublish = false
    @State private var tappedBeach: TappedBeach? = nil
    @State private var air: IosAir? = nil
    @State private var airExpanded = false

    private func reload() {
        points = IosEntryKt.mapPoints().map { p in
            MapPoint(
                id: p.id,
                coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng),
                title: p.title
            )
        }
        let shapes = IosEntryKt.beachShapes()
        let polyIds = Set(shapes.filter { !$0.polygonCsv.isEmpty }.map { $0.id })
        // Beaches with an OSM outline → coloured polygons (green protected / amber unprotected).
        beachPolygons = shapes.filter { !$0.polygonCsv.isEmpty }.map { s in
            let coords = s.polygonCsv.split(separator: ";").compactMap { pair -> CLLocationCoordinate2D? in
                let xy = pair.split(separator: ",")
                guard xy.count == 2, let lat = Double(xy[0]), let lng = Double(xy[1]) else { return nil }
                return CLLocationCoordinate2D(latitude: lat, longitude: lng)
            }
            return BeachPolygon(id: s.id, name: s.name, coords: coords, isProtected: s.isProtected)
        }
        // Community beach dots (only those without an outline) — coloured + openable → beach card.
        let communityDots = IosEntryKt.beachPoints().filter { !polyIds.contains($0.id) }.map { p in
            MapPoint(
                id: p.id,
                coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng),
                title: p.title,
                protectedBeach: p.status == "beach-green",
                openable: true
            )
        }
        // Baked official protected areas — green overview dots, name callout only.
        let areaDots = IosEntryKt.protectedAreaPoints().map { p in
            MapPoint(
                id: p.id,
                coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng),
                title: p.title,
                protectedBeach: true,
                openable: false
            )
        }
        beaches = communityDots + areaDots
        // Community hubs — orange dots at each community's registered city.
        communities = IosEntryKt.communityPoints().map { p in
            MapPoint(
                id: p.id,
                coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng),
                title: p.title
            )
        }
        air = IosEntryKt.airStatus()
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack(alignment: .top) {
                MapLibreView(
                    points: points,
                    beaches: beaches,
                    center: MapLibreView.gazipasa,
                    zoomLevel: 12,
                    showsCallout: false,
                    onSelect: { id in path.append(Route.nest(id)) },
                    onTapBeach: { tb in withAnimation(.easeInOut(duration: 0.2)) { tappedBeach = tb } },
                    communities: communities,
                    onSelectCommunity: { id in
                        path.append(Route.community(id.hasPrefix("cm:") ? String(id.dropFirst(3)) : id))
                    },
                    track: patrol.coords,
                    beachPolygons: beachPolygons
                )
                .ignoresSafeArea()

                // top overlays: filter chips + coverage pill
                VStack(alignment: .leading, spacing: 10) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip("All", "all")
                            chip("🥚 Nests", "nests")
                            chip("● Hatching", "hatching")
                            chip("🧺 Trash", "trash")
                        }
                        .padding(.horizontal, 14)
                    }
                    if let a = air {
                        let hasMore = !a.signals.isEmpty
                        Text("\(airEmoji(a)) \(airText(a))\(hasMore ? "  ›" : "")")
                            .font(.caption.weight(.bold)).foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(airColor(a).opacity(0.94)).clipShape(Capsule())
                            .padding(.leading, 14)
                            .onTapGesture { if hasMore { withAnimation(.easeInOut(duration: 0.2)) { airExpanded.toggle() } } }
                        if airExpanded {
                            airDetail(a).padding(.leading, 14).padding(.trailing, 14)
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                    }
                    if patrol.isRecording {
                        Text("● Recording · \(patrol.timeLabel) · \(patrol.kmLabel)")
                            .font(.caption.weight(.bold)).foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(Color.cfCoral).clipShape(Capsule())
                            .padding(.leading, 14)
                    }
                }
                .padding(.top, 8)

                // bottom overlays: beach tooltip badge + start patrol + FAB
                VStack {
                    Spacer()
                    if let tb = tappedBeach {
                        beachTooltip(tb)
                            .padding(.horizontal, 16)
                            .padding(.bottom, 8)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                    HStack(alignment: .bottom) {
                        Button {
                            if patrol.isRecording {
                                let r = patrol.stop()
                                savedPatrolId = IosEntryKt.savePatrol(meters: Int32(r.meters), seconds: Int32(r.seconds), trackCsv: r.trackCsv)
                                reload()
                                showPublish = true
                            } else {
                                patrol.start()
                            }
                        } label: {
                            let dust = (air?.patrolAdvisable == false) && !patrol.isRecording
                            Text(patrol.isRecording ? "■ Stop patrol" : (dust ? "⚠ Dust — not advised" : "● Start patrol"))
                                .font(.subheadline.weight(.bold)).foregroundColor(.white)
                                .padding(.horizontal, 14).padding(.vertical, 10)
                                .background((patrol.isRecording || dust) ? Color.cfCoral : Color.cfGood).clipShape(Capsule())
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 16).padding(.bottom, 10)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Route.self) { route in
                if case .addNest = route {
                    DetailScreen(fullBleed: false) {
                        ComposeHost {
                            IosEntryKt.AddNestVC(
                                onDone: { if !path.isEmpty { path.removeLast() } },
                                onCamera: { showCamera = true }
                            )
                        }
                    }
                } else {
                    destinationView(route, path: $path)
                }
            }
        }
        .onAppear {
            reload()
            deviceLoc.onFix = { lat, lng in IosEntryKt.setDeviceLocation(lat: lat, lng: lng) }
            deviceLoc.request()
        }
        .task {
            // The native map reads a snapshot, not the Kotlin store — re-pull a few times so
            // async first-launch data (OSM beaches, synced nests) shows without a manual relaunch.
            for _ in 0..<6 {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                reload()
            }
        }
        .onChange(of: path.count) { _ in reload() }
        .fullScreenCover(isPresented: $showCamera) {
            CameraCaptureView(
                author: "You",
                onDone: { imagePath, lat, lng in
                    showCamera = false
                    IosEntryKt.setPendingPhoto(
                        path: imagePath,
                        lat: lat ?? 0,
                        lng: lng ?? 0,
                        hasLocation: lat != nil && lng != nil
                    )
                    if path.isEmpty { path.append(Route.addNest) }
                },
                onCancel: { showCamera = false }
            )
        }
        .alert("Publish this patrol?", isPresented: $showPublish) {
            Button("Keep private", role: .cancel) {}
            Button("Publish") { if let id = savedPatrolId { IosEntryKt.publishPatrol(id: id) } }
        } message: {
            Text("Your walk is saved on your phone. Publish to share the route with your community — your live location is never shared.")
        }
    }

    private func airEmoji(_ a: IosAir) -> String {
        switch a.level {
        case "DUST": return "🌫️"
        case "UNHEALTHY": return "😷"
        case "MODERATE": return "🌤️"
        default: return "🍃"
        }
    }

    private func airText(_ a: IosAir) -> String {
        let base: String
        switch a.level {
        case "DUST": base = "Dust · PM10 \(a.pm10) — patrol not advised"
        case "UNHEALTHY": base = "Unhealthy · PM2.5 \(a.pm25)"
        case "MODERATE": base = "Moderate air · PM2.5 \(a.pm25)"
        default: base = "Air clean · PM2.5 \(a.pm25)"
        }
        return a.comfort >= 0 ? "\(base) · ☺ \(a.comfort)" : base
    }

    private func airColor(_ a: IosAir) -> Color {
        switch a.level {
        case "DUST", "UNHEALTHY": return .cfCoral
        case "MODERATE": return .cfAmber
        default: return .cfGood
        }
    }

    /// Tap-to-expand air panel: the safety advice + extra signals (waves / fire / UV / …).
    @ViewBuilder
    private func airDetail(_ a: IosAir) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(a.advice).font(.caption.weight(.bold)).foregroundColor(.cfDeep)
            ForEach(Array(a.signals.enumerated()), id: \.offset) { _, s in
                HStack(spacing: 8) {
                    Text(s.emoji).font(.caption)
                    Text(s.label).font(.caption2.weight(.bold)).foregroundColor(.secondary)
                        .frame(width: 84, alignment: .leading)
                    Text(s.value).font(.caption).foregroundColor(.cfDeep)
                }
            }
        }
        .padding(12)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: .black.opacity(0.12), radius: 8, y: 3)
    }

    /// Small tooltip badge shown when a beach/area is tapped — name + protection status, and (for
    /// community beaches) an "Open ›" into the full card. Baked overview areas show the badge only.
    @ViewBuilder
    private func beachTooltip(_ tb: TappedBeach) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 5)
                .fill(tb.isProtected ? Color.cfGood : Color.cfAmber)
                .frame(width: 14, height: 14)
            VStack(alignment: .leading, spacing: 2) {
                Text(tb.name.isEmpty ? "Beach" : tb.name)
                    .font(.subheadline.weight(.bold)).foregroundColor(.cfDeep)
                Text(tb.isProtected ? "🛡️ Protected nesting beach" : "Beach")
                    .font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            if tb.openable {
                Button {
                    let id = tb.id
                    tappedBeach = nil
                    path.append(Route.beach(id))
                } label: {
                    Text("Open ›").font(.subheadline.weight(.bold)).foregroundColor(.white)
                        .padding(.horizontal, 12).padding(.vertical, 7)
                        .background(Color.cfSea).clipShape(Capsule())
                }
            }
            Button { withAnimation(.easeInOut(duration: 0.2)) { tappedBeach = nil } } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold)).foregroundColor(.secondary)
                    .frame(width: 30, height: 30)
                    .background(Color.black.opacity(0.06)).clipShape(Circle())
            }
        }
        .padding(12)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: .black.opacity(0.15), radius: 10, y: 4)
    }

    @ViewBuilder
    private func chip(_ label: String, _ key: String) -> some View {
        let on = filter == key
        Text(label)
            .font(.caption.weight(.bold))
            .foregroundColor(on ? .white : .cfDeep)
            .padding(.horizontal, 12).padding(.vertical, 7)
            .background(on ? Color.cfSea : Color.white.opacity(0.92))
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.12), radius: 4, y: 2)
            .onTapGesture { filter = key }
    }
}

struct ContentView: View {
    // Tab tags. `.add` is a sentinel centre tab: selecting it opens the add-a-nest flow
    // (from ANY tab) and immediately reverts to the previously-selected tab.
    private enum Tab: Hashable { case map, beaches, add, learn, profile }

    @State private var selection: Tab = .map
    @State private var prior: Tab = .map
    @State private var showAdd = false

    var body: some View {
        TabView(selection: $selection) {
            MapTab()
                .tabItem { Label("Map", systemImage: "map.fill") }
                .tag(Tab.map)

            TabStack { path in
                ComposeHost {
                    IosEntryKt.BeachesVC(
                        onOpenNest: { path.wrappedValue.append(Route.nest($0)) },
                        onOpenBeach: { path.wrappedValue.append(Route.beach($0)) }
                    )
                }
            }
            .tabItem { Label("Beaches", systemImage: "beach.umbrella.fill") }
            .tag(Tab.beaches)

            // Centre "+" — never shows its own content; it triggers the add flow.
            Color.clear
                .tabItem { Label("Add", systemImage: "plus.circle.fill") }
                .tag(Tab.add)

            TabStack { _ in
                ComposeHost { IosEntryKt.LearnVC() }
            }
            .tabItem { Label("Learn", systemImage: "book.fill") }
            .tag(Tab.learn)

            TabStack { path in
                ComposeHost {
                    IosEntryKt.ProfileVC(
                        onOpenCommunity: { path.wrappedValue.append(Route.community(IosEntryKt.primaryCommunityId())) },
                        onOpenStats: { path.wrappedValue.append(Route.stats) }
                    )
                }
            }
            .tabItem { Label("Profile", systemImage: "tortoise.fill") }
            .tag(Tab.profile)
        }
        .tint(Color.cfCoral)
        .onChange(of: selection) { newValue in
            if newValue == .add {
                showAdd = true
                selection = prior          // bounce back so the empty tab never shows
            } else {
                prior = newValue
            }
        }
        .fullScreenCover(isPresented: $showAdd) {
            AddFlow(onClose: { showAdd = false })
        }
    }
}

/// The global add-a-nest flow launched from the centre "+" tab: native camera → Compose AddNest
/// form. Mirrors the map FAB flow but works from any tab (Android has the same centre "+").
private struct AddFlow: View {
    let onClose: () -> Void
    @State private var path = NavigationPath()
    @State private var showCamera = false
    @State private var started = false

    var body: some View {
        NavigationStack(path: $path) {
            Color(.systemBackground).ignoresSafeArea()
                .navigationDestination(for: Route.self) { route in
                    if case .addNest = route {
                        DetailScreen(fullBleed: false) {
                            ComposeHost {
                                IosEntryKt.AddNestVC(
                                    onDone: { onClose() },
                                    onCamera: { showCamera = true }
                                )
                            }
                        }
                    } else {
                        destinationView(route, path: $path)
                    }
                }
                .fullScreenCover(isPresented: $showCamera) {
                    CameraCaptureView(
                        author: "You",
                        onDone: { imagePath, lat, lng in
                            showCamera = false
                            IosEntryKt.setPendingPhoto(
                                path: imagePath,
                                lat: lat ?? 0,
                                lng: lng ?? 0,
                                hasLocation: lat != nil && lng != nil
                            )
                            if path.isEmpty { path.append(Route.addNest) }
                        },
                        onCancel: {
                            showCamera = false
                            if path.isEmpty { onClose() }   // cancelled before the form → close the flow
                        }
                    )
                }
        }
        // Open the camera once, after the cover is on screen (avoids nested-present warnings).
        .onAppear { if !started { started = true; showCamera = true } }
    }
}
