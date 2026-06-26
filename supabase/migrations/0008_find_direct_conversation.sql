-- find_direct_conversation: resolve the existing non-group conversation shared by
-- the caller and p_friend in a single round-trip, replacing the client-side scan
-- that ran two selects per participation row (N+1). Returns the conversation id,
-- or null when the two users have no direct chat yet.

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
  -- Deterministic tie-break so every caller converges on the same conversation if a
  -- duplicate-creation race ever produced more than one direct chat for the pair.
  order by c.id
  limit 1;
$$;

revoke execute on function public.find_direct_conversation(uuid) from public;
grant execute on function public.find_direct_conversation(uuid) to authenticated;
