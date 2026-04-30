-- ============================================================
-- V33: 設備表補充掛載位置欄位
-- 用途：路燈組合（燈桿→燈具/控制器）需記錄元件在桿上的安裝位置
-- ============================================================

ALTER TABLE devices ADD COLUMN IF NOT EXISTS mount_position VARCHAR(50);

COMMENT ON COLUMN devices.mount_position IS '在父設備上的掛載位置（如 ARM_1, ARM_2, CTRL_1）';
