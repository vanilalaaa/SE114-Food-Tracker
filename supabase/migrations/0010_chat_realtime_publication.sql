-- Enable Postgres CDC (realtime postgres_changes) for the chat tables. The open chat already
-- gets messages instantly via channel broadcast, but the CONVERSATION LIST relies on CDC: the
-- global listener writes incoming messages to Room so the list reorders + shows unread live.
-- If these tables aren't in the supabase_realtime publication, that listener never fires and the
-- list only updates on pull-to-refresh.
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.
do $$
declare
  t text;
begin
  foreach t in array array['message', 'conversation', 'conversation_participant']
  loop
    if not exists (
      select 1 from pg_publication_tables
      where pubname = 'supabase_realtime'
        and schemaname = 'public'
        and tablename = t
    ) then
      execute format('alter publication supabase_realtime add table public.%I', t);
    end if;
  end loop;
end $$;
