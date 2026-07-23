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
- **B1 — nest geo card → OUR map**: the "На карте" card now centres+zooms the in-app map on the nest
  (transient `AppState.mapFocus` → `repo.focusMap()`/`takeMapFocus()`), instead of opening external
  openstreetmap.org. Android reactive (Compose `OsmMap.animateCamera`); iOS via `AppRouter` env-object
  → native `MapLibreView` focus. Focus is `@Transient` (one-shot, never persisted). Plan/spec:
  `docs/plan/b1-map-focus_20260724/`.
- **B3 — contextual "+"**: the bottom "+" is context-aware — on a nest detail it adds an update to
  THAT nest (opens the AddUpdate dialog), elsewhere it starts a new nest. Transient
  `AppState.addUpdateFor` → `repo.requestAddUpdate()`/`clearAddUpdate()`; the on-screen `NestDetailScreen`
  opens its dialog via `LaunchedEffect`. iOS tracks the top nest in `AppRouter.currentNestId`
  (set/clear on `.nest` appear/disappear); the coral "+" routes through `handlePlus()`.
- **D — feed range filter + today report**: the profile activity feed has time chips (Today / Week /
  Month / All → `activityFeed(range = FeedRange…)`, cutoff by `createdEpochMillis`) + an empty state,
  and a "📤 Share today" button that shares a compact emoji report (`todayReportText()` →
  `platformShare`). Shared Compose `ProfileScreen`, so both platforms get it. i18n: `rangeWeek`,
  `rangeMonth`, `shareDay`, `activityEmpty` (EN/RU/TR).
- **Photo durability (iOS)**: capture also saved to a **"Caretta Friends" Photos album** (`PhotoAlbumSaver`) so photos survive reinstall.
- **Crash fixes**: iOS photo-share (dropped fragile CoreGraphics watermark); AppStrings 255-field VerifyError (Map-backed).
- Empty states (encouraging CTA, not big "0"); excavation celebration (or supportive msg when 0); watch persists (`Profile.watchedNestIds`).

---

## 🔜 Backlog (prioritised — do in order, each its own commit + build + install both platforms)

### C — Native navigation feel
- ✅ **Android map my-location** — done: MapLibre `LocationComponent` in `OsmMap.android` (default engine,
  runtime `ACCESS_FINE_LOCATION` request, `RenderMode.COMPASS` heading arrow, `CameraMode.NONE`). Compiles +
  runs clean; the arrow needs a real GPS fix to render (emulator mock-GPS→FusedLocation is unreliable) — confirm on device.
- ✅ **iOS heading indicator** — done: `mapView.showsUserHeadingIndicator = true` (heading cone on the user
  dot). Confirm the "navigator" feel on device (needs a real compass).
- **iOS native nav bar** (⏳ needs device): use `NavigationStack`'s bar (title + native back + swipe-back) instead of the Compose `TopBar`. Add `expect fun useNativeHeader(): Boolean` (Android=false, iOS=true); Compose `TopBar` renders nothing on iOS; thread per-route titles to `.navigationTitle` in `ContentView.destinationView`. Larger refactor + swipe-back needs on-device verification.
- **Swipe between tabs** (⏳): HorizontalPager (Android) / paged content (iOS) under the persistent bottom bar. Gesture-conflict risk with the map pan — verify carefully.
- **Locate button / full compass-triangle** (⏳ needs device): optionally drop the iOS bottom-right locate button and/or use a custom `MLNUserLocationAnnotationView` triangle once the heading feel is confirmed on device.

### Other pending
- **Photo cloud sync (offline-first, V2)**: nest *metadata* already syncs to Supabase; **photo files** do not — upload to Supabase Storage (→ Cloudflare R2 later, behind `CloudBackend`). Android durability: also save captures to a public MediaStore album (iOS done).
- iOS camera permission-denied message still English (edge case).
- ✅ **Save button visible without scroll** in AddNest — done: the Save button is now a pinned footer below the scrollable form.
- ✅ Seed demo strings — moot: `seedState()` now seeds `nests = emptyList()`, so the English "Patrol — all OK"/"Looks good today" only linger in stale local state and are gone on a clean install.

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
