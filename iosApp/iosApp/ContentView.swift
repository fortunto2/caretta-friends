import SwiftUI
import ComposeApp

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

/// Map tab: the "+" opens the native camera (full-screen). After capture, the photo + GPS are
/// handed to the shared layer and the add-nest form is pushed.
struct MapTab: View {
    @State private var path = NavigationPath()
    @State private var showCamera = false

    var body: some View {
        NavigationStack(path: $path) {
            ComposeHost {
                IosEntryKt.MapVC(
                    onOpenNest: { path.append(Route.nest($0)) },
                    onAdd: { showCamera = true }
                )
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
