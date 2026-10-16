-- V5: Test Execution table
-- SRS Module 13, line 373: Test Allocation & Execution State Machine

CREATE TABLE test_execution (
    execution_id SERIAL PRIMARY KEY,
    ar_number VARCHAR(50) REFERENCES sample_inward_register(ar_number),
    parameter_id INT NOT NULL REFERENCES test_parameter_master(parameter_id),
    allocated_to_analyst_id INT NOT NULL,
    assigned_by_qc_head_id INT NOT NULL,
    allocated_units INT NOT NULL,
    consumed_units INT DEFAULT 0,

    execution_status VARCHAR(50) DEFAULT 'Allocated' CHECK (
        execution_status IN ('Allocated', 'Testing', 'Hold', 'Cancelled_Not_Performed', 'Under_Review', 'Rework_Requested', 'Released')
    ),
    hold_or_cancel_reason TEXT,
    rework_reason TEXT,

    result_numeric_value DECIMAL(12,4),
    result_text_value VARCHAR(255),
    validity_status VARCHAR(20) DEFAULT 'Valid' CHECK (validity_status IN ('Valid', 'Invalid')),
    compliance_flag VARCHAR(20) CHECK (compliance_flag IN ('PASS', 'FAIL', 'PENDING')),
    roa_file_path TEXT,
    coa_file_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,

    -- BaseEntity columns:
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);
