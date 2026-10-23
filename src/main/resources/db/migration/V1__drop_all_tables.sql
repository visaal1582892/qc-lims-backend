-- V1__drop_all_tables.sql
-- Drops all existing QC LIMS tables to provide a clean slate for fresh development.
-- This migration intentionally removes every table created by previous schema versions.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS flyway_schema_history;
DROP TABLE IF EXISTS document_registry;
DROP TABLE IF EXISTS subcontract_register;
DROP TABLE IF EXISTS trial_test_register;
DROP TABLE IF EXISTS oos_incident_register;
DROP TABLE IF EXISTS review_history;
DROP TABLE IF EXISTS review_audit_log;
DROP TABLE IF EXISTS test_execution;
DROP TABLE IF EXISTS qc_inventory;
DROP TABLE IF EXISTS storage_location;
DROP TABLE IF EXISTS receipt_checklist;
DROP TABLE IF EXISTS trf_parameters;
DROP TABLE IF EXISTS trf_test_parameters;
DROP TABLE IF EXISTS trf_receipt_checklist;
DROP TABLE IF EXISTS trf;
DROP TABLE IF EXISTS trf_master;
DROP TABLE IF EXISTS sample_inward_register;
DROP TABLE IF EXISTS test_parameter_master;
DROP TABLE IF EXISTS ar_sequence_tracker;
DROP TABLE IF EXISTS oos_sequence_tracker;

SET FOREIGN_KEY_CHECKS = 1;
