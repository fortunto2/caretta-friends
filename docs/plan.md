# Caretta Friends — Backlog / Roadmap

Working branch: **`feat/v1-mvp`** (pushed to origin). Installs on Android emulator + Rust's iPhone
(`00008120-0011754C2208201E`, coredevice `FC73117A-EDA2-5F2C-825C-6E80050F1255`).

Guiding principle (from the client): **radical simplicity**. Volunteers are ordinary people who
"snap a photo & forget". The core loop must be effortless: photograph → smart auto-fill → save.
Everything else automatic / smart-default. Offline-first: capture works with no signal, sync when online.

---

## ✅ Done (shipped this cycle)

- **Nav**: bottom tab bar always visible (Android + iOS `.toolbar(.visible,.tabBar)`); leave any screen via tabs.
- **Full-interface i18n** EN/RU/TR — one `AppStrings` catalog (⚠️ **Map-backed** now, see Gotchas).
- **People-graph**: `MemberProfileScreen` reachable from community / leaderboard / beach-leader / nest "found by" / timeline author (`AppState.resolveMember`).
- **Activity feed**: ONE reusable component `ui/screens/ActivityFeed.kt` — `activityFeed(state, s, member?/beachId?/communityId?)` + `ActivityFeed()`. Used on Profile + MemberProfile. Reusable for Beach/Community.
- **Simpler capture (AddNest)**: dropped "nest vs false crawl"; one "Add photo" → camera/gallery chooser; tap photo → full-screen; optional note (→ first comment) + found-date back-date + protection/visibility under "▸ More details".
- **Unified nest action**: 3 buttons (Update/Comment/Photo) → ONE "add update" (photo + note + condition) → one timeline entry.
- **Nest lifecycle map colours**: `Repository.nestMapPhase()` → incubating (coral) / soon (amber) / **emerging** (vivid green — HATCHING, act on it) / excavated (sea) / removed (grey, >2d post-excavation or lost). Android `CircleManager` + iOS per-phase pin image.
- **Hatch lifecycle**: a "hatching/hatched" observation advances `Nest.status` (forward-only); "hatch window open 🐣" banner; excavation CTA gated to hatched/past-window nests.
- **My location (iOS)**: blue dot + "locate" button (→ `.followWithHeading`). ⚠️ Needs refinement (see C).
- **Photo durability (iOS)**: capture also saved to a **"Caretta Friends" Photos album** (`PhotoAlbumSaver`) so photos survive reinstall.
- **Crash fixes**: iOS photo-share (dropped fragile CoreGraphics watermark); AppStrings 255-field VerifyError (Map-backed).
- Empty states (encouraging CTA, not big "0"); excavation celebration (or supportive msg when 0); watch persists (`Profile.watchedNestIds`).

---

## 🔜 Backlog (prioritised — do in order, each its own commit + build + install both platforms)

### B1 — Geo card → OUR map, centred on the nest (not external OSM)
`NestDetailScreen.kt` geo card currently opens `openstreetmap.org` via `LocalUriHandler`. Change to
open the in-app Map tab centred on the nest.
- Add `AppState.mapFocus: GeoPoint?` + `Repository.focusMap(p)` / `takeMapFocus()`.
- Geo card → `repo.focusMap(n.point)` + `onOpenMap()`.
- Android `CarettaApp`: `onOpenMap` → `nav.selectTab(Screen.Map)`; `MapScreen` centres `OsmMap` on `mapFocus` (add `focus: GeoPoint?` param to `OsmMap` expect/actual) then clears it.
- iOS: `onOpenMap` must switch the SwiftUI `TabView` to the Map tab (thread a callback to `ContentView.selectedTab`) + `MapLibreView` centres on `IosEntryKt.takeMapFocus()`.

### B3 — Contextual "+" (unify capture)
The bottom "+" should be context-aware: on the map/home → new nest; on a nest detail → add update to THAT nest.
- New `Screen.AddUpdate(nestId)` + iOS `Route.addUpdate` + VC, OR reuse the AddUpdateDialog surfaced from the bar.
- Android `CarettaApp.onAdd`: `if (current is Screen.NestDetail) → add-update to that nest, else → AddNest`.
- iOS: the native "+" overlay must know the top pushed screen (nest) to route correctly.

### C — Native navigation feel
- **iOS native nav bar**: use `NavigationStack`'s bar (title + native back + swipe-back) instead of the Compose `TopBar`. Add `expect fun useNativeHeader(): Boolean` (Android=false, iOS=true); Compose `TopBar` renders nothing on iOS; thread per-route titles to `.navigationTitle` in `ContentView.destinationView` (no screen uses TopBar `trailing`). Android keeps the Material `TopBar`.
- **Swipe between tabs** (Instagram-style): HorizontalPager (Android) / paged content (iOS) under the persistent bottom bar.
- **My location = compass arrow, not a dot** (avoid confusion with nest dots); **remove the bottom-right locate button** — show heading "like a navigator" (custom `MLNUserLocationAnnotationView` triangle, or `showsUserHeadingIndicator`).
- **Android map my-location**: MapLibre `LocationComponent` (iOS has it; Android pending).

### D — Feed report + filters
- Time filter on the activity feed: **today / week / month / all** (add a range param to `activityFeed()`).
- A **shareable "today report"** (default profile view) — screenshot/share summary of today's activity. Reuse `platformShareImage`.

### Other pending
- **Photo cloud sync (offline-first, V2)**: nest *metadata* already syncs to Supabase; **photo files** do not — upload to Supabase Storage (→ Cloudflare R2 later, behind `CloudBackend`). Android durability: also save captures to a public MediaStore album (iOS done).
- **Save button visible without scroll** in AddNest (form is shorter now; a fixed footer would guarantee it).
- Seed demo strings ("Patrol — all OK", "Looks good today") are English — they vanish on a clean install; low priority.
- iOS camera permission-denied message still English (edge case).

---

## ⚠️ Gotchas (read before editing)

- **AppStrings is Map-backed** (`class AppStrings(m: Map<String,String>, months: List<String>)` + 255 computed getters). A plain data class with ~255 constructor args tripped the ART verifier → **VerifyError on launch** (compiles fine, crashes on class-load). Add a field = getter + one `"key" to "value"` in all 3 `mapOf` bundles (EN/RU/TR). `months` is the one non-String field (explicit param).
- **iOS present/share/PHPicker**: use `topmostViewController()` (`ui/IosPresent.kt`) — `keyWindow` is nil on iOS 15+. `UIWindow.isKeyWindow` is a **method** in K/N.
- **Native Swift strings** (map chips, tooltip, patrol dialog, camera) are separate from AppStrings — bridged via `IosEntryKt.currentStrings(): AppStrings`.
- **Kotlin 2.3 ABI trap**: don't add libs whose iosArm64 klib is built with Kotlin 2.3 (we're on 2.2.20) — fails `compileKotlinIosArm64` only (Android tolerates it). No `supabase-kt`/markdown-renderer≥0.39 in commonMain.
- **iPhone install**: raise the tunnel via `xcrun devicectl device info details --device FC73117A-…` first; retry install on "Connection interrupted".
- **Android emulator** drive: `adb shell input tap X Y` (screenshot 900px → ×1.2 → 1080). iOS simulator has no CLI tap.

## Build / install

```bash
# Android
./gradlew :composeApp:installDebug
adb shell monkey -p com.carettafriends -c android.intent.category.LAUNCHER 1

# iOS (device)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination generic/platform=iOS -derivedDataPath iosApp/build-device -allowProvisioningUpdates build
xcrun devicectl device install app --device FC73117A-EDA2-5F2C-825C-6E80050F1255 \
  iosApp/build-device/Build/Products/Debug-iphoneos/iosApp.app
```
