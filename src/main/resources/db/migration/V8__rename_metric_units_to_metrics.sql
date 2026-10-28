-- =============================================================
-- V8: Rename Metric Units Master to Metrics Master
-- =============================================================

-- 1. Drop foreign key and dependent indexes on metric_specifications
ALTER TABLE metric_specifications DROP FOREIGN KEY fk_metric_spec_unit;
ALTER TABLE metric_specifications DROP INDEX idx_metric_spec_unit_id;
ALTER TABLE metric_specifications DROP INDEX idx_metric_spec_combo;
ALTER TABLE metric_specifications DROP INDEX uq_metric_spec_active_only;

-- 2. Rename table metric_unit_master -> metric_master
RENAME TABLE metric_unit_master TO metric_master;

-- 3. Rename columns and indexes in metric_master
ALTER TABLE metric_master
    RENAME COLUMN metric_unit_id TO metric_id,
    RENAME COLUMN name TO metric_name,
    RENAME COLUMN sub_name TO unit;

ALTER TABLE metric_master RENAME INDEX idx_metric_unit_updated_id TO idx_metric_updated_id;
ALTER TABLE metric_master RENAME INDEX idx_metric_unit_created_id TO idx_metric_created_id;
ALTER TABLE metric_master RENAME INDEX idx_metric_unit_status_name TO idx_metric_status_name;

-- 4. Rename table metric_unit_master_log -> metric_master_log
RENAME TABLE metric_unit_master_log TO metric_master_log;

-- 5. Rename columns in metric_master_log
ALTER TABLE metric_master_log
    RENAME COLUMN metric_unit_id TO metric_id,
    RENAME COLUMN name TO metric_name,
    RENAME COLUMN sub_name TO unit;

-- 6. Rename column metric_unit_id -> metric_id in metric_specifications and metric_specifications_log
ALTER TABLE metric_specifications
    RENAME COLUMN metric_unit_id TO metric_id;

ALTER TABLE metric_specifications_log
    RENAME COLUMN metric_unit_id TO metric_id;

-- 7. Re-add foreign key and indexes with updated column names
ALTER TABLE metric_specifications
    ADD CONSTRAINT fk_metric_spec_metric
        FOREIGN KEY (metric_id)
        REFERENCES metric_master(metric_id)
        ON DELETE RESTRICT;

CREATE INDEX idx_metric_spec_metric_id ON metric_specifications (metric_id);

CREATE INDEX idx_metric_spec_combo ON metric_specifications (config_id, metric_id, status);

CREATE UNIQUE INDEX uq_metric_spec_active_only ON metric_specifications (
    config_id,
    metric_id,
    (CASE WHEN status = 'A' THEN 1 ELSE NULL END)
);
