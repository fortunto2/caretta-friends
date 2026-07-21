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
    case community
}

/// A pushed Compose screen: native swipe-back, no tab bar, hidden nav bar.
private struct DetailScreen<Content: View>: View {
    let fullBleed: Bool
    @ViewBuilder let content: () -> Content
    var body: some View {
        content()
            .modifier(FullBleed(on: fullBleed))
            .toolbar(.hidden, for: .navigationBar)
            .toolbar(.hidden, for: .tabBar)
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
    case .community:
        DetailScreen(fullBleed: false) {
            ComposeHost { IosEntryKt.CommunityVC(onBack: { path.wrappedValue.removeLast() }) }
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
}

/// Map tab: a NATIVE MapLibre + OSM map with SwiftUI overlays. The "+" opens the native camera;
/// after capture the photo + GPS go to the shared layer and the add-nest form is pushed; tapping a
/// nest pin opens its detail.
struct MapTab: View {
    @State private var path = NavigationPath()
    @State private var showCamera = false
    @State private var points: [MapPoint] = []
    @State private var filter = "all"

    private func reload() {
        points = IosEntryKt.mapPoints().map { p in
            MapPoint(
                id: p.id,
                coordinate: CLLocationCoordinate2D(latitude: p.lat, longitude: p.lng),
                title: p.title
            )
        }
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack(alignment: .top) {
                MapLibreView(
                    points: points,
                    center: MapLibreView.gazipasa,
                    zoomLevel: 12,
                    showsCallout: false,
                    onSelect: { id in path.append(Route.nest(id)) }
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
                    Text("✓ Patrolled 6:10 today · Mert · 2.3 km")
                        .font(.caption.weight(.bold)).foregroundColor(.white)
                        .padding(.horizontal, 12).padding(.vertical, 7)
                        .background(Color.cfGood).clipShape(Capsule())
                        .padding(.leading, 14)
                }
                .padding(.top, 8)

                // bottom overlays: start patrol + FAB
                VStack {
                    Spacer()
                    HStack(alignment: .bottom) {
                        Text("● Start patrol")
                            .font(.subheadline.weight(.bold)).foregroundColor(.white)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(Color.cfGood).clipShape(Capsule())
                        Spacer()
                        Button { showCamera = true } label: {
                            Image(systemName: "plus")
                                .font(.title.weight(.bold)).foregroundColor(.white)
                                .frame(width: 58, height: 58)
                                .background(Color.cfCoral)
                                .clipShape(RoundedRectangle(cornerRadius: 19, style: .continuous))
                                .shadow(color: Color.cfCoral.opacity(0.5), radius: 10, y: 6)
                        }
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
        .onAppear { reload() }
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
    var body: some View {
        TabView {
            MapTab()
                .tabItem { Label("Map", systemImage: "map.fill") }

            TabStack { path in
                ComposeHost {
                    IosEntryKt.BeachesVC(onOpenNest: { path.wrappedValue.append(Route.nest($0)) })
                }
            }
            .tabItem { Label("Beaches", systemImage: "beach.umbrella.fill") }

            TabStack { _ in
                ComposeHost { IosEntryKt.LearnVC() }
            }
            .tabItem { Label("Learn", systemImage: "book.fill") }

            TabStack { path in
                ComposeHost {
                    IosEntryKt.ProfileVC(onOpenCommunity: { path.wrappedValue.append(Route.community) })
                }
            }
            .tabItem { Label("Profile", systemImage: "tortoise.fill") }
        }
    }
}
