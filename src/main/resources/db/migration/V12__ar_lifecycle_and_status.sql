-- ============================================================================
-- V12 — AR Lifecycle & Status fields for Sample Inward Register
-- ============================================================================
-- Purpose:
--   1. Add `ar_status` column to track the lifecycle state of each AR.
--   2. Add `test_completion_rate` column for efficient roll-up queries
--      (updated by application on test-execution state changes).
--   3. Back-fill ar_status for existing rows from test_execution data.
--   4. Add indexes for filter queries.
--
-- Migration rules:
--   a) If test_execution table exists AND has at least one active row for an
--      AR → ar_status = 'IN_PROGRESS'.
--   b) If test_execution table exists AND ALL active rows for an AR are in
--      a terminal status (Released, Cancelled_Not_Performed) → ar_status = 'ALL_TESTS_COMPLETE'.
--   c) Otherwise → ar_status = 'REGISTERED'.
--   d) test_completion_rate defaults to 0.
--
-- ============================================================================

-- 1. Add columns with NOT NULL + sensible defaults

ALTER TABLE sample_inward_register
    ADD COLUMN ar_status VARCHAR(32) NOT NULL DEFAULT 'REGISTERED';

ALTER TABLE sample_inward_register
    ADD COLUMN test_completion_rate NUMERIC(5,2) NOT NULL DEFAULT 0;

-- 2. Validate allowed values — use a CHECK constraint so PostgreSQL/MySQL/H2 all reject bad values
ALTER TABLE sample_inward_register
    ADD CONSTRAINT chk_sample_inward_ar_status
        CHECK (ar_status IN ('REGISTERED', 'IN_PROGRESS', 'ALL_TESTS_COMPLETE', 'COMPLETED', 'CANCELLED'));

-- 3. Back-fill ar_status based on existing test_execution data (if any)

-- 3a. ARs with all terminal tests → ALL_TESTS_COMPLETE
UPDATE sample_inward_register
SET ar_status = 'ALL_TESTS_COMPLETE'
WHERE id IN (
    SELECT te_sample_inward_id FROM (
        SELECT te.te_sample_inward_id
        FROM test_execution te
        WHERE te.status = 'ACTIVE'
          AND te.te_execution_status IN ('Released', 'Cancelled_Not_Performed')
        EXCEPT
        SELECT te.te_sample_inward_id
        FROM test_execution te
        WHERE te.status = 'ACTIVE'
          AND te.te_execution_status NOT IN ('Released', 'Cancelled_Not_Performed')
    ) AS all_terminal
);

-- 3b. ARs that have any active non-terminal test → IN_PROGRESS
UPDATE sample_inward_register
SET ar_status = 'IN_PROGRESS'
WHERE id IN (
    SELECT DISTINCT te.te_sample_inward_id
    FROM test_execution te
    WHERE te.status = 'ACTIVE'
      AND te.te_execution_status NOT IN ('Released', 'Cancelled_Not_Performed')
)
AND ar_status = 'REGISTERED';  -- don't downgrade ALL_TESTS_COMPLETE

-- 4. Back-fill test_completion_rate for existing ARs
UPDATE sample_inward_register
SET test_completion_rate = COALESCE(
    (
        SELECT ROUND(
            CAST(SUM(CASE WHEN te.te_execution_status IN ('Released', 'Cancelled_Not_Performed') THEN 1 ELSE 0 END) AS NUMERIC)
            / NULLIF(CAST(COUNT(*) AS NUMERIC), 0) * 100,
            2
        )
        FROM test_execution te
        WHERE te.te_sample_inward_id = sample_inward_register.id
          AND te.status = 'ACTIVE'
    ),
    0
);

-- 5. Indexes for common filter paths

CREATE INDEX idx_sample_inward_ar_status
    ON sample_inward_register (ar_status);

CREATE INDEX idx_sample_inward_product_status
    ON sample_inward_register (product_id, ar_status);

-- ============================================================================
-- Rollback (manual only — Flyway does not auto-rollback):
--
--   ALTER TABLE sample_inward_register DROP CONSTRAINT chk_sample_inward_ar_status;
--   DROP INDEX IF EXISTS idx_sample_inward_ar_status;
--   DROP INDEX IF EXISTS idx_sample_inward_product_status;
--   ALTER TABLE sample_inward_register DROP COLUMN IF EXISTS ar_status;
--   ALTER TABLE sample_inward_register DROP COLUMN IF EXISTS test_completion_rate;
-- ============================================================================
