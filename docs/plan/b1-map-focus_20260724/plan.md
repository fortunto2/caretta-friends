# Implementation Plan: B1 — Nest geo card opens OUR map, centred on the nest

**Track ID:** b1-map-focus_20260724
**Spec:** [spec.md](./spec.md)
**Created:** 2026-07-24
**Status:** [x] Complete

## Overview

Carry a one-shot `mapFocus: GeoPoint?` in the shared repo. Geo card → `focusMap(point)` + `onOpenMap()`.
Android consumes it reactively through `MapScreen`/`OsmMap`; iOS consumes it via a SwiftUI router that
switches to the Map tab and lets the native `MapLibreView` centre on the point. Focus is transient
(`@Transient`) and cleared after use.

## Phase 1: Shared core (commonMain)

Data plumbing + the geo-card change. Compiles for both targets after this phase.

### Tasks
- [x] Task 1.1: In `domain/Model.kt`, add `@Transient val mapFocus: GeoPoint? = null` to `AppState`
      (import `kotlinx.serialization.Transient`). Confirm the field is excluded from the JSON.
- [x] Task 1.2: In `data/Repository.kt`, add `fun focusMap(p: GeoPoint) { _state.value = _state.value.copy(mapFocus = p) }`
      and `fun takeMapFocus(): GeoPoint? = _state.value.mapFocus.also { if (it != null) _state.value = _state.value.copy(mapFocus = null) }`
      (place near `takePendingPhoto()` / `setDeviceLocation()`).
- [x] Task 1.3: In `ui/screens/NestDetailScreen.kt`, add param `onOpenMap: () -> Unit = {}`. Change the
      geo-card `clickable` (line ~214) from `uri.openUri("https://www.openstreetmap.org/…")` to
      `repo.focusMap(n.point); onOpenMap()`. Remove the now-dead `LocalUriHandler` import + `val uri`
      if unused elsewhere in the file.

### Verification
- [x] `./gradlew :composeApp:compileDebugKotlinAndroid` passes.
- [x] `grep mapFocus` shows the field is `@Transient` and the two repo functions exist.

## Phase 2: Android wiring

Reactive focus → camera animation on the Compose map.

### Tasks
- [x] Task 2.1: In `ui/map/OsmMap.kt` (expect), add params `focus: GeoPoint? = null` and
      `onFocusConsumed: () -> Unit = {}` (defaults on the expect only). Add the same params (no defaults)
      to both actuals: `OsmMap.android.kt` and the `OsmMap.ios.kt` stub (stub ignores them).
- [x] Task 2.2: In `OsmMap.android.kt`, store the `MapLibreMap` in a `mapRef` state var (set inside
      `getMapAsync`). Add `LaunchedEffect(mapRef, focus)` that, when both non-null, calls
      `map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(focus.lat, focus.lng), 17.0))` then
      `onFocusConsumed()`.
- [x] Task 2.3: In `ui/screens/MapScreen.kt`, add params `focus: GeoPoint? = null`,
      `onFocusConsumed: () -> Unit = {}`; pass them into the `OsmMap(...)` call (line ~103).
- [x] Task 2.4: In `CarettaApp.kt`, pass `focus = state.mapFocus, onFocusConsumed = { repo.takeMapFocus() }`
      to `MapScreen` (line ~75) and `onOpenMap = { nav.selectTab(Screen.Map) }` to `NestDetailScreen`
      (line ~110).

### Verification
- [x] `./gradlew :composeApp:installDebug` builds + installs.
- [x] Open a nest → tap the geo card → app switches to the Map tab and animates onto that nest (verified
      on emulator: GZP-12 geo card → Map tab centred+zoomed on Selinus Plajı). One-shot/not-persisted
      guaranteed by `takeMapFocus()` clear + `@Transient`.

## Phase 3: iOS wiring (hybrid SwiftUI + native map)

Thread the tab switch through the SwiftUI shell; native map reads the focus over the bridge.

### Tasks
- [x] Task 3.1: In `iosMain/.../IosEntry.kt`, add `data class IosGeo(val lat: Double, val lng: Double)`
      and `fun takeMapFocus(): IosGeo? = SharedRepo.repo.takeMapFocus()?.let { IosGeo(it.lat, it.lng) }`.
      Add `onOpenMap: () -> Unit` param to `NestDetailVC` and forward it to `NestDetailScreen`.
- [x] Task 3.2: In `iosApp/.../Map/MapLibreView.swift`, add `var focus: CLLocationCoordinate2D? = nil`
      and `var focusTick: Int = 0`; coordinator tracks `focusTick`; in `updateUIView`, when
      `focusTick` changed and `focus != nil`, `mapView.setCenter(focus, zoomLevel: 17, animated: true)`.
- [x] Task 3.3: In `iosApp/.../ContentView.swift`, add `final class AppRouter: ObservableObject`
      (`@Published var selection: AppTab`, `@Published var focusTick: Int`, `func focusMap()`); hoisted
      `Tab` → top-level `AppTab`. `ContentView` owns `@StateObject router`, binds
      `TabView(selection: $router.selection)`, injects `.environmentObject(router)`.
- [x] Task 3.4: Threaded `router` into `destinationView(_:path:router:)`; `.nest` case passes
      `onOpenMap: { router.focusMap() }`. Added `@EnvironmentObject var router` to `TabStack`, `MapTab`,
      `AddFlow` (+ explicit `.environmentObject(router)` on the AddFlow cover — covers don't always inherit).
- [x] Task 3.5: In `MapTab`, added `@State focusCoord`/`@State focusApplyTick`; `.onChange(of: router.focusTick)`
      → `if let g = IosEntryKt.takeMapFocus() { pop path; focusCoord = …; focusApplyTick += 1 }`;
      passes `focus: focusCoord, focusTick: focusApplyTick` into `MapLibreView(...)`.

### Verification
- [x] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` passes.
- [x] iOS build succeeds (`xcodebuild … -destination 'generic/platform=iOS Simulator' build` → **BUILD SUCCEEDED**);
      app installs + launches on iPhone 17 sim with no crash (AppRouter/`@EnvironmentObject` wiring sound).
- [ ] Interactive geo-card tap on iOS — pending MANUAL confirm (simulator has no CLI tap; canonical test
      is on Rust's iPhone). Mechanism is identical to the Android path, which is verified end-to-end.

## Phase 4: Docs & Cleanup

### Tasks
- [x] Task 4.1: In `docs/plan.md`, moved **B1** from Backlog to "✅ Done" with a one-line summary; added
      `focusMap/takeMapFocus` to the `CLAUDE.md` Repository structure note.
- [x] Task 4.2: Removed dead code — `LocalUriHandler` import + `val uri` gone from `NestDetailScreen.kt`;
      confirmed the only remaining `openstreetmap.org` refs are the map tile URLs (Android + iOS), not the geo card.

### Verification
- [x] `docs/plan.md` reflects B1 done; grep confirms no stray external-OSM geo-card link.
- [x] Both Kotlin compile targets clean.

## Final Verification
- [x] All acceptance criteria from spec met (iOS interactive tap pending manual device confirm — see Phase 3).
- [x] `:compileDebugKotlinAndroid` + `:compileKotlinIosSimulatorArm64` pass; iOS simulator build **BUILD SUCCEEDED**.
- [x] Manual (Android): geo card centres the in-app map, one-shot, not persisted (verified on emulator).
      iOS: compiles + launches clean; interactive tap pending device confirm.
- [x] Docs up to date.

## Context Handoff

### Session Intent
Make the nest detail geo card open the in-app map centred on the nest, instead of the external OSM site.

### Key Files
- `composeApp/src/commonMain/kotlin/com/carettafriends/domain/Model.kt` — `AppState.mapFocus` (@Transient)
- `composeApp/src/commonMain/kotlin/com/carettafriends/data/Repository.kt` — `focusMap` / `takeMapFocus`
- `composeApp/src/commonMain/kotlin/com/carettafriends/ui/screens/NestDetailScreen.kt` — geo card + `onOpenMap`
- `composeApp/src/commonMain/kotlin/com/carettafriends/ui/screens/MapScreen.kt` — `focus`/`onFocusConsumed`
- `composeApp/src/commonMain/kotlin/com/carettafriends/ui/map/OsmMap.kt` (+ `.android`/`.ios` actuals)
- `composeApp/src/commonMain/kotlin/com/carettafriends/CarettaApp.kt` — Android wiring
- `composeApp/src/iosMain/kotlin/com/carettafriends/IosEntry.kt` — `IosGeo`/`takeMapFocus`/`NestDetailVC`
- `iosApp/iosApp/ContentView.swift` — `AppRouter`, TabView selection, `.nest` onOpenMap, `MapTab`
- `iosApp/iosApp/Map/MapLibreView.swift` — `focus`/`focusTick` centre

### Decisions Made
- `mapFocus` lives in `AppState` (reactive for Android's StateFlow-driven map) but is `@Transient` so it
  is never serialized — chosen over a plain repo `var` (like `pendingPhoto`) because Android's `MapScreen`
  must recompose on focus, which only StateFlow gives; chosen over a persisted field to avoid re-focusing
  on relaunch.
- iOS uses an `@EnvironmentObject` router (idiomatic SwiftUI) rather than NotificationCenter, so the tab
  switch + focus nudge are explicit and testable; the native map reads focus via `takeMapFocus()`.
- New params on shared declarations carry defaults so the unused iOS `MapVC` and other call sites are untouched.

### Risks
- **Android MapView recreation**: `MapScreen` is recreated on each tab entry, so the map reloads and
  `centerCamera` runs first (zoom 11.5); the focus `LaunchedEffect(mapRef, focus)` must fire after
  `getMapAsync` sets `mapRef` — keep the effect keyed on both so it waits for the map.
- **iOS tab re-selection**: don't rely on `MapTab.onAppear` for focus (TabView keeps tabs alive; onAppear
  is unreliable on re-selection) — use the explicit `router.focusTick` `.onChange`.
- **Focus consume race (Android)**: after `onFocusConsumed` clears `mapFocus`, StateFlow re-emits and
  `MapScreen` recomposes with `focus = null`; `LaunchedEffect(mapRef, null)` must no-op (guard on non-null).
- `@Transient` requires the property to have a default (`= null`) — it does.

---
_Generated by /plan. Tasks marked [~] in progress and [x] complete by /build._
