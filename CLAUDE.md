# CLAUDE.md — Caretta Friends

Mobile app for volunteers protecting **loggerhead sea-turtle (Caretta caretta) nests**.
Client: **Caretta Gazipaşa** (carettafriends.com), Gazipaşa/Antalya, Turkey. Multi-community, expanding.
Vibe: **simple, cute, warm, field-legible**. Core principle: **radical simplicity** — volunteers "snap a
photo & forget"; the capture flow must be effortless, everything else smart-default. Offline-first.

> **Roadmap / backlog / gotchas → [`docs/plan.md`](docs/plan.md).** Read it before starting work.

## Stack (validated, cached versions — do not bump casually)

- **Kotlin Multiplatform + Compose Multiplatform** (shared UI/logic). Kotlin **2.2.20**, Compose MP **1.9.0**,
  AGP **8.7.3**, Gradle **8.11.1**. `compileSdk=36`, `targetSdk=36`, `minSdk=26`. Package `com.carettafriends`.
  ⚠️ Play kills uploads targeting >~1 year behind the latest Android: API 36 is required from
  **31 Aug 2026** — keep `targetSdk` on the newest API, don't let it lag (`/solo:android-release`).
- **iOS = HYBRID**: native SwiftUI shell (`iosApp/iosApp/ContentView.swift`, TabView + per-tab NavigationStack)
  hosting shared Compose screens via `IosEntry.kt` VC factories. iOS map + camera are **native Swift**.
- Map: **MapLibre + OpenStreetMap** (no key). Android = Compose `OsmMap` (expect/actual, MapView in AndroidView);
  iOS = native `Map/MapLibreView.swift`.
- Persistence: **offline-first** okio `LocalStore` (`caretta_state_v2.json`). Sync: **Supabase** Postgres+PostGIS
  via raw PostgREST over ktor (`CloudBackend`), pushed on change + `syncOnStart`. Auth: **anonymous GoTrue** (JWT),
  Google/email = V2. ⚠️ NOT supabase-kt (Kotlin-2.3 ABI trap) — see plan.md.
- kotlinx-datetime + kotlinx-serialization. i18n: `content/AppStrings.kt` (EN/RU/TR).

## Structure

```
composeApp/src/
  commonMain/kotlin/com/carettafriends/
    App.kt · CarettaApp.kt         — root, Navigator host, always-on BottomBar
    domain/Model.kt                — data classes + enums + AppState helpers (resolveMember, nestsBy, nestMapPhase…)
    data/Repository.kt             — StateFlow repo, seed, TSD model, addNest/addUpdate, sync, nestDay/nestMapPhase, focusMap/takeMapFocus
    data/CloudBackend*.kt AuthBackend.kt LocalStore.kt WeatherClient.kt BeachDiscovery.kt
    content/AppStrings.kt          — ⚠️ Map-backed i18n catalog (255 getters, 3 mapOf bundles). See plan.md gotcha.
    content/GuideContent.kt        — L10n guide facts/articles
    ui/Nav.kt                      — sealed Screen + Navigator (Map/Beaches/Learn/Profile tabs + pushes)
    ui/theme/  ui/components/       — caretta palette, Pill/CarettaCard/CountdownRing/TopBar/BottomBar…
    ui/screens/*.kt                — Map, Beaches, BeachDetail, Learn, Profile, MemberProfile, Community,
                                      NestDetail, Excavation, AddNest, Camera, Onboarding, ActivityFeed (feed component)
    ui/PhotoPicker / IosPresent    — expect/actual gallery picker; iOS topmostViewController()
  androidMain/  iosMain/           — platform actuals (OsmMap.android, IosEntry.kt VC factories, Share/PhotoPicker)
iosApp/iosApp/                     — SwiftUI shell: ContentView.swift, Map/MapLibreView.swift, Camera/CameraCaptureView.swift
supabase/migrations/               — 0001..0005 (relational + PostGIS + RLS)
docs/plan.md                       — backlog, gotchas, build commands (READ THIS)
```

## Build / install

```bash
# Android (emulator running)
./gradlew :composeApp:installDebug
adb shell monkey -p com.carettafriends -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > /tmp/shot.png        # verify (drive: adb shell input tap X Y; shot is 900px, ×1.2→1080)

# iOS (device — Rust's iPhone, UDID <your-iPhone-UDID>, coredevice <your-coredevice-UUID>)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination generic/platform=iOS -derivedDataPath iosApp/build-device -allowProvisioningUpdates build
xcrun devicectl device info details --device <your-coredevice-UUID>   # raise the tunnel first
xcrun devicectl device install app --device <your-coredevice-UUID> \
  iosApp/build-device/Build/Products/Debug-iphoneos/iosApp.app
# validate Kotlin fast: ./gradlew :composeApp:compileDebugKotlinAndroid  (Android) / :compileKotlinIosSimulatorArm64 (iOS)
```

## Conventions

- Colors via the `caretta` accessor. Emoji as glyphs (no material-icons dep). Screens = `@Composable fun XScreen(...)`.
- Add an i18n string = getter in `AppStrings` class + `"key" to "value"` in **all three** mapOf bundles (else TR build fails / getValue crashes).
- Native Swift chrome strings come from `IosEntryKt.currentStrings(): AppStrings` (not a parallel struct).
- Commit or push only when asked; end commit messages with the Co-Authored-By trailer.

## Gotchas (full list in docs/plan.md)

- **AppStrings must stay Map-backed** — a ~255-arg data-class constructor crashes on launch (ART VerifyError).
- iOS present/share/PHPicker → `topmostViewController()` (`keyWindow` nil on iOS 15+). `isKeyWindow` is a K/N method.
- Don't add libs built with Kotlin 2.3 to commonMain (iosArm64 ABI trap; Android hides it).
- iPhone install: raise the tunnel via `devicectl device info details` first; retry on "Connection interrupted".
