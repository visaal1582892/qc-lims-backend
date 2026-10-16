-- V2: Test Parameter Master table
-- SRS Module 13 DDL with BaseEntity.status column (RC-1) and UNIQUE INDEX for duplicate prevention

CREATE TABLE test_parameter_master (
    parameter_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    parameter_name VARCHAR(150) NOT NULL,
    standard_reference VARCHAR(50) CHECK (
        standard_reference IN ('IP', 'BP', 'USP', 'EP', 'JP', 'IS', 'MOA', 'STP')
    ),
    stp_document_number VARCHAR(100),
    stp_version_number VARCHAR(20),
    min_limit DECIMAL(12,4),
    max_limit DECIMAL(12,4),
    text_specification TEXT,
    is_critical TINYINT(1) DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Indexes for common query patterns
CREATE INDEX idx_test_parameter_product ON test_parameter_master(product_id);
CREATE INDEX idx_test_parameter_name ON test_parameter_master(parameter_name);

-- UNIQUE INDEX on (product_id, parameter_name, status) to enforce duplicate prevention
-- and allow re-creation after soft delete (status=INACTIVE)
CREATE UNIQUE INDEX uidx_test_parameter_product_name_status
    ON test_parameter_master(product_id, parameter_name, status);
