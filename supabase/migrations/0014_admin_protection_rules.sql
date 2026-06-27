-- FILE TỔNG HỢP TOÀN BỘ CÁC QUY TẮC BẢO VỆ ADMIN
--
--   A. Bảo vệ chéo: Không cho tự thao tác trên chính mình hoặc Admin khác.
--   B. Khóa tài khoản: Khóa vĩnh viễn KHÔNG giới hạn 30 ngày, mở khóa bất kỳ lúc nào.
--   C. Phân quyền chặt chẽ: KHÔNG cho phép biến một tài khoản đang bị khóa vĩnh viễn thành Admin.
--   D. Xóa tài khoản: Chờ ân hạn 30 ngày (Soft-delete). Quá 30 ngày tự động xóa sổ khỏi DB.

-- ─── 1. Khởi tạo các cột lưu mốc thời gian ─────────────────────────────────────
alter table public.profile
  add column if not exists deletion_expires_at timestamptz;

alter table public.profile
  add column if not exists last_banned_at timestamptz;


-- ─── 2. Hàm admin_set_ban (Khóa/Mở khóa tài khoản) ───────────────────────────
drop function if exists public.admin_set_ban(uuid, boolean, bigint);
create or replace function public.admin_set_ban(
  p_target           uuid,
  p_banned           boolean,
  p_duration_seconds bigint default null
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_target public.profile%rowtype;
begin
  -- Bảo vệ: Chỉ Admin mới được gọi
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  -- Bảo vệ: Không tự khóa chính mình
  if p_target = auth.uid() then
    raise exception 'self_action';
  end if;

  select * into v_target from public.profile where id = p_target;
  if not found then
    raise exception 'target_not_found';
  end if;

  -- Bảo vệ: Không được phép khóa một Admin khác
  if coalesce(v_target.is_admin, false) then
    raise exception 'target_is_admin';
  end if;

  if p_banned then
    -- Thực hiện khóa tài khoản
    update public.profile
    set is_banned     = (p_duration_seconds is null),
        banned_until  = case
                          when p_duration_seconds is null or p_duration_seconds <= 0 then null
                          else now() + make_interval(secs => p_duration_seconds)
                        end,
        ban_count     = ban_count + 1,
        last_banned_at = now()
    where id = p_target;
  else
    -- Mở khóa: LUÔN LUÔN cho phép mở, gỡ bỏ giới hạn 30 ngày cũ
    update public.profile
    set is_banned    = false,
        banned_until = null
    where id = p_target;
  end if;
end;
$$;

revoke execute on function public.admin_set_ban(uuid, boolean, bigint) from public;
grant  execute on function public.admin_set_ban(uuid, boolean, bigint) to authenticated;


-- ─── 3. Hàm admin_set_deleted (Xóa mềm/Khôi phục tài khoản) ────────────────────
drop function if exists public.admin_set_deleted(uuid, boolean);
create or replace function public.admin_set_deleted(
  p_target  uuid,
  p_deleted boolean
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_target public.profile%rowtype;
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  if p_target = auth.uid() then
    raise exception 'self_action';
  end if;

  select * into v_target from public.profile where id = p_target;
  if not found then
    raise exception 'target_not_found';
  end if;

  if coalesce(v_target.is_admin, false) then
    raise exception 'target_is_admin';
  end if;

  if p_deleted then
    -- Đặt lịch hết hạn đúng 30 ngày sau
    update public.profile
    set deleted_at          = now(),
        deletion_expires_at = now() + interval '30 days'
    where id = p_target;
  else
    -- Khôi phục: Chặn đứng nếu đồng hồ đã chạy quá 30 ngày
    if v_target.deletion_expires_at is not null
       and now() > v_target.deletion_expires_at
    then
      raise exception 'deletion_expired';
    end if;

    update public.profile
    set deleted_at          = null,
        deletion_expires_at = null
    where id = p_target;
  end if;
end;
$$;

revoke execute on function public.admin_set_deleted(uuid, boolean) from public;
grant  execute on function public.admin_set_deleted(uuid, boolean) to authenticated;


-- ─── 4. Hàm admin_set_admin (Cấp/Hạ quyền Admin - ĐÃ BỔ SUNG LOGIC CHẶN) ───────
drop function if exists public.admin_set_admin(uuid, boolean);
create or replace function public.admin_set_admin(
  p_target uuid,
  p_admin  boolean
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_target public.profile%rowtype;
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  if p_target = auth.uid() then
    raise exception 'self_action';
  end if;

  select * into v_target from public.profile where id = p_target;
  if not found then
    raise exception 'target_not_found';
  end if;

  -- ĐOẠN ĐƯỢC THÊM MỚI: Nếu định cấp quyền Admin nhưng tài khoản đó đang bị KHÓA VĨNH VIỄN -> Chặn!
  if p_admin
     and coalesce(v_target.is_banned, false)
     and v_target.banned_until is null
  then
    raise exception 'banned_user_cannot_be_admin';
  end if;

  update public.profile set is_admin = p_admin where id = p_target;
end;
$$;

revoke execute on function public.admin_set_admin(uuid, boolean) from public;
grant  execute on function public.admin_set_admin(uuid, boolean) to authenticated;


-- ─── 5. Hàm admin_purge_expired_deletions (Dọn sạch hoàn toàn khỏi DB) ─────────
create or replace function public.admin_purge_expired_deletions()
returns int
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_count int;
begin
  if not exists (select 1 from public.profile
                 where id = auth.uid() and is_admin = true) then
    raise exception 'not_authorized';
  end if;

  -- Quét sạch tận gốc từ bảng auth.users (Tự động CASCADE sạch các bảng liên quan)
  delete from auth.users u
  using public.profile p
  where u.id = p.id
    and p.deleted_at is not null
    and p.deletion_expires_at is not null
    and now() > p.deletion_expires_at;

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke execute on function public.admin_purge_expired_deletions() from public;
grant  execute on function public.admin_purge_expired_deletions() to authenticated;


-- ─── 6. Hàm admin_list_users (Lấy danh sách hiển thị kèm thời hạn) ─────────────
drop function if exists public.admin_list_users(text, int, int);
create or replace function public.admin_list_users(
  p_search  text,
  p_limit   int,
  p_offset  int
)
returns table (
  id                  uuid,
  display_name        text,
  user_id             text,
  avatar_url          text,
  is_admin            boolean,
  is_banned           boolean,
  banned_until        timestamptz,
  ban_count           int,
  last_banned_at      timestamptz,
  deleted_at          timestamptz,
  deletion_expires_at timestamptz,
  created_at          timestamptz
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
    select p.id,
           p.display_name,
           p.user_id,
           p.avatar_url,
           coalesce(p.is_admin,    false),
           (coalesce(p.is_banned, false)
              or (p.banned_until is not null and p.banned_until > now())),
           p.banned_until,
           coalesce(p.ban_count, 0),
           p.last_banned_at,
           p.deleted_at,
           p.deletion_expires_at,
           p.created_at
    from public.profile p
    where p_search is null
       or btrim(p_search) = ''
       or p.user_id ilike '%' || p_search || '%'
       or p.display_name ilike '%' || p_search || '%'
    order by p.created_at desc nulls last
    limit  greatest(coalesce(p_limit,  20), 0)
    offset greatest(coalesce(p_offset,  0), 0);
end;
$$;

revoke execute on function public.admin_list_users(text, int, int) from public;
grant  execute on function public.admin_list_users(text, int, int) to authenticated;


-- ─── 7. Làm mới lại Cache hệ thống API Supabase ────────────────────────────────
notify pgrst, 'reload schema';