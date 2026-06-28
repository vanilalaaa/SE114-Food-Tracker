alter table public.post_comment
add column if not exists edited_at timestamptz;
