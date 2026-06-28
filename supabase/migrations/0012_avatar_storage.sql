-- Storage setup for profile avatars (bucket "avatars"). The app uploads to
-- avatars/<auth.uid()>/avatar.jpg via ImageRepository.uploadAvatar. Profile-avatar upload
-- fails ("có lỗi xảy ra") when this bucket / its policies aren't configured, while item images
-- work because the "items" bucket already has them — so this mirrors the standard own-folder setup.
--
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.

-- 1. Public bucket so Coil can load avatar URLs by public URL.
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do update set public = true;

-- 2. RLS policies on storage.objects, scoped to bucket 'avatars'.
-- Public read.
drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read" on storage.objects
  for select to public
  using (bucket_id = 'avatars');

-- Authenticated users may write only inside their own uid folder
-- (path = avatars/<uid>/avatar.jpg, so (storage.foldername(name))[1] must equal their uid).
drop policy if exists "avatars_insert_own" on storage.objects;
create policy "avatars_insert_own" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars_update_own" on storage.objects;
create policy "avatars_update_own" on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "avatars_delete_own" on storage.objects;
create policy "avatars_delete_own" on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);
