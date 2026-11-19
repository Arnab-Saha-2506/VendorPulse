-- =====================================================================
-- VendorPulse - PostgreSQL schema (manager-approved)
-- =====================================================================

-- Drop old tables if they exist with conflicting definitions
DROP TABLE IF EXISTS invoice_items CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS inventory_sync_logs CASCADE;
DROP TABLE IF EXISTS inventory_snapshots CASCADE;
DROP TABLE IF EXISTS supplier_verification CASCADE;
DROP TABLE IF EXISTS quality_feedback CASCADE;
DROP TABLE IF EXISTS risk_alerts CASCADE;
DROP TABLE IF EXISTS supplier_scores CASCADE;
DROP TABLE IF EXISTS purchase_order_items CASCADE;
DROP TABLE IF EXISTS commitment_revisions CASCADE;

DROP TABLE IF EXISTS contact_messages CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS alerts CASCADE;
DROP TABLE IF EXISTS supplier_score_history CASCADE;
DROP TABLE IF EXISTS scoring_settings CASCADE;
DROP TABLE IF EXISTS po_messages CASCADE;
DROP TABLE IF EXISTS deliveries CASCADE;
DROP TABLE IF EXISTS agreements CASCADE;
DROP TABLE IF EXISTS po_commitments CASCADE;
DROP TABLE IF EXISTS commitments CASCADE;
DROP TABLE IF EXISTS negotiation_rounds CASCADE;
DROP TABLE IF EXISTS po_lines CASCADE;
DROP TABLE IF EXISTS purchase_orders CASCADE;
DROP TABLE IF EXISTS materials CASCADE;
DROP TABLE IF EXISTS supplier_documents CASCADE;
DROP TABLE IF EXISTS supplier_categories CASCADE;
DROP TABLE IF EXISTS material_categories CASCADE;
DROP TABLE IF EXISTS password_resets CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;

-- ---------- ENUM TYPES ----------
DROP TYPE IF EXISTS notification_type CASCADE;
DROP TYPE IF EXISTS alert_status CASCADE;
DROP TYPE IF EXISTS alert_type CASCADE;
DROP TYPE IF EXISTS inspection_result CASCADE;
DROP TYPE IF EXISTS delivery_status CASCADE;
DROP TYPE IF EXISTS commitment_type CASCADE;
DROP TYPE IF EXISTS negotiation_action CASCADE;
DROP TYPE IF EXISTS party_side CASCADE;
DROP TYPE IF EXISTS po_status CASCADE;
DROP TYPE IF EXISTS approval_status CASCADE;
DROP TYPE IF EXISTS account_status CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;

CREATE TYPE user_role AS ENUM ('PROCUREMENT_MANAGER','PROCUREMENT_EXECUTIVE','SYSTEM_ADMIN','SUPPLIER_MANAGER','SUPPLIER_USER');
CREATE TYPE account_status AS ENUM ('ACTIVE','INACTIVE','PENDING','REJECTED');
CREATE TYPE approval_status AS ENUM ('PENDING','APPROVED','REJECTED');
CREATE TYPE po_status AS ENUM ('DRAFT','SUBMITTED','CONFIRMED','IN_PROGRESS','PARTIALLY_DELIVERED','DELIVERED','CANCELLED');
CREATE TYPE party_side AS ENUM ('MANUFACTURER','SUPPLIER');
CREATE TYPE negotiation_action AS ENUM ('PROPOSAL','COUNTER','ACCEPTED','REJECTED');
CREATE TYPE commitment_type AS ENUM ('ORIGINAL','REVISION');
CREATE TYPE delivery_status AS ENUM ('PARTIAL','COMPLETE');
CREATE TYPE inspection_result AS ENUM ('PASS','CONDITIONAL','FAIL');
CREATE TYPE alert_type AS ENUM ('REVISION','DELAY','STOCK','CDI');
CREATE TYPE alert_status AS ENUM ('ACTIVE','RECOVERED','DISMISSED');
CREATE TYPE notification_type AS ENUM ('PROPOSAL','REMINDER','CANCELLED','APPROVAL','MESSAGE');

-- ---------- SUPPLIERS ----------
CREATE TABLE suppliers (
    id                UUID PRIMARY KEY,
    company_name      VARCHAR(200) NOT NULL,
    registration_no   VARCHAR(60)  NOT NULL UNIQUE,
    contact_person    VARCHAR(120) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    phone             VARCHAR(25)  NOT NULL,
    address           TEXT         NOT NULL,
    material_type     VARCHAR(120),
    business_unit     VARCHAR(120),
    approval_status   approval_status NOT NULL DEFAULT 'PENDING',
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    rejection_reason  TEXT,
    submitted_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at        TIMESTAMPTZ,
    decided_by        UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- USERS ----------
CREATE TABLE users (
    id             UUID PRIMARY KEY,
    user_code      VARCHAR(40) UNIQUE,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  TEXT NOT NULL,
    role           user_role NOT NULL,
    status         account_status NOT NULL DEFAULT 'ACTIVE',
    supplier_id    UUID REFERENCES suppliers(id),
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE suppliers
    ADD CONSTRAINT fk_suppliers_decided_by FOREIGN KEY (decided_by) REFERENCES users(id);

-- ---------- PASSWORD RESETS ----------
CREATE TABLE password_resets (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- MATERIAL CATEGORIES ----------
CREATE TABLE material_categories (
    id    SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(80) NOT NULL UNIQUE
);

-- ---------- SUPPLIER <-> CATEGORY MAPPING ----------
CREATE TABLE supplier_categories (
    supplier_id  UUID     NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    category_id  SMALLINT NOT NULL REFERENCES material_categories(id),
    PRIMARY KEY (supplier_id, category_id)
);

-- ---------- SUPPLIER DOCUMENTS (each file has its own review status) ----------
CREATE TABLE supplier_documents (
    id                    UUID PRIMARY KEY,
    supplier_id           UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    file_name             VARCHAR(255) NOT NULL,
    mime_type             VARCHAR(100) NOT NULL,
    size_bytes            BIGINT NOT NULL,
    storage_key           TEXT NOT NULL,
    review_status         approval_status NOT NULL DEFAULT 'PENDING',
    rejection_reason      TEXT,
    reviewed_by           UUID REFERENCES users(id),
    reviewed_at           TIMESTAMPTZ,
    replaces_document_id  UUID REFERENCES supplier_documents(id),
    uploaded_by           UUID REFERENCES users(id),
    uploaded_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_supdocs_supplier ON supplier_documents (supplier_id, review_status);

-- ---------- MATERIALS ----------
CREATE TABLE materials (
    id                UUID PRIMARY KEY,
    code              VARCHAR(30)  NOT NULL UNIQUE,
    name              VARCHAR(160) NOT NULL,
    category_id       SMALLINT NOT NULL REFERENCES material_categories(id),
    is_critical       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    stock_qty         NUMERIC(14,2) NOT NULL DEFAULT 0,
    daily_usage       NUMERIC(14,2) NOT NULL DEFAULT 0,
    stock_updated_at  TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_materials_category ON materials (category_id);

-- ---------- PURCHASE ORDERS ----------
CREATE TABLE purchase_orders (
    id                        UUID PRIMARY KEY,
    po_number                 VARCHAR(20) NOT NULL UNIQUE,
    supplier_id               UUID NOT NULL REFERENCES suppliers(id),
    status                    po_status NOT NULL DEFAULT 'DRAFT',
    required_date             DATE NOT NULL,
    agreed_price_total        NUMERIC(14,2),
    agreed_delivery_date      DATE,
    cancellation_cutoff_days  SMALLINT NOT NULL DEFAULT 7,
    awaiting_party            party_side,
    notes                     TEXT,
    dispatched_on             DATE,
    supplier_marked_delivered_on DATE,
    cancel_reason             TEXT,
    cancelled_by              UUID REFERENCES users(id),
    cancelled_at              TIMESTAMPTZ,
    created_by                UUID NOT NULL REFERENCES users(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_po_supplier_status ON purchase_orders (supplier_id, status);
CREATE INDEX idx_po_required_date   ON purchase_orders (required_date);

-- ---------- PO LINES ----------
CREATE TABLE po_lines (
    id             UUID PRIMARY KEY,
    po_id          UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    line_no        SMALLINT NOT NULL,
    material_id    UUID NOT NULL REFERENCES materials(id),
    quantity       NUMERIC(14,2) NOT NULL,
    unit_price     NUMERIC(14,2) NOT NULL,
    required_date  DATE NOT NULL,
    UNIQUE (po_id, line_no)
);
CREATE INDEX idx_po_lines_material ON po_lines (material_id);

-- ---------- NEGOTIATION ROUNDS ----------
CREATE TABLE negotiation_rounds (
    id              UUID PRIMARY KEY,
    po_id           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    round_no        SMALLINT NOT NULL,
    from_party      party_side NOT NULL,
    action          negotiation_action NOT NULL,
    proposed_date   DATE,
    proposed_price  NUMERIC(14,2),
    reason          TEXT NOT NULL,
    made_by         UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (po_id, round_no)
);

-- ---------- COMMITMENTS (seq_no 0 = original, 1..n = revisions) ----------
CREATE TABLE po_commitments (
    id              UUID PRIMARY KEY,
    po_id           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    seq_no          SMALLINT NOT NULL,
    type            commitment_type NOT NULL,
    committed_date  DATE NOT NULL,
    reason          TEXT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (po_id, seq_no)
);

-- ---------- AGREEMENTS ----------
CREATE TABLE agreements (
    id                           UUID PRIMARY KEY,
    po_id                        UUID NOT NULL UNIQUE REFERENCES purchase_orders(id),
    delivery_date                DATE NOT NULL,
    price_total                  NUMERIC(14,2) NOT NULL,
    cutoff_days                  SMALLINT NOT NULL,
    accepted_by_manufacturer     UUID NOT NULL REFERENCES users(id),
    accepted_by_manufacturer_at  TIMESTAMPTZ NOT NULL,
    accepted_by_supplier         UUID NOT NULL REFERENCES users(id),
    accepted_by_supplier_at      TIMESTAMPTZ NOT NULL,
    pdf_storage_key              TEXT,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- DELIVERIES + QUALITY ----------
CREATE TABLE deliveries (
    id              UUID PRIMARY KEY,
    po_id           UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    delivery_date   DATE NOT NULL,
    delivered_qty   NUMERIC(14,2) NOT NULL,
    accepted_qty    NUMERIC(14,2) NOT NULL,
    rejected_qty    NUMERIC(14,2) NOT NULL,
    completion      delivery_status NOT NULL,
    quality_rating  SMALLINT,
    inspection      inspection_result,
    remarks         TEXT,
    recorded_by     UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_deliveries_po ON deliveries (po_id);

-- ---------- PO MESSAGES ----------
CREATE TABLE po_messages (
    id           UUID PRIMARY KEY,
    po_id        UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    sender_id    UUID NOT NULL REFERENCES users(id),
    sender_side  party_side NOT NULL,
    body         TEXT NOT NULL,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_po ON po_messages (po_id, created_at);

-- ---------- SCORING SETTINGS (keep a single row, id = 1) ----------
CREATE TABLE scoring_settings (
    id                        SMALLINT PRIMARY KEY DEFAULT 1,
    w_delivery_reliability    SMALLINT NOT NULL,
    w_commitment_stability    SMALLINT NOT NULL,
    w_delivery_consistency    SMALLINT NOT NULL,
    w_quality_performance     SMALLINT NOT NULL,
    high_cdi_threshold        SMALLINT NOT NULL DEFAULT 40,
    stock_alert_horizon_days  SMALLINT NOT NULL DEFAULT 14,
    max_negotiation_rounds    SMALLINT NOT NULL DEFAULT 3,
    default_cutoff_days       SMALLINT NOT NULL DEFAULT 7,
    updated_by                UUID REFERENCES users(id),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- SUPPLIER SCORE HISTORY ----------
CREATE TABLE supplier_score_history (
    id                    UUID PRIMARY KEY,
    supplier_id           UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    period                DATE NOT NULL,
    overall_score         NUMERIC(5,2) NOT NULL,
    delivery_reliability  NUMERIC(5,2) NOT NULL,
    commitment_stability  NUMERIC(5,2) NOT NULL,
    delivery_consistency  NUMERIC(5,2) NOT NULL,
    quality_performance   NUMERIC(5,2) NOT NULL,
    cdi                   NUMERIC(5,2) NOT NULL,
    orders_counted        INTEGER NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (supplier_id, period)
);

-- ---------- ALERTS ----------
CREATE TABLE alerts (
    id           UUID PRIMARY KEY,
    type         alert_type NOT NULL,
    message      VARCHAR(300) NOT NULL,
    status       alert_status NOT NULL DEFAULT 'ACTIVE',
    po_id        UUID REFERENCES purchase_orders(id) ON DELETE CASCADE,
    material_id  UUID REFERENCES materials(id) ON DELETE CASCADE,
    supplier_id  UUID REFERENCES suppliers(id) ON DELETE CASCADE,
    raised_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ
);
CREATE INDEX idx_alerts_status_type ON alerts (status, type, raised_at DESC);

-- ---------- NOTIFICATIONS ----------
CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    supplier_id  UUID REFERENCES suppliers(id) ON DELETE CASCADE,
    user_id      UUID REFERENCES users(id) ON DELETE CASCADE,
    type         notification_type NOT NULL,
    message      VARCHAR(300) NOT NULL,
    po_id        UUID REFERENCES purchase_orders(id) ON DELETE CASCADE,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_supplier ON notifications (supplier_id, created_at DESC);

-- ---------- AUDIT LOG (insert only, never update or delete) ----------
CREATE TABLE audit_log (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id       UUID REFERENCES users(id),
    user_name     VARCHAR(120) NOT NULL,
    action        VARCHAR(80)  NOT NULL,
    record_type   VARCHAR(40)  NOT NULL,
    record_id     VARCHAR(60),
    record_label  VARCHAR(160)
);
CREATE INDEX idx_audit_time   ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_user   ON audit_log (user_id);
CREATE INDEX idx_audit_action ON audit_log (action);

-- ---------- CONTACT MESSAGES ----------
CREATE TABLE contact_messages (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    message     TEXT NOT NULL,
    handled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- SEQUENCE FOR PO NUMBER ----------
CREATE SEQUENCE IF NOT EXISTS po_number_seq START 1001;

-- ---------- SEED DATA ----------
INSERT INTO material_categories (name) VALUES
 ('Castings'), ('Fasteners'), ('Electrical'), ('Packaging'), ('Forgings'), ('Machined parts')
ON CONFLICT (name) DO NOTHING;

INSERT INTO scoring_settings
 (id, w_delivery_reliability, w_commitment_stability, w_delivery_consistency, w_quality_performance)
VALUES (1, 35, 25, 20, 20)
ON CONFLICT (id) DO NOTHING;

