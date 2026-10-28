-- ============================================================
-- QC LIMS — Seed all rights and map to ADMIN role (role_id = 1)
-- ============================================================
-- Inserts every right defined in Rights.java (excluding
-- ROLE_SYSTEM_ADMIN already seeded in V2), then maps all
-- rights to the ADMIN role so the admin user has full access.

-- ---------------------------
-- 1. Rights (all except ROLE_SYSTEM_ADMIN seeded in V2)
-- ---------------------------

INSERT INTO `rights` (`right_id`, `right_name`, `description`, `status`) VALUES
('ROLE_CHANGE_OWN_PASSWORD',      'Change Own Password',      'User may change their own password',                        'ACTIVE'),
('ROLE_VIEW_OWN_NOTIFICATIONS',   'View Own Notifications',   'User may view their own notifications',                     'ACTIVE'),

('ROLE_USER_CREATE',              'User Create',              'Create new users',                                          'ACTIVE'),
('ROLE_USER_READ',                'User Read',                'View user list and details',                                'ACTIVE'),
('ROLE_USER_UPDATE',              'User Update',              'Modify existing users',                                     'ACTIVE'),
('ROLE_USER_DELETE',              'User Delete',              'Delete (deactivate) users',                                 'ACTIVE'),

('ROLE_ROLE_CREATE',              'Role Create',              'Create new roles',                                          'ACTIVE'),
('ROLE_ROLE_READ',                'Role Read',                'View roles',                                                'ACTIVE'),
('ROLE_ROLE_UPDATE',              'Role Update',              'Modify existing roles',                                     'ACTIVE'),

('ROLE_MASTER_DATA_READ',         'Master Data Read',         'Read all master data entities',                             'ACTIVE'),
('ROLE_MASTER_DATA_CREATE',       'Master Data Create',       'Create/upload all master data entities',                    'ACTIVE'),
('ROLE_MASTER_DATA_UPDATE',       'Master Data Update',       'Update all master data entities',                           'ACTIVE'),
('ROLE_MASTER_DATA_DELETE',       'Master Data Delete',       'Delete (deactivate) all master data entities',              'ACTIVE'),

('ROLE_METRIC_READ',         'Metric Read',         'View metrics',                                         'ACTIVE'),
('ROLE_METRIC_UPLOAD',       'Metric Upload',       'Upload metrics via Excel',                             'ACTIVE'),

('ROLE_TEST_PARAMETER_READ',      'Test Parameter Read',      'View test parameters',                                      'ACTIVE'),
('ROLE_TEST_PARAMETER_UPLOAD',    'Test Parameter Upload',    'Upload test parameters via Excel',                          'ACTIVE'),

('ROLE_QUALIFICATION_READ',       'Analyst Qualification Read','View analyst qualifications',                               'ACTIVE'),
('ROLE_QUALIFICATION_UPLOAD',     'Analyst Qualification Upload','Upload analyst qualifications via Excel',                'ACTIVE'),

('ROLE_INVENTORY_CONFIG_READ',    'Inventory Config Read',    'View QC inventory configurations',                          'ACTIVE'),
('ROLE_INVENTORY_CONFIG_UPLOAD',  'Inventory Config Upload',  'Upload QC inventory configurations via Excel',              'ACTIVE')
ON DUPLICATE KEY UPDATE `right_name` = VALUES(`right_name`);

-- ---------------------------
-- 2. Map ALL rights to ADMIN (role_id = 1)
-- ---------------------------

INSERT IGNORE INTO `role_rights` (`role_id`, `right_id`)
SELECT 1, right_id FROM `rights` WHERE `status` = 'ACTIVE';
