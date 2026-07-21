# 🐢 Caretta Friends

A cute, field-friendly app for volunteers protecting **loggerhead sea-turtle (Caretta caretta)
nests** — from the crawl at dawn to the night the hatchlings reach the sea.

Built for **[Caretta Gazipaşa](https://carettafriends.com)** (Gazipaşa/Antalya, Turkey), designed
to work for any nesting-beach volunteer community.

## What it does

- **Shared map** of nests, landmarks and trash — mark a nest in ≤20 s (camera/gallery + EXIF geotag).
- **Guided nest vs false-crawl** logging (the #1 field error), confirmed/unconfirmed states.
- **Nest lifecycle**: countdown to hatch, timeline of updates & comments, then the **excavation**
  count (FWC categories → Hatching % + Emergence Success %).
- **Temperature & sex prediction** (TSD) shown honestly as a **range** — no hardware needed in V1
  (exposure tag + weather API), calibrated to regional data (Anamur 28.9 °C).
- **Patrols & coverage** so volunteers don't re-walk a covered stretch.
- **Community**: WhatsApp + site + Instagram, beach leaders. **Learn**: facts + guide.
- **Gamification**: personal impact ("N hatchlings reached the sea"), streak, badges.

Offline-first. Works without an account (viewing + adding). Multilingual (RU/TR/EN planned).

## Tech

Kotlin Multiplatform + Compose Multiplatform · MapLibre + OpenStreetMap · offline-first
(Supabase + PostGIS in V2). See [`CLAUDE.md`](CLAUDE.md) and [`docs/`](docs/) for the research,
design spec, and data model.

## Run

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

---
*Made with care for the turtles of Gazipaşa. 🐢🌊*
