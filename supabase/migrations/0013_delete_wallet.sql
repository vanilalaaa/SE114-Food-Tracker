-- 1. XÓA CÁC RPC FUNCTIONS LIÊN QUAN ĐẾN VÍ (WALLET)
DROP FUNCTION IF EXISTS public.rpc_wallet_deposit(uuid, double precision, text) CASCADE;
DROP FUNCTION IF EXISTS public.rpc_wallet_withdraw(uuid, double precision, text) CASCADE;

-- 2. XÓA RÀNG BUỘC KHÓA NGOẠI VÀ CỘT wallet_id Ở BẢNG CONVERSATION
ALTER TABLE public.conversation
  DROP CONSTRAINT IF EXISTS fk_conversation_wallet CASCADE,
  DROP CONSTRAINT IF EXISTS conversations_wallet_id_fkey CASCADE;

ALTER TABLE public.conversation
  DROP COLUMN IF EXISTS wallet_id;

-- 3. XÓA RÀNG BUỘC KHÓA NGOẠI VÀ CỘT wallet_id Ở BẢNG ITEM
ALTER TABLE public.item
  DROP CONSTRAINT IF EXISTS fk_item_wallet CASCADE,
  DROP CONSTRAINT IF EXISTS item_wallet_id_fkey CASCADE;

ALTER TABLE public.item
  DROP COLUMN IF EXISTS wallet_id;

-- 4. XÓA BỎ HOÀN TOÀN CÁC BẢNG QUỸ NHÓM + TỰ ĐỘNG QUÉT SẠCH RLS POLICIES LIÊN QUAN
DROP TABLE IF EXISTS public.wallet_transaction CASCADE;
DROP TABLE IF EXISTS public.wallet_membership CASCADE;
DROP TABLE IF EXISTS public.group_wallet CASCADE;