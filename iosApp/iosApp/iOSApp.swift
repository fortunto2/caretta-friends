import SwiftUI
import UIKit

@main
struct iOSApp: App {
    init() {
        // Force a visible, opaque tab bar with tinted icons/labels. Without this the tab bar can
        // render with invisible icons on iOS 18 (the app is built with the iOS 26 SDK).
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground
        let sea = UIColor(red: 0.05, green: 0.45, blue: 0.55, alpha: 1.0)
        let gray = UIColor.systemGray
        for item in [appearance.stackedLayoutAppearance, appearance.inlineLayoutAppearance, appearance.compactInlineLayoutAppearance] {
            item.selected.iconColor = sea
            item.selected.titleTextAttributes = [.foregroundColor: sea]
            item.normal.iconColor = gray
            item.normal.titleTextAttributes = [.foregroundColor: gray]
        }
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
