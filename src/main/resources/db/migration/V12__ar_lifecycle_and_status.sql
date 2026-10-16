-- ============================================================================
-- V12 — AR Lifecycle & Status migration for Sample Inward Register
-- ============================================================================
-- Purpose:
--   1. Migrate existing ar_status column from old V3 values
--      ('Active', 'For_Information', 'Closed', 'Rejected') to new
--      lifecycle values ('REGISTERED', 'IN_PROGRESS', 'ALL_TESTS_COMPLETE',
--      'COMPLETED', 'CANCELLED').
--   2. Add `test_completion_rate` column for efficient roll-up queries.
--   3. Back-fill test_completion_rate for existing rows.
--   4. Add indexes for filter queries.
--
-- The ar_status column was originally created in V3 with a CHECK constraint
-- for old values. This migration replaces those values and the constraint.
--
-- MySQL: Use DROP CONSTRAINT syntax and MODIFY COLUMN for default changes.
-- ============================================================================

-- 1. Drop existing CHECK constraint on ar_status (V3 defined it)
-- MySQL uses ALTER TABLE ... DROP CONSTRAINT (MySQL 8.0.29+)
-- Try multiple possible constraint names since DBs auto-generate them
SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sample_inward_register'
    AND CONSTRAINT_NAME = 'chk_sample_inward_ar_status'
);
SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE sample_inward_register DROP CONSTRAINT chk_sample_inward_ar_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sample_inward_register'
    AND CONSTRAINT_NAME = 'sample_inward_register_ar_status_check'
);
SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE sample_inward_register DROP CONSTRAINT sample_inward_register_ar_status_check',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Map old ar_status values to new lifecycle values
UPDATE sample_inward_register
SET ar_status = 'REGISTERED'
WHERE ar_status = 'Active';

UPDATE sample_inward_register
SET ar_status = 'ALL_TESTS_COMPLETE'
WHERE ar_status = 'For_Information';

UPDATE sample_inward_register
SET ar_status = 'COMPLETED'
WHERE ar_status = 'Closed';

UPDATE sample_inward_register
SET ar_status = 'CANCELLED'
WHERE ar_status = 'Rejected';

-- 3. Change default value
ALTER TABLE sample_inward_register
    MODIFY COLUMN ar_status VARCHAR(50) DEFAULT 'REGISTERED';

-- 4. Add new CHECK constraint with lifecycle values
ALTER TABLE sample_inward_register
    ADD CONSTRAINT chk_sample_inward_ar_status
        CHECK (ar_status IN ('REGISTERED', 'IN_PROGRESS', 'ALL_TESTS_COMPLETE', 'COMPLETED', 'CANCELLED'));

-- 5. Add test_completion_rate column (MySQL: check if column exists first)
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sample_inward_register'
    AND COLUMN_NAME = 'test_completion_rate'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sample_inward_register ADD COLUMN test_completion_rate DECIMAL(5,2) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. Back-fill test_completion_rate for existing ARs
UPDATE sample_inward_register
SET test_completion_rate = COALESCE(
    (
        SELECT ROUND(
            CAST(SUM(CASE WHEN te.te_execution_status IN ('Released', 'Cancelled_Not_Performed') THEN 1 ELSE 0 END) AS DECIMAL)
            / NULLIF(CAST(COUNT(*) AS DECIMAL), 0) * 100,
            2
        )
        FROM test_execution te
        WHERE te.te_sample_inward_id = sample_inward_register.id
          AND te.status = 'ACTIVE'
    ),
    0
);

-- 7. Indexes for common filter paths

CREATE INDEX IF NOT EXISTS idx_sample_inward_ar_status
    ON sample_inward_register (ar_status);

CREATE INDEX IF NOT EXISTS idx_sample_inward_product_status
    ON sample_inward_register (product_id, ar_status);

-- ============================================================================
-- Rollback (manual only — Flyway does not auto-rollback):
--
--   ALTER TABLE sample_inward_register DROP CONSTRAINT chk_sample_inward_ar_status;
--   DROP INDEX IF EXISTS idx_sample_inward_ar_status;
--   DROP INDEX IF EXISTS idx_sample_inward_product_status;
--   ALTER TABLE sample_inward_register DROP COLUMN IF EXISTS test_completion_rate;
-- ============================================================================
