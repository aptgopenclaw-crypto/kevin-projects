-- V80: 智慧電表整合索引 (D32)
-- 電表設備透過 devices.circuit_id 關聯回路，不新建表

-- 加速回路查詢: 同回路所有設備
CREATE INDEX IF NOT EXISTS idx_devices_circuit_id
    ON devices (circuit_id)
    WHERE circuit_id IS NOT NULL;

-- 加速電表類型設備查詢
CREATE INDEX IF NOT EXISTS idx_devices_type_circuit
    ON devices (device_type, circuit_id)
    WHERE device_type = 'POWER_EQUIPMENT';
