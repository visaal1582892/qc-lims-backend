-- V16: OOS Phase 6 Extensions
-- 1. Add 'Retest_Passed' to final_disposition CHECK constraint on oos_incident_register
-- 2. Add missing SRS columns to oos_incident_register (product_id, batch_no, test_code, etc.)
-- 3. Add Phase 6 columns to oos_incident_register (retest chain, RCA, revised result)
-- 4. Add linked_for_info_ar_number to sample_inward_register for For_Information cross-referencing
-- 5. Add indexes for OOS query performance
--
-- MySQL: DROP CONSTRAINT syntax, INFORMATION_SCHEMA checks, TIMESTAMP (no TIME ZONE),
--        no COMMENT ON COLUMN (use column COMMENT in CREATE/MODIFY),
--        ON DUPLICATE KEY UPDATE instead of ON CONFLICT.

-- 1. Extend final_disposition CHECK to include 'Retest_Passed' for tracking successful retest resolution
SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND CONSTRAINT_NAME = 'oos_incident_register_final_disposition_check'
);
SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE oos_incident_register DROP CONSTRAINT oos_incident_register_final_disposition_check',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE oos_incident_register ADD CONSTRAINT oos_incident_register_final_disposition_check
    CHECK (final_disposition IN ('Pending', 'Retest_Authorized', 'Retest_Passed', 'Batch_Rejected'));

-- 2. Add missing SRS columns that were in Module 13 DDL but not in V6 migration
-- MySQL: Use INFORMATION_SCHEMA to check column existence
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'product_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN product_id INT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'product_name');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN product_name VARCHAR(200) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'batch_no');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN batch_no VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'test_code');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN test_code VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'specification_reference');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN specification_reference VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'observed_value');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN observed_value VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'unit_of_measurement');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN unit_of_measurement VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'test_method_name');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN test_method_name VARCHAR(200) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'deviation_description');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN deviation_description TEXT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Phase 6 columns on oos_incident_register
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'retest_incident_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN retest_incident_id BIGINT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'root_cause');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN root_cause TEXT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'corrective_action');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN corrective_action TEXT DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'revised_result');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN revised_result VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'revised_result_unit');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN revised_result_unit VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'retest_completed_date');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN retest_completed_date TIMESTAMP DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oos_incident_register' AND COLUMN_NAME = 'oos_status');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oos_incident_register ADD COLUMN oos_status VARCHAR(50) DEFAULT ''Pending''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Add cross-reference column for For_Information AR linking
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_inward_register' AND COLUMN_NAME = 'linked_for_info_ar_number');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE sample_inward_register ADD COLUMN linked_for_info_ar_number VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Indexes for OOS query performance
-- idx_oos_disposition already created in V9
CREATE INDEX idx_oos_ar_number ON oos_incident_register(ar_number);

-- 6. OOS sequence tracker table (for atomic OOS number generation)
CREATE TABLE IF NOT EXISTS oos_sequence_tracker (
    calendar_year VARCHAR(2) PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0
);
-- MySQL: ON DUPLICATE KEY UPDATE + SUBSTRING(YEAR(CURRENT_DATE), 3, 2) for last 2 digits
INSERT INTO oos_sequence_tracker (calendar_year, last_sequence)
    VALUES (SUBSTRING(CAST(YEAR(CURRENT_DATE) AS CHAR), 3, 2), 0)
    ON DUPLICATE KEY UPDATE calendar_year = VALUES(calendar_year);
CREATE INDEX idx_oos_status ON oos_incident_register(oos_status);
CREATE INDEX idx_oos_product_batch ON oos_incident_register(product_id, batch_no);
CREATE INDEX idx_sir_for_info_link ON sample_inward_register(linked_for_info_ar_number);
