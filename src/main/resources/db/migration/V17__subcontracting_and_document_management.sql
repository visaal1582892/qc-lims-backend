-- V17: Subcontracting Module (SRS Module 10) + Document Management (SRS Module 11)
-- Creates subcontract_register table for tracking external lab dispatches
-- Subcontracted samples: NO AR Number (ar_number remains NULL)

-- Subcontract register table
CREATE TABLE IF NOT EXISTS subcontract_register (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    subcontract_no  VARCHAR(100) NOT NULL UNIQUE,
    trf_number      VARCHAR(100),
    ar_number       VARCHAR(50),
    external_lab_name VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    dispatch_date   DATE NOT NULL,
    dispatched_by_qc_head_id BIGINT NOT NULL,
    expected_return_date DATE,
    subcontract_status VARCHAR(50) NOT NULL DEFAULT 'DISPATCHED',
    remarks         TEXT,
    roa_received    TINYINT(1) DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Indexes
CREATE INDEX idx_subcontract_trf_number ON subcontract_register(trf_number);
CREATE INDEX idx_subcontract_ar_number ON subcontract_register(ar_number);
CREATE INDEX idx_subcontract_status ON subcontract_register(subcontract_status);

-- Document registry table for tracking uploaded documents (ROA/COA)
CREATE TABLE IF NOT EXISTS document_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_type   VARCHAR(50) NOT NULL,
    ar_number       VARCHAR(50),
    execution_id    BIGINT,
    file_name       VARCHAR(255) NOT NULL,
    file_path       TEXT NOT NULL,
    file_size_bytes BIGINT,
    content_type    VARCHAR(100) DEFAULT 'application/pdf',
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Indexes for document registry
CREATE INDEX idx_document_registry_ar_number ON document_registry(ar_number);
CREATE INDEX idx_document_registry_execution_id ON document_registry(execution_id);
CREATE INDEX idx_document_registry_type ON document_registry(document_type);
