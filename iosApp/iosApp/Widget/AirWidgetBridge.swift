import Foundation
import WidgetKit
import ComposeApp

// Publishes the latest air-quality reading into the shared App Group so the home-screen widget can
// read it offline (the widget never touches the KMP framework or the network). Call whenever the app
// refreshes air (MapTab.reload). Requires the "App Groups" capability with `groupId` on BOTH the app
// and widget targets.
enum AirWidgetBridge {
    static let groupId = "group.com.carettafriends.app"
    static let key = "air"

    static func publish() {
        guard let store = UserDefaults(suiteName: groupId) else { return }
        if let a = IosEntryKt.airStatus() {
            store.set([
                "level": a.level,                    // GOOD / MODERATE / UNHEALTHY / DUST
                "pm25": Int(a.pm25),
                "pm10": Int(a.pm10),
                "comfort": Int(a.comfort),           // -1 = none
                "patrol": a.patrolAdvisable,
                "ts": Date().timeIntervalSince1970,
            ], forKey: key)
        } else {
            store.removeObject(forKey: key)
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
