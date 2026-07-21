-- Caretta Friends — relational hardening (V1 → proper managed schema).
--
-- Research-driven (Supabase offline-first + relational best practices, 2026):
--  • HYBRID model: relational parents (community → beach) + aggregate roots (nest/marker/patrol)
--    with real FOREIGN KEYS between aggregates; the nest timeline (updates/excavation/temps/photos)
--    stays inside the JSONB payload (one aggregate = one document). FKs go BETWEEN aggregates,
--    never inside one (DDD aggregate boundary).
--  • Sync-control columns are REAL top-level columns (never read from JSONB): updated_at (server-set
--    via moddatetime trigger — defeats client clock skew), deleted_at (soft-delete tombstone),
--    owner_id (RLS + sync partition, V2).
--  • FK-safe offline sync rests on: client-generated UUID PKs, server-seeded parents (beaches exist
--    before any nest references them), DEFERRABLE INITIALLY DEFERRED FKs, idempotent parents-first upserts.
--  • Legacy-safe: FKs added NOT VALID then VALIDATE, so pre-existing rows never block the migration.
--
-- Idempotent: safe to re-run.

create extension if not exists moddatetime schema extensions;

-- ── moddatetime: server-managed updated_at (single monotonic ordering source for LWW) ─────────
-- Attach BEFORE UPDATE so ON CONFLICT DO UPDATE (our upsert path) always restamps updated_at.
do $$
declare t text;
begin
    -- ensure updated_at exists everywhere it must (patrols only had created_at)
    alter table patrols add column if not exists updated_at timestamptz not null default now();
    foreach t in array array['nests','markers','patrols','profiles'] loop
        execute format('drop trigger if exists %I_moddatetime_trg on %I', t, t);
        execute format(
            'create trigger %I_moddatetime_trg before update on %I for each row execute function extensions.moddatetime(updated_at)',
            t, t
        );
    end loop;
end $$;

-- ── sync-control columns (real columns, not JSONB) ────────────────────────────────────────────
alter table nests    add column if not exists owner_id   uuid;
alter table nests    add column if not exists deleted_at timestamptz;
alter table markers  add column if not exists owner_id   uuid;
alter table markers  add column if not exists deleted_at timestamptz;
alter table patrols  add column if not exists owner_id   uuid;
alter table patrols  add column if not exists deleted_at timestamptz;

-- ── community (reference data — seeded server-side, volunteers pick from it) ───────────────────
create table if not exists community (
    id            text primary key,
    slug          text unique,
    name          text not null,
    tagline       text,
    country       text default 'TR',
    website_url   text,
    whatsapp_url  text,
    instagram_url text,
    center        geography(Point, 4326),
    payload       jsonb,
    updated_at    timestamptz not null default now(),
    deleted_at    timestamptz
);

-- ── beach (relational child of community, parent of nests/markers/patrols) ─────────────────────
create table if not exists beach (
    id           text primary key,
    community_id text not null references community(id) on delete cascade deferrable initially deferred,
    name         text not null,
    city         text,
    lat          double precision,
    lng          double precision,
    center       geography(Point, 4326),
    boundary     geography(Polygon, 4326),
    leader_name  text,
    leader_avatar text default '🐢',
    payload      jsonb,
    updated_at   timestamptz not null default now(),
    deleted_at   timestamptz
);
create index if not exists beach_community_idx on beach (community_id);
create index if not exists beach_center_gix    on beach using gist (center);

-- moddatetime + geog triggers for the new parents
do $$
declare t text;
begin
    foreach t in array array['community','beach'] loop
        execute format('drop trigger if exists %I_moddatetime_trg on %I', t, t);
        execute format(
            'create trigger %I_moddatetime_trg before update on %I for each row execute function extensions.moddatetime(updated_at)',
            t, t
        );
    end loop;
end $$;

-- beach.center derived from lat/lng on write (client sends doubles; DB builds geography — offline-friendly)
create or replace function beach_set_center() returns trigger as $$
begin
    if new.lat is not null and new.lng is not null then
        new.center := ST_SetSRID(ST_MakePoint(new.lng, new.lat), 4326)::geography;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists beach_center_trg on beach;
create trigger beach_center_trg before insert or update on beach
    for each row execute function beach_set_center();

-- ── nests: link to community directly (beach_id already present) ───────────────────────────────
alter table nests add column if not exists community_id text;

-- ── seed reference data (matches the app's seedState) so FKs are always satisfiable ────────────
insert into community (id, slug, name, tagline, country, website_url, whatsapp_url, instagram_url)
values (
    'gazipasa-caretta', 'gazipasa-caretta', 'Gazipaşa Caretta', 'Protecting loggerheads & sand lilies',
    'TR', 'https://carettafriends.com',
    'https://chat.whatsapp.com/gazipasa-caretta',
    'https://instagram.com/gazipasa_caretta_ve_kumzambagi'
)
on conflict (id) do update set
    name = excluded.name, tagline = excluded.tagline, website_url = excluded.website_url,
    whatsapp_url = excluded.whatsapp_url, instagram_url = excluded.instagram_url;

insert into beach (id, community_id, name, city, lat, lng, leader_name, leader_avatar) values
    ('bidibidi', 'gazipasa-caretta', 'Bıdı Bıdı', 'Gazipaşa', 36.2691, 32.3108, 'Mert', '🦊'),
    ('selinus',  'gazipasa-caretta', 'Selinus',  'Gazipaşa', 36.2760, 32.2980, 'Lena', '🐬')
on conflict (id) do update set
    community_id = excluded.community_id, name = excluded.name, city = excluded.city,
    lat = excluded.lat, lng = excluded.lng, leader_name = excluded.leader_name;

-- backfill community_id on existing nests via their beach
update nests n set community_id = b.community_id
from beach b where n.beach_id = b.id and n.community_id is null;

-- ── foreign keys between aggregates (DEFERRABLE + NOT VALID → legacy-safe, offline-order-safe) ──
-- ON DELETE SET NULL: deleting a beach must not cascade-delete nests. Deferrable: a batched
-- (beach + its nests) insert inside one txn resolves at COMMIT regardless of intra-batch order.
do $$
begin
    begin
        alter table nests add constraint nests_beach_fk
            foreign key (beach_id) references beach(id) on delete set null deferrable initially deferred not valid;
    exception when duplicate_object then null; end;
    begin
        alter table nests add constraint nests_community_fk
            foreign key (community_id) references community(id) on delete set null deferrable initially deferred not valid;
    exception when duplicate_object then null; end;

    -- markers gains beach_id (aggregate placed on a beach)
    begin alter table markers add column if not exists beach_id text; exception when others then null; end;
    begin
        alter table markers add constraint markers_beach_fk
            foreign key (beach_id) references beach(id) on delete set null deferrable initially deferred not valid;
    exception when duplicate_object then null; end;

    begin
        alter table patrols add constraint patrols_beach_fk
            foreign key (beach_id) references beach(id) on delete set null deferrable initially deferred not valid;
    exception when duplicate_object then null; end;
end $$;

-- validate now that parents are seeded (existing rows reference bidibidi/selinus which exist)
do $$
begin
    begin alter table nests   validate constraint nests_beach_fk;     exception when others then null; end;
    begin alter table nests   validate constraint nests_community_fk; exception when others then null; end;
    begin alter table markers validate constraint markers_beach_fk;   exception when others then null; end;
    begin alter table patrols validate constraint patrols_beach_fk;   exception when others then null; end;
end $$;

-- ── indexes for incremental sync (updated_at,id keyset) + owner partition + FK joins ──────────
create index if not exists nests_sync_idx        on nests   (updated_at, id);
create index if not exists nests_owner_sync_idx  on nests   (owner_id, updated_at, id);
create index if not exists nests_community_idx    on nests   (community_id);
create index if not exists nests_live_idx         on nests   (deleted_at) where deleted_at is null;
create index if not exists markers_sync_idx      on markers (updated_at, id);
create index if not exists markers_beach_idx      on markers (beach_id);
create index if not exists patrols_sync_idx      on patrols (updated_at, id);

-- ── spatial RPC: "nests near me" (ST_DWithin is index-accelerated; <-> gives KNN order) ───────
create or replace function nests_near(in_lat double precision, in_lng double precision, in_radius_m int default 1000)
returns setof nests language sql stable as $$
    select * from nests
    where geog is not null and deleted_at is null
      and ST_DWithin(geog, ST_MakePoint(in_lng, in_lat)::geography, in_radius_m)
    order by geog <-> ST_MakePoint(in_lng, in_lat)::geography
$$;

-- ── RLS for new parent tables (V1: trusted volunteer community, anon. Tighten by owner_id in V2). ──
alter table community enable row level security;
alter table beach     enable row level security;
do $$
begin
    begin create policy "v1 anon rw community" on community for all using (true) with check (true); exception when duplicate_object then null; end;
    begin create policy "v1 anon rw beach"     on beach     for all using (true) with check (true); exception when duplicate_object then null; end;
end $$;

grant usage on schema public to anon, authenticated;
grant all on all tables in schema public to anon, authenticated;

-- ── Realtime (optional latency optimization; the pull+push loop stays the reliable backbone) ──
do $$
begin
    alter publication supabase_realtime add table nests;
exception when duplicate_object then null; when others then null;
end $$;
