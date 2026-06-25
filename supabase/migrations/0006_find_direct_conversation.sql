-- find_direct_conversation: resolve the existing non-group conversation shared by
-- the caller and p_friend in a single round-trip, replacing the client-side scan
-- that ran two selects per participation row (N+1). Returns the conversation id,
-- or null when the two users have no direct chat yet.
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.
create or replace function public.find_direct_conversation(p_friend uuid)
returns uuid
language sql
stable
security definer
set search_path = public, pg_temp
as $$
  select c.id
  from public.conversation c
  join public.conversation_participant me
    on me.conversation_id = c.id and me.user_id = auth.uid()
  join public.conversation_participant friend
    on friend.conversation_id = c.id and friend.user_id = p_friend
  where c.is_group = false
  limit 1;
$$;

revoke execute on function public.find_direct_conversation(uuid) from public;
grant execute on function public.find_direct_conversation(uuid) to authenticated;
