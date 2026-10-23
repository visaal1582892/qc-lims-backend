-- ============================================================
-- QC LIMS — Seed Data
-- Roles, Rights, Role-Right mappings, default admin user, master data
-- ============================================================

-- ---------------------------
-- 1. Rights
-- ---------------------------

INSERT INTO `rights` (`right_id`, `right_name`, `description`) VALUES
('INWARD_CREATE',         'Create Inward Entry',          'Create new sample inward entries'),
('INWARD_READ',           'Read Inward Entries',          'View inward entries'),
('INWARD_UPDATE',         'Update Inward Entries',        'Modify inward entries'),
('INWARD_DELETE',         'Delete Inward Entries',        'Delete inward entries'),
('INWARD_APPROVE',        'Approve Inward Entries',       'Approve incoming samples'),
('SAMPLE_VIEW',           'View Samples',                 'View test sample details'),
('SAMPLE_UPDATE',         'Update Samples',               'Modify sample records'),
('RESULT_SUBMIT',         'Submit Test Results',          'Submit test results for parameters'),
('RESULT_VIEW',           'View Test Results',            'View test results'),
('RESULT_REVIEW',         'Review Test Results',          'Review and approve test results'),
('TRF_GENERATE',          'Generate TRF',                 'Generate Test Report Forms'),
('TRF_APPROVE',           'Approve TRF',                  'Approve Test Report Forms'),
('TRF_VIEW',              'View TRF',                     'View Test Report Forms'),
('CERTIFICATE_GENERATE',  'Generate Certificate',         'Generate certificates of analysis'),
('CERTIFICATE_APPROVE',   'Approve Certificate',          'Approve certificates'),
('OOS_INITIATE',          'Initiate OOS',                 'Initiate Out of Specification investigation'),
('OOS_INVESTIGATE',       'Investigate OOS',              'Conduct OOS investigation'),
('OOS_REVIEW',            'Review OOS',                   'Review OOS investigation results'),
('OOS_APPROVE',           'Approve OOS',                  'Approve OOS resolution'),
('OOT_INITIATE',          'Initiate OOT',                 'Initiate Out of Trend investigation'),
('OOT_INVESTIGATE',       'Investigate OOT',              'Conduct OOT investigation'),
('OOT_REVIEW',            'Review OOT',                   'Review OOT investigation results'),
('OOT_APPROVE',           'Approve OOT',                  'Approve OOT resolution'),
('DASHBOARD_VIEW',        'View Dashboard',               'Access dashboard views'),
('ANALYTICS_VIEW',        'View Analytics',               'Access analytics and reports'),
('AUDIT_TRAIL_VIEW',      'View Audit Trail',             'View audit trail logs'),
('USER_CREATE',           'Create User',                  'Create new users'),
('USER_READ',             'Read Users',                   'View user list and details'),
('USER_UPDATE',           'Update User',                  'Modify user profiles and roles'),
('USER_DELETE',           'Delete User',                  'Disable/delete users'),
('ROLE_CREATE',           'Create Role',                  'Create new roles'),
('ROLE_READ',             'Read Roles',                   'View roles'),
('ROLE_UPDATE',           'Update Role',                  'Modify roles and rights'),
('ROLE_DELETE',           'Delete Role',                  'Delete roles'),
('MASTER_DATA_READ',      'Read Master Data',             'View master data tables'),
('MASTER_DATA_CREATE',    'Create Master Data',           'Add master data records'),
('MASTER_DATA_UPDATE',    'Update Master Data',           'Modify master data records'),
('MASTER_DATA_DELETE',    'Delete Master Data',           'Delete master data records'),
('RACING_CONFIG_READ',    'Read Routing Config',          'View routing rules'),
('RACING_CONFIG_UPDATE',  'Update Routing Config',        'Create/modify routing rules'),
('CHANGE_OWN_PASSWORD',   'Change Own Password',          'Change own password'),
('VIEW_OWN_NOTIFICATIONS','View Own Notifications',       'View own notifications');

-- ---------------------------
-- 2. Roles
-- ---------------------------

INSERT INTO `roles` (`role_code`, `role_name`, `description`) VALUES
('LAB_MANAGER',        'Lab Manager',         'Overall QC lab administration and final approvals'),
('INWARD_OFFICER',     'Inward Officer',      'Receives and registers incoming samples'),
('CHEMIST',            'Chemist',             'Performs chemical analysis'),
('MICROBIOLOGIST',     'Microbiologist',      'Performs microbiological analysis'),
('PHYSICIST',          'Physicist',           'Performs physical/pharmaceutical analysis'),
('DATA_ENTRY',         'Data Entry Operator',  'Enters test results into system'),
('QA_OFFICER',         'QA Officer',          'Quality assurance review and compliance'),
('QAU_MANAGER',        'QAU Manager',         'Quality assurance unit head'),
('DOCUMENTATION',      'Documentation Officer','TRF and certificate management'),
('INSTRUMENT_SCIENTIST','Instrument Scientist','Instrument management and calibration'),
('ADMIN',              'Admin',               'System administrator with full access'),
('TRAINING_COORDINATOR','Training Coordinator','Training records and scheduling'),
('SUPERVISOR',         'Supervisor',          'Team supervision and result review'),
('REPORTING_MANAGER',  'Reporting Manager',   'Analytics and management reports'),
('STORE_KEEPER',       'Store Keeper',        'Sample storage and retention management');

-- ---------------------------
-- 3. Role-Right Mappings
-- ---------------------------

-- ADMIN — full access
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`)
SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'ADMIN';

-- LAB_MANAGER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'LAB_MANAGER' AND ri.right_id IN (
  'DASHBOARD_VIEW','ANALYTICS_VIEW','AUDIT_TRAIL_VIEW','SAMPLE_VIEW','RESULT_VIEW','RESULT_REVIEW',
  'OOS_REVIEW','OOS_APPROVE','OOT_REVIEW','OOT_APPROVE','TRF_APPROVE','CERTIFICATE_APPROVE',
  'INWARD_APPROVE','USER_READ','ROLE_READ','MASTER_DATA_READ','RACING_CONFIG_READ',
  'CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- INWARD_OFFICER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'INWARD_OFFICER' AND ri.right_id IN (
  'INWARD_CREATE','INWARD_READ','INWARD_UPDATE','INWARD_APPROVE','SAMPLE_VIEW','DASHBOARD_VIEW',
  'CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- CHEMIST
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'CHEMIST' AND ri.right_id IN (
  'SAMPLE_VIEW','RESULT_SUBMIT','RESULT_VIEW','OOS_INITIATE','OOS_INVESTIGATE',
  'OOT_INITIATE','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- MICROBIOLOGIST
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'MICROBIOLOGIST' AND ri.right_id IN (
  'SAMPLE_VIEW','RESULT_SUBMIT','RESULT_VIEW','OOS_INITIATE','OOS_INVESTIGATE',
  'OOT_INITIATE','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- PHYSICIST
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'PHYSICIST' AND ri.right_id IN (
  'SAMPLE_VIEW','RESULT_SUBMIT','RESULT_VIEW','OOS_INITIATE','OOS_INVESTIGATE',
  'OOT_INITIATE','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- DATA_ENTRY
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'DATA_ENTRY' AND ri.right_id IN (
  'RESULT_SUBMIT','RESULT_VIEW','SAMPLE_VIEW','DASHBOARD_VIEW',
  'CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- QA_OFFICER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'QA_OFFICER' AND ri.right_id IN (
  'RESULT_REVIEW','RESULT_VIEW','SAMPLE_VIEW','OOS_INVESTIGATE','OOS_REVIEW',
  'OOT_INVESTIGATE','OOT_REVIEW','AUDIT_TRAIL_VIEW','DASHBOARD_VIEW','TRF_VIEW',
  'CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- QAU_MANAGER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'QAU_MANAGER' AND ri.right_id IN (
  'RESULT_REVIEW','RESULT_VIEW','SAMPLE_VIEW','OOS_REVIEW','OOS_APPROVE',
  'OOT_REVIEW','OOT_APPROVE','AUDIT_TRAIL_VIEW','DASHBOARD_VIEW','ANALYTICS_VIEW',
  'TRF_APPROVE','TRF_VIEW','CERTIFICATE_APPROVE','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- DOCUMENTATION
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'DOCUMENTATION' AND ri.right_id IN (
  'TRF_GENERATE','TRF_VIEW','TRF_APPROVE','CERTIFICATE_GENERATE','CERTIFICATE_APPROVE',
  'SAMPLE_VIEW','RESULT_VIEW','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- INSTRUMENT_SCIENTIST
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'INSTRUMENT_SCIENTIST' AND ri.right_id IN (
  'DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- SUPERVISOR
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'SUPERVISOR' AND ri.right_id IN (
  'RESULT_REVIEW','RESULT_VIEW','SAMPLE_VIEW','OOS_REVIEW',
  'DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- REPORTING_MANAGER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'REPORTING_MANAGER' AND ri.right_id IN (
  'ANALYTICS_VIEW','DASHBOARD_VIEW','AUDIT_TRAIL_VIEW','SAMPLE_VIEW','RESULT_VIEW','TRF_VIEW',
  'CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- TRAINING_COORDINATOR
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'TRAINING_COORDINATOR' AND ri.right_id IN (
  'USER_READ','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- STORE_KEEPER
INSERT INTO `role_rights_mapping` (`role_id`, `right_id`) SELECT r.id, ri.right_id FROM roles r, rights ri WHERE r.role_code = 'STORE_KEEPER' AND ri.right_id IN (
  'SAMPLE_VIEW','DASHBOARD_VIEW','CHANGE_OWN_PASSWORD','VIEW_OWN_NOTIFICATIONS');

-- ---------------------------
-- 4. Default Admin User (password: Admin@123)
-- ---------------------------

INSERT INTO `users` (`full_name`, `email`, `password_hash`, `department_name`, `status`)
VALUES ('System Administrator', 'admin@medplus.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'QC Lab', 'ACTIVE');

INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT 1, id FROM roles WHERE role_code = 'ADMIN';

-- ---------------------------
-- 5. Master Data — Departments
-- ---------------------------

INSERT INTO `departments` (`department_id`, `department_name`, `description`) VALUES
('QC',       'Quality Control',         'QC laboratory'),
('QA',       'Quality Assurance',       'Quality assurance unit'),
('R&D',      'Research and Development','R&D department'),
('PROD',     'Production',              'Manufacturing'),
('STORES',   'Stores',                  'Warehouse and inventory');

-- ---------------------------
-- 6. Master Data — Designations
-- ---------------------------

INSERT INTO `designations` (`designation_id`, `designation_name`) VALUES
('JUNIOR',  'Junior Analyst'),
('SR',      'Senior Analyst'),
('EXEC',    'Executive'),
('MANAGER', 'Manager'),
('DGM',     'Deputy General Manager'),
('GM',      'General Manager');

-- ---------------------------
-- 7. Master Data — Sample Types
-- ---------------------------

INSERT INTO `sample_types` (`type_code`, `type_name`, `description`, `retention_days`) VALUES
('RAW',          'Raw Material',       'Incoming raw material samples',           180),
('API',          'Active Pharma Ing.','API bulk material',                        180),
('PACK',         'Packaging Material', 'Primary and secondary packaging',         90),
('BLK',          'Blank',              'Blank samples for instrument calibration',30),
('INP',          'In-Process',         'In-process control samples',             90),
('FGP',          'Finished Product',   'Finished pharmaceutical product',        365),
('DISP',         'Dispensed',          'Dispensed samples',                      30),
('RET',          'Retest',             'Retest samples',                         365),
('STB',          'Stability',          'Stability study samples',                730),
('ENV',          'Environmental',      'Environmental monitoring samples',       90),
('WTR',          'Water',              'Water samples (WFI, PW, Pure Steam)',    30),
('CLN',          'Cleaned',            'Cleaned equipment swab samples',         90),
('REF',          'Reference Std',      'Reference standard samples',             365);

-- ---------------------------
-- 8. Master Data — Test Parameters
-- ---------------------------

INSERT INTO `test_parameters` (`parameter_code`, `parameter_name`, `category`, `unit`, `spec_min`, `spec_max`, `method`) VALUES
('ASSAY',       'Assay',              'Chemical',  '%',    95.0,  105.0,  'HPLC'),
('DL',          'Dissolution',        'Chemical',  '%',    0,     100,    'USP <711>'),
('HP',          'Heavy Metals',       'Chemical',  'ppm',  0,     20,     'Atomic Absorption'),
('LC',          'Loss on Drying',     'Chemical',  '%',    0,     5.0,    'Drying Oven'),
('PH',          'pH',                 'Chemical',  'pH',   3.0,   11.0,   'pH Meter'),
('WT',          'Water Content',      'Chemical',  '%',    0,     1.0,    'Karl Fischer'),
('CR',          'Crushing Strength',  'Physical',  'kp',   0,     50,     'Crushing Strength Tester'),
('TF',          'Thickness',          'Physical',  'mm',   0.5,   8.0,    'Vernier Caliper'),
('HD',          'Hardness',           'Physical',  'kp',   0,     15,     'Hardness Tester'),
('FD',          'Friability',         'Physical',  '%',    0,     1.0,    'Friability Tester'),
('UT',          'Uniformity of Weight','Physical', '%RSD', 0,     10,     'Weighing'),
('WD',          'Water Determination','Chemical',  '%',    0,     1.0,    'Karl Fischer'),
('CRD',         'Content Uniformity', 'Chemical',  '%',    80,    120,    'HPLC'),
('IR',          'IR Spectroscopy',    'Chemical',  NULL,   NULL,  NULL,   'IR Spectrophotometer'),
('CV',          'Chromatographic Purity','Chemical','%Area',98.0, 100.0, 'HPLC/GC'),
('TTC',         'Total Plate Count',  'Microbial', 'cfu/g',0,     1000,   'Microbial Culture'),
('TVC',         'Total Viable Count', 'Microbial', 'cfu/g',0,     1000,   'Microbial Culture'),
('TH',          'Total Aerobic Bacteria','Microbial','cfu/g',0, 100, 'Microbial Culture'),
('YM',          'Yeast and Mold Count','Microbial','cfu/g',0, 100, 'Microbial Culture'),
('SP',          'Specific Pathogens', 'Microbial', 'NMT',  0,     0,      'Microbial Culture'),
('ET',          'Endotoxin Test',     'Microbial', 'EU/mL',0,     0.25,   'LAL Method'),
('SD',          'Sterility Test',     'Microbial', 'Pass/Fail', NULL, NULL, 'Membrane Filtration'),
('VP',          'Viscosity',          'Physical',  'cP',   0,     1000,   'Viscometer'),
('RS',          'Residual Solvents',  'Chemical',  'ppm',  0,     5000,   'GC');

-- ---------------------------
-- 9. Master Data — Instruments
-- ---------------------------

INSERT INTO `instruments` (`instrument_id`, `instrument_name`, `model`, `status`) VALUES
('HPLC-01',   'HPLC System',              'Shimadzu LC-20AT',          'ACTIVE'),
('GC-01',     'Gas Chromatograph',         'Shimadzu GC-2014',          'ACTIVE'),
('UV-01',     'UV-Vis Spectrophotometer',  'Shimadzu UV-1800',          'ACTIVE'),
('IR-01',     'FTIR Spectrophotometer',    'Perkin Elmer Spectrum',     'ACTIVE'),
('KF-01',     'Karl Fischer Titrator',     'Mettler Toledo DL32',       'ACTIVE'),
('PH-01',     'pH Meter',                  'Mettler Toledo SevenCompact','ACTIVE'),
('CS-01',     'Crushing Strength Tester',  'Erweka ZRT',               'ACTIVE'),
('FT-01',     'Friability Tester',         'Erweka TA',                'ACTIVE'),
('VS-01',     'Viscometer',                'Brookfield DV2T',          'ACTIVE'),
('BAL-01',    'Analytical Balance',        'Mettler Toledo XP205',      'ACTIVE');

-- ---------------------------
-- 10. Master Data — Sample Storage Conditions
-- ---------------------------

INSERT INTO `sample_storage_conditions` (`condition_name`, `temperature_min`, `temperature_max`, `humidity_min`, `humidity_max`) VALUES
('Room Temperature',     20.0, 25.0, 40.0, 60.0),
('Refrigerated',         2.0,  8.0,  NULL, NULL),
('Freezer',             -25.0, -15.0, NULL, NULL),
('Desiccated',          15.0, 25.0,  0.0,  40.0),
('Cold Room',           2.0,  8.0,  NULL, NULL);

-- ---------------------------
-- 11. Master Data — OOS Escalation Rules
-- ---------------------------

INSERT INTO `oos_escalation_rules` (`phase`, `escalation_role_code`, `sla_hours`, `description`) VALUES
('LEVEL_1', 'SUPERVISOR',    24, 'Level 1 investigation by supervisor within 24 hours'),
('LEVEL_2', 'LAB_MANAGER',  72, 'Level 2 investigation by lab manager within 72 hours'),
('APPEAL',  'QAU_MANAGER',  168, 'Appeal reviewed by QAU manager within 7 days');

-- ---------------------------
-- 12. Init OOS Sequence Tracker
-- ---------------------------

INSERT INTO `oos_sequence_tracker` (`current_number`) VALUES (0);
