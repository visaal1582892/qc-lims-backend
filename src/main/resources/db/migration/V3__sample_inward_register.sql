-- V3: Sample Inward Register table
-- SRS Module 2: Inward Registration & AR Number Generation
-- Includes BaseEntity.status column (RC-3) for soft-delete support

CREATE TABLE sample_inward_register (
    inward_id SERIAL PRIMARY KEY,
    ar_number VARCHAR(50) UNIQUE,  -- NULL if subcontracted
    trf_id INT,                    -- Nullable FK to trf_master (no FK constraint yet)
    product_id INT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    received_quantity_packs INT NOT NULL,
    total_received_units INT NOT NULL,
    storage_location VARCHAR(100), -- Ambient, Freezer, Refrigerator, Others
    ar_status VARCHAR(50) DEFAULT 'Active' CHECK (
        ar_status IN ('Active', 'For_Information', 'Closed', 'Rejected')
    ),
    is_subcontracted BOOLEAN DEFAULT FALSE,
    subcontract_remarks TEXT,
    receipt_date DATE DEFAULT CURRENT_DATE,
    -- BaseEntity columns (RC-3):
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_inward_ar_number ON sample_inward_register(ar_number);
CREATE INDEX idx_inward_product ON sample_inward_register(product_id);
CREATE INDEX idx_inward_batch ON sample_inward_register(batch_no);
CREATE INDEX idx_inward_receipt_date ON sample_inward_register(receipt_date);
CREATE INDEX idx_inward_trf_id ON sample_inward_register(trf_id);
