-- Caretta Friends — retire the pre-launch test data.
--
-- Everything recorded before 2026-08-07 was development: nests dropped by the iOS simulator in
-- Cupertino (11 400 km from Gazipaşa), empty records pinned to a beach centre because the photo had
-- no GPS, the same photo submitted three times before duplicate detection existed, and the walk-
-- throughs of the capture flow itself. Left in place it would be the first thing a volunteer sees
-- on the map.
--
-- One record survives: GZP-21, a real find by a Gazipaşa volunteer on 2026-08-06 with a live GPS
-- fix. Season data starts from it.
--
-- Soft delete, not DELETE — 0005 removed deletion from the app's reach on purpose, and the same
-- reasoning holds for us: the rows stay recoverable, and `deleted_at` is the tombstone the sync
-- layer already filters on (`selectLive`). Date-bounded rather than a list of ids, so re-running it
-- against a restored database is a no-op and nothing recorded after launch can ever be caught by it.
--
-- Caveat worth knowing: a device that already holds these nests locally keeps them — mergeNests()
-- is additive (local ∪ remote), so tombstones do not propagate to installed apps. Fresh installs
-- and every other volunteer see the clean state.
--
-- Idempotent.

update nests
   set deleted_at = now()
 where deleted_at is null
   and created_at < timestamptz '2026-08-07 00:00:00+00'
   and id <> 'a8942216-cdfc-489a-9db7-01c255a0f2f6';  -- GZP-21, the real one

-- A VIOLATION marker with an empty note at a beach centre, and two patrols of 0 m / 2 s.
update markers
   set deleted_at = now()
 where deleted_at is null
   and updated_at < timestamptz '2026-08-07 00:00:00+00';

update patrols
   set deleted_at = now()
 where deleted_at is null
   and created_at < timestamptz '2026-08-07 00:00:00+00';
