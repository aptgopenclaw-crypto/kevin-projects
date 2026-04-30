-- ============================================================
-- V58: KPI 績效管理 — 建立核心 4 張表
-- ============================================================
-- Phase 6: 績效管理模組
-- Tables: kpi_indicators, kpi_raw_data, kpi_results, kpi_periods
-- Design: SD-08-performance.md §1

-- ── 1. kpi_indicators: KPI 指標定義 ─────────────────────────

CREATE TABLE kpi_indicators (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    indicator_code  VARCHAR(50)  NOT NULL,
    indicator_name  VARCHAR(200) NOT NULL,
    category        VARCHAR(30)  NOT NULL,           -- MAINTENANCE / POWER / RESPONSE / QUALITY / CUSTOM
    formula_type    VARCHAR(10)  NOT NULL DEFAULT 'SPEL', -- SPEL / JS (JS 暫不啟用)
    formula         TEXT         NOT NULL,
    target_value    NUMERIC(10, 4),
    weight          NUMERIC(5, 2) DEFAULT 1.0,
    data_source     VARCHAR(30),                     -- IOT / REPAIR / MATERIAL / MANUAL
    unit            VARCHAR(20),
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / INACTIVE
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uk_kpi_indicators_code UNIQUE (tenant_id, indicator_code)
);

CREATE INDEX idx_kpi_indicators_tenant_status ON kpi_indicators (tenant_id, status);
CREATE INDEX idx_kpi_indicators_category      ON kpi_indicators (tenant_id, category);

COMMENT ON TABLE  kpi_indicators IS '績效指標定義';
COMMENT ON COLUMN kpi_indicators.formula_type IS 'SPEL=Spring Expression Language; JS=GraalJS (暫不啟用)';
COMMENT ON COLUMN kpi_indicators.data_source  IS '自動收集來源: IOT/REPAIR/MATERIAL; MANUAL=需手動匯入';

-- ── 2. kpi_raw_data: 績效原始數據 ──────────────────────────

CREATE TABLE kpi_raw_data (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    indicator_id   BIGINT      NOT NULL REFERENCES kpi_indicators(id),
    period_year    INT         NOT NULL,
    period_month   INT         NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    contract_id    BIGINT      REFERENCES contracts(id),
    raw_value      NUMERIC(15, 4) NOT NULL,
    source         VARCHAR(20)    NOT NULL,           -- AUTO / MANUAL_IMPORT
    imported_at    TIMESTAMP      NOT NULL DEFAULT now(),
    imported_by    VARCHAR(50)
);

-- partial unique indexes to handle nullable contract_id (D3)
CREATE UNIQUE INDEX uk_kpi_raw_data_with_contract
    ON kpi_raw_data (tenant_id, indicator_id, period_year, period_month, contract_id)
    WHERE contract_id IS NOT NULL;

CREATE UNIQUE INDEX uk_kpi_raw_data_without_contract
    ON kpi_raw_data (tenant_id, indicator_id, period_year, period_month)
    WHERE contract_id IS NULL;

CREATE INDEX idx_kpi_raw_data_period      ON kpi_raw_data (tenant_id, period_year, period_month);
CREATE INDEX idx_kpi_raw_data_indicator   ON kpi_raw_data (indicator_id);

COMMENT ON TABLE  kpi_raw_data IS '績效原始數據 (自動收集或手動匯入)';
COMMENT ON COLUMN kpi_raw_data.source IS 'AUTO=排程自動收集; MANUAL_IMPORT=手動匯入';

-- ── 3. kpi_results: 績效計算結果 ───────────────────────────

CREATE TABLE kpi_results (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    indicator_id   BIGINT      NOT NULL REFERENCES kpi_indicators(id),
    period_year    INT         NOT NULL,
    period_month   INT         NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    contract_id    BIGINT      REFERENCES contracts(id),
    result_value   NUMERIC(15, 4) NOT NULL,
    target_value   NUMERIC(10, 4),
    achievement    NUMERIC(8, 4),                     -- result / target * 100
    calculated_at  TIMESTAMP      NOT NULL DEFAULT now()
);

-- partial unique indexes to handle nullable contract_id (D3)
CREATE UNIQUE INDEX uk_kpi_results_with_contract
    ON kpi_results (tenant_id, indicator_id, period_year, period_month, contract_id)
    WHERE contract_id IS NOT NULL;

CREATE UNIQUE INDEX uk_kpi_results_without_contract
    ON kpi_results (tenant_id, indicator_id, period_year, period_month)
    WHERE contract_id IS NULL;

CREATE INDEX idx_kpi_results_period    ON kpi_results (tenant_id, period_year, period_month);
CREATE INDEX idx_kpi_results_indicator ON kpi_results (indicator_id);
CREATE INDEX idx_kpi_results_contract  ON kpi_results (contract_id);

COMMENT ON TABLE  kpi_results IS '績效計算結果';
COMMENT ON COLUMN kpi_results.achievement IS '達成率 = result_value / target_value * 100';

-- ── 4. kpi_periods: 期間管理 (含鎖定機制) ──────────────────

CREATE TABLE kpi_periods (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    period_year    INT          NOT NULL,
    period_month   INT          NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    locked         BOOLEAN      NOT NULL DEFAULT false,
    locked_at      TIMESTAMP,
    locked_by      VARCHAR(50),
    unlock_reason  TEXT,

    CONSTRAINT uk_kpi_periods UNIQUE (tenant_id, period_year, period_month)
);

CREATE INDEX idx_kpi_periods_tenant ON kpi_periods (tenant_id);

COMMENT ON TABLE  kpi_periods IS '績效期間管理 (鎖定/解鎖)';
COMMENT ON COLUMN kpi_periods.locked_by IS '執行鎖定/解鎖的使用者 username';
