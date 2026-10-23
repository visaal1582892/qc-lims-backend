-- ============================================================
-- QC LIMS — Seed Data (Phase 1)
-- Base role, right, and admin user only
-- ============================================================

-- ---------------------------
-- 1. Right
-- ---------------------------

INSERT INTO `rights` (`right_id`, `right_name`, `description`, `status`) VALUES
('ROLE_SYSTEM_ADMIN', 'System Administrator', 'Full system access — grants all rights', 'ACTIVE');

-- ---------------------------
-- 2. Role
-- ---------------------------

INSERT INTO `roles` (`role_code`, `role_name`, `description`, `status`) VALUES
('ADMIN', 'Admin', 'System administrator role');

-- ---------------------------
-- 3. Role-Right mapping
-- ---------------------------

INSERT INTO `role_rights` (`role_id`, `right_id`)
SELECT r.role_id, 'ROLE_SYSTEM_ADMIN' FROM roles r WHERE r.role_code = 'ADMIN';

-- ---------------------------
-- 4. Admin User (password: medplus@123)
-- ---------------------------

INSERT INTO `users` (`employee_id`, `full_name`, `password_hash`, `status`) VALUES
('admin', 'System Administrator', '$2b$10$7Dohn16/XeoHmDMYUPiOd.qSiWgt4bKJQ.19DuEwdR.jqNO5TkwIK', 'ACTIVE');

INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.user_id, r.role_id FROM users u, roles r
WHERE u.employee_id = 'admin' AND r.role_code = 'ADMIN';
