-- Per-user read position for conversations. last_read_at was introduced in 0005 as
-- BIGINT epoch-millis to stay consistent with the millis-based Room model (message
-- created_at, conversation last_message_at are all BIGINT millis); this migration keeps
-- that type and adds the gated RPC clients call to advance their own marker.
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.

-- Defensive: the column already exists from 0005; keep this self-contained.
alter table public.conversation_participant
    add column if not exists last_read_at bigint not null default 0;

-- mark_conversation_read: advance the caller's own read marker to "now" (epoch millis,
-- matching the client clock domain). Gated to auth.uid() so a user can only mark their
-- own participant row — clients never need direct UPDATE on conversation_participant.
create or replace function public.mark_conversation_read(p_conversation uuid)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if auth.uid() is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  update public.conversation_participant
  set last_read_at = (extract(epoch from now()) * 1000)::bigint
  where conversation_id = p_conversation
    and user_id = auth.uid();
end;
$$;

revoke execute on function public.mark_conversation_read(uuid) from public;
grant execute on function public.mark_conversation_read(uuid) to authenticated;
