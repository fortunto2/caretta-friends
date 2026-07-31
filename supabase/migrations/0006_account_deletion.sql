-- Caretta Friends — in-app account deletion (App Store guideline 5.1.1(v)).
--
-- App Review rejected 1.0 (3): the app lets a volunteer create an account (anonymous sign-in, and
-- linkEmail upgrades it to a real email account), so it must also let them delete it from inside
-- the app.
--
-- This has to coexist with 0005_no_delete.sql, whose rule is that volunteers never delete field
-- records — a nest is a scientific observation and the community's, not the finder's. So deleting
-- an account does NOT delete the nests, markers and patrols it recorded: those are DETACHED
-- (owner_id → null) and stay on the shared map, while everything that identifies the person — the
-- auth user and their profile row — is removed for good.
--
-- Runs as SECURITY DEFINER because a client holding only its own JWT cannot touch auth.users, and
-- 0005 deliberately leaves no DELETE policy on the data tables. The function only ever acts on
-- auth.uid(), so a caller can delete themselves and nobody else.
-- Idempotent.

create or replace function public.delete_account()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
    uid uuid := auth.uid();
begin
    if uid is null then
        raise exception 'delete_account: no authenticated user';
    end if;

    -- Keep the conservation record, detach it from the person.
    update nests   set owner_id = null where owner_id = uid;
    update markers set owner_id = null where owner_id = uid;
    update patrols set owner_id = null where owner_id = uid;

    -- profiles.id is text (device id in V1, auth uid once signed in) — match on the text form.
    delete from profiles where id = uid::text;

    -- Removes the identity itself: email, anonymous user, sessions and refresh tokens.
    delete from auth.users where id = uid;
end;
$$;

comment on function public.delete_account() is
    'Deletes the calling user''s account (auth.users + profile). Field records are kept but '
    'anonymised (owner_id → null), per the project rule that observations are never destroyed.';

revoke all on function public.delete_account() from public, anon;
grant execute on function public.delete_account() to authenticated;
