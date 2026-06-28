-- Storage setup for chat message images + group avatars (bucket "chat-images"). The app uploads
-- to chat-images/<uuid>.jpg and shows them by public URL. Images fail to load when this bucket
-- isn't public / has no read policy. Mirrors the standard public-image bucket setup.
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.

-- 1. Public bucket so Coil can load image URLs.
insert into storage.buckets (id, name, public)
values ('chat-images', 'chat-images', true)
on conflict (id) do update set public = true;

-- 2. RLS policies on storage.objects scoped to bucket 'chat-images'.
-- Public read (URLs are unguessable UUIDs; same model as the existing flow).
drop policy if exists "chat_images_public_read" on storage.objects;
create policy "chat_images_public_read" on storage.objects
  for select to public
  using (bucket_id = 'chat-images');

-- Any authenticated user may upload/replace a chat image (paths aren't user-scoped).
drop policy if exists "chat_images_insert_auth" on storage.objects;
create policy "chat_images_insert_auth" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'chat-images');

drop policy if exists "chat_images_update_auth" on storage.objects;
create policy "chat_images_update_auth" on storage.objects
  for update to authenticated
  using (bucket_id = 'chat-images')
  with check (bucket_id = 'chat-images');
