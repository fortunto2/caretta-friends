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
    case community(String)
    case beach(String)
    case stats
    /// Read-only profile of a volunteer, keyed by a Member id or a stored name ("you" = self).
    case member(String)
}

/// A pushed Compose screen: native swipe-back, no tab bar, hidden nav bar.
private struct DetailScreen<Content: View>: View {
    let fullBleed: Bool
    @ViewBuilder let content: () -> Content
    var body: some View {
        content()
            .modifier(FullBleed(on: fullBleed))
            .toolbar(.hidden, for: .navigationBar)
            // Keep the bottom tab bar visible on pushed detail screens (per request) — so you can
            // leave via the tabs instead of the top-left back arrow.
            .toolbar(fullBleed ? .hidden : .visible, for: .tabBar)
    }
}

private struct FullBleed: ViewModifier {
    let on: Bool
    func body(content: Content) -> some View {
        if on { content.ignoresSafeArea() } else { content }
    }
}

@ViewBuilder
private func destinationView(_ route: Route, path: Binding<NavigationPath>, router: AppRouter) -> some View {
    switch route {
    case .nest(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.NestDetailVC(
                    nestId: id,
                    onBack: { path.wrappedValue.removeLast() },
                    onExcavate: { nid in path.wrappedValue.append(Route.excavation(nid)) },
                    onOpenMember: { key in path.wrappedValue.append(Route.member(key)) },
                    onOpenMap: { router.focusMap() }
                )
            }
        }
        // Track "a nest is the top screen" so the global "+" adds an update to THIS nest (B3).
        .onAppear { router.currentNestId = id }
        .onDisappear { if router.currentNestId == id { router.currentNestId = nil } }
    case .excavation(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.ExcavationVC(nestId: id, onBack: { path.wrappedValue.removeLast() }) }
        }
    case .addNest:
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.AddNestVC(
                    onDone: { path.wrappedValue.removeLast() },
                    // Hand over to the REAL native camera (map tab owns it); it re-enters this
                    // form with the captured photo. It used to push a mock viewfinder that could
                    // not take a picture at all.
                    onCamera: { router.startAddNest(fromForm: true) },
                    onNestSaved: { id in path.wrappedValue.removeLast(); path.wrappedValue.append(Route.nest(id)) }
                )
            }
        }
    case .community(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.CommunityVC(
                    communityId: id,
                    onBack: { path.wrappedValue.removeLast() },
                    onOpenMember: { key in path.wrappedValue.append(Route.member(key)) }
                )
            }
        }
    case .beach(let id):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.BeachDetailVC(
                    beachId: id,
                    onBack: { path.wrappedValue.removeLast() },
                    onOpenNest: { nid in path.wrappedValue.append(Route.nest(nid)) },
                    onOpenMember: { key in path.wrappedValue.append(Route.member(key)) }
                )
            }
        }
    case .stats:
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.StatsVC(onBack: { path.wrappedValue.removeLast() }) }
        }
    case .member(let key):
        DetailScreen(fullBleed: false) {
            ComposeHost {
                IosEntryKt.MemberVC(
                    memberKey: key,
                    onBack: { path.wrappedValue.removeLast() },
                    onOpenNest: { nid in path.wrappedValue.append(Route.nest(nid)) },
                    onOpenBeach: { bid in path.wrappedValue.append(Route.beach(bid)) }
                )
            }
        }
    }
}

/// A tab owning its navigation stack of Compose screens. Root content is inset above the
/// native (Liquid Glass) tab bar; detail pushes hide the tab bar for a full sub-flow.
struct TabStack<Root: View>: View {
    @EnvironmentObject private var router: AppRouter
    @State private var path = NavigationPath()
    @ViewBuilder let root: (Binding<NavigationPath>) -> Root

    var body: some View {
        NavigationStack(path: $path) {
            root($path)
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Route.self) { route in
                    destinationView(route, path: $path, router: router)
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
    @EnvironmentObject private var router: AppRouter
    @State private var path = NavigationPath()
    @State private var showCamera = false
    /// The camera was opened from the add-nest form (not from the global "+").
    @State private var cameFromForm = false
    @State private var showAR = false
    @State private var focusCoord: CLLocationCoordinate2D? = nil
    @State private var focusApplyTick = 0
    @State private var points: [MapPoint] = []
    @State private var beaches: [MapPoint] = []
    @State private var communities: [MapPoint] = []
    @State private var beachPolygons: [BeachPolygon] = []
    @State private var violations: [MapPoint] = []
    @State private var filter = "all"
    @State private var recenter = 0
    @State private var timelapse = false
    @State private var tlDay: Double = 0
    @State private var tlPlaying = false
    @State private var tlTimer: Timer? = nil

    /// Nest points to draw: timelapse day-filtered, else violation-filter-aware.
    private var displayPoints: [MapPoint] {
        if timelapse {
            return IosEntryKt.mapPointsUpTo(day: Int32(tlDay)).map { p in
                MapPoint(id: p.id, coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng), title: p.title, phase: p.status)
            }
        }
        return filter == "violations" ? [] : points
    }
    @StateObject private var patrol = PatrolRecorder()
    @StateObject private var deviceLoc = DeviceLocation()
    @State private var savedPatrolId: String? = nil
    @State private var showPublish = false
    @State private var tappedBeach: TappedBeach? = nil
    @State private var air: IosAir? = nil
    @State private var airExpanded = false

    private func reload() {
        strings = IosEntryKt.currentStrings()
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
        // Recent violation reports — red dots (filter-gated at render time).
        violations = IosEntryKt.violationPoints().map { p in
            MapPoint(id: p.id, coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng), title: p.title, phase: p.status)
        }
        air = IosEntryKt.airStatus()
        AirWidgetBridge.publish()   // keep the home-screen air widget fresh
    }

    /// Localized native-chrome strings (map chips, tooltip, patrol dialog), cached so a body re-render
    /// (once/second during a patrol) never re-crosses the KMP bridge. Refreshed in `reload()` on appear,
    /// which fires when the tab returns to front after a Profile → Language change.
    @State private var strings = IosEntryKt.currentStrings()

    var body: some View {
        NavigationStack(path: $path) {
            ZStack(alignment: .top) {
                MapLibreView(
                    points: displayPoints,
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
                    beachPolygons: beachPolygons,
                    violations: filter == "violations" ? violations : [],
                    recenterTick: recenter,
                    focus: focusCoord,
                    focusTick: focusApplyTick
                )
                .ignoresSafeArea()
                .overlay(alignment: .bottom) {
                    if timelapse { timelapseControl().padding(.bottom, 100) }
                }
                // "Locate me" — recenters on the volunteer's position with a heading (compass) cone.
                .overlay(alignment: .bottomTrailing) {
                    VStack(spacing: 12) {
                        // AR nest finder — point the phone around to see nests where they physically are.
                        Button { showAR = true } label: {
                            Image(systemName: "arkit")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 46, height: 46)
                                .background(Color.cfCoral, in: Circle())
                                .shadow(color: Color.cfCoral.opacity(0.4), radius: 6, y: 2)
                        }
                        Button {
                            recenter += 1
                        } label: {
                            Image(systemName: "location.fill")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.cfSea)
                                .frame(width: 46, height: 46)
                                .background(.regularMaterial, in: Circle())
                                .overlay(Circle().stroke(Color.black.opacity(0.06)))
                                .shadow(color: .black.opacity(0.15), radius: 6, y: 2)
                        }
                    }
                    .padding(.trailing, 14)
                    .padding(.bottom, 100)
                }

                // top overlays: filter chips + coverage pill
                VStack(alignment: .leading, spacing: 10) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip(strings.filterAll, "all")
                            chip(strings.filterNests, "nests")
                            chip(strings.filterHatching, "hatching")
                            chip(strings.filterTrash, "trash")
                            chip(strings.filterViolations, "violations")
                            timelapseChip()
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
                        // Patrol recording is a coordinator's tool. As a big green pill in the corner
                        // it was the loudest control on the map and visitors kept tapping it, so it
                        // is a small icon now and only for a signed-in member with a role.
                        if IosEntryKt.canRecordPatrol() {
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
                                Image(systemName: patrol.isRecording ? "stop.fill" : "figure.walk")
                                    .font(.title3.weight(.semibold))
                                    .foregroundColor(patrol.isRecording ? .white : Color.cfDeep)
                                    .frame(width: 44, height: 44)
                                    .background(patrol.isRecording ? Color.cfCoral : Color.white)
                                    .clipShape(Circle())
                                    .shadow(color: .black.opacity(0.12), radius: 4, y: 2)
                            }
                        } else if air?.patrolAdvisable == false {
                            // Dust is safety information for everyone, not a control.
                            Text(strings.dustNotAdvised)
                                .font(.subheadline.weight(.bold)).foregroundColor(.white)
                                .padding(.horizontal, 14).padding(.vertical, 10)
                                .background(Color.cfCoral).clipShape(Capsule())
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
                                onCamera: { showCamera = true },
                                onNestSaved: { id in if !path.isEmpty { path.removeLast() }; path.append(Route.nest(id)) }
                            )
                        }
                    }
                } else {
                    destinationView(route, path: $path, router: router)
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
        // A nest's geo card asked to centre the map here → pop any pushed detail, consume the focus,
        // and drive MapLibreView onto the point.
        .onChange(of: router.focusTick) { _ in
            if let g = IosEntryKt.takeMapFocus() {
                if !path.isEmpty { path = NavigationPath() }
                focusCoord = CLLocationCoordinate2D(latitude: g.lat, longitude: g.lng)
                focusApplyTick += 1
            }
        }
        .onChange(of: router.startAddTick) { _ in
            cameFromForm = router.startedFromForm
            // Global "+" (from any tab) → run the add-nest flow on THIS tab's stack: camera → form →
            // saved nest, all with the bottom tab bar visible. Reset to the map root for a clean start.
            if !path.isEmpty { path = NavigationPath() }
            showCamera = true
        }
        .fullScreenCover(isPresented: $showAR) {
            ARNestView(
                onClose: { showAR = false },
                onOpenNest: { id in showAR = false; path.append(Route.nest(id)) }
            )
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraCaptureView(
                author: "You",
                onDone: { imagePath, lat, lng, sourceId in
                    showCamera = false
                    IosEntryKt.setPendingPhoto(
                        path: imagePath,
                        lat: lat ?? 0,
                        lng: lng ?? 0,
                        hasLocation: lat != nil && lng != nil,
                        sourceId: sourceId ?? ""
                    )
                    if path.isEmpty { path.append(Route.addNest) }
                },
                onCancel: {
                    showCamera = false
                    // Coming from the add-nest form, the camera reset the stack to open — landing
                    // back on a bare map after "cancel" looks like the app threw the work away.
                    // (The form's typed note/date are lost either way; the form itself comes back.)
                    if cameFromForm { path.append(Route.addNest) }
                    cameFromForm = false
                }
            )
        }
        .alert(strings.patrolPublishTitle, isPresented: $showPublish) {
            Button(strings.patrolKeepPrivate, role: .cancel) {}
            Button(strings.patrolPublish) { if let id = savedPatrolId { IosEntryKt.publishPatrol(id: id) } }
        } message: {
            Text(strings.patrolPublishBody)
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
        let s = IosEntryKt.currentStrings()
        let base: String
        switch a.level {
        case "DUST": base = "\(s.airDustLabel) · PM10 \(a.pm10) — \(s.patrolNotAdvised)"
        case "UNHEALTHY": base = "\(s.airUnhealthy) · PM2.5 \(a.pm25)"
        case "MODERATE": base = "\(s.airModerate) · PM2.5 \(a.pm25)"
        default: base = "\(s.airCleanLabel) · PM2.5 \(a.pm25)"
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
                Text(tb.name.isEmpty ? strings.beachWord : tb.name)
                    .font(.subheadline.weight(.bold)).foregroundColor(.cfDeep)
                Text(tb.isProtected ? strings.protectedBeachLabel : strings.beachWord)
                    .font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            if tb.openable {
                Button {
                    let id = tb.id
                    tappedBeach = nil
                    path.append(Route.beach(id))
                } label: {
                    Text("\(strings.openWord) ›").font(.subheadline.weight(.bold)).foregroundColor(.white)
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

    private func timelapseChip() -> some View {
        Text(strings.timelapse)
            .font(.caption.weight(.bold))
            .foregroundColor(timelapse ? .white : .cfDeep)
            .padding(.horizontal, 12).padding(.vertical, 7)
            .background(timelapse ? Color.cfSea : Color.white.opacity(0.92))
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.12), radius: 4, y: 2)
            .onTapGesture {
                timelapse.toggle()
                if timelapse { tlDay = Double(IosEntryKt.todayEpochDay()) } else { stopTimelapse() }
            }
    }

    @ViewBuilder
    private func timelapseControl() -> some View {
        let first = Double(IosEntryKt.firstNestDay())
        let today = Double(IosEntryKt.todayEpochDay())
        let count = IosEntryKt.mapPointsUpTo(day: Int32(tlDay)).count
        VStack(spacing: 4) {
            HStack(spacing: 10) {
                Button { toggleTlPlay() } label: {
                    Image(systemName: tlPlaying ? "pause.fill" : "play.fill")
                        .foregroundColor(.white).frame(width: 38, height: 38)
                        .background(Color.cfSea).clipShape(Circle())
                }
                Text(tlDateLabel(tlDay)).font(.subheadline.weight(.heavy)).foregroundColor(.cfDeep)
                Spacer()
                Text("\(count) 🥚").font(.subheadline.weight(.bold)).foregroundColor(.cfSea)
            }
            Slider(value: $tlDay, in: first...max(today, first + 1)) { editing in if editing { stopTimelapse() } }
                .tint(.cfSea)
        }
        .padding(12).background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: .black.opacity(0.15), radius: 10, y: 4)
        .padding(.horizontal, 14)
    }

    private func toggleTlPlay() {
        if tlPlaying { stopTimelapse(); return }
        tlPlaying = true
        tlTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { _ in
            if tlDay >= Double(IosEntryKt.todayEpochDay()) { stopTimelapse() } else { tlDay += 1 }
        }
    }

    private func stopTimelapse() {
        tlPlaying = false
        tlTimer?.invalidate()
        tlTimer = nil
    }

    private func tlDateLabel(_ day: Double) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f.string(from: Date(timeIntervalSince1970: day * 86400))
    }
}

/// Tab tags. `.add` is a sentinel centre tab: selecting it opens the add-a-nest flow
/// (from ANY tab) and immediately reverts to the previously-selected tab.
enum AppTab: Hashable { case map, beaches, add, learn, profile }

/// Shared shell router: drives which tab is selected and signals the map to consume a pending
/// focus (e.g. a nest's geo card → open the map centred on that nest). Injected as an
/// `@EnvironmentObject` so any pushed Compose screen's closures can reach it.
final class AppRouter: ObservableObject {
    /// Store screenshots are captured per language, and the simulator cannot be tapped from the
    /// command line — so `-startTab beaches|learn|profile` opens straight onto a tab. Debug-only
    /// convenience; without the argument the app always starts on the map.
    @Published var selection: AppTab = {
        guard let raw = UserDefaults.standard.string(forKey: "startTab") else { return .map }
        switch raw {
        case "beaches": return .beaches
        case "learn": return .learn
        case "profile": return .profile
        default: return .map
        }
    }()
    /// Bumped to ask the Map tab to read `IosEntryKt.takeMapFocus()` and centre on it.
    @Published var focusTick: Int = 0
    /// The nest id currently shown as the top pushed screen, or nil. Drives the context-aware "+" (B3).
    @Published var currentNestId: String? = nil
    /// Bumped by the global "+" to start the add-nest flow on the Map tab's OWN NavigationStack, so the
    /// bottom tab bar stays visible the whole time (a full-screen cover would hide it — user request).
    @Published var startAddTick: Int = 0

    /// Switch to the Map tab and request it to consume the pending map focus.
    func focusMap() { selection = .map; focusTick += 1 }
    /// Switch to the Map tab and start the add-a-nest flow there (camera → form → saved nest).
    /// [fromForm] tells the map tab the camera was opened from an add-nest form, so cancelling
    /// returns there instead of dropping the volunteer on the map.
    private(set) var startedFromForm = false

    func startAddNest(fromForm: Bool = false) {
        startedFromForm = fromForm
        selection = .map
        startAddTick += 1
    }

    init() {
        // Screenshot helper only: `SIMCTL_CHILD_CF_TAB=beaches|learn|profile` preselects a tab so
        // App Store shots can be captured without UI taps. No-op in normal launches (env unset).
        switch ProcessInfo.processInfo.environment["CF_TAB"] {
        case "beaches": selection = .beaches
        case "learn": selection = .learn
        case "profile": selection = .profile
        default: break
        }
    }
}

struct ContentView: View {
    @StateObject private var router = AppRouter()
    @State private var prior: AppTab = .map
    @State private var showOnboarding = false

    /// Context-aware "+" (B3): on a nest detail → add an update to THAT nest; elsewhere → new-nest flow.
    private func handlePlus() {
        if let nid = router.currentNestId {
            IosEntryKt.requestAddUpdate(nestId: nid)
        } else {
            router.startAddNest()   // push the add flow on the Map tab → tab bar stays visible
        }
    }

    var body: some View {
        let nav = IosEntryKt.navLabels()
        TabView(selection: $router.selection) {
            MapTab()
                .tabItem { Label(nav.map, systemImage: "map.fill") }
                .tag(AppTab.map)

            TabStack { path in
                ComposeHost {
                    IosEntryKt.BeachesVC(
                        onOpenNest: { path.wrappedValue.append(Route.nest($0)) },
                        onOpenBeach: { path.wrappedValue.append(Route.beach($0)) }
                    )
                }
            }
            .tabItem { Label(nav.beaches, systemImage: "beach.umbrella.fill") }
            .tag(AppTab.beaches)

            // Centre "+" — never shows its own content; the coral overlay button triggers the add
            // flow. Empty tab item (blank space, no icon) so nothing peeks from under the coral button.
            Color.clear
                .tabItem { Text(" ") }
                .tag(AppTab.add)

            TabStack { _ in
                ComposeHost { IosEntryKt.LearnVC() }
            }
            .tabItem { Label(nav.learn, systemImage: "book.fill") }
            .tag(AppTab.learn)

            TabStack { path in
                ComposeHost {
                    IosEntryKt.ProfileVC(
                        onOpenCommunity: { path.wrappedValue.append(Route.community(IosEntryKt.primaryCommunityId())) },
                        onOpenStats: { path.wrappedValue.append(Route.stats) },
                        onOpenNest: { path.wrappedValue.append(Route.nest($0)) },
                        onOpenBeach: { path.wrappedValue.append(Route.beach($0)) },
                        onAddNest: { path.wrappedValue.append(Route.addNest) }
                    )
                }
            }
            .tabItem { Label(nav.profile, systemImage: "tortoise.fill") }
            .tag(AppTab.profile)
        }
        .tint(Color.cfCoral)
        // Prominent coral "+" over the centre tab (Android has the same). Sits on top of the
        // sentinel .add tab; tapping either opens the add flow.
        .overlay(alignment: .bottom) {
            Button { handlePlus() } label: {
                Image(systemName: "plus")
                    .font(.system(size: 26, weight: .heavy))
                    .foregroundColor(.white)
                    .frame(width: 52, height: 52)
                    .background(Color.cfCoral)
                    .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
                    .shadow(color: Color.cfCoral.opacity(0.45), radius: 8, y: 4)
            }
            .offset(y: -14)
        }
        .environmentObject(router)
        .onChange(of: router.selection) { newValue in
            if newValue == .add {
                router.selection = prior   // bounce back so the empty tab never shows
                router.startAddNest()      // …then start the add flow on the Map tab (keeps the tab bar)
            } else {
                prior = newValue
            }
        }
        .onAppear { showOnboarding = !IosEntryKt.isOnboarded() }
        .fullScreenCover(isPresented: $showOnboarding) {
            ComposeHost { IosEntryKt.OnboardingVC(onDone: { showOnboarding = false }) }
                .ignoresSafeArea()
        }
    }
}

// (The global "+" add-a-nest flow now runs inside the Map tab's own NavigationStack — see
// MapTab's `router.startAddTick` handler — so the bottom tab bar stays visible throughout.)
