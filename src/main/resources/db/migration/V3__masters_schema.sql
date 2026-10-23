-- ===================================
-- Phase 2: Masters & Bulk Upload Schema
-- goal.md §1 — Database Schema Definitions
-- ===================================

-- 1.1 Mock QC Location Master (no audit, no log — mock only)
CREATE TABLE mock_qc_location_master (
    location_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_name VARCHAR(150) NOT NULL
);

-- 1.2 Metric Unit Master
CREATE TABLE metric_unit_master (
    metric_unit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sub_name VARCHAR(100),
    status VARCHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

-- Metric Unit Log
CREATE TABLE metric_unit_master_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_unit_id BIGINT,
    name VARCHAR(100),
    sub_name VARCHAR(100),
    status VARCHAR(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

-- 1.3 Test Parameter Master
CREATE TABLE test_parameter_master (
    param_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    result_type VARCHAR(20) NOT NULL CHECK (result_type IN ('TEXTUAL', 'METRICS')),
    metric_unit_id BIGINT,
    status VARCHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),

    CONSTRAINT fk_test_param_metric_unit
        FOREIGN KEY (metric_unit_id)
        REFERENCES metric_unit_master(metric_unit_id)
        ON DELETE RESTRICT
);

-- Test Parameter Log
CREATE TABLE test_parameter_master_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    param_id BIGINT,
    name VARCHAR(150),
    result_type VARCHAR(20),
    metric_unit_id BIGINT,
    status VARCHAR(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

-- 1.4 Analyst Qualification Master
CREATE TABLE analyst_qualification_master (
    qualification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    param_id BIGINT NOT NULL,
    qualified_on DATE NOT NULL,
    status VARCHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),

    CONSTRAINT fk_analyst_qual_param
        FOREIGN KEY (param_id)
        REFERENCES test_parameter_master(param_id)
        ON DELETE RESTRICT,

    UNIQUE KEY uq_emp_param_status (employee_id, param_id, status)
);

-- Analyst Qualification Log
CREATE TABLE analyst_qualification_master_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qualification_id BIGINT,
    employee_id VARCHAR(50),
    param_id BIGINT,
    qualified_on DATE,
    status VARCHAR(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

-- 1.5 QC Inventory Configuration
CREATE TABLE qc_inventory_configuration (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    param_id BIGINT NOT NULL,
    count_needed INT NOT NULL CHECK (count_needed > 0),
    status VARCHAR(1) DEFAULT 'A' CHECK (status IN ('A', 'I')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),

    CONSTRAINT fk_inventory_config_param
        FOREIGN KEY (param_id)
        REFERENCES test_parameter_master(param_id)
        ON DELETE RESTRICT,

    UNIQUE KEY uq_prod_param_status (product_id, param_id, status)
);

-- QC Inventory Configuration Log
CREATE TABLE qc_inventory_configuration_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT,
    product_id VARCHAR(50),
    param_id BIGINT,
    count_needed INT,
    status VARCHAR(1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

-- Seed mock locations
INSERT INTO mock_qc_location_master (location_name) VALUES
    ('QC Lab - Floor 1'),
    ('QC Lab - Floor 2'),
    ('Stability Chamber'),
    ('Cold Storage - Room A'),
    ('Cold Storage - Room B');
