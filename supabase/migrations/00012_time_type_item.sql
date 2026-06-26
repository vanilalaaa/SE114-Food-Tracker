BEGIN;

  -- 1. Drop constraint cũ
  ALTER TABLE public.item
    DROP CONSTRAINT IF EXISTS item_time_type_check;

  -- 2. Cập nhật dữ liệu cũ TRƯỚC khi áp luật mới
  -- Chuyển toàn bộ "Tối cũ" (2) thành "Tối mới" (3)
  UPDATE public.item
    SET time_type = 3
    WHERE time_type = 2;

  -- 3. Thêm constraint mới sau khi dữ liệu đã được dọn sạch
  ALTER TABLE public.item
    ADD CONSTRAINT item_time_type_check
    CHECK (time_type = ANY (ARRAY[0, 1, 2, 3]));

COMMIT;