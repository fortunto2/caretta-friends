-- Caretta Friends — remove DELETE from the app's reach.
--
-- User rule: volunteers must NOT be able to delete data — not others' nests/markers, not even their
-- OWN after it's recorded. Only admins may delete (for now, via the backend / Supabase dashboard,
-- which bypasses RLS). So we simply drop the owner-scoped DELETE policies: with RLS on and no DELETE
-- policy, no anon/authenticated client can DELETE any row. INSERT/UPDATE stay owner-scoped; SELECT
-- stays authenticated. (Corrections happen via edits/updates, never deletion.)
-- Idempotent.

drop policy if exists nests_delete   on nests;
drop policy if exists markers_delete on markers;
drop policy if exists patrols_delete on patrols;

-- community/beach have a broad "for all" write policy (V1) that also covers DELETE — split it so
-- authenticated volunteers can insert/update reference data but NOT delete beaches/community.
do $$
declare t text;
begin
    foreach t in array array['community','beach'] loop
        execute format('drop policy if exists %I_write on %I', t, t);
        execute format('create policy %I_insert on %I for insert to authenticated with check (true)', t, t);
        execute format('create policy %I_update on %I for update to authenticated using (true) with check (true)', t, t);
        -- no DELETE policy → deletes blocked for the app
    end loop;
end $$;
