import WidgetKit
import SwiftUI

// Home-screen widget: air quality for the community's beach (PM2.5 + level + "safe to patrol?").
// Reads the last reading the app wrote into the App Group — no network, no KMP framework here.
//
// SETUP (Xcode, ~2 min — see the chat steps):
//   1. File → New → Target → Widget Extension, name "CarettaAirWidget" (static, no Configuration Intent).
//   2. Delete the generated boilerplate; add THIS file to the CarettaAirWidget target.
//   3. Signing & Capabilities → + App Groups → "group.com.carettafriends.app" on BOTH the app AND
//      the CarettaAirWidget target.

private let appGroup = "group.com.carettafriends.app"

struct AirEntry: TimelineEntry {
    let date: Date
    let level: String       // GOOD / MODERATE / UNHEALTHY / DUST
    let pm25: Int
    let comfort: Int        // -1 = none
    let patrol: Bool
    let hasData: Bool
}

struct AirProvider: TimelineProvider {
    func placeholder(in context: Context) -> AirEntry {
        AirEntry(date: Date(), level: "GOOD", pm25: 8, comfort: 80, patrol: true, hasData: true)
    }
    func getSnapshot(in context: Context, completion: @escaping (AirEntry) -> Void) { completion(load()) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<AirEntry>) -> Void) {
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
        completion(Timeline(entries: [load()], policy: .after(next)))
    }
    private func load() -> AirEntry {
        guard let d = UserDefaults(suiteName: appGroup)?.dictionary(forKey: "air") else {
            return AirEntry(date: Date(), level: "GOOD", pm25: 0, comfort: -1, patrol: true, hasData: false)
        }
        return AirEntry(
            date: Date(),
            level: d["level"] as? String ?? "GOOD",
            pm25: d["pm25"] as? Int ?? 0,
            comfort: d["comfort"] as? Int ?? -1,
            patrol: d["patrol"] as? Bool ?? true,
            hasData: true
        )
    }
}

private func levelColor(_ l: String) -> Color {
    switch l {
    case "DUST", "UNHEALTHY": return Color(red: 0.88, green: 0.33, blue: 0.24)   // coral/red
    case "MODERATE": return Color(red: 0.88, green: 0.66, blue: 0.18)            // amber
    default: return Color(red: 0.18, green: 0.62, blue: 0.36)                    // green
    }
}

private func levelEmoji(_ l: String) -> String {
    switch l {
    case "DUST": return "🌫️"
    case "UNHEALTHY": return "😷"
    case "MODERATE": return "🌤️"
    default: return "🍃"
    }
}

struct CarettaAirWidgetEntryView: View {
    var entry: AirEntry
    var body: some View {
        let accent = levelColor(entry.level)
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("🐢 Воздух").font(.caption2.weight(.bold)).foregroundColor(.secondary)
                Spacer()
                Text(levelEmoji(entry.level)).font(.title3)
            }
            if entry.hasData {
                Text("PM2.5 \(entry.pm25)").font(.system(size: 30, weight: .heavy)).foregroundColor(accent)
                Text(entry.patrol ? "Патрулировать можно" : "Патруль не советуем")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(entry.patrol ? .secondary : accent)
                if entry.comfort >= 0 {
                    Text("Комфорт \(entry.comfort)").font(.caption2).foregroundColor(.secondary)
                }
            } else {
                Text("Открой приложение").font(.subheadline.weight(.bold)).foregroundColor(.secondary)
                Text("чтобы обновить воздух").font(.caption2).foregroundColor(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .containerBackground(for: .widget) { accent.opacity(0.12) }
    }
}

@main
struct CarettaAirWidget: Widget {
    let kind = "CarettaAirWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: AirProvider()) { entry in
            CarettaAirWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Воздух · Caretta")
        .description("Качество воздуха на пляже: PM2.5 и можно ли патрулировать.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
