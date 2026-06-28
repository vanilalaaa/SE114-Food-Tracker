-- 0015_chat_unread_counts.sql
--
-- Per-conversation unread count for the current user, so the conversation list can show an
-- exact "new messages" number for EVERY conversation — not just the ones whose messages have
-- been synced into the device (messages sync lazily on open, which is why the list previously
-- showed a bare dot for unopened-but-unread conversations).
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.
--
-- "Unread" = a message from someone else, newer than my last_read_at (epoch millis, the same
-- domain mark_conversation_read writes). Conversations with zero unread simply don't appear in
-- the result; the client treats a missing conversation as count 0.

create or replace function public.unread_message_counts()
returns table (conversation_id uuid, unread_count int)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select m.conversation_id, count(*)::int
  from public.message m
  join public.conversation_participant cp
    on cp.conversation_id = m.conversation_id
   and cp.user_id = auth.uid()
  where coalesce(m.sender_id::text, '') <> auth.uid()::text
    and (extract(epoch from m.created_at) * 1000)::bigint > coalesce(cp.last_read_at, 0)
  group by m.conversation_id;
$$;

revoke execute on function public.unread_message_counts() from public;
grant execute on function public.unread_message_counts() to authenticated;

notify pgrst, 'reload schema';
