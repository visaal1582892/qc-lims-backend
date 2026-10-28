-- ===================================
-- Phase 2.1: Composite Search & Sort Indexes
-- database-search-patterns-guide.md
-- ===================================

-- Metric Unit Master Indexes
CREATE INDEX idx_metric_unit_updated_id ON metric_unit_master (updated_at DESC, metric_unit_id DESC);
CREATE INDEX idx_metric_unit_created_id ON metric_unit_master (created_at DESC, metric_unit_id DESC);
CREATE INDEX idx_metric_unit_status_name ON metric_unit_master (status, name);

-- Test Parameter Master Indexes
CREATE INDEX idx_test_param_updated_id ON test_parameter_master (updated_at DESC, param_id DESC);
CREATE INDEX idx_test_param_created_id ON test_parameter_master (created_at DESC, param_id DESC);
CREATE INDEX idx_test_param_status_name ON test_parameter_master (status, name);

-- Analyst Qualification Master Indexes
CREATE INDEX idx_analyst_qual_updated_id ON analyst_qualification_master (updated_at DESC, qualification_id DESC);
CREATE INDEX idx_analyst_qual_created_id ON analyst_qualification_master (created_at DESC, qualification_id DESC);
CREATE INDEX idx_analyst_qual_status_emp ON analyst_qualification_master (status, employee_id);

-- QC Inventory Configuration Indexes
CREATE INDEX idx_qc_inv_updated_id ON qc_inventory_configuration (updated_at DESC, config_id DESC);
CREATE INDEX idx_qc_inv_created_id ON qc_inventory_configuration (created_at DESC, config_id DESC);
CREATE INDEX idx_qc_inv_status_prod ON qc_inventory_configuration (status, product_id);
