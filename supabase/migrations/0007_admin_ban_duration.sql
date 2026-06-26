-- 0007_admin_ban_duration.sql
--
-- Temporary bans (auto-expiring) + a lifetime ban counter on top of the boolean
-- ban from 0005_admin. A ban now carries an optional duration; permanent bans keep the
-- is_banned flag, temporary bans set banned_until and lift themselves once it passes.

-- 1. New lifecycle columns ----------------------------------------------------
alter table public.profile
  add column if not exists banned_until timestamptz;
alter table public.profile
  add column if not exists ban_count int not null default 0;

-- 2. Active check now honours temporary-ban expiry ---------------------------
create or replace function public.am_i_active()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select coalesce(
    (select (deleted_at is null)
        and (is_banned is not true)
        and (banned_until is null or banned_until <= now())
       from public.profile where id = auth.uid()),
    false);
$$;

revoke execute on function public.am_i_active() from public;
grant execute on function public.am_i_active() to authenticated;

-- 3. Ban / unban with optional duration --------------------------------------
-- p_duration_seconds NULL + p_banned true => permanent. A positive duration sets
-- a temporary ban. Any ban (p_banned true) increments ban_count; unban keeps it.
-- The 0005 two-arg signature is dropped so callers move to the duration-aware one.
drop function if exists public.admin_set_ban(uuid, boolean);
create or replace function public.admin_set_ban(
  p_target uuid,
  p_banned boolean,
  p_duration_seconds bigint default null
)
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

  if p_banned then
    update public.profile
    set is_banned = (p_duration_seconds is null),
        banned_until = case
          when p_duration_seconds is null or p_duration_seconds <= 0 then null
          else now() + make_interval(secs => p_duration_seconds)
        end,
        ban_count = ban_count + 1
    where id = p_target;
  else
    update public.profile
    set is_banned = false,
        banned_until = null
    where id = p_target;
  end if;

  if not found then
    raise exception 'target_not_found';
  end if;
end;
$$;

revoke execute on function public.admin_set_ban(uuid, boolean, bigint) from public;
grant execute on function public.admin_set_ban(uuid, boolean, bigint) to authenticated;

-- 4. Admin reads expose the new fields ---------------------------------------
-- is_banned is returned as "currently banned" (permanent OR not-yet-expired) so the
-- UI badge stays correct; banned_until distinguishes permanent (null) from temporary,
-- and ban_count is the lifetime counter. RETURNS TABLE shape changed -> drop first.
drop function if exists public.admin_list_users(text, int, int);
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
  is_admin boolean,
  is_banned boolean,
  banned_until timestamptz,
  ban_count int,
  deleted_at timestamptz,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
  if not exists (select 1 from public.profile pr
                 where pr.id = auth.uid() and pr.is_admin = true) then
    raise exception 'not_authorized';
  end if;

  return query
    select p.id, p.display_name, p.user_id, p.avatar_url,
           coalesce(p.is_admin, false),
           (coalesce(p.is_banned, false) or (p.banned_until is not null and p.banned_until > now())),
           p.banned_until,
           coalesce(p.ban_count, 0),
           p.deleted_at, p.created_at
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

-- Reports gain the target's lifetime ban count (and current ban window) so the
-- admin sees a repeat offender straight from the report. Shape changed -> drop first.
drop function if exists public.admin_list_reports(text, int, int);
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
  target_ban_count int,
  target_banned_until timestamptz,
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
  if not exists (select 1 from public.profile pr
                 where pr.id = auth.uid() and pr.is_admin = true) then
    raise exception 'not_authorized';
  end if;

  return query
    select r.id, r.reporter_id, rep.user_id, rep.display_name,
           r.target_id, tgt.user_id, tgt.display_name,
           coalesce(tgt.ban_count, 0), tgt.banned_until,
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

-- Dashboard "banned" counter includes still-active temporary bans.
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
      (select count(*) from public.profile
         where is_banned = true or (banned_until is not null and banned_until > now())),
      (select count(*) from public.profile where deleted_at is not null),
      (select count(*) from public.report where status = 'pending');
end;
$$;

revoke execute on function public.admin_dashboard_stats() from public;
grant execute on function public.admin_dashboard_stats() to authenticated;

-- 5. Reload PostgREST schema cache -------------------------------------------
notify pgrst, 'reload schema';
