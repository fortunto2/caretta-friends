-- Caretta Friends — photos moved to Cloudflare R2; retire the Supabase bucket added in 0007.
--
-- Supabase earns its keep as the sync database and the identity provider. Storing several megabytes
-- of JPEG per nest there is the wrong shape and the wrong bill, so the files went to R2 behind a
-- Worker that verifies the same Supabase access token (workers/photos). Nothing was ever uploaded
-- to this bucket — it existed for minutes between 0007 and this migration.
--
-- Only the policies are dropped here: Supabase refuses `delete from storage.buckets` ("Direct
-- deletion from storage tables is not allowed"), and removing the row needs the Storage API with a
-- service-role key. With no policies left, the empty private bucket is unreachable; delete it from
-- the dashboard (Storage → nest-photos) when convenient. Idempotent.

drop policy if exists nest_photos_select on storage.objects;
drop policy if exists nest_photos_insert on storage.objects;
drop policy if exists nest_photos_update on storage.objects;
