-- ============================================================================
-- V13 — Review Workflow tables for Multi-Tier Review, Rework & Approval
-- ============================================================================
-- Purpose:
--   1. Create review_audit_log table for immutable audit trail of review decisions.
--   2. Add review tracking columns to test_execution (review_status, timestamps).
--   3. Add indexes for review queue queries.
--
-- Review flow:
--   Under_Review -> QC Head Review -> Released (to QA) or Rework_Requested
--   Released     -> QA Head Review -> QA_Approved (COA locked) or Rework_Requested
--
-- MySQL: Use INFORMATION_SCHEMA checks for column existence before ADD COLUMN.
-- ============================================================================

-- 1. Add review tracking columns to test_execution (MySQL: check existence)
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'test_execution'
    AND COLUMN_NAME = 'review_status'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE test_execution ADD COLUMN review_status VARCHAR(32) DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'test_execution'
    AND COLUMN_NAME = 'qc_head_reviewed_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE test_execution ADD COLUMN qc_head_reviewed_at TIMESTAMP DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'test_execution'
    AND COLUMN_NAME = 'qa_head_reviewed_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE test_execution ADD COLUMN qa_head_reviewed_at TIMESTAMP DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Create review_audit_log table (immutable audit trail)
CREATE TABLE IF NOT EXISTS review_audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES test_execution(execution_id),
    review_stage VARCHAR(20) NOT NULL CHECK (review_stage IN ('QC_HEAD', 'QA_HEAD')),
    reviewer_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVE', 'REWORK')),
    reason TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BaseEntity columns
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- 3. Indexes for review queue queries
CREATE INDEX idx_test_exec_review_status
    ON test_execution (review_status);

CREATE INDEX idx_test_exec_exec_status
    ON test_execution (execution_status);

CREATE INDEX idx_review_log_execution
    ON review_audit_log (execution_id);

CREATE INDEX idx_review_log_stage
    ON review_audit_log (review_stage);

-- ============================================================================
-- Rollback (manual only — Flyway does not auto-rollback):
--
--   DROP INDEX IF EXISTS idx_review_log_stage;
--   DROP INDEX IF EXISTS idx_review_log_execution;
--   DROP INDEX IF EXISTS idx_test_exec_exec_status;
--   DROP INDEX IF EXISTS idx_test_exec_review_status;
--   DROP TABLE IF EXISTS review_audit_log;
--   ALTER TABLE test_execution DROP COLUMN IF EXISTS review_status;
--   ALTER TABLE test_execution DROP COLUMN IF EXISTS qc_head_reviewed_at;
--   ALTER TABLE test_execution DROP COLUMN IF EXISTS qa_head_reviewed_at;
-- ============================================================================
