---
type: research
status: draft
title: "Deep Research — Caretta Gazipaşa Sea Turtle Nest Monitoring App"
created: 2026-07-21
tags: [caretta-gazipasa, research, competitive-analysis, conservation, mobile, sea-turtles]
product_type: mobile
---

# Deep Research: Caretta Gazipaşa Sea Turtle Nest Monitoring App

**Client:** Caretta Gazipaşa (carettafriends.com) — an existing volunteer NGO in Gazipaşa/Antalya, Turkey monitoring loggerhead (*Caretta caretta*) nests on Koru, Selinus, and Kahyalar beaches (~150–200 nests/season), installing protective cages, and running beach cleanups/education.

**Note on scope:** This is a nonprofit conservation tool, not a revenue product — no naming/domain work (brand already exists), no company registration, no TAM/SAM/SOM dollar sizing. Instead, Section 8 sizes the opportunity as a **reach/impact estimate**: how many similar volunteer orgs worldwide could reuse this app.

## Executive Summary

Every existing sea-turtle nest-monitoring tool forces small volunteer orgs into a bad trade-off: heavyweight enterprise GIS (Esri ArcGIS Survey123/Field Maps + $1,000s of survey-grade GNSS hardware, built for state wildlife agencies like Florida FWC), an aging web-only shared database (seaturtle.org, ~20-year-old UX), or a lightweight sightings app (iNaturalist, TurtleSAT) that never models the full nest lifecycle from discovery through incubation to post-hatch excavation. The two organizations most comparable to Caretta Gazipaşa in size, region, and species — ARCHELON (Greece) and DEKAMER (Turkey) — show no public evidence of any digital nest-data tool at all, and Caretta Gazipaşa's own materials confirm the same: this is a paper/manual-only operation today. The scientific data schema itself, however, is well-standardized (Florida FWC's Marine Turtle Conservation Handbook + seaturtle.org network), so a new app can be both dramatically simpler than ArcGIS and export-compatible with the global data standard. Field research (a 2017 Akumal, Mexico pilot) found paper datasheets carry an **8x higher error rate** than a purpose-built digital tool (46.7% vs 5.8% of records with errors) — the underlying case for going digital at all is not hypothetical, it's measured.

**Verdict: GO.**

## 1. Competitive Landscape

| Tool | Platform | Cost | Primary Users | Key Features | Why It's a Poor Fit for Small Volunteer Orgs |
|---|---|---|---|---|---|
| **ArcGIS Survey123 / Field Maps** (Esri) + Eos Arrow/Trimble GNSS | iOS/Android + ArcGIS Online backend; requires paired survey-grade GNSS receiver | Field Worker license ≈ $350/user/year; nonprofit discounts exist but need approval; GNSS hardware $1,000–$7,000+ | Florida FWC's Statewide/Index Nesting Beach Survey (SNBS/INBS, fully digital since 2020), 12+ FL State Parks, Hilton Head Island Sea Turtle Patrol | Custom forms, offline sync, org-wide GIS dashboards | Built for GIS professionals/state agencies, not volunteers; needs an ArcGIS Online org to administer; per-seat pricing scales badly for 10–30 volunteers; needs expensive external GPS hardware for useful accuracy (consumer phone GPS was "roughly six feet off" per one field report — see §2); steep learning curve; zero gamification, generic enterprise UI |
| **SEATURTLE.ORG Nest Monitoring System** (`seaturtle.org/nestdb`) | Web-only portal, no native mobile app | Free / by-request onboarding via `nestdb@seaturtle.org` | De facto US regional data-sharing standard: Florida, Georgia, North/South Carolina; historically Costa Rica, Greece, Syria | Standardized cross-org schema for regional comparison; also runs STAT (satellite tracking) and STRAND (necropsy DB) | Web-only, no offline field capture; ~20-year-old UX with little visible recent investment; functions as an after-the-fact reporting backend, not a day-to-day patrol tool; zero gamification; manual staff approval to onboard |
| **Turtle Nest Tracker** (turtlenesttracker.org) | Planned mobile app | Unknown | None yet | Aspirational nest-tracking app | Still pre-launch, actively recruiting help to "get the app into production" — no shipped product; validates that even a from-scratch attempt hasn't reached traction |
| **STC Turtle Tracker** (Sea Turtle Conservancy) | iOS + Android, free | Free | General public / education | Maps of individually satellite-tagged adult turtles' migrations | **Not a nest-patrol data-entry tool at all** — passive public-education viewer for existing satellite telemetry. No volunteer data entry, no nest logging. Name is misleading for this use case |
| **TurtleSAT** ("1 Million Turtles", Australia) | iOS + Android + web map, free | Free | Australian citizen scientists, **freshwater** turtles | Crowd-sourced sightings/roadkill map, predation hotspots, AI "Turtle Nest Predictor"; 1,000+ turtles saved, 200+ nests protected (2022) | Wrong species/workflow (freshwater roadkill & fox-predation reporting, not marine incubation monitoring) — but a genuinely valuable **UX/gamification reference**, not adoptable as-is |
| **We Spot Turtles** (wespotturtles.org, 2023) | iOS + Android, free | Free | Public sightings + partner NGOs/govt | Geolocated photo sightings, species ID help, org dashboard for partners | Closest analog found, but still a **sightings** app — no clutch size, no relocation tracking, no incubation countdown, no excavation/hatch-success workflow |
| **TurtleOps** | iOS (App Store) | Unknown | Field teams (tag-encounter focus) | Dark red-light night UI, offline drafts that sync when coverage returns, team check-in/"Beach Chat" | Validates that dark-mode/offline are already recognized as must-haves industry-wide, but public description centers on tag encounters, not clearly on excavation/hatch-success calculation — a plausible feature gap |
| **Zooniverse** | Web (image classification) | Free | Varies by project | Crowd classification of camera-trap/drone imagery | No active dedicated sea-turtle nest classification project found — a genuine unused opportunity, not a competitor |
| **iNaturalist** (Sea Turtle Spotter/Observatory) | iOS + Android + web | Free | Ocean-goers reporting live sightings | Species-verified "research grade" sightings | Live-sighting tool, not nest-patrol/incubation data — different use case |
| **ARCHELON** (Greece) | Manual/paper — field notebooks, no digital tool found | N/A | 75–110 km of Greek coastline, systematic monitoring since 1984, largest Mediterranean loggerhead network | Morning/night surveys, tagging, nest marking/relocation, excavation | **No evidence of any primary digital field-data tool.** Same species, same region, comparable scale to Caretta Gazipaşa |
| **DEKAMER** (Dalyan, Muğla, Turkey) | Manual/paper — volunteer rangers | N/A | International volunteers, rescue/rehab + nest patrol | Nest protection, rehab, satellite-tag releases | **No publicly documented digital nest-data tool.** Same conclusion as ARCHELON — the exact peer-org category Caretta Gazipaşa belongs to |
| **MEDASSET** | Advocacy/assessment reports since 1988 | N/A | Turkey (Patara, Fethiye) & Greece beach assessments | Publishes survey reports, campaigns for protection | Not a field data-collection tool; aggregates/advocates using others' data |

### Gap Analysis

No existing tool combines: (1) a purpose-built mobile-first workflow for the entire nest lifecycle — discovery → marking → relocation decision → incubation countdown → excavation/hatch-success scoring; (2) free-or-cheap with zero GIS expertise or per-seat licensing required; (3) offline-first capture using free OpenStreetMap tiles instead of $1,000+ survey-grade GNSS hardware; (4) volunteer-facing gamification tuned for small unpaid teams (not competitive leaderboards — see §4); and (5) a data model export-compatible with the FWC/seaturtle.org scientific standard. Today, orgs like Caretta Gazipaşa, ARCHELON, and DEKAMER are stuck choosing between enterprise GIS built for state agencies, or plain paper. That is the white space.

## 2. Volunteer Field Workflow & Pain Points

### 2.1 The Real Protocol

**Dawn patrol.** Volunteers walk (or slow-drive an ATV, daylight only) the most recent high-tide line at first light looking for fresh crawls left overnight; nesting season runs roughly May–October in the Mediterranean/Florida. Critical checks (relocations, clearing driving lanes) typically must finish by 8–9am. Some night-nesting sites (Cabo Verde) run night patrols instead, specifically to deter poachers.

Per crawl: identify incoming vs. outgoing track (shorter/underlying = incoming); identify species by track width and gait — loggerhead 70–124cm (mean 94cm, alternating gait, no tail-drag), green 95–144cm (simultaneous gait, tail-drag), leatherback 175–214cm (often S-shaped); classify **nest vs. false crawl**; record; then erase the track above the tideline (never over the clutch) to avoid double-counting tomorrow.

**Nest vs. false crawl** is the single most error-prone judgment call in the whole protocol — made under real time pressure, from visual evidence only, never by digging. FWC's own handbook warns "the visible evidence... will deteriorate, and there is a danger the crawl might be erased before a decision is made." Criteria: a nest shows a covering escarpment and sand thrown/misted over the emerging track; a false crawl shows little disturbance beyond tracks, or a "backstop" of pushed (not thrown) sand.

**Marking & caging.** Two distinct goals: protective marking (4 stakes ~36" tall, ribbon, warning sign, nest-ID) for traffic/construction beaches; and precision marking for hatch-success tracking via triangulation — two dune stakes aligned toward the clutch with measured distances, plus a buried, metal-detector-locatable aluminum marker as insurance. GPS coordinates are explicitly recommended for every nest. Predator cages (wire mesh) are then installed against foxes, dogs, raccoons, ghost crabs — field trials found unprotected nests suffered 33–63% predation vs. 0% for properly caged nests (though mesh alone was breached ~25% of the time by determined foxes).

**Incubation-period monitoring** (~45–75 days, species/region-dependent; ~55–60 days commonly cited for Mediterranean loggerheads/Gazipaşa). Nests are checked ideally every morning, minimum every other day, for: predation (and predator type), wash-over/erosion/storm inundation (nests can survive one wash-over but prolonged submersion kills embryos), vandalism/poaching, and marker/stake longevity (lost stakes = a nest that literally can't be found again — a cited data-quality risk). Partial depredation means removing/counting destroyed eggs, re-covering survivors, and continuing to monitor them — you never swap in a "cleaner" nest, or you bias the season's hatch-success statistics.

**Estimating the hatch window.** Lay date + expected incubation duration gives an estimated window, written directly on the nest stake at some US programs as two flanking dates. Once the window opens, the nest gets flagged for daily inspection.

**Emergence/hatching event.** Hatchlings emerge at night, crawling toward the brightest, lowest horizon (historically the moonlit sea). Artificial light pollution is a leading cause of hatchling mortality — streetlights, building lights, even flashlights misdirect hatchlings inland toward predators/roads. Florida alone estimates 100,000+ disorientations/year. If a patrol finds clear misdirection toward a light source, protocol requires notifying the property owner/code enforcement — a separate reporting workflow from the nest record itself. Volunteers who miss the live event usually find fan-shaped hatchling tracks converging on the sea the next morning.

**Nest excavation** (typically 72 hours after first emergence, or 70 days after laying — 80 for leatherbacks — whichever comes first; extended to 80 days/96 hours if the nest was cooled by rain/inundation/shading, which delays hatching). Dig by hand only, down to the chamber. If live hatchlings are found before eggshells, wait 24+ hours and retry. Sort contents into hatched eggshells (>50% intact = 1), live hatchlings, dead hatchlings, live/dead pipped eggs, whole unhatched eggs (further split by embryo development stage), and damaged eggs. Live hatchlings are released. Then calculate:
- **Hatching Success % = Empty Shells ÷ (Hatched + Unhatched Eggs) × 100**
- **Emergence Success % = [Empty Shells − (Live + Dead in-nest)] ÷ (Hatched + Unhatched Eggs) × 100**

(Worked FWC example: 98 hatched shells, 3 live-in-nest, 1 dead-in-nest, 5 unhatched → 95.1% hatching success, 91.3% emergence success.)

ARCHELON and DEKAMER follow essentially the same protocol; DEKAMER additionally opens unhatched eggs to classify embryo development stage and takes genetic samples.

### 2.2 Turkey / Gazipaşa Specifics

Caretta Gazipaşa monitors Koru, Selinus, and Kahyalar beaches, ~150–200 nests/season; incubation locally cited at ~55–60 days. Beach access is legally prohibited after 20:00 during nesting season (with fines), meaning enforcement/reporting of human disturbance is part of the volunteer's job. Volunteers cordon nests with tape and place fundraised protective cages. Nearby DEKAMER (Dalyan/Muğla) runs the same protocol at Iztuzu Beach. Fethiye and Patara — other major Turkish loggerhead sites — remain flagged by MEDASSET/the Bern Convention as under threat from unmanaged tourism, light pollution, and dune erosion (Fethiye's case still "Open File" as of the 2025 Standing Committee review) — meaning consistent, exportable, timestamped digital nest records could carry secondary value as evidence for habitat-protection advocacy. Critically: **no data-collection app or tool of any kind is mentioned anywhere in Caretta Gazipaşa's own materials**, confirming this is a genuine paper/manual-only operation today.

### 2.3 Volunteer & Organizational Pain Points

| Pain Point | Source | Sentiment |
|---|---|---|
| Paper forms: **46.7%** of field records contained an error (31.4% missing GPS alone) vs. **5.8%** with a tablet app — real Akumal, Mexico pilot | Scientific Reports 2017 | Strong negative (quantified) |
| Paper→spreadsheet transcription of a season's data took "several weeks"; standardizing the paper database took 3 working days | same study | Negative — double-work burden |
| Digital-form users recorded **more than double** the data per record (18.3 vs 8.6 fields); paper users rarely collected weather data at all | same study | Negative for paper |
| Cheap GPS was "roughly six feet off," making relocating a storm-buried marker "unhelpful" — a buried nest + temperature logger nearly lost | Eos GNSS case study, Sea Turtle Patrol Hilton Head Island | Negative — accuracy risks losing physical assets/data |
| **20% of permit-holder supervisors** named inaccurate/incomplete data (missed/unmarked nests) as their #1 concern with volunteers — mainly false-crawl misidentification, not lack of biology knowledge | Bradford & Israel (2004), EDIS/UF-IFAS | Negative — direct case for in-app guided decision support |
| **11%** cited finding/retaining committed volunteers for the full season as a top concern | same study | Negative — turnover problem |
| Volunteer motivation is overwhelmingly "concern for turtles" (mean 6.45/7) — far above career or social motives | same study | Positive baseline, but means bureaucratic/corporate-feeling tools undercut the core motivator |
| Purpose-built apps (RASTR/TURT, Loma Linda Univ.) exist specifically because paper "degrades" in field conditions and researchers need offline capture with no wifi/satellite access | Loma Linda Univ. press release | Negative (paper) / validates offline-first demand |
| A direct competitor, TurtleOps, ships a dark red-light night UI and offline drafts that sync when coverage returns — evidence the industry already recognizes these needs | TurtleOps App Store listing | Validating |
| Red light is specifically preferred over white in the field — less disturbing to turtles, preserves human night vision | NPS Gulf Islands "Turtle T.H.i.S." program | Confirms red-light/dark-mode is a hard field requirement |
| Physical/psychological toll of night patrols: sweat, blisters, sand-flea bites, fear on isolated sections, long down-time between walks | Duke Sea Turtle Ecology blog, first-person account | Negative — physically demanding, not "just a beach walk" |
| Generic offline form tools (KoboToolbox, ODK) exist and are free for nonprofits, but have **no false-crawl decision tree, no hatch-success auto-calculation, no track-width species reference** | M&E tooling comparison | Neutral — shows domain-specific logic is the real differentiator, not just "an offline form app" |

### 2.4 Synthesis — What This Means for the App

The false-crawl/nest decision is the single highest-value place to intervene: it's made under time pressure from visual pattern-matching, it's the #1 cited data-quality failure, and the official criteria translate cleanly into a structured, photo-illustrated decision tree — something paper can't do. Offline-first is table stakes across every serious tool in this space, not a differentiator. GPS accuracy has a real, cited failure mode (six feet off, lost equipment), so a triangulation-style manual backup matching the official FWC method is worth a first-class feature, not an afterthought. Red-light/dark mode at night is expected, not a differentiator — but genuinely good one-handed, wet-screen-tolerant ergonomics is less commonly nailed by anyone. Volunteers are mission-driven, not gamification-driven — the tool must visibly connect data entry to conservation outcomes, not feel like corporate reporting software. Nest excavation / hatch-success calculation appears to be an underserved feature even among the few dedicated apps that exist (e.g., TurtleOps skews toward tag encounters) — a plausible distinctive gap for this app to fill.

## 3. Canonical Nest-Monitoring Data Schema

Sourced primarily from the **Florida FWC Marine Turtle Conservation Handbook (2016)**, Section 2 "Nesting Beach Survey Activities" and Appendix A "Nesting Forms" — the most detailed publicly available field-level schema found. Cross-validated against the NPS Cape Lookout National Seashore 2021 Annual Report, which reports 30+ years of data in this exact schema and explicitly feeds it to **seaturtle.org**, confirming the two systems are compatible in practice.

**Identification** — Nest ID (sequential per season); species code (`Cc` loggerhead, `Cm` green, `Dc` leatherback, `Lk` Kemp's ridley, `Lo` olive ridley, `Ei` hawksbill, `Un`/`O` unidentified); beach name/zone/county/landmark; **GPS in decimal degrees, WGS84 datum** (the standard our schema should match); date/time found; nest vs. false-crawl classification; incoming/outgoing track direction; species-from-track-width identification.

**Nest details** — clutch size/egg count (at relocation or excavation, mean ≈118 eggs per FWC 2021 data); relocation (yes/no + reason + new GPS + new chamber depth/shape — standard depth: loggerhead 60cm, green/leatherback 80cm, spherical bottom); nest-marking method (perimeter stakes, triangulation to two reference-stake pairs, optional buried metal-detector marker); screening/caging status (**none** / **self-releasing** screen-or-cage that lets hatchlings escape unaided / **restraining** cage that requires twice-nightly checks / **hatchery** — a fenced area where many nests are reburied); Obstructed Nesting Attempt codes (beach furniture, dune crossover, escarpment, marine debris, seawall, etc.).

**Incubation** — incubation start date (= lay date); incubation duration (typically 45–70+ days; FWC statewide mean 55–67 days year to year); inventory-eligibility rule (72 hours after first emergence **or** 70 days after laying — 80 for leatherbacks — whichever first; extended to 80 days/96 hours if cooled by rain/inundation/shading); late-season nest flag (laid after Aug 31, may incubate past Oct 31); recommended monitoring frequency: daily, minimum every other day.

**Monitoring events log** — date, observer; disturbance/threat type (predation + predator code, wash-over/inundation/storm, vandalism/poaching, eggs scattered by another nesting turtle); partial vs. total depredation (count eggs lost, rebury survivors, continue monitoring — never swap in a replacement nest); hatchling/adult disorientation events (species, event type, nest treatment in place, survey type, # disoriented bucketed 1/2–10/11–50/>50, # reaching water, # found dead, light-source category checklist); free-text notes.

**Emergence/hatching event** — date of first emergence; hatchling emergence evidence/count; false-crawl reclassification rule (if hatching evidence later appears on a crawl logged as false, add to annual total-nest count but keep as false-crawl for standardized index-beach surveys, to preserve cross-observer protocol consistency).

**Excavation (post-hatch nest inventory)** — the core scientific schema, dig by hand, sort into piles:

| Field | Definition |
|---|---|
| Hatched eggs (empty shells) | Each fragment >50% complete = 1 |
| Live hatchlings in nest | Found alive, released |
| Dead hatchlings in nest | Found dead |
| Live pipped eggs | Shell broken through, hatchling alive but not free |
| Dead pipped eggs | Same, hatchling dead |
| Unhatched eggs — total | Split: no visible embryo / partially developed / fully developed (dead) |
| Damaged eggs | e.g. destroyed by predation during incubation |
| Total eggs | = hatched + unhatched |

**Success formulas:** Hatching Success % = Empty Shells ÷ Total Eggs × 100. Emergence Success % = [Empty Shells − (Live + Dead in-nest)] ÷ Total Eggs × 100. Total Emerged (unaided) = Empty Shells − (Live + Dead in-nest).

**Predator/threat codes** — raccoon, armadillo, coyote, fox, feral hog, domestic dog, cat, bird, ghost crab, fire ants (each with species-specific removal guidance in the Handbook).

**On seaturtle.org's own schema:** direct field-by-field documentation of `seaturtle.org/nestdb` wasn't publicly accessible (new orgs onboard by contacting `nestdb@seaturtle.org`), but aggregated descriptions confirm it tracks the same core categories (nests in-situ vs. relocated, incubation status, false crawls, estimated eggs, eggs lost, mean clutch count). Since FWC data and Cape Lookout data both flow into seaturtle.org, building to the FWC field set above should also satisfy seaturtle.org compatibility.

## 4. Gamification / Engagement Patterns

### What the evidence shows, precedent by precedent

- **eBird** — a prominent 365-day "Checklist-a-day Challenge" streak; personal life/year/state lists (pure self-referential progress, no comparison needed); rare-bird alerts shown in a peer-reviewed study to measurably increase checklist volume for ~10 years running; an explicit "Top 100" leaderboard framed as social comparison.
- **iNaturalist** — "Research Grade" status once 2/3 of identifiers agree (visible completion feedback loop); Seek companion app adds points/levels/badges; peer-reviewed research found "improving species knowledge" is the single strongest motivator across all participation-intensity tiers, stronger than social or competitive factors; only 7% of users both observe and identify, so iNaturalist is piloting interventions to get identifiers engaging new users faster for retention.
- **Zooniverse** — "Talk" discussion boards (~40% of volunteers use them; direct researcher participation makes volunteers "feel heard"); contribution counters used to celebrate collective **group goals**, not individual ranking; Gravity Spy raised classification accuracy from 54% to 90% by reducing volunteer friction, improving retention alongside it (friction-reduction is itself a retention lever, not just gamification); most citizen-science volunteers participate only once — sustained-initiative design is what separates high- from low-retention projects.
- **Duolingo** (UX reference) — the single strongest documented mechanic in this whole research: 7+ day streaks correlate with 2.4x retention; next-day retention lifted from 12% to 55%; churn fell from 47% to 28% in major markets; onboarding commits users to day-1 of a streak before the signup screen; "streak freezes" (grace tokens) preserve the loss-aversion core loop while reducing anxiety — directly relevant to a seasonal, weather-dependent activity like turtle patrols; shareable milestone cards drove a 5-10x increase in organic sharing (6M+ daily streak shares).
- **Turtle-specific & beach-cleanup apps** — TurtleSAT (~1,600 volunteers, ~18,000 sightings since 2014, credited with saving 1,000+ turtles and 200+ nests in 2022 alone) uses **no explicit gamification** — engagement comes from tangible "your data saved X turtles" outcome messaging, not game mechanics. Litterati received an NSF grant specifically because it lacked gamification and needed to add it. Clean Swell (Ocean Conservancy) is the closest existing analog: personal impact stats (total weight collected, distance cleaned, historical record), badges, social sharing — explicitly framed as "track your individual impact over time."
- **The important caveat** — a direct academic comparison of Foldit (gamified, competitive scoring) vs. Galaxy Zoo (non-gamified) found leaderboards were **explicitly rejected** by the Galaxy Zoo community ("why complicate matters... try to make it something that it is not?"), while Foldit's competitive scoring created "jealous guarding" of techniques and made players question whether high scores actually helped science. Crucially: **leaderboards motivate top performers and demotivate the bottom of the distribution** — a real risk for a small NGO with only a handful of active volunteers. The broad academic consensus: game elements aren't necessary to *attract* volunteers, but do help *sustain* long-term engagement through social interaction and recognition — gamification is a retention tool, not an acquisition tool.

### 7 Concrete Mechanics Recommended

1. **Personal impact stats as the primary framing, not leaderboards** — "X hatchlings you helped reach the sea," nests found, patrols walked, km surveyed. Strongest evidence base (Clean Swell, eBird lists, iNaturalist counts); avoids the demotivation risk a small volunteer pool faces from competitive ranking.
2. **Patrol streaks with a "freeze" mechanic**, not a rigid daily streak — model as "consecutive scheduled patrols attended" with grace periods for weather/tide-dependent scheduling, borrowing Duolingo's loss-aversion structure without its unrealistic daily cadence.
3. **Milestone badges tied to real conservation events**, not arbitrary point thresholds — first nest found, first excavation, 10th patrol, first hatchling release witnessed.
4. **Hatch-countdown timer per adopted/found nest** — a personal, non-competitive "your nest hatches in N days" mechanic. Confirmed gap: no existing turtle app in this research has this feature.
5. **Photo journal per nest** — combines iNaturalist's observation-journal pattern with Zooniverse's personal-stats page; gives volunteers a private narrative of "their" nest's lifecycle.
6. **Team/squad framing over individual competition** — model after Zooniverse's collective group goals rather than Litterati/Duolingo-style individual leaderboards, given the small-community leaderboard-demotivation risk documented above.
7. **Shareable seasonal recap / milestone cards** — end-of-season "your season in review," Instagram/WhatsApp-shareable, modeled on Duolingo's share-card mechanic (5–10x organic sharing lift) — valuable for a small NGO needing organic volunteer recruitment.

**Explicitly avoid:** a public, all-time individual leaderboard as the primary engagement device — the strongest evidence in this research (Foldit/Galaxy Zoo) shows this discourages the majority of participants and can shift focus from mission to metric-gaming. Use only optionally/locally (e.g. seasonal team totals) if at all, never as the headline feature.

## 5. Tech Feasibility

**Stack is already decided** — Kotlin Multiplatform + Compose Multiplatform, MapLibre + OpenStreetMap, Supabase (Postgres + PostGIS, Storage, Auth), offline-first via SQLDelight, V1 ships with no authentication.

**Library landscape:**

| Library | Coverage | KMP path | Status |
|---|---|---|---|
| MapLibre Native (`org.maplibre.gl`, C++14) | Android, iOS, macOS | Underlying native SDK, not a Kotlin wrapper itself | Active, BSD-2-Clause fork of pre-license-change Mapbox GL Native |
| **MapLibre Compose** (`org.maplibre.compose:maplibre-compose`, official MapLibre org) | Android ~90%, iOS ~90%, Web/Desktop unfinished | **Yes — the actual Compose Multiplatform wrapper to use** | Active, v0.13.0, 659 commits, 518 stars |
| Ramani Maps | **Android only** despite the name | No — open issue #77 unresolved | Active but not a viable KMP path today |

**Recommendation:** `org.maplibre.compose:maplibre-compose` is the right choice — the only actively-maintained, official-org option with genuine iOS + Android Compose Multiplatform coverage.

**Feature confirmation:** Offline tile/region caching is a mature, long-standing capability of the underlying MapLibre Native SDK (`OfflineManager`/`OfflineRegion` API — region defined by style URL + geometry + zoom range, downloaded with progress callbacks) — not something the Compose wrapper needs to reinvent. Custom markers per type (nest/landmark/trash/cage) work via `SymbolLayer` + `GeoJsonSource` + `iconImage`, a declarative pattern (a small learning curve vs. imperative marker APIs from Mapbox/Google Maps, not a blocker). GPS/location puck + orientation shipped in v0.12–0.13, sufficient for patrol-track logging and "you are here" display. **KMP gotchas:** cinterop bridging on iOS means some `expect`/`actual` boilerplate at the edges; Web/Desktop support is unfinished but irrelevant for a mobile-only V1; license is clean BSD-2-Clause with no legal risk; budget contingency time as this is a maturing OSS project (82 open issues), not a battle-hardened commercial SDK.

**Supabase PostGIS** — well-established, low-risk: `CREATE INDEX ... USING GIST (location)` for a spatial index, `ST_DWithin(location::geography, ST_MakePoint(lon, lat)::geography, radius_meters)` for proximity queries ("nests within 500m"), or `<->` for nearest-neighbor sorting. First-party Supabase extension with official docs.

**SQLDelight** — confirmed as the de-facto standard for KMP offline/local-cache persistence, generating type-safe Kotlin APIs from SQL with drivers for Android, iOS, JVM, desktop. Officially documented in Kotlin Multiplatform docs as the accepted offline-mode approach.

## 6. Nest Temperature & Temperature-Dependent Sex Determination (TSD)

Client-requested area — scientifically important and, per this research, currently not addressed by any competitor studied in §1.

### 6.1 The science: pivotal temperature, transitional range, thresholds, directionality

Loggerhead sex is set by sand temperature at nest depth during roughly the middle third of incubation. The **pivotal temperature** (the constant temperature producing a 50:50 sex ratio) clusters around 29°C worldwide but is population-specific — every Mediterranean site measured has its own value: Kyparissia Bay, Greece 29.3°C; Patara, Turkey ~29.2°C; Fethiye, Turkey ~29°C; Georgia, USA 29.3°C; Northwest Atlantic loggerheads generally 28.94°C. Most relevant for Gazipaşa: **Anamur Beach, Mersin** — the closest published population, ~60-100km east along the same unbroken Turkish coastline — measured **28.9°C**, with 72-79% female sex ratios. This is a far better regional default than Florida/Georgia figures, with Kyparissia (29.3°C) as the next-best Mediterranean-wide fallback.

The **Transitional Range of Temperatures (TRT)** — the band producing a mix of both sexes — is unusually narrow for loggerheads: Georgia's is ~28.7-30.1°C (~1.4°C wide), and the literature notes TRTs "as narrow as 1°C, making offspring sex ratios highly sensitive to small temperature changes." Treat the mixed-sex zone as roughly **28-30.5°C**, with essentially all-male broods below ~28°C and all-female above ~31°C at Mediterranean sites.

**Upper lethal threshold** is commonly cited at **33-35°C**, with mortality rising sharply above this and continuous 34-36°C incubation producing no hatchlings in experimental trials; mortality depends on *cumulative* exposure, not just a single peak, and late-stage embryos tolerate brief spikes better than early-stage ones. The **lower threshold** is far less sharply quantified in the literature — the commonly-cited viable range for sea turtle embryos generally is ~25-35°C, with no single widely-cited hard floor analogous to the upper ceiling.

**Directionality is confirmed:** warmer sand → shorter incubation + more females; cooler sand → longer incubation + more males (Yntema & Mrosovsky's foundational critical-period work). Quantified rate: **~8.5 fewer incubation days per +1°C** of constant incubation temperature. Measured Mediterranean sex ratios reflect this region's warmth: Alagadi, N. Cyprus 89-99% female; Patara, Turkey 86-96% female; Anamur, Turkey 72-79% female; Fethiye, Turkey 60-65% female (a regional outlier, relatively balanced); Turkey-wide synthesis mean 81.6% female. Important scientific caveat: a 2022 FAU study found the actual thermosensitive window does **not** line up neatly with "the middle third of incubation" — the simplifying assumption every study above uses — and that this approximation "substantially decreases the accuracy of sex ratio estimates." Any in-app prediction should be presented with this caveat in mind, not as false precision.

### 6.2 How field programs actually measure it

**Buried data loggers** are the standard research-grade method: **Onset HOBO Pendant/MX** (~$75-90/unit, Bluetooth-readable, factory waterproof, multi-year battery) and **Maxim/Dallas iButton thermochrons** (~$45-110/unit, need an added waterproof capsule for burial) are the two most-cited logger families in published studies. Standard protocol: bury one logger at mid-clutch depth as eggs are laid/relocated, often with a control logger nearby in egg-free sand to isolate the clutch's own metabolic heating; hourly sampling is most common. Loggers are typically tethered to the existing nest-marker stake system for retrieval at excavation. A DIY open-hardware alternative (Hackaday project, ~€5/unit in a batch of 50, ~0.1°C accuracy, 180-day battery life) exists but is unvalidated hobbyist hardware, viable only with a university partner willing to build/calibrate units. Logger accuracy matters more than expected: a comparison study found HOBO/iButton readings can be off by ~0.4-0.5°C, which alone can swing a sex-ratio estimate near the pivotal point by ±0.1-0.15 in fraction female — a strong argument against presenting predictions as precise single numbers.

**Crucially, none of the three reference organizations treat temperature logging as routine volunteer patrol work.** FWC's standard SNBS/INBS protocols (the ones ordinary permit-holder volunteers follow) do not include temperature monitoring at all — it's a specialized research layer run separately, usually by university teams. ARCHELON's public "What We Do" page describes marking, caging, relocation, shading, and excavation as the routine protocol, with temperature recorders mentioned only in connection with the EU-funded "LIFE ADAPTS" research project, not as default patrol equipment. DEKAMER has no published routine-logger protocol either, though Pamukkale-University-affiliated researchers have published sex-ratio studies at its Dalyan site — again pointing to a research-team activity layered on top of, not embedded in, everyday volunteer duties. **This validates a low-hardware-dependency approach for V1.**

### 6.3 Models usable for a predicted hatch date / predicted sex ratio

For **incubation duration**, a Northwest Florida study (133 nests) built a regression from mean sand temperature near the clutch that predicted incubation length **to within 2.2 days** (2.2→1.9 days when adding egg depth as a second variable) — combined with the general ~8.5-days-per-°C rate, this gives two directly implementable numbers for a "predicted hatch date" feature: a regional baseline duration (~50-63 days depending on season) nudged earlier/later by any available temperature signal.

For **sex ratio**, every published Mediterranean study uses the same applied method: mean sand temperature during the middle third of incubation, run against the local pivotal temperature and TRT. This is implementable, but given the FAU caveat above, an app should show a **range** ("likely 60-85% female, typical for this region") rather than one confident percentage.

**Air temperature and rainfall as proxies for sand temperature** — usable directionally but explicitly flagged as weak substitutes by the literature. A 2026 critique paper is blunt: warming air affects sand heating differently than solar radiation does (the actual driver), and warmer/drier air can even *reduce* thermal conductivity into the sand, so air-temperature proxies can produce "inaccurate or even reverse projections" at the individual-nest scale. Rainfall has a better-quantified, real short-term effect: heavy rain events measurably cool sand/nest temperature by 2-4°C for several days and shift seasons toward more males (multiple Boca Raton, Florida studies, 2010-2013).

### 6.4 What a volunteer can realistically log — and the V1/later split

| Method | Cost | Accuracy | Disturbance risk | Realistic for daily patrol? |
|---|---|---|---|---|
| Buried logger (HOBO/iButton) | $45-110/nest, must be bought + retrieved | Best — incubation duration ±2 days; sex-ratio still has real uncertainty | Low if placed once at lay/relocation | No — upfront cost + retrieval step, exactly why every reference org treats it as a research add-on |
| Hand probe, once/day | Cheap (~$10-20) | Poor — single point-in-time snapshot, misses the full incubation trend | **Real** — inserting a probe near the clutch risks disturbing eggs; no professional program does this | Not recommended — worst of both worlds |
| Shaded air temperature (auto-pulled from a weather API) | Free | Rough, literature-flagged as a weak proxy | None | Yes — fully automatable, zero volunteer effort |
| Qualitative site-exposure tag (full sun / partial shade / full shade; vegetation proximity) | Free | Directional only, but shading is a real, literature-confirmed lever on nest temperature (used as an actual mitigation strategy) | None | Yes — this is exactly what a volunteer already observes at marking time |

**Recommended split:**
- **V1 (no hardware required):** at nest-marking time, capture a simple categorical exposure tag (sun/partial shade/full shade + vegetation proximity + substrate) plus auto-pulled daily air temperature and rainfall (free weather API, e.g. Open-Meteo) for the nest's coordinates and date range. Use this to show a **predicted hatch-date window** (regional baseline nudged by the season's temperature anomaly using the ~8.5-day/°C rule as a rough adjustment, not a precise calculation) and a **qualitative sex-ratio hint framed as a range** with a regional citation (e.g. "Eastern Mediterranean nests trend strongly female, historically 60-85%+; sunnier, more exposed nests skew further female") rather than a single confident percentage — appropriately hedged given that even professional studies using buried loggers carry real estimation uncertainty.
- **V2 / optional enhancement:** let a research-partnered patrol (e.g. if Caretta Gazipaşa collaborates with a university, mirroring how DEKAMER/ARCHELON's temperature work is actually run) attach real buried-logger readings to a specific nest, upgrading that nest's hatch-date estimate to ~2-day accuracy and, over a season or two, letting the org calibrate its **own local pivotal temperature** — turning Gazipaşa into a genuine contributor to the Mediterranean TSD literature rather than just a consumer of Anamur/Kyparissia numbers. Explicitly not recommended for V1: a "log one probe reading per day" workflow — it costs volunteer time and disturbance risk daily while remaining the scientifically weakest of the four methods.

## 7. User Personas

| Persona | Segment | JTBD | Top Pain | Current Solution | Switching Trigger |
|---|---|---|---|---|---|
| **The Dawn Patroller** — student/early-career volunteer, walks the beach at sunrise 3-5x/week in season | Front-line field volunteer, unpaid, "concern for turtles" is their #1 motivator (mean 6.45/7 in academic surveys) | "When I find a fresh crawl at first light, I want to quickly tell if it's a nest or a false crawl and log GPS + a photo in under a minute, so I can keep walking the rest of the beach before the tide erases the evidence." | Misjudging nest vs. false crawl under time pressure (the #1 cited data-quality failure); paper forms illegible/lost in field conditions | Paper datasheet + memory, later handed to a coordinator | An app that makes the nest/false-crawl call *easier and faster* than paper, not harder |
| **The Coordinator** — permit holder / org lead at Caretta Gazipaşa, schedules volunteers and owns data quality/reporting | Organizer, juggles volunteer turnover (cited as a top-3 concern for supervisors) and reporting obligations to local authorities/partner NGOs | "When the season ends, I want one export of every nest's full lifecycle — location, dates, hatch success — in a standard format, so I can report without re-typing weeks of paper sheets by hand." | Retyping/standardizing a season of paper data takes days-to-weeks (per the Akumal, Mexico pilot); inconsistent volunteer data entry; losing nests when physical stakes wash away | Spreadsheet compiled by hand at season's end from volunteer notebooks | A dashboard view + one-click export compatible with the seaturtle.org/FWC schema |
| **The Night Excavator** — experienced volunteer who handles hatching events and nest excavations, often at night | Skilled/senior volunteer, motivated by tangible outcomes ("I want to know my nest did well") | "When I dig up a hatched nest at night, I want the app to walk me through the count categories and calculate hatch/emergence success automatically, so I don't botch the math in the dark and I know instantly how well 'my' nest did." | Doing hatch-success arithmetic by hand at night, by flashlight, after physically exhausting excavation work; white light disturbs turtles and ruins night vision | Manual tally on paper + calculator, done later at home | An in-app guided excavation counter with red-light/dark-mode UI and instant success-percentage feedback |

A short JTBD interview script for validating these personas with real Caretta Gazipaşa volunteers is in `docs/interview-script.md`.

## 8. Reach / Impact Estimate

Not a revenue market — this is a reuse/impact estimate: how many other volunteer orgs, doing essentially the same job, could adopt this app if it's built to be region-agnostic from day one.

- **Global pool:** The State of the World's Sea Turtles (SWOT) directory lists **nearly 500 registered sea-turtle conservation projects worldwide** — the broadest addressable pool of potential adopter organizations across all species and regions.
- **Mediterranean core (loggerhead-specific, same species/protocol as Caretta Gazipaşa):** a MAVA-funded "Conservation of Marine Turtles in the Mediterranean Region" coalition spans **13 countries** (Greece, Turkey, Albania, Algeria, Egypt, Morocco, Libya, Lebanon, Cyprus, Tunisia, Italy, Spain, France) through **9 direct partner organizations** — ARCHELON, DEKAMER, MEDASSET, National Marine Park of Zakynthos, MedPAN, SPA/RAC, WWF Greece, WWF North Africa, WWF Turkey. This is the natural near-term expansion set: same species, same daily-patrol/excavation protocol, same lack of any digital tool found in this research.
- **Immediate beachhead (Turkey + Greece + Cyprus):** ARCHELON alone runs **8 active volunteer project sites** in Greece (Zakynthos, Kyparissia Bay, Lakonikos Bay, Koroni, Rethymno, Chania, Messara Bay, Amvrakikos Gulf); Turkey has DEKAMER (Dalyan/Iztuzu) plus multiple other loggerhead sites (Fethiye, Patara, Antalya coast beyond Gazipaşa); Cyprus has two active NGOs, SPOT (North, ~2,400 nests/year — an all-time record) and MedTRACS (South, est. 2022). Realistic near-term beachhead: **roughly 15-20 distinct beach-monitoring operations** across these three countries alone, most run by small volunteer teams with no digital tool today.
- **Longer-range opportunity (if ever pursued):** the US/Florida network is much larger — **228 beaches surveyed, ~835 miles of coastline, ~3,000 people** involved in the Statewide Nesting Beach Survey citizen-science effort — but ArcGIS/Survey123 is already entrenched there via state-agency mandate, making it a harder, lower-priority displacement target than the Mediterranean, where no digital incumbent exists at all.

**Bottom line:** even the conservative near-term beachhead (Turkey/Greece/Cyprus, ~15-20 orgs) represents meaningful reach for a tool built once for Caretta Gazipaşa and made region-agnostic from day one; the full Mediterranean coalition (9 orgs, 13 countries) and the global SWOT-listed pool (~500 projects) represent the realistic multi-year expansion ceiling.

## 9. Recommendation

**Verdict: GO.**

The gap is real and validated from multiple independent angles: peer organizations (ARCHELON, DEKAMER) run the identical protocol on paper with no digital tool; the closest existing digital options are either enterprise-grade and prohibitively complex/expensive (ArcGIS) or scope-mismatched (sightings apps, satellite-tracking viewers); the canonical scientific data schema is well-documented and gives a clear, achievable export-compatibility target; and quantified field evidence (the Akumal pilot's 46.7% vs 5.8% error rate) proves the underlying paper→digital problem is real, not assumed. Gamification research adds a genuine, evidence-based differentiation angle (personal-impact framing + non-leaderboard mechanics) that no existing turtle-specific tool has executed well. Temperature/TSD (§6) adds a second, scientifically credible differentiator no competitor studied has attempted at all — but should ship as cheap proxy-logging in V1, with the accuracy caveats made explicit, not as a false-precision prediction engine.

### Top Data-Model Fields for V1

Nest ID · species code · GPS (WGS84 decimal) · date/time found · nest vs. false-crawl flag · clutch size · relocation (bool + reason + new coords) · marking/cage status (none/self-releasing/restraining) · incubation start date + estimated hatch window · monitoring log (date, observer, disturbance type) · emergence date · excavation counts (hatched, live/dead hatchlings, live/dead pipped, unhatched by embryo stage, damaged) · hatching success % · emergence success % · photo attachments · free-text notes · **temperature fields** (site-exposure tag: sun/partial-shade/full-shade + vegetation proximity, at marking time; auto-pulled daily air temperature + rainfall for the nest's coordinates/date range; optional buried-logger readings — source flagged as manual/logger — for research-partnered nests).

### Top 5 Features for V1

1. **Offline-first shared map with multi-type markers** (nest / landmark / trash / cage, extensible) — the core value proposition; offline is non-negotiable per field research, confirmed technically feasible via MapLibre's mature OfflineRegion API.
2. **Guided nest-vs-false-crawl entry flow** with photo + GPS capture in under a minute — directly targets the #1 cited volunteer data-quality failure.
3. **Hatch-countdown + guided excavation calculator** (auto hatch/emergence success %) — a genuine, confirmed gap versus every competitor studied, and the piece that makes the data scientifically exportable.
4. **Personal-impact stats + milestone badges + streak-with-freeze** (explicitly not a public leaderboard) — the evidence-based engagement mechanic tuned for a small, mission-driven volunteer pool.
5. **One-click export in FWC/seaturtle.org-compatible schema** — the credibility and portability feature that lets any adopting org plug into the existing global scientific data network without lock-in.

**Temperature/TSD feature — V1 vs. later split (§6.4):** V1 logs cheap, non-invasive proxies only (site-exposure tag + auto-pulled weather data) and shows a hedged, range-based predicted hatch date and sex-ratio hint. A precision prediction engine backed by buried loggers (HOBO/iButton) is a V2+ enhancement, gated on a research partnership — it needs hardware spend, retrieval logistics, and ideally a university collaborator to be worth the accuracy it buys.

## Sources

### Competitive landscape & data schema
1. [Digitally Transforming Field Data Capture to Save Sea Turtles — Esri ArcUser](https://www.esri.com/about/newsroom/arcuser/sea-turtles)
2. [Beach Crawl: Using Field Data Capture to Save Endangered Sea Turtles](https://www.directionsmag.com/article/11003)
3. [Sea Turtle Monitoring (SNBS/INBS) — FWC](https://myfwc.com/research/wildlife/sea-turtles/nesting/monitoring/)
4. [Sea Turtle Patrol Hilton Head Island Protects Endangered Hatchlings with GIS — Eos](https://eos-gnss.com/successes/sea-turtle-patrol-hilton-head-island)
5. [Collector and Arrow 100 Monitor Sea Turtle Nesting Efforts — AGS GIS](https://www.agsgis.com/Collector-and-Arrow-100-Successfully-Aid-Monitoring-of-Sea-Turtle-Nesting-on-Hilton-Head-Island_b_1069.html)
6. [Sea Turtle Patrol HHI](https://seaturtlepatrolhhi.org/)
7. [Arrow Series — Eos GNSS](https://eos-gnss.com/products/hardware/arrow-series)
8. [ArcGIS Survey123 Pricing — Esri](https://www.esri.com/en-us/arcgis/products/arcgis-survey123/buy)
9. [Esri Nonprofit Program](https://www.esri.com/en-us/industries/nonprofit/nonprofit-program)
10. [Esri Conservation Program Application](https://www.esri.com/en-us/industries/conservation/program-application)
11. [SEATURTLE.ORG — Sea Turtle Nest Monitoring System](http://www.seaturtle.org/nestdb/)
12. [SEATURTLE.ORG — About](http://www.seaturtle.org/about/)
13. [SEATURTLE.ORG — Global Sea Turtle Network](https://www.seaturtle.org/)
14. [Turtle Nest Tracker](https://www.turtlenesttracker.org/)
15. [STC Turtle Tracker — Sea Turtle Conservancy](https://conserveturtles.org/sea-turtle-conservancy-apps/)
16. [Tracking Turtles — Sea Turtle Conservancy](https://conserveturtles.org/turtle-tracking/)
17. [STC Turtle Tracker — Google Play](https://play.google.com/store/apps/details?id=com.mapotic.turtletracker&hl=en_US)
18. [1 Million Turtles / TurtleSAT](https://1millionturtles.com/)
19. [Fostering active participation in turtle conservation — Australian Museum](https://australian.museum/blog/science/1-million-turtles/)
20. [Turtle Nest Predictor](https://1millionturtles.com/turtle-nest-predictor)
21. [We Spot Turtles](https://www.wespotturtles.org)
22. [Sea Turtle Spotter/Observatory — iNaturalist](https://inaturalist.org/projects/sea-turtle-observatory)
23. [DEKAMER — About](https://www.dekamer.org.tr/about.html)
24. [DEKAMER — Become a Volunteer](http://www.dekamer.org.tr/volunteer.html)
25. [MEDASSET — Turkey Protecting Important Sea Turtle Nesting Habitats](https://medasset.org/portfolio-item/turkey-protecting-important-sea-turtle-nesting-habitats/)
26. [ARCHELON — What We Do](https://archelon.gr/en/what-we-do)
27. [ARCHELON — Population and migration studies](https://archelon.gr/en/what-we-do/monitoring)
28. [Florida FWC Marine Turtle Conservation Handbook (2016)](https://flrules.org/gateway/readRefFile.asp?refId=7547&filename=FWC+Marine+Turtle+Conservation+Handbook.pdf) (mirror: [guyharveyresortstaugustinebeach.com PDF](https://guyharveyresortstaugustinebeach.com/wp-content/uploads/2023/12/fwc-mtconservationhandbook.pdf))
29. [FWC Crawl Identification Guidelines](https://myfwc.com/media/11936/crawlidentificationguidelines.pdf)
30. [Sea Turtle Monitoring and Management at Cape Lookout National Seashore — 2021 Annual Report (NPS)](https://www.nps.gov/calo/learn/management/upload/2021_Sea_Turtle_Summary-Report_508.pdf)
31. [carettafriends.com](https://carettafriends.com)

### Volunteer workflow & pain points
32. [SC DNR Marine Turtle Conservation Program](https://www.dnr.sc.gov/marine/turtles/volprog.htm)
33. [NSB Turtle Trackers — Survey protocol](https://nsbturtletrackers.org/our-sea-turtles/survey/)
34. [Coastal Wildlife Club — Sea Turtle Patrol](https://coastalwildlifeclub.org/sea-turtle-patrol)
35. [ARCHELON — Become a Volunteer](https://archelon.gr/en/volunteer)
36. [DEKAMER volunteer experience — Yabangee](http://yabangee.com/turtle-conservation-volunteering-dalyan-dekamer-experience/)
37. [Wildlife Act — Sea Turtle Monitoring, Nesting and Hatching Methods, Seychelles](https://www.wildlifeact.com/blog/sea-turtle-monitoring-nesting-hatching-and-volunteer-methodology)
38. [Anti-predator meshing may provide greater protection for sea turtle nests than predator removal — PLOS ONE](https://journals.plos.org/plosone/article?id=10.1371%2Fjournal.pone.0171831)
39. [Mesh grids protect loggerhead nests from red fox predation — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0006320797000037)
40. [Assessing sea turtle nest protection strategies against coyotes — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0022098120303051)
41. [Sea Turtle Conservancy — Beachfront Lighting](https://conserveturtles.org/program/beach-lighting/)
42. [Sea Turtle Conservancy — Artificial Lighting threat page](https://conserveturtles.org/threat/artificial-lighting/)
43. [NPS Park Science — Sea turtle monitoring, Gulf Islands National Seashore, light pollution photometer program](https://www.nps.gov/articles/parkscience32_2_79_shelley_3845.htm)
44. ["Software for improved field surveys of nesting marine turtles" — Scientific Reports 2017](https://pmc.ncbi.nlm.nih.gov/articles/PMC5589930/)
45. [Eos GNSS Customer Spotlight — Amber Kuehn / Sea Turtle Patrol Hilton Head Island](https://eos-gnss.com/successes/customer-spotlight/amber-kuehn)
46. [Loma Linda University — sea turtle research smartphone apps (TURT/RASTR)](https://news.llu.edu/research/loma-linda-university-researchers-expand-sea-turtle-research-smartphone-apps)
47. [TurtleOps — App Store listing](https://apps.apple.com/dk/app/turtleops/id6758343154)
48. [Top Smartphone Apps for Sea Turtle Work — SWOT](https://www.seaturtlestatus.org/articles/2020/2/27/top-smartphone-apps-for-sea-turtle-work)
49. [KoboToolbox vs ODK vs SurveyCTO comparison](https://www.monitoringevaluationstudio.com/resources/reference/compare/kobo-vs-odk-vs-surveycto)
50. [Bradford & Israel (2004), "Evaluating Volunteer Motivation for Sea Turtle Conservation in Florida" — EDIS/UF-IFAS](https://journals.flvc.org/edis/article/view/112136) ([PDF](https://journals.flvc.org/edis/article/download/112136/107328))
51. [Duke Sea Turtle Ecology blog — "Turtle Patrol" first-person account](https://blogs.nicholas.duke.edu/seaturtleecology/turtle-patrol/)
52. [Antalya Gündem — Gazipaşa first 2026 nest](https://www.antalyagundem.com.tr/gazipasada-caretta-cerattalar-ilk-yumurtalarini-birakti)
53. [DHA — Gazipaşa first nest of the year protected](https://www.dha.com.tr/yerel-haberler/antalya/gazipasa/antalya-gazipasada-yilin-ilk-caretta-yuvasi-ko-2880585)
54. [Gazete Vatan — Gazipaşa 60-day incubation coverage](https://www.gazetevatan.com/galeri/antalyada-caretta-carettalar-icin-seferberlik-60-gunluk-buyuk-mucize-basladi-sahile-adim-atmadan-once-bu-yaziyi-mutlaka-okuyun-2373906)
55. [MEDASSET — Turkey's Fethiye & Patara nesting beaches still need better protection](https://medasset.org/turkey-s-fethiye-amp-patara-sea-turtle-nesting-beaches-still-need-better-protection/)

### Gamification / engagement
56. [eBird Alerts](https://ebird.org/alerts)
57. [eBird Alerts and Targets FAQ](https://support.ebird.org/en/support/solutions/articles/48000960317-ebird-alerts-and-targets-faqs)
58. [2026 Checklist-a-day Challenge — eBird](https://ebird.org/news/2026-checklist-a-day-challenge/)
59. [Tracking your lists with eBird](https://ebird.org/about/tracking-your-lists-with-ebird)
60. [The influence of rare birds on observer effort — PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7827972/)
61. [Identification Pilot to Onboard New Users — iNaturalist](https://www.inaturalist.org/blog/100580-identification-pilot-to-onboard-new-users)
62. [AOTW: Citizen Science – iNaturalist — Florida Museum](https://www.floridamuseum.ufl.edu/earth-systems/blog/aotw-citizen-science-inaturalist/)
63. [The benefits of contributing to iNaturalist as an identifier — PLOS Biology / PMC](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC9648699/)
64. [Participation Intensity Influences Motivations for Contributing to iNaturalist](https://theoryandpractice.citizenscienceassociation.org/articles/10.5334/cstp.823)
65. [Building community in a citizen science project — Participatory Sciences](https://participatorysciences.org/2025/02/28/building-community-in-a-citizen-science-project-fostering-social-interaction-for-meaningful-engagement/)
66. [Zooniverse — Nesta case study](https://www.nesta.org.uk/feature/ai-and-collective-intelligence-case-studies/zooniverse/)
67. [Gravity Spy: Lessons Learned and a Path Forward — arXiv](https://arxiv.org/pdf/2308.15530)
68. [What Do We Know about Young Volunteers? Zooniverse study — PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC7612984/)
69. [Duolingo Streaks: How the Mechanic Drives 2x Daily Retention](https://duolingo.deconstructoroffun.com/mechanics/streaks)
70. [App Teardown: How Duolingo's Streak Mechanic Actually Works — Apptitude](https://apptitude.io/blog/how-duolingos-streak-mechanic-actually-works/)
71. [Duolingo UX Design Breakdown — 925 Studios](https://www.925studios.co/blog/duolingo-design-breakdown)
72. [Duolingo Gamification Strategy: Full Case Study — Trophy](https://trophy.so/blog/duolingo-gamification-case-study)
73. [Duolingo gamification explained — StriveCloud](https://www.strivecloud.io/blog/gamification-examples-boost-user-retention-duolingo)
74. [TurtleSAT | 1 Million Turtles](https://1millionturtles.com/turtlesat)
75. [The scientists hoping to boost turtle numbers — Nature](https://www.nature.com/articles/d42473-023-00295-2)
76. [National Science Foundation Awards Litterati Grant — Forbes](https://www.forbes.com/sites/cognitiveworld/2019/04/22/national-science-foundation-litterati-ai-for-a-cleaner-planet/)
77. [Clean Swell FAQ — Ocean Conservancy](https://oceanconservancy.org/work/plastics/cleanups-icc/clean-swell-app/clean-swell-faq/)
78. [Ocean Conservancy Introduces Newly Revamped Clean Swell App](https://oceanconservancy.org/newsroom/press-release/2022/05/16/clean-swell-2-0/)
79. [Meeting volunteer expectations — Tandfonline](https://www.tandfonline.com/doi/full/10.1080/09640568.2020.1853507)
80. [Getting it Right or Being Top Rank: Games in Citizen Science](https://theoryandpractice.citizenscienceassociation.org/articles/10.5334/cstp.101)
81. [Gamifying Citizen Science: Lessons and Future Directions (Bowser, Hansen, Preece)](http://gamification-research.org/wp-content/uploads/2013/03/Bowser_Hansen_Preece.pdf)
82. [Sea Turtle Trackers — What We Are Doing](https://seaturtletrackers.org/learn/what-we-are-doing/)

### Tech feasibility
83. [MapLibre Native — GitHub](https://github.com/maplibre/maplibre-native)
84. [MapLibre Native — FORK.md](https://github.com/maplibre/maplibre-native/blob/main/FORK.md)
85. [OfflineManager/OfflineRegion — MapLibre Native Android API docs](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/-offline-region/index.html)
86. [OfflineManager — MapLibre React Native docs](https://maplibre.org/maplibre-react-native/docs/modules/offline-manager/)
87. [Offline maps using MBTiles files — GitHub Discussion #393](https://github.com/maplibre/maplibre-native/discussions/393)
88. [MapLibre Compose — GitHub](https://github.com/maplibre/maplibre-compose)
89. [MapLibre Compose — Releases](https://github.com/maplibre/maplibre-compose/releases)
90. [How to add a Marker Symbol? — MapLibre Compose Discussion #647](https://github.com/maplibre/maplibre-compose/discussions/647)
91. [Ramani Maps — GitHub](https://github.com/ramani-maps/ramani-maps)
92. [Multiplatform support — Ramani Maps Issue #77](https://github.com/ramani-maps/ramani-maps/issues/77)
93. [MapLibre GL Native: open-source mobile SDK — MapTiler](https://www.maptiler.com/news/2021/06/maplibre-gl-native-open-source-mobile-sdk-for-android-and-ios/)
94. [PostGIS: Geo queries — Supabase Docs](https://supabase.com/docs/guides/database/extensions/postgis)
95. [Geo Queries with PostGIS in Ionic Angular — Supabase Blog](https://supabase.com/blog/geo-queries-with-postgis-in-ionic-angular)
96. [Create a multiplatform app using Ktor and SQLDelight — Kotlin docs](https://kotlinlang.org/docs/multiplatform/multiplatform-ktor-sqldelight.html)
97. [Introduction to Multiplatform Persistence with SQLDelight](https://johnoreilly.dev/posts/sqldelight-multiplatform/)

### Reach / impact estimate
98. [Sea Turtle Volunteer Opportunities Worldwide | Map & Directory — SWOT](https://www.seaturtlestatus.org/sea-turtle-volunteering)
99. [Index Nesting Beach Survey Totals (1989-2025) — FWC](https://myfwc.com/research/wildlife/sea-turtles/nesting/beach-survey-totals/)
100. [Monitoring Sea Turtle Nests in Florida's State Parks](https://www.floridastateparks.org/learn/monitoring-sea-turtle-nests-floridas-state-parks)
101. [ARCHELON — Projects & collaborations in progress](https://archelon.gr/en/what-we-do/projects-and-collaborations)
102. [Turtle conservation hits the SPOT in North Cyprus — Mongabay](https://news.mongabay.com/2021/03/turtle-conservation-hits-the-spot-in-north-cyprus/)
103. [Society for the Protection of Turtles (SPOT) Cyprus](https://www.cyprusturtles.org/)
104. [Conservation of Marine Turtles in the Mediterranean Region — DEKAMER/MAVA](https://www.dekamer.org.tr/mava-eng.html)
105. [MedTRACS — LIFE ADAPTS project partner](https://archelon.gr/en/news/new-adaptation-strategies-for-sea-turtles-and-mediterranean-monk-seals)

### Nest temperature & TSD
106. [Mrosovsky, Kamel, Rees & Margaritoulis (2002), "Pivotal temperature for loggerhead turtles from Kyparissia Bay, Greece" — Can. J. Zool.](https://cdnsciencepub.com/doi/10.1139/z02-204)
107. [Godley et al. (2001), "Thermal conditions in nests of loggerhead turtles... Mediterranean" — J. Exp. Mar. Biol. Ecol.](https://cyprusturtles.org/uploads/home/file/Thermal-conditions-in-nests-of-loggerhead-turtles-further-evidence-suggesting-female-skewed-sex-ratios-of-hatchling-production-in-the-Mediterranean.pdf)
108. [Öz et al. (2004), "Nest temperatures and sex-ratio estimates of loggerhead turtles at Patara beach, Turkey" — Can. J. Zool.](https://cdnsciencepub.com/doi/10.1139/z03-200)
109. [Kaska et al. (2006), "Sex ratio estimations... at Fethiye beach, Turkey" — Naturwissenschaften](https://link.springer.com/article/10.1007/s00114-006-0110-5)
110. ["Sex Ratio Estimation of the Most Eastern Main Loggerhead Sea Turtle Nesting Site: Anamur Beach, Mersin, Turkey" — Israel J. Ecol. Evol.](https://www.tandfonline.com/doi/abs/10.1560/IJEE.58.1.87)
111. ["Nest Temperatures And Sex Ratio Variations among The Hatchlings and Embryos of Loggerhead Turtles Along The Mediterranean Coast of Turkey"](https://www.researchgate.net/publication/289649279_Nest_Temperatures_And_Sex_Ratio_Variations_among_The_Hatchlings_and_Embryos_of_Loggerhead_Turtles_Along_The_Mediterranean_Coast_of_Turkey)
112. ["Spatial variations of loggerhead hatchling sex ratio... Dalyan and Goksu delta in Turkey"](https://www.researchgate.net/publication/306263998_Spatial_variations_of_loggerhead_hatchling_sex_ratio_along_the_eastern_and_western_loggerhead_turtle_nesting_beaches_Dalyan_and_Goksu_delta_in_Turkey)
113. [Yntema & Mrosovsky, "Critical periods and pivotal temperatures for sexual differentiation in loggerhead sea turtles"](https://cdnsciencepub.com/doi/10.1139/z82-141)
114. [Wibbels (2003), "Critical Approaches to Sex Determination in Sea Turtles"](https://bermudaturtleproject.org/wp-content/uploads/2024/07/WibbelsT_2003_InThebiologyofseaturtlesVolume2_p103-134.pdf)
115. ["Key parameters describing temperature-dependent sex determination in the southernmost population of loggerhead sea turtles" — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0022098113003092)
116. [FAU press release on Wyneken et al. (2022), "New Critical Period of Sex Determination in Sea Turtles Identified"](https://www.fau.edu/newsdesk/articles/sea-turtle-sex-ratios-missing-link)
117. ["Models of primary sex ratios at a major flatback turtle rookery show an anomalous masculinising trend" — Climate Change Responses](https://link.springer.com/article/10.1186/s40665-014-0003-3)
118. ["Embryonic mortality in green and loggerhead sea turtle nests increases with cumulative exposure to elevated temperatures" — J. Exp. Mar. Biol. Ecol.](https://www.sciencedirect.com/science/article/abs/pii/S0022098119300784)
119. ["Thermal tolerances of sea turtle embryos: Current understanding and future directions"](https://www.researchgate.net/publication/272864073_Thermal_tolerances_of_sea_turtle_embryos_Current_understanding_and_future_directions)
120. ["Incubation temperature and energy expenditure during development in loggerhead sea turtle embryos" — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0022098109003074)
121. ["Temperature-based modeling of incubation period to protect loggerhead hatchlings on an urban beach in Northwest Florida"](https://www.sciencedirect.com/science/article/abs/pii/S0022098121001374)
122. [Lolavar & Wyneken, "The effect of rainfall on loggerhead turtle nest temperatures, sand temperatures and hatchling sex"](https://www.researchgate.net/publication/282448157_The_effect_of_rainfall_on_loggerhead_turtle_nest_temperatures_sand_temperatures_and_hatchling_sex)
123. [Laloë et al. (2021), "Extreme rainfall events and cooling of sea turtle clutches" — Ecol. Evol.](https://onlinelibrary.wiley.com/doi/10.1002/ece3.7076)
124. ["The Wrong Assumptions of the Effects of Climate Change on Marine Turtle Nests with Temperature-Dependent Sex Determination"](https://pmc.ncbi.nlm.nih.gov/articles/PMC12784875/)
125. ["The ecological importance of the accuracy of environmental temperature measurements"](https://pmc.ncbi.nlm.nih.gov/articles/PMC9364146/)
126. ["Sand temperatures for nesting sea turtles in the Caribbean: Implications for hatchling sex ratios in the face of climate change" — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0022098115300289)
127. ["Optimism for mitigation of climate warming impacts for sea turtles through nest shading and relocation"](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6279794/)
128. [Hackaday.io, "Low-cost/power/size temperature logger" (DIY sea-turtle-nest logger project)](https://hackaday.io/project/27560-low-costpowersize-temperature-logger)
129. [Onset HOBO, "Data Loggers Aid Sea Turtle Preservation in Malaysia"](https://www.onsetcomp.com/resources/application-stories/hobo-data-loggers-aid-sea-turtle-preservation-in-malaysia)
130. [Onset HOBO Pendant MX2201 product page](https://www.onsetcomp.com/products/data-loggers/mx2201)
131. [iButtonLink DS1921G / DS1922L product pages](https://www.ibuttonlink.com/products/ds1921g)
