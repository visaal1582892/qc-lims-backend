-- ===================================
-- Phase 2.2: Remove Composite Unique Constraints & Add Functional Unique Indexes
-- ===================================

-- 1. Analyst Qualification Master
ALTER TABLE analyst_qualification_master DROP INDEX uq_emp_param_status;

-- Functional unique index: enforces uniqueness only when status = 'A' (when status != 'A', evaluates to NULL which is non-unique)
CREATE UNIQUE INDEX uq_analyst_qual_active_only ON analyst_qualification_master (
    employee_id,
    param_id,
    (CASE WHEN status = 'A' THEN 1 ELSE NULL END)
);

-- Fast composite lookup index for batch duplicate validation
CREATE INDEX idx_analyst_qual_combo ON analyst_qualification_master (employee_id, param_id, status);

-- 2. QC Inventory Configuration
ALTER TABLE qc_inventory_configuration DROP INDEX uq_prod_param_status;

-- Functional unique index: enforces uniqueness only when status = 'A'
CREATE UNIQUE INDEX uq_qc_inv_active_only ON qc_inventory_configuration (
    product_id,
    param_id,
    (CASE WHEN status = 'A' THEN 1 ELSE NULL END)
);

-- Fast composite lookup index for batch duplicate validation
CREATE INDEX idx_qc_inv_combo ON qc_inventory_configuration (product_id, param_id, status);
