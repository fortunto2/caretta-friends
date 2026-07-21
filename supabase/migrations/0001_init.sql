-- Caretta Friends — initial cloud schema (V1).
-- Pragmatic + sync-friendly: key columns for querying + a JSONB payload that round-trips the
-- app's @Serializable models. PostGIS enabled for future geo queries. Backend-agnostic by design
-- (storage/DB may migrate to Cloudflare later; auth stays Supabase).

create extension if not exists postgis;

-- ── profiles ────────────────────────────────────────────────────────────────
create table if not exists profiles (
    id           text primary key,            -- device id (V1) / auth uid (V2)
    display_name text,
    language     text default 'en',
    payload      jsonb,
    updated_at   timestamptz not null default now()
);

-- ── nests (full aggregate in payload: updates/excavation/photos/temps) ────────
create table if not exists nests (
    id          text primary key,
    code        text,
    beach_id    text,
    lat         double precision,
    lng         double precision,
    species     text,
    is_nest     boolean default true,
    confidence  text,
    status      text,
    found_date  date,
    created_by  text,
    payload     jsonb not null,
    geog        geography(Point, 4326),
    updated_at  timestamptz not null default now(),
    created_at  timestamptz not null default now()
);
create index if not exists nests_beach_idx on nests (beach_id);
create index if not exists nests_geog_gix on nests using gist (geog);

-- ── simple markers (landmark / trash / ...) ───────────────────────────────────
create table if not exists markers (
    id         text primary key,
    type       text,
    lat        double precision,
    lng        double precision,
    note       text,
    payload    jsonb,
    updated_at timestamptz not null default now()
);

-- ── patrols (coverage / km) ───────────────────────────────────────────────────
create table if not exists patrols (
    id          text primary key,
    beach_id    text,
    distance_m  int,
    by_name     text,
    payload     jsonb,
    created_at  timestamptz not null default now()
);

-- keep geog in sync with lat/lng on write
create or replace function nests_set_geog() returns trigger as $$
begin
    if new.lat is not null and new.lng is not null then
        new.geog := ST_SetSRID(ST_MakePoint(new.lng, new.lat), 4326)::geography;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists nests_geog_trg on nests;
create trigger nests_geog_trg before insert or update on nests
    for each row execute function nests_set_geog();

-- ── RLS (V1: trusted volunteer community, no-auth). Tighten with Supabase Auth (V2). ──
alter table profiles enable row level security;
alter table nests    enable row level security;
alter table markers  enable row level security;
alter table patrols  enable row level security;

do $$
begin
    create policy "v1 anon read/write profiles" on profiles for all using (true) with check (true);
    create policy "v1 anon read/write nests"    on nests    for all using (true) with check (true);
    create policy "v1 anon read/write markers"  on markers  for all using (true) with check (true);
    create policy "v1 anon read/write patrols"  on patrols  for all using (true) with check (true);
exception when duplicate_object then null;
end $$;

grant usage on schema public to anon, authenticated;
grant all on all tables in schema public to anon, authenticated;
grant all on all sequences in schema public to anon, authenticated;
