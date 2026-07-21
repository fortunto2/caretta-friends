# CLAUDE.md — Caretta Friends

Mobile app for volunteers protecting **loggerhead sea-turtle (Caretta caretta) nests**.
Client: **Caretta Gazipaşa** (carettafriends.com), Gazipaşa/Antalya, Turkey. Multi-community,
designed to expand. Vibe: simple, cute, warm, field-legible.

## Stack (validated, all cached versions)

- **Kotlin Multiplatform + Compose Multiplatform** (shared UI). Android-first; iOS targets added later.
- Kotlin **2.2.20**, Compose MP **1.9.0**, AGP **8.7.3**, Gradle **8.11.1** (wrapper).
- `compileSdk=35`, `minSdk=26`, `targetSdk=35`. Package `com.carettafriends`.
- kotlinx-datetime, kotlinx-serialization. Map: **MapLibre + OSM** (planned native integration).
- Backend: **Supabase + PostGIS** (V2); V1 is **offline-first, in-memory repository** (no auth).

## Structure

```
composeApp/src/
  commonMain/kotlin/com/carettafriends/
    App.kt, CarettaApp.kt          — root + nav host + bottom bar
    domain/Model.kt                — data classes + enums (mirrors docs/data-model.md)
    data/Repository.kt             — in-memory StateFlow repo + Gazipaşa seed + TSD model
    ui/Nav.kt                      — sealed Screen + Navigator
    ui/theme/Theme.kt              — CarettaTheme, palette (sand/sea/coral), typography, shapes
    ui/components/Components.kt     — Pill, CarettaCard, CountdownRing, SexRangeBar, Stepper, BottomBar, TopBar…
    ui/screens/*.kt                — Map, Beaches, Learn, Profile, AddNest, NestDetail, Excavation, Camera, Community
  androidMain/                     — MainActivity, AndroidManifest, platform actuals
docs/                              — research.md, design-spec.md, data-model.md, interview-script.md
```

## Commands

```bash
# build + install + run on emulator (Medium_Phone, API 37)
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n com.carettafriends/.MainActivity
adb exec-out screencap -p > /tmp/shot.png     # verify UI
```

Emulator boot (headless): `$ANDROID_HOME/emulator/emulator -avd Medium_Phone -no-window -gpu swiftshader_indirect &`

## V1 scope (this build)

- No authentication — view + add works locally, offline. Auth (Supabase/Google) gates writes in V2.
- Shared map with multi-type markers (nest/landmark/trash), nest/false-crawl guided add, camera/gallery
  + EXIF geotag, confirmed/unconfirmed, nest timeline (updates+comments), FWC excavation counts
  (Hatching % + Emergence %), TSD sex prediction as a **range** (regional model, no loggers in V1),
  patrols/coverage, Learn (facts+guide), Community (links+leaders), Profile (impact/streak/badges).

## Conventions

- Colors via the `caretta` accessor (LocalCaretta). Emoji as glyphs (no material-icons dep).
- Screens are `@Composable fun XScreen(...)`; navigation via `Navigator` (sealed `Screen`).
- Keep new deps out of V1 unless necessary. Match ProfileScreen.kt for style.
- Data model source of truth: `docs/data-model.md`. Design source of truth: `docs/design-spec.md`.

## Next steps

- MapLibre native map (expect/actual, Android AndroidView + OSM style) replacing the stylized map.
- iOS targets (iosX64/iosArm64/iosSimulatorArm64) + iosApp entry + framework build.
- SQLDelight persistence + Supabase sync + Google auth (V2).
- Real CameraX capture + photo storage + EXIF read.
