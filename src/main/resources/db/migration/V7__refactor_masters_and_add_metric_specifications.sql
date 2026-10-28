-- ============================================================
-- Phase 2.3: Masters Refactoring & Metric Specifications Schema
-- ============================================================

-- -------------------------------------------------------------
-- 1. Refactor Test Parameter Master
-- -------------------------------------------------------------

-- Drop FK constraint and columns from main table
ALTER TABLE test_parameter_master DROP FOREIGN KEY fk_test_param_metric_unit;
ALTER TABLE test_parameter_master DROP COLUMN result_type, DROP COLUMN metric_unit_id;

-- Drop columns from log table
ALTER TABLE test_parameter_master_log DROP COLUMN result_type, DROP COLUMN metric_unit_id;

-- -------------------------------------------------------------
-- 2. Refactor QC Inventory Configuration
-- -------------------------------------------------------------

-- Add new business columns to main table
ALTER TABLE qc_inventory_configuration 
    ADD COLUMN test_method_id VARCHAR(50) NOT NULL DEFAULT 'TM-DEFAULT' AFTER param_id,
    ADD COLUMN is_critical VARCHAR(1) NOT NULL DEFAULT 'N' CHECK (is_critical IN ('Y', 'N')) AFTER test_method_id,
    ADD COLUMN result_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (result_type IN ('METRICS', 'TEXT')) AFTER is_critical,
    ADD COLUMN specification_text VARCHAR(300) NULL AFTER result_type;

-- Add matching columns to log table (without constraints per rules.md §2.2)
ALTER TABLE qc_inventory_configuration_log
    ADD COLUMN test_method_id VARCHAR(50) AFTER param_id,
    ADD COLUMN is_critical VARCHAR(1) AFTER test_method_id,
    ADD COLUMN result_type VARCHAR(20) AFTER is_critical,
    ADD COLUMN specification_text VARCHAR(300) AFTER result_type;

-- -------------------------------------------------------------
-- 3. Metric Specifications Master & Log Tables
-- -------------------------------------------------------------

-- Main Table
CREATE TABLE metric_specifications (
    metric_specification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    metric_unit_id BIGINT NOT NULL,
    lower_bound DECIMAL(12, 2) NULL,
    upper_bound DECIMAL(12, 2) NULL,
    status VARCHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),

    CONSTRAINT fk_metric_spec_config
        FOREIGN KEY (config_id)
        REFERENCES qc_inventory_configuration(config_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_metric_spec_unit
        FOREIGN KEY (metric_unit_id)
        REFERENCES metric_unit_master(metric_unit_id)
        ON DELETE RESTRICT
);

-- Log Table (Historical Tracking per rules.md §2.2)
CREATE TABLE metric_specifications_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_specification_id BIGINT,
    config_id BIGINT,
    metric_unit_id BIGINT,
    lower_bound DECIMAL(12, 2),
    upper_bound DECIMAL(12, 2),
    status VARCHAR(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

-- Search, Sort & Deferred Join Indexes
CREATE INDEX idx_metric_spec_updated_id ON metric_specifications (updated_at DESC, metric_specification_id DESC);
CREATE INDEX idx_metric_spec_created_id ON metric_specifications (created_at DESC, metric_specification_id DESC);
CREATE INDEX idx_metric_spec_config_id ON metric_specifications (config_id, status);
CREATE INDEX idx_metric_spec_unit_id ON metric_specifications (metric_unit_id);

-- Functional Unique Index (Guardrail 3: prevents duplicate active units for the same config)
CREATE UNIQUE INDEX uq_metric_spec_active_only ON metric_specifications (
    config_id,
    metric_unit_id,
    (CASE WHEN status = 'A' THEN 1 ELSE NULL END)
);

-- Composite lookup index for batch duplicate validation
CREATE INDEX idx_metric_spec_combo ON metric_specifications (config_id, metric_unit_id, status);

-- -------------------------------------------------------------
-- 4. Seed Rights & Role Mappings
-- -------------------------------------------------------------

INSERT INTO `rights` (`right_id`, `right_name`, `description`, `status`) VALUES
('ROLE_METRIC_SPECIFICATION_READ',   'Metric Specification Read',   'View metric specifications',             'ACTIVE'),
('ROLE_METRIC_SPECIFICATION_UPLOAD', 'Metric Specification Upload', 'Upload metric specifications via Excel', 'ACTIVE')
ON DUPLICATE KEY UPDATE `right_name` = VALUES(`right_name`);

INSERT IGNORE INTO `role_rights` (`role_id`, `right_id`)
SELECT 1, right_id FROM `rights` WHERE right_id IN ('ROLE_METRIC_SPECIFICATION_READ', 'ROLE_METRIC_SPECIFICATION_UPLOAD');
