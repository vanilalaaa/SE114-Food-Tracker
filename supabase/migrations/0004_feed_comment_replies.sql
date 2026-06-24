alter table public.post_comment
add column if not exists parent_comment_id uuid references public.post_comment(id) on delete cascade;

create index if not exists post_comment_parent_comment_id_idx
on public.post_comment(parent_comment_id);
