---
type: design-spec
project: caretta-friends
title: Caretta Friends — Design PRD
status: draft
created: 2026-07-21
platform: mobile (iOS + Android, Compose Multiplatform)
audience: sea turtle nest volunteers (mixed age, non-technical, outdoors)
---

# Caretta Friends — Design PRD

A field app for volunteers who protect **loggerhead sea turtle (Caretta caretta)** nests
on Mediterranean beaches. This document is the **design handoff**: hand it to Claude Design
(or any designer) to generate high-fidelity screens. It defines the visual identity, the
component system, and every V1 screen with its states.

> **Product one-liner:** A shared map where volunteers mark and care for turtle nests —
> from the moment a nest is found, through the ~55-day wait, to the night the hatchlings
> reach the sea.

---

## 1. Design principles (the brief)

The app lives on a bright beach at dawn, in the hands of a volunteer who may be tired,
wearing gloves, one-handed, offline, and squinting into the sun. Cute must never cost clarity.

1. **Field-first.** Big tap targets (min 48dp), high contrast for sunlight, one-handed
   reach (primary actions in the bottom third), works fully offline.
2. **Cute but credible.** Warm, rounded, hopeful — this is a labour of love. But it carries
   real science (TSD, hatch success), so it must feel trustworthy, not a toy.
3. **The map is home.** Everything starts and ends on the map. Adding a marker is one tap away.
4. **Progress you can feel.** A nest is a small story with a countdown and a happy ending.
   Make the wait tangible and the hatch celebratory.
5. **Quiet gamification.** Motivate volunteers without turning conservation into a slot machine.
   Impact ("312 hatchlings reached the sea") over vanity points.
6. **Multilingual by default.** RU / TR / EN from day one (Gazipaşa is a mixed community).

---

## 2. Visual identity

### 2.1 Palette

A warm **sand + sea** world with a **coral sunset** accent. Neutrals are biased warm/teal,
never pure grey. Semantic colors are separate from the accent.

**Light theme**

| Token | Hex | Use |
|-------|-----|-----|
| `sand` | `#FBF6EC` | app background (warm cream) |
| `surface` | `#FFFFFF` | cards, sheets |
| `sea` | `#0F7A82` | primary — teal, nav, nest brand |
| `deep` | `#0B3B3F` | headings, deep sea |
| `coral` | `#FF7A59` | accent — primary CTA, FAB |
| `sunlit` | `#F6C453` | gold — badges, highlights, "hatching soon" |
| `ink` | `#22302E` | body text (warm charcoal, not black) |
| `muted` | `#6E827E` | secondary text, captions |
| `line` | `#E7E0D2` | hairline dividers, borders |

**Dark theme ("night patrol"** — hatchlings emerge at night; lean into it)

| Token | Hex | Use |
|-------|-----|-----|
| `sand` | `#0B1F22` | background (deep sea-night) |
| `surface` | `#122E32` | cards |
| `sea` | `#3BB7BE` | primary (brightened for dark) |
| `deep` | `#EAF3EF` | headings (cream) |
| `coral` | `#FF8A6B` | accent CTA |
| `sunlit` | `#F6C453` | gold (unchanged, glows on dark) |
| `ink` | `#E7F0EC` | body text |
| `muted` | `#8AA39E` | secondary text |
| `line` | `#1E4147` | dividers |

**Semantic (status), same in both themes, tuned for contrast**

| Meaning | Light | Dark | Use |
|---------|-------|------|-----|
| active / incubating | `sea` | `sea` | nest doing fine |
| hatching soon | `#E8A93C` | `#F6C453` | within ~7 days of predicted hatch |
| hatched / success | `#4FA96A` | `#5FCB80` | emerged, high success |
| at risk / predation / wash-over | `#E4574B` | `#FF7367` | needs attention |
| lost | `muted` | `muted` | nest failed/destroyed |

### 2.2 Marker types (map iconography)

Rounded "map-pin bubble" with a soft drop shadow; the type sets the color + glyph.

| Type | Glyph | Color | Notes |
|------|-------|-------|-------|
| **Nest** 🥚 | turtle egg / hatchling | `sea` (ring shows status) | the hero object; ring color = status |
| **Landmark / ориентир** | flag / pin | `deep` | reference points for navigation |
| **Trash / мусор** | leaf-crossed bin | `#8A6F4B` sandy-brown | cleanup logging |
| *(extensible)* predator sign, crawl, obstacle… | — | — | add later without redesign |

Nest pins carry a thin **status ring** (semantic color) and, when < 7 days to hatch, a gentle
pulsing halo in `sunlit`.

### 2.3 Typography

CSP/native reality: **no webfont dependency.** Use a rounded system display face and a warm
sans body. Never let type silently fall back.

- **Display / headings:** `ui-rounded` → **SF Pro Rounded** on Apple; on Android/Compose use
  **Baloo 2** or **Nunito** bundled as an app resource (rounded, friendly, still legible).
- **Body / UI:** `system-ui` → SF Pro Text / Roboto. Warm, neutral, highly readable.
- **Numeric / data:** tabular numerals for countdowns, counts, temperatures.

**Type scale** (mobile): Display 28/34 · Title 22/28 · Headline 17/22 semibold ·
Body 16/22 · Label 13/16 (uppercase, +0.04em tracking) · Caption 12/16 muted.

### 2.4 Shape, elevation, motion

- **Radius:** cards 20dp, sheets 28dp (top), buttons/pills fully rounded, inputs 14dp.
- **Elevation:** soft, low, warm-tinted shadows (`rgba(11,59,63,0.10)`), never harsh black.
- **Motion:** gentle and organic (ease-out, 200–300ms). A hatch is the one celebratory
  moment — confetti/rising-hatchlings animation. Respect `prefers-reduced-motion`.
- **Illustration:** simple, flat, rounded turtle/egg/wave motifs. A friendly loggerhead
  mascot ("Caretta") appears in empty states and the hatch celebration. Warm line-fills,
  2-3 palette colors, no heavy outlines.

---

## 3. Component library

- **Bottom nav** (3 tabs V1): Map · Beaches · Profile. Active tab in `sea`, filled icon.
- **FAB** — coral, "＋", bottom-right above nav; opens Add-marker sheet.
- **Filter chips** — horizontal scroll row over the map: All · Nests · Landmarks · Trash ·
  "Hatching soon". Selected = filled `sea`.
- **Nest card** — thumbnail, nest code, beach, status pill, mini countdown. Used in lists.
- **Countdown ring** — circular progress "Day 38 / ~55", color = status. Centerpiece of nest detail.
- **Sex-ratio bar** — horizontal split ♂/♀ from temperature model, with a "predicted" tag
  and an info affordance (explains TSD, pivotal ~29 °C).
- **Stat tile** — big number + label ("312 hatchlings → sea"), used on profile.
- **Badge chip** — circular emblem, earned = full color, locked = greyed with progress.
- **Timeline row** — date · icon · note · optional photo (observation history).
- **Stepper counter** — big +/− for excavation counts (glove-friendly, huge tap targets).
- **Public/Private toggle** — segmented, with a plain-language explainer ("Public: visible to
  all volunteers on the map. Private: only you.").
- **Empty state** — mascot + one friendly line + a clear action.

---

## 4. Screens (V1)

V1 has **no authentication** — a lightweight local volunteer identity (name + avatar) is
enough. Auth (Google/Supabase) arrives in V2 for cross-device profiles and the shared cloud map.

### 4.1 Onboarding / region select
- Pick **region → city → beach(es)** (default Gazipaşa; extensible). Warm one-screen flow.
- Optional: name + cute avatar (turtle color). Skippable.
- Sets which beaches the volunteer is "assigned" to (filters the map, feeds leaderboard).

### 4.2 Map (home) — the hero screen
- Full-bleed **OpenStreetMap (MapLibre)** with the coastline.
- Markers by type with status rings; clustering when zoomed out.
- **Filter chips** row (top). **Search / locate-me** button.
- **Coral FAB "＋"** → Add marker sheet.
- Tapping a nest pin → mini card peek (thumb, code, "Day 38 / ~55", status) → expand to detail.
- **Coverage layer:** today's patrol tracks as faded lines + a per-beach/zone chip
  ("✓ Patrolled 07:10 · Mert · 2.3 km") so late arrivals don't re-walk a covered stretch.
- **Start patrol** control: begins recording the GPS track (km + path) for the current walk.
- Bottom nav. Offline banner when no connection (data still works).

### 4.3 Add marker (bottom sheet)
- **Type selector** first: Nest 🥚 · Landmark 📍 · Trash 🗑 (extensible).
- **Photo:** built-in **camera** *or* **gallery upload**. On import the app reads the photo's
  **EXIF GPS** — if present it auto-places the marker at that point.
- **Location, 3 ways:** (1) from photo EXIF, (2) device GPS, (3) **manual** — drop/drag the pin on
  the map (long-press). A small badge shows the source ("from photo" / "GPS ±5 m" / "placed by hand").
- **Nest vs false crawl** (guided, because it's the #1 field error): after choosing Nest, a clear
  two-option step — "🥚 Nest (eggs laid)" or "🌀 False crawl (came ashore, no nest)". False crawls
  are still saved (valuable data), just without incubation/excavation.
- **Confirmed vs unconfirmed:** a nest with a photo **and** a real location (EXIF/GPS) is
  **Confirmed**. Without them it saves as **Unconfirmed** — a faded/dashed pin with a "needs
  verification" chip that anyone can complete later by adding the photo/pin.
- **If Nest**, reveal extra fields: species (default Caretta caretta), estimated clutch size,
  cage installed, and a quick **exposure tag** (☀️ full sun / ⛅ partial / 🌴 shade + vegetation).
  Air temperature & recent rain are pulled **automatically from a weather API** by the nest's
  coordinates — **no sand probe or logger needed** in V1 (buried loggers are an optional research
  add-on later). All optional to keep dawn-patrol logging ≤ 20 s.
- Other fields: date/time found (defaults now), note, **Public/Private** toggle. One big coral **Save**.

### 4.4 Nest detail — the story of one nest
- Hero photo + nest code + beach + status pill.
- **Countdown ring**: "Day 38 of ~55 · hatch window Aug 2–9".
- **Predicted sex ratio** shown as a **range** (e.g. ♀ 70–85%), never a false-precise number, with a
  "predicted · regional model" tag + info sheet (TSD, pivotal ~29 °C, calibrated to the nearest
  regional data — **Anamur 28.9 °C**, not Florida; the thermosensitive-period estimate is approximate).
- **Conditions strip**: exposure tag + auto weather (air temp, recent rain) for the nest; a sand-temp
  sparkline appears only if research loggers are attached (V2).
- **Unconfirmed banner** (if applicable): "Needs a photo / precise location — tap to confirm."
- **Timeline (updates + comments)** — one unified feed, newest at the entry point. A row can be:
  a **patrol observation** (condition: OK / predated / washed over / disturbed / hatched), a
  **status change** ("Destroyed after the storm" → status *lost*, with a comment), the **hatch
  event**, or a free **comment**. Each row: author, time, optional photo. Two actions at the
  bottom: **"Add update"** (structured) and **"Comment"** (free text) — comments live under the nest.
- **Excavation card**: locked/greyed until a hatch is recorded, then unlocks the count flow.
- Public/Private, **Watch 🔔**, edit, and "who found it" attribution.

### 4.5 Log observation (sheet)
- Quick daily patrol entry: date, condition (OK · predated · washed over · disturbed · hatched),
  optional photo + note, optional temp reading. Two taps for the common "all OK" case.

### 4.6 Excavation count — the payoff
- Triggered after hatch. Glove-friendly **stepper counters**:
  hatched (empty shells) · unhatched eggs · dead-in-nest · **live helped out** (rescued).
- Auto-computes **hatch success %** and updates the nest + the volunteer's impact stat.
- Celebratory confirmation ("47 hatchlings reached the sea 🐢") feeding gamification.

### 4.7 Beaches
- List of beaches (assigned first), each: name, active nests count, hatching-soon count,
  volunteers assigned. Tap → filtered map + beach stats.

### 4.8 Profile / gamification
- Avatar + volunteer name + assigned beaches.
- **Impact stat tile**: "312 hatchlings reached the sea" — the **north-star** number, front and center.
- **Streak** (patrol days) with a "freeze" so a missed day doesn't punish — gentle, not a daily whip.
- **Badges** grid for real events: "First Nest", "First Excavation", "10 Patrols", "Hatchling
  Rescuer", "Guardian of <Beach>". Locked badges show progress.
- **Leaderboard is de-emphasised, opt-in** (research: public rankings demotivate most volunteers in
  small teams). Default view is personal impact; a small "Beach board" is a secondary, friendly tab.
- Settings: language (RU/TR/EN), default public/private, night mode, **red-light mode**.

---

### 4.9 In-app camera (built-in, fast)
- **Custom camera**, not the system picker — open, tap, done, without leaving the app.
- One-tap shutter, auto-attaches GPS + timestamp, optional flash/torch (dawn/dusk), front/back.
- Instant local save (blurhash preview) so it works offline; uploads when back online.
- Reachable from Add-marker, Log-observation, and Excavation. Multi-shot allowed per nest.

### 4.10 Hatch watch (the emotional hook)
- On any nest: a **"Watch this nest 🔔"** toggle. Watchers get gentle notifications:
  "GZP-24's hatch window opens in 3 days" and the payoff "GZP-24 hatched — 47 reached the sea!".
- A **"Hatching soon"** home strip lists nests entering their window — the anticipation people
  are already feeling ("waiting for the hatch, saving lives") made visible and shareable.
- (Push needs auth → V2; V1 shows the strip + local reminders.)

### 4.11 Learn — facts & guide
- **Facts:** a rotating "Did you know?" card on the map/home + a swipeable facts feed
  (turtle biology, TSD, why lights matter). Light, delightful, shareable.
- **Guide:** article list sourced from **carettafriends.com** — "What to do if you find a nest",
  "Don't touch / don't use flash", patrol protocol — each links back to the site.
- Prominent **links**: official website, Instagram, and the community WhatsApp chat.

### 4.12 Community (Gazipaşa & beyond)
- Community home: cover, mission line, **Join** button (admin approves the link), and the
  **WhatsApp community chat** entry point.
- **Roles visible:** each beach/zone shows its **leader (ответственный за участок)**; admins and
  beach-leaders can assign volunteers. Volunteers see who leads their beach and how to reach them.
- Multi-community ready: a volunteer picks their community/city on onboarding; the map, beaches,
  and leaderboard scope to it.

### 4.13 Patrol & coverage
- **Start / stop patrol:** records a GPS track for the walk — distance (km), path, duration, which
  beach/zone. Runs in the background so the phone can stay pocketed; battery-light.
- **Coverage map:** the day's tracks overlay the map; each beach/zone shows who covered it and when.
  The point is **coordination, not competition** — avoid two people walking the same stretch, and
  reveal gaps no one has checked today.
- **Personal patrol stats** (Profile): kilometres walked, patrol days, this-season totals. Framed as
  personal effort/impact, not a public ranking.

> **Navigation update:** V1 nav grows to **Map · Beaches · Learn · Profile** (Community links and
> Hatch-watch live inside Profile/Beaches to keep the bar to 4 tabs). Camera is a modal, not a tab.

## 5. Field-usability constraints (non-negotiable)

- **Sunlight:** high-contrast text, avoid thin light-grey on white; large type.
- **Offline-first:** every screen works with no signal; a subtle sync indicator, never a blocker.
- **One-handed / gloves:** primary actions bottom third, ≥48dp targets, generous spacing.
- **Battery/night:** true dark "night patrol" theme for pre-dawn use; minimal bright flashes.
- **Red-light mode:** a deep-red, low-brightness night skin for hatch watches & excavations —
  preserves dark-adapted vision and avoids disorienting hatchlings with white light. One-tap toggle.
- **Speed:** logging a found nest ≤ 20 seconds (photo + GPS + save; rest optional).

---

## 6. Accessibility & localization

- WCAG AA contrast on both themes; semantic color always paired with an icon/label
  (never color alone).
- Dynamic type / font scaling supported.
- Full RU / TR / EN strings (Compose Resources); no hard-coded text; date/number localization.
- VoiceOver/TalkBack labels on all interactive elements and map markers.

---

## 7. Handoff notes for the designer

- Deliver: Onboarding, Map, Add-marker, Nest detail, Log observation, Excavation, Beaches,
  Profile — each in **light + dark**, plus the empty states and the hatch-celebration moment.
- Build a small design-token sheet (the palette + type scale above) so it maps 1:1 to the
  Compose `AppTheme` (`Colors`, `Typography`, `Shapes`).
- Mascot: one friendly loggerhead ("Caretta") in 3-4 poses (wave/hello, waiting/egg,
  celebrating/hatch, empty-state). Flat, rounded, 2-3 palette colors.
- Keep the identity distinct from generic "eco-green" apps: the signature is **sand + teal +
  coral sunset**, rounded and warm, with the countdown ring and the sex-ratio bar as the two
  memorable, science-carrying components.

### Tech & evidence notes (for engineering)
- **Map:** MapLibre Compose (`org.maplibre.compose`) — the official KMP-ready path; offline regions
  via the native SDK. (Ramani Maps is Android-only — do not use.)
- **Export:** ship a FWC / seaturtle.org-compatible CSV/JSON export in V1 — it's a cheap feature
  that gives the data scientific credibility and reuse (see `docs/data-model.md` → Export).
- **Why these choices:** the leaderboard de-emphasis, the nest/false-crawl guided flow, offline as a
  hard requirement, red-light mode, and the hatch countdown are all grounded in field evidence —
  full sourcing (105 refs) in `docs/research.md`. Verdict: **GO**.
