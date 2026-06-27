-- Enable Postgres CDC for feed and friendship screens.
-- Without these tables in supabase_realtime, postgresChangeFlow subscribers
-- connect successfully but never receive INSERT/UPDATE/DELETE events.

do $$
declare
  t text;
begin
  foreach t in array array['post', 'post_comment', 'post_like', 'friendship']
  loop
    if not exists (
      select 1 from pg_publication_tables
      where pubname = 'supabase_realtime'
        and schemaname = 'public'
        and tablename = t
    ) then
      execute format('alter publication supabase_realtime add table public.%I', t);
    end if;

    execute format('alter table public.%I replica identity full', t);
  end loop;
end $$;
