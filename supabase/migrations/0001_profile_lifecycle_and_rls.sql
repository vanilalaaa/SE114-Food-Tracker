-- 0001_profile_lifecycle_and_rls.sql
--
-- Profile lifecycle + row/column security for public.profile.
-- Run MANUALLY via Supabase Dashboard → SQL Editor (the Supabase CLI is not
-- configured for this project). The script is idempotent and safe to re-run.

-- 1. Schema adjustments to the existing public.profile (ALTER only) ------------

-- Ensure id references auth.users(id) ON DELETE CASCADE. Verify the actual
-- target table and delete action, not merely that some FK on id exists: a FK
-- pointing elsewhere or without CASCADE is dropped and recreated correctly.
do $$
declare
  fk record;
begin
  select c.conname, c.confrelid, c.confdeltype
    into fk
  from pg_constraint c
  join pg_class t on t.oid = c.conrelid
  join pg_namespace n on n.oid = t.relnamespace
  where n.nspname = 'public'
    and t.relname = 'profile'
    and c.contype = 'f'
    and array_length(c.conkey, 1) = 1
    and (select attname
         from pg_attribute
         where attrelid = t.oid and attnum = c.conkey[1]) = 'id'
  limit 1;

  if fk.conname is null then
    alter table public.profile
      add constraint profile_id_fkey
      foreign key (id) references auth.users (id) on delete cascade;
  elsif fk.confrelid <> 'auth.users'::regclass or fk.confdeltype <> 'c' then
    execute format('alter table public.profile drop constraint %I', fk.conname);
    alter table public.profile
      add constraint profile_id_fkey
      foreign key (id) references auth.users (id) on delete cascade;
  end if;
end $$;

-- display_name is optional: Google metadata may not carry a name, and the
-- trigger stores NULL rather than an empty string in that case.
alter table public.profile alter column display_name drop not null;

-- user_id becomes nullable (set during onboarding, not at signup).
alter table public.profile alter column user_id drop not null;

-- Onboarding flag.
alter table public.profile
  add column if not exists onboarding_completed boolean not null default false;

-- Existing users that already picked a user_id are clearly onboarded; the
-- column default would otherwise reset them. One-time backfill (re-runs touch 0 rows).
update public.profile
  set onboarding_completed = true
  where user_id is not null and onboarding_completed = false;

-- Cooldown bookkeeping for user_id changes.
alter table public.profile
  add column if not exists user_id_changed_at timestamptz;

-- 2. Case-insensitive uniqueness on user_id (the DB is the final authority) ----

-- Drop the legacy case-sensitive UNIQUE constraint on user_id from the base
-- schema; it conflicts with the case-insensitive index below. The auto-generated
-- name is unknown, so find it by introspection.
do $$
declare
  cname text;
begin
  select c.conname into cname
  from pg_constraint c
  join pg_class t on t.oid = c.conrelid
  join pg_namespace n on n.oid = t.relnamespace
  where n.nspname = 'public' and t.relname = 'profile' and c.contype = 'u'
    and array_length(c.conkey, 1) = 1
    and (select attname
         from pg_attribute
         where attrelid = t.oid and attnum = c.conkey[1]) = 'user_id';
  if cname is not null then
    execute format('alter table public.profile drop constraint %I', cname);
  end if;
end $$;

create unique index if not exists profile_user_id_unique_ci
  on public.profile (lower(trim(user_id)))
  where user_id is not null;

-- 3. Auto-create a minimal profile for every new auth user --------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  insert into public.profile (id, display_name, avatar_url, user_id, onboarding_completed)
  values (
    new.id,
    nullif(coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name'), ''),
    coalesce(new.raw_user_meta_data->>'avatar_url', new.raw_user_meta_data->>'picture'),
    null,
    false
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

revoke execute on function public.handle_new_user() from public;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 4. Enforce the 30-day cooldown on user_id changes ---------------------------
create or replace function public.enforce_user_id_change()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  -- Defense in depth: the trigger is already scoped to user_id changes below.
  if old.user_id is distinct from new.user_id then
    if old.user_id_changed_at is not null
       and now() - old.user_id_changed_at < interval '30 days' then
      raise exception 'user_id_cooldown_active'
        using errcode = 'P0001',
              hint = format('user_id was last changed at %s; cooldown is 30 days', old.user_id_changed_at);
    else
      new.user_id_changed_at = now();
    end if;
  end if;
  return new;
end;
$$;

revoke execute on function public.enforce_user_id_change() from public;

drop trigger if exists enforce_user_id_change on public.profile;
create trigger enforce_user_id_change
  before update of user_id on public.profile
  for each row
  when (old.user_id is distinct from new.user_id)
  execute function public.enforce_user_id_change();

-- 5./6. RLS + column privileges -----------------------------------------------
-- Column-level GRANT controls which columns authenticated may read/write;
-- the row-level POLICY pins writes to the caller's own row. onboarding_completed
-- is intentionally grantable so a user can read and flip it on their own row.
alter table public.profile enable row level security;

revoke all on table public.profile from anon, authenticated;

grant select (id, display_name, user_id, avatar_url, onboarding_completed)
  on public.profile to authenticated;
grant update (display_name, avatar_url, user_id, onboarding_completed)
  on public.profile to authenticated;

drop policy if exists profile_select_authenticated on public.profile;
create policy profile_select_authenticated on public.profile
  for select to authenticated using (true);

drop policy if exists profile_update_own on public.profile;
create policy profile_update_own on public.profile
  for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

-- No INSERT/DELETE grant: inserts come from handle_new_user(); deletes cascade
-- from auth.users. Sensitive columns (is_admin, is_banned, user_id_changed_at,
-- created_at) are neither selectable nor updatable by authenticated.

-- 7. Cooldown read-back RPC ---------------------------------------------------
-- user_id_changed_at is deliberately off the SELECT grant (so a user cannot see
-- when others changed their handle); expose only the caller's own remaining
-- cooldown via this function. NULL if no user_id set yet; interval '0' if elapsed.
create or replace function public.user_id_cooldown_remaining()
returns interval
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select greatest(interval '0', interval '30 days' - (now() - user_id_changed_at))
  from public.profile
  where id = auth.uid() and user_id_changed_at is not null;
$$;

revoke execute on function public.user_id_cooldown_remaining() from public;
grant execute on function public.user_id_cooldown_remaining() to authenticated;
