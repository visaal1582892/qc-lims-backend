-- V6: OOS Incident Register table
-- SRS Module 13, line 399: Out of Specification Incident tracking

CREATE TABLE oos_incident_register (
    incident_id SERIAL PRIMARY KEY,
    incident_number VARCHAR(100) NOT NULL UNIQUE,
    ar_number VARCHAR(50) NOT NULL REFERENCES sample_inward_register(ar_number),
    execution_id INT NOT NULL REFERENCES test_execution(execution_id),
    raised_by_user_id INT NOT NULL,
    qc_head_authorized_id INT,
    investigation_summary TEXT,
    retest_attempt_count INT DEFAULT 1,
    final_disposition VARCHAR(50) CHECK (final_disposition IN ('Pending', 'Retest_Authorized', 'Batch_Rejected')),

    -- BaseEntity columns:
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);
