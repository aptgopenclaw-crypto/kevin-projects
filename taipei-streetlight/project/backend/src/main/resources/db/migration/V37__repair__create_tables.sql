-- =============================================================
-- V35: 報修維護 — repair_tickets + repair_dispatches + ticket_attachments
-- 前置：V30(devices,circuits,contracts), V32(fault_tickets)
-- =============================================================

-- 05-1 報修維護工單
CREATE TABLE repair_tickets (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    ticket_number       VARCHAR(50)     NOT NULL,
    fault_ticket_id     BIGINT          REFERENCES fault_tickets(id),
    device_id           BIGINT          REFERENCES devices(id),
    circuit_id          BIGINT          REFERENCES circuits(id),
    contract_id         BIGINT          REFERENCES contracts(id),

    -- 報修資訊
    source              VARCHAR(30)     NOT NULL,
    reporter_name       VARCHAR(100),
    reporter_phone      VARCHAR(50),
    reporter_email      VARCHAR(200),
    report_address      TEXT,
    report_description  TEXT,
    reported_at         TIMESTAMP       NOT NULL DEFAULT now(),

    -- 維護資訊
    fault_category      VARCHAR(50),
    fault_cause         VARCHAR(50),
    repair_description  TEXT,
    completed_at        TIMESTAMP,

    -- 狀態
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    priority            VARCHAR(10)     DEFAULT 'NORMAL',
    dept_id             BIGINT          REFERENCES dept_info(dept_id),

    -- 審計
    created_by          VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, ticket_number)
);

CREATE INDEX idx_repair_tickets_tenant ON repair_tickets(tenant_id);
CREATE INDEX idx_repair_tickets_fault ON repair_tickets(fault_ticket_id);
CREATE INDEX idx_repair_tickets_device ON repair_tickets(device_id);
CREATE INDEX idx_repair_tickets_status ON repair_tickets(tenant_id, status);
CREATE INDEX idx_repair_tickets_dept ON repair_tickets(dept_id);

COMMENT ON TABLE repair_tickets IS '報修維護工單：由障礙申告審核通過後建立，或外部系統(1999)直接立案';
COMMENT ON COLUMN repair_tickets.source IS
  'FAULT_TICKET(障礙轉立) / CITIZEN_WEB(民眾網頁) / EXTERNAL_1999(陳情系統) / PATROL(巡檢) / PHONE(電話)';
COMMENT ON COLUMN repair_tickets.status IS
  'PENDING(未收案) / ACCEPTED(已收案) / DISPATCHED(已派工) / IN_PROGRESS(處理中) / '
  'COMPLETION_REPORTED(完工回報) / PENDING_REVIEW(完工審核中) / RETURNED(退回補件) / '
  'TRANSFERRED(改分轉送) / TRACKING(追蹤中) / CLOSED(結案)';

-- 05-2 派工紀錄
CREATE TABLE repair_dispatches (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    repair_ticket_id    BIGINT          NOT NULL REFERENCES repair_tickets(id),
    contract_id         BIGINT          REFERENCES contracts(id),
    assigned_to         BIGINT,
    assigned_org        VARCHAR(200),
    dispatch_note       TEXT,
    dispatched_at       TIMESTAMP       NOT NULL DEFAULT now(),
    dispatched_by       BIGINT          NOT NULL,
    due_date            DATE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DISPATCHED',
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_repair_dispatches_ticket ON repair_dispatches(repair_ticket_id);
CREATE INDEX idx_repair_dispatches_assigned ON repair_dispatches(assigned_to);

COMMENT ON TABLE repair_dispatches IS '派工紀錄：一張報修單可能多次派工（退回重派）';
COMMENT ON COLUMN repair_dispatches.dispatched_by IS '派工人 user_id，完工後由此人審核結案';

-- 05-3 工單附件（多態）
CREATE TABLE ticket_attachments (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    ticket_type     VARCHAR(30)     NOT NULL,
    ticket_id       BIGINT          NOT NULL,
    file_type       VARCHAR(20)     NOT NULL,
    file_url        VARCHAR(500)    NOT NULL,
    file_name       VARCHAR(300),
    file_size       BIGINT,
    description     VARCHAR(500),
    gps_lat         NUMERIC(10,7),
    gps_lng         NUMERIC(11,7),
    taken_at        TIMESTAMP,
    phase           VARCHAR(20),
    scan_status     VARCHAR(20)     DEFAULT 'PENDING',
    uploaded_by     VARCHAR(50),
    uploaded_at     TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_attachments_ticket ON ticket_attachments(ticket_type, ticket_id);

COMMENT ON TABLE ticket_attachments IS '工單附件：照片/影片/錄音/文件，跨工單類型共用';
COMMENT ON COLUMN ticket_attachments.ticket_type IS 'FAULT_TICKET / REPAIR_TICKET / REPLACEMENT_ORDER';
COMMENT ON COLUMN ticket_attachments.file_type IS 'PHOTO / VIDEO / AUDIO / DOCUMENT';
COMMENT ON COLUMN ticket_attachments.phase IS 'BEFORE(維修前) / DURING(維修中) / AFTER(維修後) / REPORT(報修時)';
COMMENT ON COLUMN ticket_attachments.scan_status IS 'PENDING / CLEAN / INFECTED（病毒掃描結果）';
