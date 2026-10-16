-- V7: QC Inventory table + Storage Allocation
-- SRS Module 4 (Sample Labelling, Barcoding & Storage Allocation)
-- SRS Module 5 (QC Inventory & Consumption Reconciliation)
-- SRS Module 13 DDL lines 358-371 (qc_inventory)
--
-- Review corrections applied:
--   C-1 [BLOCKER]: Renamed from V4 to V7 (V4 taken by trf_tables.sql)
--   C-4 [HIGH]: Added manufacturer_id per SRS Module 5.1
--   MEDIUM: Removed ALTER TABLE for storage_location (already in V3)
--   MEDIUM: Added BaseEntity audit columns explicitly
--   MEDIUM: Added UNIQUE constraint on ar_number

CREATE TABLE IF NOT EXISTS qc_inventory (
    inventory_id SERIAL PRIMARY KEY,
    ar_number VARCHAR(50),
    manufacturer_id INT NOT NULL,  -- C-4: SRS Module 5.1 requirement
    product_id INT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    total_quantity_received INT NOT NULL,
    quantity_allocated INT DEFAULT 0,
    quantity_consumed INT DEFAULT 0,
    current_balance INT GENERATED ALWAYS AS (total_quantity_received - quantity_consumed) STORED,
    storage_location VARCHAR(100),
    mfg_date DATE,
    exp_date DATE,
    received_by_user_id INT NOT NULL,
    -- BaseEntity audit columns (explicit per MEDIUM correction):
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Constraints
    CONSTRAINT chk_qty_received_positive CHECK (total_quantity_received > 0),
    CONSTRAINT chk_qty_allocated_non_negative CHECK (quantity_allocated >= 0),
    CONSTRAINT chk_qty_consumed_non_negative CHECK (quantity_consumed >= 0),
    CONSTRAINT chk_no_over_allocation CHECK (quantity_allocated <= total_quantity_received),
    CONSTRAINT uq_inventory_ar_number UNIQUE (ar_number)
);

-- FK to sample_inward_register (created in V3)
ALTER TABLE qc_inventory
    ADD CONSTRAINT fk_inventory_inward
    FOREIGN KEY (ar_number) REFERENCES sample_inward_register(ar_number);

-- Indexes for query patterns (IF NOT EXISTS for idempotency — L-4 fix)
CREATE INDEX IF NOT EXISTS idx_inventory_ar_number ON qc_inventory(ar_number);
CREATE INDEX IF NOT EXISTS idx_inventory_product_id ON qc_inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_batch_no ON qc_inventory(batch_no);
CREATE INDEX IF NOT EXISTS idx_inventory_storage_location ON qc_inventory(storage_location);
CREATE INDEX IF NOT EXISTS idx_inventory_product_batch ON qc_inventory(product_id, batch_no);
CREATE INDEX IF NOT EXISTS idx_inventory_manufacturer_id ON qc_inventory(manufacturer_id);
