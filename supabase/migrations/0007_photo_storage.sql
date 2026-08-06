-- Caretta Friends — storage for nest photos.
--
-- Until now only nest METADATA synced: the photo files stayed in the app's private storage on the
-- phone that took them. On any other device a nest someone else logged had no picture — and a photo
-- of the sand is the whole point of "snap a photo & forget". This adds the bucket the client uploads
-- to and reads from.
--
-- Same trust model as the nest rows themselves (0003):
--  • READ  — any signed-in volunteer (including anonymous sign-ins). Photos are nest evidence shared
--            within the app, never public: nest GPS is a poacher target and the images carry it.
--  • WRITE — only under your own uid prefix (`<uid>/<nest-id>/<photo-id>.jpg`), so nobody can
--            overwrite another volunteer's evidence.
--  • DELETE — no policy at all, matching 0005: field records are not destroyed from the app.
-- Idempotent.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'nest-photos',
    'nest-photos',
    false,                                   -- private: served through an authenticated request
    15728640,                                -- 15 MB — a phone JPEG with room to spare
    array['image/jpeg', 'image/png', 'image/heic', 'image/heif']
)
on conflict (id) do update
    set public = excluded.public,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists nest_photos_select on storage.objects;
create policy nest_photos_select on storage.objects
    for select to authenticated
    using (bucket_id = 'nest-photos');

-- The first path segment is the uploader's uid; `owner` is filled in by Storage AFTER the check
-- runs, so the prefix is what the policy can rely on.
drop policy if exists nest_photos_insert on storage.objects;
create policy nest_photos_insert on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'nest-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists nest_photos_update on storage.objects;
create policy nest_photos_update on storage.objects
    for update to authenticated
    using (
        bucket_id = 'nest-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'nest-photos'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
