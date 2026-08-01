# Caretta Friends — Backlog / Roadmap

Working branch: **`feat/v1-mvp`** (pushed to origin). Installs on Android emulator + Rust's iPhone
(`<your-iPhone-UDID>`, coredevice `<your-coredevice-UUID>`).

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
  Month / All → `activityFeed(range = FeedRange…)`, cutoff by `createdEpochMillis`) + an empty state.
  The "share today report" (`todayReportText()` → `platformShare`) lives in a **"⋮" overflow menu** in
  the Profile `TopBar` (moved out of the filter row per feedback — non-essential actions tuck away there).
  Shared Compose `ProfileScreen`, both platforms. i18n: `rangeWeek`, `rangeMonth`, `shareDay`, `activityEmpty`.
- **Photo durability (iOS)**: capture also saved to a **"Caretta Friends" Photos album** (`PhotoAlbumSaver`) so photos survive reinstall.
- **Crash fixes**: iOS photo-share (dropped fragile CoreGraphics watermark); AppStrings 255-field VerifyError (Map-backed).
- Empty states (encouraging CTA, not big "0"); excavation celebration (or supportive msg when 0); watch persists (`Profile.watchedNestIds`).

---

## 🔜 Backlog (prioritised — do in order, each its own commit + build + install both platforms)

### C — Native navigation feel
- ✅ **Android map my-location** — done: MapLibre `LocationComponent` in `OsmMap.android` (default engine,
  runtime `ACCESS_FINE_LOCATION` request, **`RenderMode.NORMAL`** stable puck, `CameraMode.NONE`). Needs a real
  GPS fix to render (emulator mock-GPS→FusedLocation unreliable). ⚠️ `RenderMode.COMPASS` was reverted — its
  magnetometer handler (`updateCompassHeading`→`getSourceAs`) crashes on the MapLibre style race; heading-arrow deferred.
- ✅ **iOS heading indicator** — done: `mapView.showsUserHeadingIndicator = true` (heading cone on the user
  dot). Confirm the "navigator" feel on device (needs a real compass).
- **iOS native nav bar** (⏳ needs device): use `NavigationStack`'s bar (title + native back + swipe-back) instead of the Compose `TopBar`. Add `expect fun useNativeHeader(): Boolean` (Android=false, iOS=true); Compose `TopBar` renders nothing on iOS; thread per-route titles to `.navigationTitle` in `ContentView.destinationView`. Larger refactor + swipe-back needs on-device verification.
- **Swipe between tabs** (⏳): HorizontalPager (Android) / paged content (iOS) under the persistent bottom bar. Gesture-conflict risk with the map pan — verify carefully.
- **Locate button / full compass-triangle** (⏳ needs device): optionally drop the iOS bottom-right locate button and/or use a custom `MLNUserLocationAnnotationView` triangle once the heading feel is confirmed on device.

### Excavation record (tutanak) — done + next
- ✅ **On-site record + PDF export** — done: the excavation screen is the tutanak. Data-driven authority
  note (`Community.authorityName/Url`, Türkiye = DKMP 6. Bölge, per-country). Official fields: team
  (defaults to the filler) + excavation date (‹/› stepper, back-datable). Counts support **direct entry**
  (tap the number → type; shared `Stepper` gained `onSet`). Export a one-page **PDF** (`platformSharePdf`
  expect/actual — Android `PdfDocument`, iOS `UIGraphicsPDFRenderer`) via the OS share sheet. Verified on
  Android (`caretta_GZP-12.pdf`); iOS compiles. i18n: `excReportBtn/Title`, `excTeamLabel`, `excDateLabel`,
  `excReportCoord`, `excAuthNote`, `authorityGeneric`.
- **⏳ At-home aggregate reports (V2)**: not just one nest — select nests over a date range / a whole
  beach into one PDF; filter by dates; mark a record "submitted" (in the DB). Optional: pick team from
  community members. Submission stays: export → coordinator → DKMP (server aggregation = later, no direct
  DKMP API). Also add DKMP coordination note to the `beach-rules` guide + a `docs/regulations.md` record.

### iOS surfaces (widgets / Live Activity) — in progress + ideas
- 🔧 **Air widget** (home screen) — code done (`iosApp/CarettaAirWidget/AirWidget.swift` + app-side
  `Widget/AirWidgetBridge.swift` publishing `AirStatus` to App Group `group.com.carettafriends.app`,
  called from `MapTab.reload`). Needs a **Widget Extension target + App Group capability** created in
  Xcode (2-min wizard) — hand-pbxproj can't safely add a whole target / entitlement.
- **⏳ Live Activity / Dynamic Island** ("шторка"): active patrol (distance/time) or "you're at nest GZP-14".
  Reuses the same extension target + App Group. ActivityKit.
- **⏳ Lock-screen widgets** (accessory families) — air / next hatch window.
- **⏳ Nearest nest ≤10 m** — CoreLocation region monitoring / continuous distance → local notification or a
  Live Activity ("ты у гнезда"). Great field UX.

### Store & release readiness — done (2026-07-27)
- ✅ **iOS 1.0 approved** (2026-08-01) — live at `https://apps.apple.com/app/id6794324877`; the landing
  page's App Store button points there. Play still has **no production listing** (Internal testing only),
  so the landing keeps the Google Play button as "soon".
- ✅ **New hatchling icon** (2026-08-01) — Android `1.0.2 (3)` live in Internal testing; iOS `1.0.1 (5)`
  submitted, **WAITING_FOR_REVIEW**. Android gained a real **adaptive icon** (it had none — only legacy
  mipmaps); foreground/background generated by cropping the render, flood-filling the teal plate from
  the border and fitting the egg into the 66/108dp safe circle.
- ⚠️ **Nest coordinates are readable by anyone who installs the app** — `0003_rls_auth.sql` grants
  `select … using (true)` to `authenticated`, and sign-in is anonymous. Gazipaşa is a protected nesting
  beach under DKMP circular **2009/10** (which also requires ministry permission for work on those
  beaches). Conservation practice is to generalize sensitive locations (GBIF best practices). Fix before
  the app gets any real audience: verified-volunteer role for exact coords, generalized/withheld for the
  rest, strip photo EXIF.
- ✅ **targetSdk 36 / v1.0.1 (2)** published to Play **Internal testing** (2026-08-01) — Play mails
  "app must target Android 16" and blocks updates below API 36 from **31 Aug 2026**. `compileSdk` was
  already 36, so only `targetSdk` moved; edge-to-edge was already on. Keep targetSdk on the newest API.
- ✅ **Google Play build**: signed release **AAB** at `docs/store/android/caretta-friends-1.0.0.aab`
  (gitignored). Signing reads `keystore.properties` → `keystore/caretta-upload.jks` (both gitignored —
  **BACK THESE UP**; losing the keystore = can't update the Play listing). Config in
  `composeApp/build.gradle.kts` (`signingConfigs`). Tested on emulator: launches, map + location OK.
- ✅ **16 KB page alignment** (Play requirement for targetSdk 35+): MapLibre **11.5.2 → 11.13.5** +
  forced `androidx.graphics:graphics-path:1.0.1` → both native `.so` ship 16 KB-aligned LOAD segments.
- ✅ **Store screenshots (EN)**: Play `docs/store/play/` (1080×2160, `compose_play.py`); App Store
  `docs/store/ios/` (1320×2868 6.9", `compose_ios.py`, native iOS captures). Raw tabs in `*/raw`,`ios-raw`.
- ✅ **iOS screenshot tooling**: `AppRouter` reads `CF_TAB` env (SIMCTL_CHILD_CF_TAB=beaches|learn|profile)
  to preselect a tab headlessly; onboarding skipped by setting `profile.onboarded=true` in the sim's
  `caretta_state_v2.json`; demo data comes from **Supabase cloud sync on start** (17 nests), not the seed.
  System Events `click at` is TCC-blocked on the sim (only `key code` works) — hence the env approach.
- ✅ **Missing `iosApp` Xcode scheme** added (shared). Only `CarettaAirWidget` was shared, which disabled
  scheme autogen → `-scheme iosApp` builds/device installs were failing.
- **⏳ Submit**: create the App Store + Play listings, upload the AAB / iOS build, fill descriptions +
  App Privacy, privacy URL `https://app.carettafriends.com/privacy`. (BETA_CONTRACT_MISSING = Apple-side.)

### Other pending
- **Photo cloud sync (offline-first, V2)**: nest *metadata* already syncs to Supabase; **photo files** do not — upload to Supabase Storage (→ Cloudflare R2 later, behind `CloudBackend`). Android durability: also save captures to a public MediaStore album (iOS done).
- ✅ iOS camera permission/starting messages — done: localized via `currentStrings().cameraDenied`/`cameraStarting` (EN/RU/TR).
- ✅ **Save button visible without scroll** in AddNest — done: the Save button is now a pinned footer below the scrollable form.
- ✅ Seed demo strings — moot: `seedState()` now seeds `nests = emptyList()`, so the English "Patrol — all OK"/"Looks good today" only linger in stale local state and are gone on a clean install.

---

## ⚠️ Gotchas (read before editing)

- **AppStrings is Map-backed** (`class AppStrings(m: Map<String,String>, months: List<String>)` + 255 computed getters). A plain data class with ~255 constructor args tripped the ART verifier → **VerifyError on launch** (compiles fine, crashes on class-load). Add a field = getter + one `"key" to "value"` in all 3 `mapOf` bundles (EN/RU/TR). `months` is the one non-String field (explicit param).
- **iOS present/share/PHPicker**: use `topmostViewController()` (`ui/IosPresent.kt`) — `keyWindow` is nil on iOS 15+. `UIWindow.isKeyWindow` is a **method** in K/N.
- **Native Swift strings** (map chips, tooltip, patrol dialog, camera) are separate from AppStrings — bridged via `IosEntryKt.currentStrings(): AppStrings`.
- **Kotlin 2.3 ABI trap**: don't add libs whose iosArm64 klib is built with Kotlin 2.3 (we're on 2.2.20) — fails `compileKotlinIosArm64` only (Android tolerates it). No `supabase-kt`/markdown-renderer≥0.39 in commonMain.
- **iPhone install**: raise the tunnel via `xcrun devicectl device info details --device FC73117A-…` first; retry install on "Connection interrupted".
- **Android emulator** drive: `adb shell input tap X Y` (screenshot 900px → ×1.2 → 1080). iOS simulator has no CLI tap (System Events `click at` is TCC-blocked; only `key code` works — e.g. Return to dismiss the location alert).
- **MapLibre LocationComponent crash** (`IllegalStateException: getSourceAs when a newer style is loading`): a real GPS fix drives the stale-state timer → `refreshSource` during a style race. Fixed in `OsmMap.android.kt` via `LocationComponentOptions.enableStaleState(false)`. (Same race the reverted `RenderMode.COMPASS` hit.) MapLibre pinned to **11.13.x** (11.x = OpenGL-ES; 12/13 = Vulkan → emulator MESA crash).

## Build / install

```bash
# Android
./gradlew :composeApp:installDebug
adb shell monkey -p com.carettafriends -c android.intent.category.LAUNCHER 1

# iOS (device)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination generic/platform=iOS -derivedDataPath iosApp/build-device -allowProvisioningUpdates build
xcrun devicectl device install app --device <your-coredevice-UUID> \
  iosApp/build-device/Build/Products/Debug-iphoneos/iosApp.app
```
