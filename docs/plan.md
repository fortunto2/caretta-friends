# Caretta Friends — Backlog / Roadmap

Working branch: **`feat/v1-mvp`** (pushed to origin). Installs on Android emulator + Rust's iPhone
(`<your-iPhone-UDID>`, coredevice `<your-coredevice-UUID>`).

Guiding principle (from the client): **radical simplicity**. Volunteers are ordinary people who
"snap a photo & forget". The core loop must be effortless: photograph → smart auto-fill → save.
Everything else automatic / smart-default. Offline-first: capture works with no signal, sync when online.

---

## ✅ Done (shipped this cycle)

- **Field-test fixes (2026-08-06, from Alina's session — GZP-21…24 in prod)**. Four separate bugs
  made "I logged a nest and it's not in my profile" true:
  - **Timeline entries had `createdEpochMillis = 0`** (`FOUND`, `STATUS_CHANGE`, `EXCAVATED`). The
    activity feed sorts and time-filters on it, so a nest you just logged sank to the bottom and
    disappeared entirely under Today/Week/Month. Now stamped; legacy zeros are backfilled on load
    from the entry's date (`repairTimestamps`).
  - **Identity was the display name.** Every install starts as "Volunteer" / author "you", so
    `nestsBy` handed each volunteer everyone else's nests ("мусор от других") and lost their own the
    moment they set a real name. Attribution now runs on the auth uid: `Nest.foundByUserId` +
    `Profile.userId/knownUserIds` (a SET — anonymous → linked email → sign-in all stay "me"), and
    `pullNests` lifts the server's `owner_id` onto old rows so history is attributed too. Names are
    a fallback only, and never a placeholder one (`isPlaceholderName`).
  - **`code = "GZP-${nests.size + 1}"`** counted the whole local list, other volunteers' synced nests
    included → prod has GZP-3 and GZP-16 twice. Now `nextNestCode()` = highest in use + 1.
  - **The same photo made a new nest every time.** `findDuplicate()` catches it two ways —
    `PhotoRef.hash` (md5 of the gallery file / the iOS Photos asset id; the file itself is useless,
    iOS re-encodes each import) and a ≤8 m proximity check (Caretta do nest metres apart, so the
    radius stays under a phone's GPS scatter) — and the save dialog offers "add photo
    to GZP-xx" (folds into the existing nest as an observation) before "it's a different nest".
- **Capture actually works on Android** — the "camera" was a mock viewfinder whose shutter produced
  no photo (and iOS's in-form camera button opened that same mock). Android now launches the system
  camera (`rememberCameraCapture`, TakePicture + FileProvider) and stamps the device's last known fix
  into the JPEG's EXIF; iOS's in-form button hands over to its real native camera (`router.startAddNest()`).
  The mock `CameraScreen` and its route are gone.
- **Gallery photos keep their location** (`PickedPhoto.lat/lng`) and back-date the nest from EXIF —
  on iOS. Android's photo picker redacts GPS unconditionally (see Gotchas), so the form now says so
  instead of silently pinning the nest to the beach centre.
- **Platform-native dialogs** (`ui/PlatformDialog.kt`): `PlatformChoiceDialog` / `PlatformTextPrompt`
  are a real `UIAlertController` on iOS and a Material dialog on Android. Material dialogs inside the
  Compose screens were the main reason the iPhone build read as "an Android app": wrong radius,
  wrong button row, wrong dismiss gesture, wrong keyboard. Migrated: photo source, duplicate nest,
  beach pick, home beach, language, your name, patrol publish. Still Compose (complex content):
  email sign-in, add-update sheet, excavation.
- **Guardian names** (`content/GuardianNames.kt`): a fresh install is "Dawn Guardian" / "Лунный
  Страж" / "Kumul Bekçisi" instead of the literal "Volunteer" every install shared. Existing
  installs still on the placeholder are renamed on load. The profile still nudges for a real name.
- **Android parity (base)**: real patrol recording — a `PatrolService` foreground service
  (`type=location`, ongoing notification showing the distance) owns the GPS, so a walk survives the
  screen lock and leaving the map; `PatrolTrack` holds it as Compose state. The pill used to be
  decoration that recorded nothing, then a recorder tied to the map screen's composition that any
  navigation destroyed. Background location is deliberately NOT requested: the service is only ever
  started while the app is on screen, captures saved to a "Caretta Friends" gallery album so they survive a
  reinstall (iOS already did), and a "locate me" button (`OsmMap(recenterTick)`).
- **Patrol is a coordinator's tool now** — the big "Start patrol" pill confused visitors, so it's a
  small icon on the right edge, shown only to a signed-in member with a role; the "no patrol yet
  today" pill hides for everyone else.
- **Beaches**: your home beach is pinned to the top of the list and marked; a beach's nest feed is
  ordered by what needs attention today (hatching → soon → incubating → finished) and its stat tiles
  hide zeros. AddNest shows ONE auto-picked beach with "change" instead of the full inline list.
- **Profile simplified**: "My nests" list (the thing volunteers open the screen for), zero-value stat
  tiles hidden, badges derived from real activity, share actions moved into the "⋮" menu, settings
  grouped at the bottom, and a nudge to set a real name while it's still the default.

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

### Release 1.0.3 / 1.0.2 (2026-08-07) — artifacts built, upload pending

- **Android `1.0.3 (4)`** — signed AAB at `docs/store/android/caretta-friends-1.0.3.aab` (gitignored),
  signed with the CARETTA upload key from `keystore.properties`.
- **iOS `1.0.2 (6)`** — archive at `iosApp/build-archive/CarettaFriends.xcarchive`.
- **Upload is manual**: this machine has no Play service-account JSON and no App Store Connect API
  key, so neither `altool` nor a Gradle publisher can push. Either drop credentials in (ASC API key
  `.p8` + issuer/key id; Play service account JSON) and it can be scripted, or upload by hand —
  Play Console → Internal testing → new release → drop the AAB; Xcode → Window → Organizer →
  Distribute App, or Transporter with the exported .ipa.
- Contents: everything from the 2026-08-06/07 field-test fixes through photo sync on R2, the
  foreground-service patrol and the native dialogs.

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

- **Onboarding asks for your beach** — a 4th page lists the nearest beaches (with their community)
  and sets `homeBeachId`; skippable, changeable in the profile. It's the one setting that shapes
  every list after it, and nobody ever found it in settings.
- ✅ **Photo cloud sync — done (2026-08-06)**: files live in **Cloudflare R2**, records stay in
  Supabase. `workers/photos` (Worker + R2 binding, `photos.carettafriends.com`) is the only door:
  it verifies the volunteer's Supabase access token against the published JWKS (ES256 — no shared
  secret), serves any object to a signed-in volunteer and accepts writes only under the caller's own
  uid prefix; no DELETE at all, matching `0005_no_delete.sql`. Client: `data/PhotoStorage.kt`
  (`R2PhotoStorage` + `PhotoFiles` resolver/disk cache), `PhotoRef.remotePath`, uploads on capture
  and on every sync (offline-first — the record saves immediately, the file follows), `NestPhoto()`
  fetches on demand. Verified end to end: photographed on the Android emulator → visible on the iOS
  simulator. Supabase Storage was tried first and dropped (0007 → 0008): Supabase is the sync
  database and identity provider, not a blob store.
- ✅ iOS camera permission/starting messages — done: localized via `currentStrings().cameraDenied`/`cameraStarting` (EN/RU/TR).
- ✅ **Save button visible without scroll** in AddNest — done: the Save button is now a pinned footer below the scrollable form.
- ✅ Seed demo strings — moot: `seedState()` now seeds `nests = emptyList()`, so the English "Patrol — all OK"/"Looks good today" only linger in stale local state and are gone on a clean install.

---

## 🔴 Known, NOT fixed (decide before a public release)

- **Nest photos are readable by anyone who can mint an anonymous token** — i.e. anyone who extracts
  the app's publishable key from the APK/IPA. The photo Worker enforces "signed-in volunteer", and
  anonymous sign-up is open, so the door is exactly as wide as it already is for `nests` rows
  (`0003_rls_auth.sql`, `select using (true)`). Same fix as the coordinate exposure below: a
  verified-volunteer role, granted by a community admin, gating both.
- **iOS: opening the camera from inside the add-nest form discards the typed note / date / beach.**
  `router.startAddNest()` resets the tab's navigation stack. Cancel now returns to a fresh form
  instead of a bare map, but the entered values are gone. Proper fix: present the camera over the
  existing stack and feed the photo back into the live form.

## ⚠️ Gotchas (read before editing)

- **AppStrings is Map-backed** (`class AppStrings(m: Map<String,String>, months: List<String>)` + 255 computed getters). A plain data class with ~255 constructor args tripped the ART verifier → **VerifyError on launch** (compiles fine, crashes on class-load). Add a field = getter + one `"key" to "value"` in all 3 `mapOf` bundles (EN/RU/TR). `months` is the one non-String field (explicit param).
- **Never identify a volunteer by name.** `Profile.displayName` starts as "Volunteer" on every
  install and `NestUpdate.author` defaults to "you" — matching on those gave everyone everyone
  else's records. Ownership = `foundByUserId` ∈ `AppState.myUserIds` (`AppState.isMine`).
- **Every timeline entry needs `createdEpochMillis`.** The activity feed both sorts and filters on
  it; a zero silently hides the entry from every time range. There is no "unset" that behaves.
- **Android's photo picker always strips GPS** — `MediaStore.setRequireOriginal` can't reach its
  `content://media/picker/…` URIs, and ACCESS_MEDIA_LOCATION doesn't change that (verified on API
  36). A gallery import on Android has no coordinates, by platform design; capture is the only path
  to a real fix there. iOS PHPicker keeps the EXIF, so the same code does work on iOS.
- **Launching IMAGE_CAPTURE needs a granted CAMERA permission** whenever the app *declares* it —
  otherwise Android refuses with "Permission Denial: … with revoked permission", and the camera
  simply never opens. Ask at the tap, then launch.
- **iOS present/share/PHPicker**: use `topmostViewController()` (`ui/IosPresent.kt`) — `keyWindow` is nil on iOS 15+. `UIWindow.isKeyWindow` is a **method** in K/N.
- **Native Swift strings** (map chips, tooltip, patrol dialog, camera) are separate from AppStrings — bridged via `IosEntryKt.currentStrings(): AppStrings`.
- **Kotlin 2.3 ABI trap**: don't add libs whose iosArm64 klib is built with Kotlin 2.3 (we're on 2.2.20) — fails `compileKotlinIosArm64` only (Android tolerates it). No `supabase-kt`/markdown-renderer≥0.39 in commonMain.
- **iPhone install**: raise the tunnel via `xcrun devicectl device info details --device FC73117A-…` first; retry install on "Connection interrupted".
- **Photo Worker**: `cd workers/photos && wrangler deploy`. Bucket `caretta-nest-photos` (EEUR).
  The Worker trusts nothing but the Supabase JWKS — if Supabase ever switches signing algorithm,
  `verify()` must learn the new one (it deliberately refuses anything but ES256).
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
