-- Group conversations can have a custom avatar image (stored in the chat-images bucket).
-- Run MANUALLY via Supabase Dashboard -> SQL Editor (no CLI configured). Idempotent.
alter table public.conversation
    add column if not exists avatar_url text;
