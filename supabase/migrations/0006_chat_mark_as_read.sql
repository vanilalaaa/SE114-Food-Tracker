-- ── 1. SUPABASE: add last_message_at and snippet to conversation ──
ALTER TABLE public.conversation
    ADD COLUMN IF NOT EXISTS last_message_at BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_message_snippet TEXT;

-- Auto-update last_message_at whenever a message is inserted
CREATE OR REPLACE FUNCTION public.fn_update_conversation_last_message()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.conversation
    SET last_message_at      = EXTRACT(EPOCH FROM NEW.created_at) * 1000,
        last_message_snippet = CASE
            WHEN NEW.is_system THEN '📢 Tin nhắn hệ thống'
            WHEN NEW.body IS NOT NULL THEN LEFT(NEW.body, 80)
            ELSE NULL
        END
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_conversation_last_message ON public.message;
CREATE TRIGGER trg_update_conversation_last_message
    AFTER INSERT ON public.message
    FOR EACH ROW EXECUTE FUNCTION public.fn_update_conversation_last_message();

-- ── 2. SUPABASE: add last_read_at to conversation_participant ──
ALTER TABLE public.conversation_participant
    ADD COLUMN IF NOT EXISTS last_read_at BIGINT NOT NULL DEFAULT 0;

-- ── 3. QUICK BACKFILL (Run once to populate old data) ──
UPDATE public.conversation c
SET last_message_at = (
    SELECT COALESCE(MAX(EXTRACT(EPOCH FROM m.created_at) * 1000), 0)
    FROM public.message m
    WHERE m.conversation_id = c.id
),
    last_message_snippet = (
    SELECT CASE
        WHEN m.is_system THEN '📢 Tin nhắn hệ thống'
        WHEN m.body IS NOT NULL THEN LEFT(m.body, 80)
        ELSE NULL
    END
    FROM public.message m
    WHERE m.conversation_id = c.id
    ORDER BY m.created_at DESC
    LIMIT 1
);