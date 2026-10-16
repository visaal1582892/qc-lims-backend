-- V9: Performance indexes for dashboard aggregation and analytics queries
-- SRS Module 12: QC Dashboard & Multi-Dimensional Tracking
-- All source tables defined in V3-V8 migrations; no new tables created here.
-- Only indexes on columns NOT already covered by V3/V4 are included.
-- Redundant indexes removed per plan review C-1:
--   idx_trf_status (already in V4), idx_inward_ar_number/batch/receipt_date (already in V3)

-- test_execution: execution_status for Active Execution & Quality Outcomes (R-02, R-03)
CREATE INDEX IF NOT EXISTS idx_test_execution_status ON test_execution(execution_status);

-- test_execution: allocated_to_analyst_id for analyst-wise productivity (R-08)
CREATE INDEX IF NOT EXISTS idx_test_execution_analyst ON test_execution(allocated_to_analyst_id);

-- test_execution: created_at for date-range filtering (R-09)
CREATE INDEX IF NOT EXISTS idx_test_execution_created_at ON test_execution(created_at);

-- test_execution: parameter_id for parameter-wise analytics (R-10)
CREATE INDEX IF NOT EXISTS idx_test_execution_parameter ON test_execution(parameter_id);

-- test_execution: compliance_flag for quality outcome filtering
CREATE INDEX IF NOT EXISTS idx_test_execution_compliance ON test_execution(compliance_flag);

-- test_execution: ar_number for JOINs with sample_inward_register
CREATE INDEX IF NOT EXISTS idx_test_execution_ar ON test_execution(ar_number);

-- oos_incident_register: final_disposition for active OOS count (R-04)
CREATE INDEX IF NOT EXISTS idx_oos_disposition ON oos_incident_register(final_disposition);

-- sample_inward_register: ar_status for quality outcome joins (not in V3)
CREATE INDEX IF NOT EXISTS idx_sample_inward_ar_status ON sample_inward_register(ar_status);
