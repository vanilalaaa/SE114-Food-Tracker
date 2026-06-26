-- 0002_login_helpers.sql
--
-- Login-by-user_id support + a pre-auth user_id availability probe.
-- Both functions are security definer and granted to `anon` because they run
-- during the login / register flows, before the caller has an authenticated
-- session. RLS hides public.profile from anon, and auth.users.email is never
-- exposed to clients, so a SQL-level definer function is the only safe path.

-- 1. Resolve the email registered for a user_id (login by handle) -------------
-- Case-insensitive match. Returns NULL when no profile matches; the app maps
-- NULL to InvalidCredentials so it never reveals whether a handle exists.
create or replace function public.email_for_user_id(p_user_id text)
returns text
language sql
stable
security definer
set search_path = public, auth, pg_temp
as $$
  select u.email
  from public.profile p
  join auth.users u on u.id = p.id
  where lower(trim(p.user_id)) = lower(trim(p_user_id))
  limit 1;
$$;

revoke execute on function public.email_for_user_id(text) from public;
grant execute on function public.email_for_user_id(text) to anon, authenticated;

-- 2. user_id availability probe (UX hint during register/onboarding) ----------
-- The register screen runs before authentication, so it cannot SELECT from the
-- RLS-protected profile table directly. This definer function returns only a
-- boolean (no row data leaks), and the DB's case-insensitive unique index
-- remains the final authority on submit.
create or replace function public.user_id_available(p_user_id text)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select not exists (
    select 1
    from public.profile
    where lower(trim(user_id)) = lower(trim(p_user_id))
  );
$$;

revoke execute on function public.user_id_available(text) from public;
grant execute on function public.user_id_available(text) to anon, authenticated;
