-- V17: Subcontracting Module (SRS Module 10) + Document Management (SRS Module 11)
-- Creates subcontract_register table for tracking external lab dispatches
-- Subcontracted samples: NO AR Number (ar_number remains NULL)

-- Subcontract register table
CREATE TABLE IF NOT EXISTS subcontract_register (
    id              BIGSERIAL PRIMARY KEY,
    subcontract_no  VARCHAR(100) NOT NULL UNIQUE,
    trf_id          INTEGER,
    ar_number       VARCHAR(50),
    external_lab_name VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    dispatch_date   DATE NOT NULL,
    dispatched_by_qc_head_id BIGINT NOT NULL,
    expected_return_date DATE,
    subcontract_status VARCHAR(50) NOT NULL DEFAULT 'DISPATCHED',
    remarks         TEXT,
    roa_received    BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Index for lookup by TRF
CREATE INDEX IF NOT EXISTS idx_subcontract_trf_id ON subcontract_register(trf_id);

-- Index for lookup by AR number (will be NULL for subcontracted samples)
CREATE INDEX IF NOT EXISTS idx_subcontract_ar_number ON subcontract_register(ar_number) WHERE ar_number IS NOT NULL;

-- Index for subcontract_status filtering
CREATE INDEX IF NOT EXISTS idx_subcontract_status ON subcontract_register(subcontract_status);

-- Document registry table for tracking uploaded documents (ROA/COA)
CREATE TABLE IF NOT EXISTS document_registry (
    id              BIGSERIAL PRIMARY KEY,
    document_type   VARCHAR(50) NOT NULL,
    ar_number       VARCHAR(50),
    execution_id    BIGINT,
    file_name       VARCHAR(255) NOT NULL,
    file_path       TEXT NOT NULL,
    file_size_bytes BIGINT,
    content_type    VARCHAR(100) DEFAULT 'application/pdf',
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Index for AR number lookup
CREATE INDEX IF NOT EXISTS idx_document_registry_ar_number ON document_registry(ar_number);

-- Index for execution_id lookup
CREATE INDEX IF NOT EXISTS idx_document_registry_execution_id ON document_registry(execution_id) WHERE execution_id IS NOT NULL;

-- Index for document type filtering
CREATE INDEX IF NOT EXISTS idx_document_registry_type ON document_registry(document_type);
