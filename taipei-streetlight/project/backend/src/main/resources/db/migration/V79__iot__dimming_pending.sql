-- V79: 調光 result 欄位支援 PENDING 狀態 (D21)
-- 放寬 dimming_logs.result 欄位以接受 PENDING 值
-- PostgreSQL varchar 不需額外 DDL，此處保留為版本里程碑

-- DimmingLogRepository 需要的查詢索引
CREATE INDEX IF NOT EXISTS idx_dimming_logs_result_sent
    ON dimming_logs (result, sent_at)
    WHERE result = 'PENDING';
