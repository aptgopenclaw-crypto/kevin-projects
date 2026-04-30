-- ============================================================
-- V60: 儀表板 Dashboard — 建立版面配置 2 張表
-- ============================================================
-- Phase 8: 儀表板模組
-- Tables: dashboard_layouts, dashboard_default_layouts
-- Design: SD-10-dashboard.md §1

-- ── 1. dashboard_layouts: 使用者個人版面 ────────────────────

CREATE TABLE dashboard_layouts (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    user_id      VARCHAR(50) NOT NULL,
    layout_json  JSONB       NOT NULL,           -- vue-grid-layout format: [{i,x,y,w,h,widget}]
    is_default   BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT uk_dashboard_layouts_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_dashboard_layouts_tenant ON dashboard_layouts (tenant_id);

COMMENT ON TABLE  dashboard_layouts IS '使用者個人儀表板版面配置';
COMMENT ON COLUMN dashboard_layouts.layout_json IS 'grid-layout-plus format: [{i, x, y, w, h, widget}]';

-- ── 2. dashboard_default_layouts: 預設版面範本 ──────────────

CREATE TABLE dashboard_default_layouts (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    role_type    VARCHAR(30),                    -- ADMIN / MANAGER / CHIEF / null=global
    layout_json  JSONB       NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_dashboard_default_tenant_role ON dashboard_default_layouts (tenant_id, role_type);

COMMENT ON TABLE  dashboard_default_layouts IS '儀表板預設版面範本 (依角色)';
COMMENT ON COLUMN dashboard_default_layouts.role_type IS 'ADMIN/MANAGER/CHIEF/null(全域預設)';
