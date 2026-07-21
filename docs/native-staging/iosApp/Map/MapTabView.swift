import SwiftUI
import CoreLocation

struct MapTabView: View {
    @State private var selectedID: String?

    // Example turtle spots around Gazipaşa.
    private let spots: [MapPoint] = [
        MapPoint(id: "gazipasa-beach",
                 coordinate: .init(latitude: 36.2700, longitude: 32.3100),
                 title: "Gazipaşa Beach", subtitle: "Caretta nesting site"),
        MapPoint(id: "selinus",
                 coordinate: .init(latitude: 36.2570, longitude: 32.3010),
                 title: "Selinus (Antiochia ad Cragum)")
    ]

    var body: some View {
        MapLibreView(points: spots) { id in
            selectedID = id            // -> push a detail screen (native or a Compose UIViewController)
            print("Tapped annotation id:", id)
        }
        .ignoresSafeArea(edges: .bottom)   // let the map bleed under the tab bar
    }
}