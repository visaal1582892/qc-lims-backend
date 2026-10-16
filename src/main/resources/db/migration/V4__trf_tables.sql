-- V4: e-TRF Management tables (SRS Module 3)
-- trf_master, trf_test_parameters, trf_receipt_checklist
-- All tables include BaseEntity.status columns (RC-3)

-- e-TRF Master (SRS Module 3, 13)
CREATE TABLE trf_master (
    trf_id INT AUTO_INCREMENT PRIMARY KEY,
    grn_number VARCHAR(100),
    manufacturer_id INT NOT NULL,
    product_id INT NOT NULL,

    -- Sample Particulars (SRS 3.1 B)
    batch_numbers TEXT NOT NULL,
    customer_ar_no VARCHAR(100),
    material_type VARCHAR(100) CHECK (material_type IN ('RM', 'In-process', 'API', 'Drug product', 'Cosmetics', 'Others')),
    other_material_type_desc VARCHAR(150),
    mfg_date DATE NOT NULL,
    exp_date DATE NOT NULL,
    retest_date DATE NOT NULL,
    batch_size INT,
    sample_qty INT NOT NULL,
    packing_condition VARCHAR(150),
    storage_condition VARCHAR(100) NOT NULL CHECK (storage_condition IN ('Ambient', 'Freezer', 'Refrigerator', 'Others')),
    other_storage_desc VARCHAR(150),

    -- Sample Type Flags (SRS 3.1 B)
    is_hygroscopic TINYINT(1) DEFAULT FALSE,
    is_light_sensitive TINYINT(1) DEFAULT FALSE,
    is_non_hazardous TINYINT(1) DEFAULT FALSE,
    is_hazardous TINYINT(1) DEFAULT FALSE,
    is_msds_attached TINYINT(1) DEFAULT FALSE,
    other_sample_type_desc VARCHAR(150),

    -- Submission & Analysis (SRS 3.1 C)
    submission_type VARCHAR(50) CHECK (submission_type IN ('By Person', 'By Courier', 'By Post', 'Others')),
    submission_type_desc VARCHAR(150),
    analysis_type VARCHAR(100) NOT NULL CHECK (analysis_type IN ('Regulatory', 'Non-Regulatory/R&D')),
    regulatory_form_type VARCHAR(100),
    pharmacopoeia_reference VARCHAR(50) NOT NULL CHECK (
        pharmacopoeia_reference IN ('USP', 'BP', 'EP', 'IP', 'JP', 'IS', 'MOA', 'STP')
    ),
    stp_document_no VARCHAR(100),
    stp_version_no VARCHAR(50),

    -- Customer/Manufacturer Details (SRS 3.1 A)
    company_name VARCHAR(255) NOT NULL,
    company_address TEXT,
    manufacturing_lic_no VARCHAR(100) NOT NULL,
    contact_person_name VARCHAR(150),
    phone_mobile VARCHAR(50),
    email_id VARCHAR(150),

    -- Workflow Status (SRS 3.2)
    trf_status VARCHAR(50) DEFAULT 'Pending_Feasibility' CHECK (
        trf_status IN ('Pending_Feasibility', 'Rejected', 'Accepted')
    ),
    submitted_by_user_id INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- BaseEntity columns (RC-3):
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_trf_product ON trf_master(product_id);
CREATE INDEX idx_trf_manufacturer ON trf_master(manufacturer_id);
CREATE INDEX idx_trf_status ON trf_master(trf_status);
CREATE INDEX idx_trf_grn ON trf_master(grn_number);

-- Junction table: e-TRF to Test Parameters (surrogate id per OQ-05)
CREATE TABLE trf_test_parameters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trf_id INT NOT NULL REFERENCES trf_master(trf_id) ON DELETE CASCADE,
    parameter_id INT NOT NULL REFERENCES test_parameter_master(parameter_id),
    is_active_for_testing TINYINT(1) DEFAULT TRUE,
    -- BaseEntity columns (RC-3):
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE UNIQUE INDEX uidx_trf_test_param_status ON trf_test_parameters(trf_id, parameter_id, status);

-- Receipt Checklist (SRS 3.1 D)
CREATE TABLE trf_receipt_checklist (
    checklist_id INT AUTO_INCREMENT PRIMARY KEY,
    trf_id INT NOT NULL REFERENCES trf_master(trf_id) ON DELETE CASCADE,
    ar_number VARCHAR(50) REFERENCES sample_inward_register(ar_number),

    -- Physical Verification (SRS 3.1 D)
    match_sample_name_batch VARCHAR(10) NOT NULL CHECK (match_sample_name_batch IN ('Yes', 'No', 'NA')),
    free_from_damage_leakage VARCHAR(10) NOT NULL CHECK (free_from_damage_leakage IN ('Yes', 'No', 'NA')),
    pharmacopoeial_ref_mentioned VARCHAR(10) NOT NULL CHECK (pharmacopoeial_ref_mentioned IN ('Yes', 'No', 'NA')),

    verified_by_user_id INT NOT NULL,
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- BaseEntity columns (RC-3):
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE UNIQUE INDEX uidx_receipt_trf ON trf_receipt_checklist(trf_id);
CREATE INDEX idx_receipt_ar ON trf_receipt_checklist(ar_number);
