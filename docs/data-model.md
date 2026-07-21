---
type: data-model
project: caretta-friends
title: Caretta Friends — Data Model (Supabase / PostGIS)
status: draft
created: 2026-07-21
db: PostgreSQL 15 + PostGIS (Supabase)
offline_cache: SQLDelight (mirror of these tables, minus RLS)
standard: seaturtle.org / FWC Marine Turtle Conservation Handbook (export-compatible)
---

# Caretta Friends — Data Model

Draft schema for Supabase (Postgres + PostGIS). Designed **complete** (the schema is designed
once), with **V1 / V2 phasing** so we ship the no-auth public map first and layer identity,
roles, and notifications on top without a migration rewrite.

- **V1 (no auth):** shared public map, markers + nests with in-app-camera **or gallery** photos,
  the nest timeline (updates + comments), excavation, temperature logging, facts & guide,
  community links (static). Identity is a lightweight local `device profile`; writes are `public`.
- **V2 (auth = Supabase/Google):** accounts, community membership + roles (admin / beach-leader /
  volunteer), beach assignments, private visibility via RLS, hatch-watch push, cross-device sync,
  cloud leaderboards.

Multi-community from day one (Gazipaşa first). Nest + excavation fields are **export-compatible
with the FWC / seaturtle.org standard** (Miller 1999 hatch/emergence success).

---

## ER overview

```
community ─┬─< beach ─┬─< zone (участок, optional)         geo: PostGIS
           │          ├─< patrol (GPS track → "covered today", km)
           │          └─< marker ─┬─(1:1)─ nest ─┬─< nest_update  (timeline: obs│comment│status│hatch)
           │                      │              ├─< temperature_reading
           │                      │              ├─< hatch_watch      (V2)
           │                      │              └─(1:1)─ excavation
           │                      └─< photo   (camera│gallery, EXIF geotag → marker location)
           ├─< membership >─ profile   (role: admin|beach_leader|volunteer)   (V2)
           ├─< assignment  >─ profile ─ beach/zone   (who leads which section)(V2)
           ├─< fact                                   (rotating useful facts)
           └─< guide_article                          (from carettafriends.com)

profile ─< user_badge >─ badge        (gamification)
```

---

## Extensions & enums

```sql
create extension if not exists postgis;
create extension if not exists "uuid-ossp";

create type marker_type    as enum ('nest','landmark','trash','predator_sign','crawl','obstacle','other');
create type nest_status     as enum ('incubating','hatching','hatched','excavated','lost','predated','washed_over','poached','false_crawl');
create type obs_condition   as enum ('ok','predated','washed_over','disturbed','poached','hatching','hatched','relocated','other');
create type temp_source     as enum ('manual_surface','manual_probe','logger_ibutton','logger_hobo','water','air_shade','weather_api');
create type sun_exposure    as enum ('full_sun','partial','shade');   -- V1 qualitative TSD input
create type member_role     as enum ('admin','beach_leader','volunteer');
create type member_status   as enum ('pending','active','removed');
create type visibility      as enum ('public','private');
create type species         as enum ('caretta_caretta','chelonia_mydas','other','unknown');
-- how the marker got its coordinates; drives the "confirmed/unconfirmed" logic
create type location_source as enum ('photo_exif','device_gps','manual_map','none');
create type nest_confidence as enum ('confirmed','unconfirmed');
create type photo_source    as enum ('camera','gallery');
-- one unified nest timeline of different update kinds
create type update_kind     as enum ('found','observation','status_change','comment','hatched','relocated','excavated');
```

---

## 1. Community & geography (PostGIS)

```sql
create table community (
  id           uuid primary key default uuid_generate_v4(),
  slug         text unique not null,             -- 'gazipasa-caretta'
  name         text not null,
  country      text,                             -- 'TR'
  description   text,
  website_url   text,                            -- https://carettafriends.com
  whatsapp_url  text,                            -- community chat invite
  instagram_url text,                            -- @gazipasa_caretta_ve_kumzambagi
  cover_photo   text,
  center        geography(Point,4326),           -- map default focus
  created_at    timestamptz not null default now()
);

create table beach (
  id           uuid primary key default uuid_generate_v4(),
  community_id uuid not null references community(id) on delete cascade,
  name         text not null,                    -- 'Bıdı Bıdı', 'Selinus'
  city         text,                             -- 'Gazipaşa'  (drives city selector)
  description  text,
  boundary     geography(Polygon,4326),
  center       geography(Point,4326) not null,
  created_at   timestamptz not null default now()
);
create index beach_community_idx on beach(community_id);
create index beach_center_gix on beach using gist(center);

create table zone (
  id         uuid primary key default uuid_generate_v4(),
  beach_id   uuid not null references beach(id) on delete cascade,
  name       text not null,                       -- 'Section A (0-500m)'
  boundary   geography(Polygon,4326),
  created_at timestamptz not null default now()
);
create index zone_beach_idx on zone(beach_id);
```

---

## 2. Identity, membership & roles  *(profile: V1 local → V2 auth)*

```sql
create table profile (
  id                 uuid primary key,            -- device id (V1) / auth uid (V2)
  display_name       text,
  avatar             text,
  home_community_id  uuid references community(id),
  language           text default 'ru',           -- 'ru' | 'tr' | 'en'
  default_visibility visibility not null default 'public',
  is_anonymous       boolean not null default true,
  created_at         timestamptz not null default now()
);

create table membership (
  id           uuid primary key default uuid_generate_v4(),
  community_id uuid not null references community(id) on delete cascade,
  profile_id   uuid not null references profile(id) on delete cascade,
  role         member_role  not null default 'volunteer',
  status       member_status not null default 'pending',   -- admin approves the link
  joined_at    timestamptz not null default now(),
  unique(community_id, profile_id)
);
create index membership_community_idx on membership(community_id);

create table assignment (
  id         uuid primary key default uuid_generate_v4(),
  profile_id uuid not null references profile(id) on delete cascade,
  beach_id   uuid not null references beach(id) on delete cascade,
  zone_id    uuid references zone(id) on delete cascade,   -- null = whole beach
  is_leader  boolean not null default false,               -- true = ответственный за участок
  created_at timestamptz not null default now()
);
create index assignment_beach_idx on assignment(beach_id);
create index assignment_profile_idx on assignment(profile_id);
```

---

## 3. Markers & nests

```sql
-- Generic map object. Location can come from photo EXIF, device GPS, or a manual map tap.
-- geom is nullable so a nest can be "reported" and pinned/confirmed later.
create table marker (
  id              uuid primary key default uuid_generate_v4(),
  community_id    uuid not null references community(id) on delete cascade,
  beach_id        uuid references beach(id) on delete set null,
  zone_id         uuid references zone(id) on delete set null,
  type            marker_type not null,
  geom            geography(Point,4326),                         -- null until placed
  location_source location_source not null default 'device_gps',-- exif | gps | manual | none
  location_acc_m  numeric(6,1),                                  -- GPS accuracy if known
  note            text,
  visibility      visibility not null default 'public',
  created_by      uuid references profile(id) on delete set null,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);
create index marker_geom_gix on marker using gist(geom);
create index marker_beach_idx on marker(beach_id);
create index marker_type_idx  on marker(type);

-- Nest-specific fields (FWC / seaturtle.org export-compatible).
create table nest (
  marker_id           uuid primary key references marker(id) on delete cascade,
  external_nest_code  text,                    -- 'GZP-24' / seaturtle.org id
  species             species not null default 'caretta_caretta',
  is_nest             boolean not null default true,   -- false crawl (non-nesting emergence) => false, still logged
  confidence          nest_confidence not null default 'unconfirmed',
                        -- 'confirmed' once it has a photo AND a real location (exif/device_gps);
                        -- no photo & no precise pin => stays 'unconfirmed' (needs verification)
  found_date          date not null,
  laid_date_est       date,
  clutch_size_est     int,
  relocated           boolean not null default false,
  relocation_reason   text,
  cage_installed      boolean not null default false,
  shade_exposure      sun_exposure,            -- V1 qualitative TSD input (sun/partial/shade)
  vegetation_nearby   boolean,                 -- shading vegetation next to the nest
  incubation_days_est int,                     -- temp-adjusted model output (a window, not exact)
  hatch_window_start  date,                    -- predicted
  hatch_window_end    date,                    -- predicted
  predicted_female_pct_low  numeric(4,1),      -- TSD prediction as a RANGE, not an exact %
  predicted_female_pct_high numeric(4,1),      --   (regional model; caveat shown in UI)
  emergence_date      date,                    -- first hatchlings seen
  status              nest_status not null default 'incubating',
  hatch_success_pct   numeric(4,1),            -- copied from excavation on finish
  emergence_success_pct numeric(4,1),
  found_by            uuid references profile(id) on delete set null
);
```

**Confirmed vs unconfirmed** — client sets `confidence='confirmed'` when the nest has ≥1 photo and
`marker.location_source in ('photo_exif','device_gps')`. Unconfirmed nests render with a dashed/faded
pin and a "needs verification" chip; anyone on the beach can add the missing photo/pin to confirm.

---

## 4. Nest timeline, temperature, excavation

```sql
-- THE TIMELINE. One unified feed per nest: patrol observations, free-text comments,
-- status changes ('destroyed after the storm'), the hatch event, relocation, excavation link.
-- Comments live here (kind='comment'); the common patrol is kind='observation' + condition='ok'.
create table nest_update (
  id          uuid primary key default uuid_generate_v4(),
  nest_id     uuid not null references nest(marker_id) on delete cascade,
  author_id   uuid references profile(id) on delete set null,
  kind        update_kind not null default 'observation',
  condition   obs_condition,          -- when kind='observation'
  new_status  nest_status,            -- when kind='status_change' (e.g. -> 'lost','hatched')
  body        text,                   -- free comment / note
  created_at  timestamptz not null default now()
);
create index nest_update_nest_idx on nest_update(nest_id, created_at);

-- Temperature/conditions time-series → hatch-date + TSD sex prediction.
-- V1 rows come mostly from the weather API (source='weather_api') + optional water/air readings;
-- logger_* sources are the V2 research add-on.
create table temperature_reading (
  id          uuid primary key default uuid_generate_v4(),
  nest_id     uuid not null references nest(marker_id) on delete cascade,
  recorded_at timestamptz not null default now(),
  source      temp_source not null,
  value_c     numeric(4,1) not null,
  depth_cm    int,                              -- nest-chamber depth if probe/logger
  recorded_by uuid references profile(id) on delete set null
);
create index temp_nest_idx on temperature_reading(nest_id, recorded_at);

-- Excavation (post-hatch), 1:1 with nest. FWC categories + auto success (Miller 1999).
create table excavation (
  nest_id             uuid primary key references nest(marker_id) on delete cascade,
  excavated_by        uuid references profile(id) on delete set null,
  excavated_at        timestamptz not null default now(),
  shells_count        int not null default 0,   -- empty shells >50% (= hatched)
  unhatched_count     int not null default 0,   -- whole eggs, no/partial embryo
  pipped_live_count   int not null default 0,   -- egg pipped, hatchling alive
  pipped_dead_count   int not null default 0,   -- egg pipped, hatchling dead
  live_in_nest_count  int not null default 0,   -- fully hatched, alive, stuck in chamber
  dead_in_nest_count  int not null default 0,   -- fully hatched, dead in chamber
  live_helped_count   int not null default 0,   -- rescued alive & released to sea
  notes               text,
  -- clutch total = shells + all eggs that did not produce an emerged hatchling
  eggs_total          int generated always as
                       (shells_count + unhatched_count + pipped_live_count + pipped_dead_count) stored,
  -- Hatching success % = shells / (shells + unhatched + pipped) × 100
  hatch_success_pct   numeric(4,1) generated always as (
      case when (shells_count + unhatched_count + pipped_live_count + pipped_dead_count) > 0
      then round(100.0 * shells_count /
           (shells_count + unhatched_count + pipped_live_count + pipped_dead_count), 1) end) stored,
  -- Emergence success % = (shells − hatchlings left in nest) / (shells + unhatched + pipped) × 100
  emergence_success_pct numeric(4,1) generated always as (
      case when (shells_count + unhatched_count + pipped_live_count + pipped_dead_count) > 0
      then round(100.0 * (shells_count - live_in_nest_count - dead_in_nest_count) /
           (shells_count + unhatched_count + pipped_live_count + pipped_dead_count), 1) end) stored
);
```

> **Temperature in V1 — no hardware required.** Volunteers do **not** bury loggers. On nest
> creation they set a quick **exposure tag** (full sun / partial / shade + vegetation); the app
> auto-pulls **air temperature + rainfall from a weather API (Open-Meteo)** by the nest's
> coordinates. From that it shows a **predicted hatch window** and a **predicted sex range** (not an
> exact %), calibrated to the nearest regional data (Anamur, Mersin: 28.9 °C, 72–79% female — not
> Florida). Buried loggers (HOBO/iButton, `logger_*` sources) are a **V2 research add-on** for a
> university partnership to calibrate the community's own pivotal temperature. In-app caveat: the
> "middle third of incubation = thermosensitive period" is an approximation → always present a range.

---

## 4b. Patrols & coverage

```sql
-- A walked patrol = a GPS track. Powers "who already covered this beach today" (so volunteers
-- don't re-walk the same stretch) + personal km / patrol-day stats. Distance is enough for V1;
-- the full path (LineString) is optional and can be recorded when the volunteer taps "Start patrol".
create table patrol (
  id          uuid primary key default uuid_generate_v4(),
  profile_id  uuid references profile(id) on delete set null,
  beach_id    uuid references beach(id) on delete set null,
  zone_id     uuid references zone(id) on delete set null,   -- which section was covered
  started_at  timestamptz not null,
  ended_at    timestamptz,
  distance_m  int,                              -- meters walked
  path        geography(LineString,4326),       -- optional recorded track (draw on map)
  created_at  timestamptz not null default now()
);
create index patrol_beach_time_idx on patrol(beach_id, started_at);
create index patrol_profile_idx on patrol(profile_id);
```

**Coverage** — "already walked today, don't duplicate":

```sql
select b.id, b.name, max(pt.started_at) as last_patrol, count(*) as patrols_today
from beach b join patrol pt on pt.beach_id = b.id
where pt.started_at::date = current_date
group by b.id, b.name;
```

The map draws today's patrol tracks as faded lines and shows a per-beach/zone chip
("✓ Patrolled 07:10 · Mert · 2.3 km"), so a volunteer arriving later knows it's covered.

---

## 5. Photos (in-app camera OR gallery → Supabase Storage)

```sql
-- Polymorphic attachment. source = built-in camera or gallery upload.
-- On import the client reads EXIF GPS; if present it can auto-place the parent marker
-- (marker.geom = exif point, location_source='photo_exif'). Offline: local first, path on upload.
create table photo (
  id           uuid primary key default uuid_generate_v4(),
  owner_type   text not null check (owner_type in ('marker','nest_update','excavation','nest')),
  owner_id     uuid not null,
  source       photo_source not null default 'camera',
  storage_path text,                             -- null while pending offline upload
  thumb_path   text,
  blurhash     text,
  exif_lat     double precision,                 -- extracted from photo, if present ->
  exif_lng     double precision,                 --   used to auto-place / confirm the marker
  exif_taken_at timestamptz,
  width        int,
  height       int,
  taken_at     timestamptz not null default now(),
  taken_by     uuid references profile(id) on delete set null,
  created_at   timestamptz not null default now()
);
create index photo_owner_idx on photo(owner_type, owner_id);
```

---

## 6. Engagement — hatch watch  *(V2: needs auth + push)*

```sql
create table hatch_watch (
  profile_id uuid not null references profile(id) on delete cascade,
  nest_id    uuid not null references nest(marker_id) on delete cascade,
  notify     boolean not null default true,
  created_at timestamptz not null default now(),
  primary key (profile_id, nest_id)
);
```

---

## 7. Content — facts & guide  *(from carettafriends.com)*

```sql
create table fact (
  id           uuid primary key default uuid_generate_v4(),
  community_id uuid references community(id) on delete cascade,  -- null = global
  title        text not null,
  body         text not null,
  image        text,
  tags         text[],
  published    boolean not null default true,
  created_at   timestamptz not null default now()
);

create table guide_article (
  id           uuid primary key default uuid_generate_v4(),
  community_id uuid references community(id) on delete cascade,  -- null = global
  slug         text not null,
  title        text not null,
  body         text not null,                    -- markdown
  source_url   text,                             -- carettafriends.com/...
  sort_order   int not null default 0,
  published    boolean not null default true,
  created_at   timestamptz not null default now(),
  unique(community_id, slug)
);
```

---

## 8. Gamification

```sql
create table badge (
  id          uuid primary key default uuid_generate_v4(),
  code        text unique not null,              -- 'first_nest','ten_patrols','rescuer','first_excavation'
  name        text not null,
  description text,
  icon        text,
  criteria    jsonb
);

create table user_badge (
  profile_id uuid not null references profile(id) on delete cascade,
  badge_id   uuid not null references badge(id) on delete cascade,
  progress   numeric(4,1) not null default 0,    -- 0..100
  earned_at  timestamptz,
  primary key (profile_id, badge_id)
);
```

**Impact is the north star; leaderboards are de-emphasised** (research: public rankings demotivate
most volunteers in small teams — Foldit vs Galaxy Zoo). Personal impact is a derived view:

```sql
-- Hatchlings a volunteer helped reach the sea = shells that emerged + ones they rescued,
-- on nests they excavated; plus their found/observation activity.
-- Draft view (uses per-key subqueries to avoid join fan-out on the aggregates).
create view v_volunteer_impact as
select p.id as profile_id, p.display_name,
  (select count(*) from nest n where n.found_by = p.id)                                 as nests_found,
  (select count(*) from nest_update u where u.author_id = p.id and u.kind='observation') as observations,
  (select count(*) from patrol pt where pt.profile_id = p.id)                           as patrols,
  (select coalesce(sum(distance_m),0)/1000.0 from patrol pt where pt.profile_id = p.id) as km_walked,
  (select coalesce(sum(e.shells_count - e.dead_in_nest_count + e.live_helped_count),0)
     from excavation e where e.excavated_by = p.id)                                     as hatchlings_reached
from profile p;
```

---

## 9. Row-Level Security (V2 sketch)

- `marker` / `nest` / `nest_update`: **public** rows readable by the community; **private** rows
  only by `created_by` (+ the beach's `beach_leader` and community `admin`). Insert requires active
  membership. Update/delete: owner, beach_leader, or admin.
- `membership`, `assignment`: admin (and beach_leader for their beach) manage; others read.
- `fact`, `guide_article`: read = all; write = admin/beach_leader.
- V1 runs with RLS off / anon key, everything `public`. Enabling policies later needs **no schema
  change** — only policies.

---

## 10. Common queries (PostGIS)

```sql
-- Nests on a beach, freshest first:
select * from marker m join nest n on n.marker_id = m.id
where m.beach_id = $1 and m.type = 'nest' order by n.found_date desc;

-- Markers within 300 m of the volunteer:
select * from marker
where geom is not null and ST_DWithin(geom, ST_MakePoint($lng,$lat)::geography, 300);

-- Unconfirmed nests that need a photo/pin:
select * from nest where confidence = 'unconfirmed';

-- "Hatching soon" (predicted window opens within 7 days):
select * from nest where status='incubating' and hatch_window_start <= current_date + 7;
```

---

## Export (FWC / seaturtle.org)

A CSV/JSON export maps our fields to the standard datasheet columns: `external_nest_code`, species
code (Cc/Cm/Dc), WGS84 lat/lng, found date, nest vs false crawl (`is_nest`), clutch size,
relocation (flag + reason), cage type, incubation start + emergence date, and the full excavation
count set → Hatching Success % and Emergence Success %. This makes the app's data usable by
researchers and compatible with existing regional databases. (V1 feature.)

---

## V1 build cut (what ships first)

Ship tables: `community`, `beach`, `marker`, `nest`, `nest_update`, `temperature_reading`,
`excavation`, `photo`, `patrol`, `fact`, `guide_article`, local `profile`, `badge`/`user_badge`.
Defer to V2: `membership`, `assignment`, `zone` leadership, `hatch_watch` push, RLS policies,
`v_volunteer_impact` as a cloud leaderboard (V1 computes impact locally).
