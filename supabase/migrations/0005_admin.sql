-- 0005_admin.sql
--
-- In-app admin capability: lifecycle flags, admin RPCs, self-flag helpers, and
-- admin read policies for public.profile / public.report.
-- Run MANUALLY via Supabase Dashboard → SQL Editor (the Supabase CLI is not
-- configured for this project). The script is idempotent and safe to re-run.
--
-- Numbered 0005 (not 0003) because 0003/0004 are already taken by the feed
-- migrations; the original task referenced 0003_admin.sql before those landed.
--
-- SECURITY MODEL (see Admin_rules.md): hiding the admin UI is not security.
-- Every admin action is a `security definer` function gated on `is_admin`; the
-- additive RLS policies below let admins read what normal users can't. The
-- column-level GRANTs from 0001 still hide is_admin / is_banned / deleted_at from
-- `authenticated`, so even an admin cannot SELECT those columns directly — admin
-- reads of sensitive fields go through the definer RPCs (admin_list_users etc.),
-- and a user's own flags are exposed only via am_i_admin() / am_i_active().

-- 1. Lifecycle columns on public.profile ------------------------------------
-- May already exist from the base schema (0001 references is_admin/is_banned as
-- existing "sensitive columns"); guarded so re-runs and partial schemas are safe.
alter table public.profile
  add column if not exists is_admin boolean not null default false;
alter table public.profile
  add column if not exists is_banned boolean not null default false;
alter table public.profile
  add column if not exists deleted_at timestamptz;
alter table public.profile
  add column if not exists created_at timestamptz not null default now();

-- report.created_at is required by admin_list_reports (the "time" column) and by
-- ordering; guarded in case the base report table predates it.
alter table public.report
  add column if not exists created_at timestamptz not null default now();

-- 2. Self-flag helpers --------------------------------------------------------
-- The client reads only its OWN flags to toggle UI; the value is re-checked
-- server-side by every admin RPC. These are NOT admin-gated (a non-admin must be
-- able to learn it is not an admin → false). security definer so they bypass the
-- column GRANTs that hide is_admin / is_banned / deleted_at from authenticated.

-- True only when the current session's profile has is_admin = true.
create or replace function public.am_i_admin()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select coalesce((select is_admin from public.profile where id = auth.uid()), false);
$$;

revoke execute on function public.am_i_admin() from public;
grant execute on function public.am_i_admin() to authenticated;

-- False when the current user is banned or soft-deleted (or has no profile row);
-- the app's session guard signs such accounts out on the next session check.
create or replace function public.am_i_active()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select coalesce(
    (select (is_banned is not true) and (deleted_at is null)
       from public.profile where id = auth.uid()),
    false);
$$;

revoke execute on function public.am_i_active() from public;
grant execute on function public.am_i_active() to authenticated;

-- 3. Admin action RPCs --------------------------------------------------------
-- Each starts with the admin gate from Admin_rules.md. A non-admin who reaches
-- these (e.g. via the leaked secret code) is rejected with 'not_authorized',
-- which the app maps to a message.

-- Ban / unban a user.
create or replace function public.admin_set_ban(p_target uuid, p_banned boolean)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  update public.profile set is_banned = p_banned where id = p_target;
  if not found then
    raise exception 'target_not_found';
  end if;
end;
$$;

revoke execute on function public.admin_set_ban(uuid, boolean) from public;
grant execute on function public.admin_set_ban(uuid, boolean) to authenticated;

-- Soft-delete / restore a user (deleted_at timestamp).
create or replace function public.admin_set_deleted(p_target uuid, p_deleted boolean)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  update public.profile
  set deleted_at = case when p_deleted then now() else null end
  where id = p_target;
  if not found then
    raise exception 'target_not_found';
  end if;
end;
$$;

revoke execute on function public.admin_set_deleted(uuid, boolean) from public;
grant execute on function public.admin_set_deleted(uuid, boolean) to authenticated;

-- Resolve / dismiss a report. Any other status is rejected.
create or replace function public.admin_resolve_report(p_report uuid, p_status text)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  if p_status not in ('resolved', 'dismissed') then
    raise exception 'invalid_status';
  end if;

  update public.report set status = p_status where id = p_report;
  if not found then
    raise exception 'report_not_found';
  end if;
end;
$$;

revoke execute on function public.admin_resolve_report(uuid, text) from public;
grant execute on function public.admin_resolve_report(uuid, text) to authenticated;

-- 4. Admin read RPCs ----------------------------------------------------------
-- These return columns the column-level GRANTs hide from authenticated, so they
-- are the only path for the admin UI to read is_banned / deleted_at / handles.

-- Paginated user list with optional case-insensitive search on user_id or
-- display_name (empty/NULL search returns everyone).
create or replace function public.admin_list_users(
  p_search text,
  p_limit int,
  p_offset int
)
returns table (
  id uuid,
  display_name text,
  user_id text,
  avatar_url text,
  is_banned boolean,
  deleted_at timestamptz,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  return query
    select p.id, p.display_name, p.user_id, p.avatar_url,
           coalesce(p.is_banned, false), p.deleted_at, p.created_at
    from public.profile p
    where p_search is null
       or btrim(p_search) = ''
       or p.user_id ilike '%' || p_search || '%'
       or p.display_name ilike '%' || p_search || '%'
    order by p.created_at desc nulls last
    limit greatest(coalesce(p_limit, 20), 0)
    offset greatest(coalesce(p_offset, 0), 0);
end;
$$;

revoke execute on function public.admin_list_users(text, int, int) from public;
grant execute on function public.admin_list_users(text, int, int) to authenticated;

-- Dashboard counters in one round-trip.
create or replace function public.admin_dashboard_stats()
returns table (
  total_users bigint,
  banned_count bigint,
  deleted_count bigint,
  pending_reports bigint
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  return query
    select
      (select count(*) from public.profile),
      (select count(*) from public.profile where is_banned = true),
      (select count(*) from public.profile where deleted_at is not null),
      (select count(*) from public.report where status = 'pending');
end;
$$;

revoke execute on function public.admin_dashboard_stats() from public;
grant execute on function public.admin_dashboard_stats() to authenticated;

-- Paginated reports joined with reporter and target handles; optional status
-- filter (empty/NULL returns all statuses).
create or replace function public.admin_list_reports(
  p_status text,
  p_limit int,
  p_offset int
)
returns table (
  id uuid,
  reporter_id uuid,
  reporter_user_id text,
  reporter_display_name text,
  target_id uuid,
  target_user_id text,
  target_display_name text,
  reason text,
  status text,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  return query
    select r.id, r.reporter_id, rep.user_id, rep.display_name,
           r.target_id, tgt.user_id, tgt.display_name,
           r.reason, r.status, r.created_at
    from public.report r
    left join public.profile rep on rep.id = r.reporter_id
    left join public.profile tgt on tgt.id = r.target_id
    where p_status is null
       or btrim(p_status) = ''
       or r.status = p_status
    order by r.created_at desc nulls last
    limit greatest(coalesce(p_limit, 20), 0)
    offset greatest(coalesce(p_offset, 0), 0);
end;
$$;

revoke execute on function public.admin_list_reports(text, int, int) from public;
grant execute on function public.admin_list_reports(text, int, int) to authenticated;

-- 5. Admin RLS policies (additive) -------------------------------------------
-- Postgres ORs multiple permissive policies, so these sit alongside the existing
-- user policies. am_i_admin() is security definer, so referencing profile inside
-- a policy ON profile does not recurse through RLS.

drop policy if exists admin_select_all_profiles on public.profile;
create policy admin_select_all_profiles on public.profile
  for select to authenticated
  using (public.am_i_admin());

alter table public.report enable row level security;

drop policy if exists admin_all_reports on public.report;
create policy admin_all_reports on public.report
  for all to authenticated
  using (public.am_i_admin())
  with check (public.am_i_admin());

-- 6. Session-guard note -------------------------------------------------------
-- Banned / soft-deleted users are rejected at the SESSION layer: the client
-- session guard calls am_i_active() on session resolve / app resume and signs
-- them out. Broadly rewriting every table's RLS to also exclude banned users
-- from writes is intentionally out of scope for this migration (separate change);
-- this file focuses on admin capability + admin read access.
