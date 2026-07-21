-- Caretta Friends — geo parity for markers (landmarks / trash / predator signs / crawls).
-- Store every geo object as a real PostGIS geography(Point) with a GiST index (not just lat/lng),
-- so the map can draw fast and analytics can run spatially: markers-near, density, per-beach counts,
-- point-in-polygon against beach.boundary. Client keeps sending plain lat/lng; DB derives geography.
-- Idempotent.

alter table markers add column if not exists geog geography(Point, 4326);
create index if not exists markers_geog_gix on markers using gist (geog);

create or replace function markers_set_geog() returns trigger as $$
begin
    if new.lat is not null and new.lng is not null then
        new.geog := ST_SetSRID(ST_MakePoint(new.lng, new.lat), 4326)::geography;
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists markers_geog_trg on markers;
create trigger markers_geog_trg before insert or update on markers
    for each row execute function markers_set_geog();

-- backfill existing rows
update markers set geog = ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography
where geog is null and lat is not null and lng is not null;

-- markers near a point (landmarks/trash cleanup planning), index-accelerated
create or replace function markers_near(in_lat double precision, in_lng double precision, in_radius_m int default 1000)
returns setof markers language sql stable as $$
    select * from markers
    where geog is not null and deleted_at is null
      and ST_DWithin(geog, ST_MakePoint(in_lng, in_lat)::geography, in_radius_m)
    order by geog <-> ST_MakePoint(in_lng, in_lat)::geography
$$;

-- analytics: live (non-excavated, non-deleted) nest counts + hatch status per beach, for dashboards/maps
create or replace view beach_nest_stats as
select b.id as beach_id, b.name, b.community_id,
       count(n.*) filter (where n.deleted_at is null) as nests_total,
       count(n.*) filter (where n.deleted_at is null and n.status = 'INCUBATING') as incubating,
       count(n.*) filter (where n.deleted_at is null and n.status in ('HATCHING','HATCHED','EXCAVATED')) as hatched
from beach b left join nests n on n.beach_id = b.id
group by b.id, b.name, b.community_id;

grant select on beach_nest_stats to authenticated;
