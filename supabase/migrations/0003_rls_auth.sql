-- Caretta Friends — tighten RLS for authenticated (incl. anonymous) users.
--
-- Model: every device signs in (anonymous sign-in → role 'authenticated', stable auth.uid()).
--  • Nest GPS is SENSITIVE (poacher target) → SELECT is authenticated-only, NEVER public/anon-key.
--    A shared map among signed-in volunteers, not a public scrape target.
--  • Writes are owner-scoped: you may only insert/update/delete rows whose owner_id = your auth.uid().
--  • community/beach are reference data → readable by authenticated, writable by any authenticated
--    volunteer in V1 (so the app can upsert the seeded rows); tighten to admins/beach-leaders in V2.
--
-- Anonymous users use the 'authenticated' role (Supabase), so `to authenticated` covers them; the
-- raw anon key alone (no sign-in) is role 'anon' and gets NOTHING here. Idempotent.

-- ── drop the permissive V1 policies (using(true) for everyone) ────────────────────────────────
drop policy if exists "v1 anon read/write profiles" on profiles;
drop policy if exists "v1 anon read/write nests"    on nests;
drop policy if exists "v1 anon read/write markers"  on markers;
drop policy if exists "v1 anon read/write patrols"  on patrols;
drop policy if exists "v1 anon rw community" on community;
drop policy if exists "v1 anon rw beach"     on beach;

-- ── owner-scoped aggregate roots: nests / markers / patrols ───────────────────────────────────
do $$
declare t text;
begin
    foreach t in array array['nests','markers','patrols'] loop
        execute format('drop policy if exists %I_select on %I', t, t);
        execute format('drop policy if exists %I_insert on %I', t, t);
        execute format('drop policy if exists %I_update on %I', t, t);
        execute format('drop policy if exists %I_delete on %I', t, t);
        -- shared map: any signed-in volunteer can READ all rows
        execute format('create policy %I_select on %I for select to authenticated using (true)', t, t);
        -- but only create/edit/delete your OWN rows (owner_id = your auth.uid())
        execute format('create policy %I_insert on %I for insert to authenticated with check (auth.uid() = owner_id)', t, t);
        execute format('create policy %I_update on %I for update to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id)', t, t);
        execute format('create policy %I_delete on %I for delete to authenticated using (auth.uid() = owner_id)', t, t);
    end loop;
end $$;

-- ── reference data: community / beach (V1 = any authenticated volunteer; V2 = admins/leaders) ──
do $$
declare t text;
begin
    foreach t in array array['community','beach'] loop
        execute format('drop policy if exists %I_select on %I', t, t);
        execute format('drop policy if exists %I_write on %I', t, t);
        execute format('create policy %I_select on %I for select to authenticated using (true)', t, t);
        execute format('create policy %I_write on %I for all to authenticated using (true) with check (true)', t, t);
    end loop;
end $$;

-- ── profiles: your own row only (id = auth.uid()); readable by authenticated ───────────────────
drop policy if exists profiles_select on profiles;
drop policy if exists profiles_write  on profiles;
create policy profiles_select on profiles for select to authenticated using (true);
create policy profiles_write  on profiles for all to authenticated using (auth.uid()::text = id) with check (auth.uid()::text = id);

-- Note: pre-auth rows with owner_id IS NULL become read-only (no one's auth.uid() = null) — expected
-- for legacy/test data. New rows written by the signed-in app carry the volunteer's owner_id.
