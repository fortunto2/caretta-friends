# Specification: B1 — Nest geo card opens OUR map, centred on the nest

**Track ID:** b1-map-focus_20260724
**Type:** Feature
**Created:** 2026-07-24
**Status:** Draft

## Summary

Today the geo card on the nest detail screen (`NestDetailScreen.kt:210-237`) opens the **external**
`openstreetmap.org` in the system browser via `LocalUriHandler`. This breaks the offline-first,
in-app experience: the volunteer leaves the app to see a location they already have on our own map.

B1 changes the geo card to open the **in-app Map tab centred (and zoomed) on that nest**. The
mechanism is a one-shot "map focus" signal carried in the shared layer: tapping the card stores the
nest's `GeoPoint` in the repo (`focusMap`), switches to the Map tab, the map animates its camera to
that point, then the signal is consumed (`takeMapFocus`) so it never re-triggers — including after
an app restart (focus is transient, never persisted).

Both platforms are covered: Android uses the Compose `OsmMap` (expect/actual `MapView`); iOS uses
the **native** `MapTab`/`MapLibreView` (the Compose `MapScreen`/`MapVC` is not used on iOS), so the
tab switch is threaded through the SwiftUI shell and the native map reads the focus over the KMP bridge.

## Acceptance Criteria

- [ ] Tapping the nest geo card no longer opens external `openstreetmap.org` (no `LocalUriHandler` call).
- [ ] Tapping the geo card switches to the in-app **Map** tab.
- [ ] The map animates/centres + zooms in on the nest's coordinate (Android Compose `OsmMap`).
- [ ] The map animates/centres + zooms in on the nest's coordinate (iOS native `MapLibreView`).
- [ ] The focus is **one-shot**: consumed after centring; navigating the map afterwards does not snap back.
- [ ] The focus is **not persisted**: after a full app restart the map opens at its default view, not the last-focused nest.
- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid` and `:compileKotlinIosSimulatorArm64` both pass.
- [ ] iOS device build (`xcodebuild … -scheme iosApp`) succeeds.

## Dependencies

- None external. Uses existing MapLibre (Android `org.maplibre.android`, iOS `MapLibre`) already in the project.

## Out of Scope

- B3 (contextual "+"), C (native nav / compass heading / Android my-location), D (feed filters).
- Changing the map's default (non-focused) camera behaviour.
- Any Supabase/cloud change — focus is a purely local, transient UI signal.

## Technical Notes

- **`AppState.mapFocus: GeoPoint?`** — new nullable field, marked **`@Transient`** (`Model.kt:271` is
  `@Serializable`, repo Json uses `encodeDefaults = true` → without `@Transient` the focus would be
  written to `caretta_state_v2.json` and re-applied on relaunch). Precedent for a nullable-GeoPoint
  AppState field: `deviceLocation` (`Model.kt:290`).
- **Repository**: `focusMap(p: GeoPoint)` sets it via `_state.value = _state.value.copy(mapFocus = p)`;
  `takeMapFocus(): GeoPoint?` reads-and-clears (mirrors `takePendingPhoto()` at `Repository.kt:282`).
  Android clears reactively via StateFlow; iOS reads the snapshot over the bridge.
- **Android reactive path**: `MapScreen(state, …)` reads `state.mapFocus`, passes it to
  `OsmMap(focus = …, onFocusConsumed = …)`. `OsmMap.android` stores the `MapLibreMap` in a state var
  and, in `LaunchedEffect(mapRef, focus)`, calls `map.animateCamera(newLatLngZoom(focus, ~17))` then
  `onFocusConsumed()` (→ `repo.takeMapFocus()`), which emits `mapFocus = null` and settles.
- **iOS native path**: `MapTab`/`MapLibreView` pull snapshots, not the StateFlow. Add
  `IosEntryKt.takeMapFocus(): IosGeo?` and a SwiftUI `AppRouter` (`@EnvironmentObject`) carrying the
  TabView `selection` + a `focusTick`. The geo card's `onOpenMap` closure (threaded through
  `NestDetailVC` → `destinationView` `.nest`) sets `router.selection = .map; router.focusTick += 1`.
  `MapTab` observes `focusTick`, calls `takeMapFocus()`, centres `MapLibreView` (new `focus`/`focusTick`
  params, symmetric to the existing `recenterTick` locate mechanism at `MapLibreView.swift:92,119`),
  and pops its own `NavigationPath` (so a nest opened from the Map tab uncovers the map).
- **Signature additions carry defaults** on the common declarations (`MapScreen`, `expect fun OsmMap`,
  `NestDetailScreen`) so the unused iOS `MapVC` and every other call site keep compiling.
