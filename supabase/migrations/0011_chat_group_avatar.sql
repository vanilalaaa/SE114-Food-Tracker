-- Group conversations can have a custom avatar image (stored in the chat-images bucket).
alter table public.conversation
    add column if not exists avatar_url text;
