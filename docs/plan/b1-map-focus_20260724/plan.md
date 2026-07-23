# Implementation Plan: B1 — Nest geo card opens OUR map, centred on the nest

**Track ID:** b1-map-focus_20260724
**Spec:** [spec.md](./spec.md)
**Created:** 2026-07-24
**Status:** [ ] Not Started

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
- [ ] Task 3.1: In `iosMain/.../IosEntry.kt`, add `data class IosGeo(val lat: Double, val lng: Double)`
      and `fun takeMapFocus(): IosGeo? = SharedRepo.repo.takeMapFocus()?.let { IosGeo(it.lat, it.lng) }`.
      Add `onOpenMap: () -> Unit` param to `NestDetailVC` and forward it to `NestDetailScreen`.
- [ ] Task 3.2: In `iosApp/.../Map/MapLibreView.swift`, add `var focus: CLLocationCoordinate2D? = nil`
      and `var focusTick: Int = 0`; in the coordinator track `lastFocusTick`; in `updateUIView`, when
      `focusTick` changed and `focus != nil`, `mapView.setCenter(focus, zoomLevel: 17, animated: true)`.
- [ ] Task 3.3: In `iosApp/.../ContentView.swift`, add `final class AppRouter: ObservableObject`
      (`@Published var selection: Tab`, `@Published var focusTick: Int`); hoist `Tab` out of
      `ContentView` (or reference it from the router). `ContentView` owns `@StateObject router`, binds
      `TabView(selection: $router.selection)`, injects `.environmentObject(router)`.
- [ ] Task 3.4: Thread `router` into `destinationView(_:path:router:)`; in the `.nest` case pass
      `onOpenMap: { router.selection = .map; router.focusTick += 1 }`. Add `@EnvironmentObject var router`
      to `TabStack`, `MapTab`, `AddFlow` (the three `destinationView` call sites).
- [ ] Task 3.5: In `MapTab`, add `@State focusCoord`/`@State focusApplyTick`; `.onChange(of: router.focusTick)`
      → `if let g = IosEntryKt.takeMapFocus() { focusCoord = CLLocationCoordinate2D(latitude: g.lat, longitude: g.lng); focusApplyTick += 1; path = NavigationPath() }`;
      pass `focus: focusCoord, focusTick: focusApplyTick` into `MapLibreView(...)`.

### Verification
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` passes.
- [ ] iOS device build succeeds (`xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp … build`).
- [ ] On device: nest → tap geo card → Map tab comes forward centred on the nest (from a nest opened
      on the Map tab, and from one opened on Beaches/Profile).

## Phase 4: Docs & Cleanup

### Tasks
- [ ] Task 4.1: In `docs/plan.md`, move **B1** from Backlog to "✅ Done" with a one-line summary; update
      `CLAUDE.md` structure notes only if a new public helper needs mention (mapFocus/focusMap).
- [ ] Task 4.2: Remove dead code — unused `LocalUriHandler` import in `NestDetailScreen.kt`, any unused
      imports introduced; confirm no `openstreetmap.org` reference remains for the geo card.

### Verification
- [ ] `docs/plan.md` reflects B1 done; grep confirms no stray external-OSM geo-card link.
- [ ] Both Kotlin compile targets clean.

## Final Verification
- [ ] All acceptance criteria from spec met.
- [ ] `:compileDebugKotlinAndroid` + `:compileKotlinIosSimulatorArm64` pass; iOS device build succeeds.
- [ ] Manual: geo card centres the in-app map on both platforms; one-shot; not persisted across restart.
- [ ] Docs up to date.

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
