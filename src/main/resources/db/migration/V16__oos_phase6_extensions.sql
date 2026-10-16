-- V16: OOS Phase 6 Extensions
-- 1. Add 'Retest_Passed' to final_disposition CHECK constraint on oos_incident_register
-- 2. Add missing SRS columns to oos_incident_register (product_id, batch_no, test_code, etc.)
-- 3. Add Phase 6 columns to oos_incident_register (retest chain, RCA, revised result)
-- 4. Add linked_for_info_ar_number to sample_inward_register for For_Information cross-referencing
-- 5. Add indexes for OOS query performance

-- 1. Extend final_disposition CHECK to include 'Retest_Passed' for tracking successful retest resolution
ALTER TABLE oos_incident_register DROP CONSTRAINT IF EXISTS oos_incident_register_final_disposition_check;
ALTER TABLE oos_incident_register ADD CONSTRAINT oos_incident_register_final_disposition_check
    CHECK (final_disposition IN ('Pending', 'Retest_Authorized', 'Retest_Passed', 'Batch_Rejected'));

-- 2. Add missing SRS columns that were in Module 13 DDL but not in V6 migration
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS product_id INT DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS product_name VARCHAR(200) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS batch_no VARCHAR(100) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS test_code VARCHAR(50) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS specification_reference VARCHAR(500) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS observed_value VARCHAR(50) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS unit_of_measurement VARCHAR(50) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS test_method_name VARCHAR(200) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS deviation_description TEXT DEFAULT NULL;

-- 3. Phase 6 columns on oos_incident_register
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS retest_incident_id BIGINT DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS root_cause TEXT DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS corrective_action TEXT DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS revised_result VARCHAR(50) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS revised_result_unit VARCHAR(50) DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS retest_completed_date TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;
ALTER TABLE oos_incident_register ADD COLUMN IF NOT EXISTS oos_status VARCHAR(50) DEFAULT 'Pending';

-- 4. Add cross-reference column for For_Information AR linking
--    Stores the AR number of the For_Information AR on the new Active AR.
--    No FK constraint — logical reference only to allow deletion of For_Information ARs.
ALTER TABLE sample_inward_register ADD COLUMN IF NOT EXISTS linked_for_info_ar_number VARCHAR(50) DEFAULT NULL;
COMMENT ON COLUMN sample_inward_register.linked_for_info_ar_number IS
    'Cross-reference to For_Information AR number (trial test / fresh analysis link)';

-- 5. Indexes for OOS query performance
CREATE INDEX IF NOT EXISTS idx_oos_ar_number ON oos_incident_register(ar_number);
CREATE INDEX IF NOT EXISTS idx_oos_disposition ON oos_incident_register(final_disposition);

-- 6. OOS sequence tracker table (for atomic OOS number generation)
CREATE TABLE IF NOT EXISTS oos_sequence_tracker (
    calendar_year VARCHAR(2) PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0
);
INSERT INTO oos_sequence_tracker (calendar_year, last_sequence)
    VALUES (SUBSTRING(CAST(EXTRACT(YEAR FROM CURRENT_DATE) AS TEXT), 3, 2), 0)
    ON CONFLICT (calendar_year) DO NOTHING;
CREATE INDEX IF NOT EXISTS idx_oos_status ON oos_incident_register(oos_status);
CREATE INDEX IF NOT EXISTS idx_oos_product_batch ON oos_incident_register(product_id, batch_no);
CREATE INDEX IF NOT EXISTS idx_sir_for_info_link ON sample_inward_register(linked_for_info_ar_number);
