# 🐢 Caretta Friends

**🌐 [carettafriends.com](https://carettafriends.com)** · iOS + Android · Kotlin Multiplatform

A cute, field-friendly app for volunteers protecting **loggerhead sea-turtle (Caretta caretta)
nests** — from the crawl at dawn to the night the hatchlings reach the sea.

Built for **[Caretta Gazipaşa](https://carettafriends.com)** (Gazipaşa/Antalya, Turkey), designed
to work for any nesting-beach volunteer community.

## What it does

- **Shared map** of nests, beaches and trash — mark a nest in ≤20 s (camera/gallery + EXIF geotag),
  plus an **AR nest finder**: point the phone and see nests pinned where they physically are.
- **Nest lifecycle**: countdown to hatch, timeline of updates & comments, then the **excavation**
  record (FWC categories → Hatching % + Emergence Success %), exportable as a **PDF** for the
  authority — coordinated with the community's regulator (e.g. DKMP in Türkiye).
- **Temperature & sex prediction** (TSD) shown honestly as a **range** — no hardware needed in V1
  (exposure tag + weather API), calibrated to regional data (Anamur 28.9 °C).
- **Patrols & coverage** so volunteers don't re-walk a covered stretch.
- **Community**: WhatsApp + site + Instagram, beach leaders. **Learn**: facts + guide.
- **Gamification**: personal impact ("N hatchlings reached the sea"), streak, badges.

Offline-first. Works without an account (viewing + adding). Multilingual — **Russian, Turkish, English**.

## Tech

Kotlin Multiplatform + Compose Multiplatform (shared UI/logic) · **iOS = native SwiftUI shell +
ARKit + MapLibre** hosting shared Compose screens · MapLibre + OpenStreetMap · offline-first, syncs to
**Supabase** (Postgres + PostGIS). See [`CLAUDE.md`](CLAUDE.md) and [`docs/`](docs/) for the research,
design spec, data model, and backlog.

## Run

**Android:**
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :composeApp:installDebug     # build + install on a running emulator/device
```

**iOS** (open `iosApp/iosApp.xcodeproj` in Xcode and run, or CLI):
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination generic/platform=iOS -allowProvisioningUpdates build
```

App Store screenshots live in [`docs/store/`](docs/store/).

## License

**MIT** — see [`LICENSE`](LICENSE). Open source: fork it, adapt it, and run your own sea-turtle
(or any wildlife) nesting-beach app for your community. Contributions welcome.

---
*Made with care for the turtles of Gazipaşa. 🐢🌊*
