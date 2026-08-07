-- Caretta Friends — retire the last pre-launch record.
--
-- 0009 kept GZP-21 as the season's one real find. It isn't one: its coordinates put it in Pazarcı
-- Mahallesi, 875 m from the water, among apartment blocks and a hotel — the same spot as GZP-22 and
-- GZP-23, to within a metre. It was photographed in town like the rest of them, and it would be
-- refused today by the shoreline check that reads this same geometry.
--
-- So the season starts from an empty map, and the first nest on it will be one a volunteer actually
-- stood over. Soft delete, like everything else here — recoverable, and already filtered by the
-- sync layer.
--
-- Idempotent.

update nests
   set deleted_at = now()
 where deleted_at is null
   and id = 'a8942216-cdfc-489a-9db7-01c255a0f2f6';
