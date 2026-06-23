-- Soft-delete feed posts through an RPC so clients do not need direct UPDATE
-- access to the deleted columns. The function still enforces ownership with
-- auth.uid() before changing the row.
create or replace function public.soft_delete_post(p_post_id uuid)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then
    raise exception 'not_authenticated'
      using errcode = '28000';
  end if;

  update public.post
  set is_deleted = true,
      deleted_at = now()
  where id = p_post_id
    and author_id = auth.uid();

  if not found then
    raise exception 'post_not_found_or_not_author'
      using errcode = 'P0001';
  end if;
end;
$$;

revoke execute on function public.soft_delete_post(uuid) from public;
grant execute on function public.soft_delete_post(uuid) to authenticated;
