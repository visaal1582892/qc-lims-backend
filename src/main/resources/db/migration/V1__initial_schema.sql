-- ============================================================
-- QC LIMS — Initial Schema
-- MySQL 8.0+ / InnoDB / utf8mb4
-- ============================================================

-- Drop flyway baseline marker if present (clean slate)
DROP TABLE IF EXISTS `review_audit_log`;
DROP TABLE IF EXISTS `oos_reviews`;
DROP TABLE IF EXISTS `oot_reviews`;
DROP TABLE IF EXISTS `oos_investigations`;
DROP TABLE IF EXISTS `oot_investigations`;
DROP TABLE IF EXISTS `oos_incidents`;
DROP TABLE IF EXISTS `oot_incidents`;
DROP TABLE IF EXISTS `oos_sequence_tracker`;
DROP TABLE IF EXISTS `test_results`;
DROP TABLE IF EXISTS `trf_records`;
DROP TABLE IF EXISTS `parameter_routing`;
DROP TABLE IF EXISTS `routing_rules`;
DROP TABLE IF EXISTS `sample_parameters`;
DROP TABLE IF EXISTS `test_samples`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `user_capabilities`;
DROP TABLE IF EXISTS `role_rights_mapping`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `rights`;
DROP TABLE IF EXISTS `users`;

-- ============================================================
-- 1. IAM (Identity & Access Management)
-- ============================================================

CREATE TABLE `rights` (
  `right_id`   VARCHAR(80)  NOT NULL,
  `right_name` VARCHAR(120) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status`     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`right_id`),
  UNIQUE KEY `uq_rights_name` (`right_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `role_code`  VARCHAR(40)  NOT NULL,
  `role_name`  VARCHAR(200) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status`     ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_code` (`role_code`),
  UNIQUE KEY `uq_roles_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_rights_mapping` (
  `role_id`   BIGINT       NOT NULL,
  `right_id`  VARCHAR(80)  NOT NULL,
  `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`, `right_id`),
  CONSTRAINT `fk_rrm_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rrm_right` FOREIGN KEY (`right_id`) REFERENCES `rights` (`right_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `full_name`            VARCHAR(255)      NOT NULL,
  `email`                VARCHAR(255)      NOT NULL,
  `password_hash`        VARCHAR(255)      NOT NULL,
  `department_id`        VARCHAR(20)       DEFAULT NULL,
  `department_name`      VARCHAR(200)      DEFAULT NULL,
  `designation_id`       VARCHAR(20)       DEFAULT NULL,
  `current_active_tasks` INT               DEFAULT 0,
  `status`               ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`           TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_users_email` (`email`),
  KEY `idx_users_dept` (`department_id`),
  KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_roles` (
  `user_id`  BIGINT      NOT NULL,
  `role_id`  BIGINT      NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_capabilities` (
  `user_id`        BIGINT       NOT NULL,
  `capability_id`  BIGINT       NOT NULL,
  `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `capability_id`),
  CONSTRAINT `fk_uc_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. Core QC LIMS — Sample Management
-- ============================================================

CREATE TABLE `test_samples` (
  `sample_id`       BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_code`     VARCHAR(50)       NOT NULL,
  `barcode`         VARCHAR(100)      DEFAULT NULL,
  `sample_type`     VARCHAR(100)      NOT NULL,
  `product_name`    VARCHAR(255)      DEFAULT NULL,
  `batch_number`    VARCHAR(100)      DEFAULT NULL,
  `manufacturer`    VARCHAR(255)      DEFAULT NULL,
  `received_date`   DATETIME          NOT NULL,
  `received_by`     BIGINT            NOT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'RECEIVED',
  `priority`        VARCHAR(20)       DEFAULT 'NORMAL',
  `expiry_date`     DATE              DEFAULT NULL,
  `quantity`        DECIMAL(18,4)     DEFAULT NULL,
  `unit`            VARCHAR(20)       DEFAULT NULL,
  `description`     TEXT,
  `remarks`         TEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sample_id`),
  UNIQUE KEY `uq_samples_code` (`sample_code`),
  KEY `idx_samples_barcode` (`barcode`),
  KEY `idx_samples_status` (`status`),
  KEY `idx_samples_received_date` (`received_date`),
  KEY `idx_samples_received_by` (`received_by`),
  CONSTRAINT `fk_samples_received_by` FOREIGN KEY (`received_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sample_parameters` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_id`       BIGINT            NOT NULL,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `parameter_name`  VARCHAR(255)      NOT NULL,
  `method`          VARCHAR(255)      DEFAULT NULL,
  `spec_min`        DECIMAL(18,6)     DEFAULT NULL,
  `spec_max`        DECIMAL(18,6)     DEFAULT NULL,
  `assigned_to`     BIGINT            DEFAULT NULL,
  `assigned_date`   DATETIME          DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'PENDING',
  `due_date`        DATETIME          DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sp_sample` (`sample_id`),
  KEY `idx_sp_parameter` (`parameter_code`),
  KEY `idx_sp_assigned` (`assigned_to`),
  KEY `idx_sp_status` (`status`),
  CONSTRAINT `fk_sp_sample` FOREIGN KEY (`sample_id`) REFERENCES `test_samples` (`sample_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sp_assigned` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. Routing Engine
-- ============================================================

CREATE TABLE `routing_rules` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `rule_name`       VARCHAR(255)      NOT NULL,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `target_role_code` VARCHAR(40)      NOT NULL,
  `priority`        INT               DEFAULT 0,
  `auto_assign`     BOOLEAN           NOT NULL DEFAULT TRUE,
  `sla_hours`       INT               DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rr_parameter` (`parameter_code`),
  KEY `idx_rr_target_role` (`target_role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `parameter_routing` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_parameter_id` BIGINT        NOT NULL,
  `routing_rule_id` BIGINT            NOT NULL,
  `routed_to`       BIGINT            DEFAULT NULL,
  `routed_date`     DATETIME          DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'PENDING',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pr_sample_param` (`sample_parameter_id`),
  KEY `idx_pr_rule` (`routing_rule_id`),
  KEY `idx_pr_routed_to` (`routed_to`),
  CONSTRAINT `fk_pr_sample_param` FOREIGN KEY (`sample_parameter_id`) REFERENCES `sample_parameters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pr_rule` FOREIGN KEY (`routing_rule_id`) REFERENCES `routing_rules` (`id`),
  CONSTRAINT `fk_pr_routed_to` FOREIGN KEY (`routed_to`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. Test Results
-- ============================================================

CREATE TABLE `test_results` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_parameter_id` BIGINT        NOT NULL,
  `result_value`    DECIMAL(18,6)     DEFAULT NULL,
  `result_text`     TEXT,
  `unit`            VARCHAR(30)       DEFAULT NULL,
  `observation`     TEXT,
  `instrument_id`   VARCHAR(50)       DEFAULT NULL,
  `instrument_reading` TEXT,
  `is_within_spec`  BOOLEAN           DEFAULT NULL,
  `result_status`   VARCHAR(30)       NOT NULL DEFAULT 'SUBMITTED',
  `tested_by`       BIGINT            NOT NULL,
  `tested_date`     DATETIME          NOT NULL,
  `reviewed_by`     BIGINT            DEFAULT NULL,
  `reviewed_date`   DATETIME          DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tr_sample_param` (`sample_parameter_id`),
  KEY `idx_tr_tested_by` (`tested_by`),
  KEY `idx_tr_reviewed_by` (`reviewed_by`),
  KEY `idx_tr_result_status` (`result_status`),
  CONSTRAINT `fk_tr_sample_param` FOREIGN KEY (`sample_parameter_id`) REFERENCES `sample_parameters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tr_tested_by` FOREIGN KEY (`tested_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_tr_reviewed_by` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5. TRF (Test Report Form)
-- ============================================================

CREATE TABLE `trf_records` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_id`       BIGINT            NOT NULL,
  `trf_number`      VARCHAR(50)       NOT NULL,
  `generated_by`    BIGINT            NOT NULL,
  `generated_date`  DATETIME          NOT NULL,
  `approved_by`     BIGINT            DEFAULT NULL,
  `approved_date`   DATETIME          DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'DRAFT',
  `notes`           TEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trf_number` (`trf_number`),
  KEY `idx_trf_sample` (`sample_id`),
  CONSTRAINT `fk_trf_sample` FOREIGN KEY (`sample_id`) REFERENCES `test_samples` (`sample_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trf_generated` FOREIGN KEY (`generated_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_trf_approved` FOREIGN KEY (`approved_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6. OOS / OOT Incidents
-- ============================================================

CREATE TABLE `oos_sequence_tracker` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `current_number`  INT               NOT NULL DEFAULT 0,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oos_incidents` (
  `incident_id`     BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_number` VARCHAR(50)       NOT NULL,
  `sample_id`       BIGINT            NOT NULL,
  `sample_parameter_id` BIGINT        NOT NULL,
  `test_result_id`  BIGINT            DEFAULT NULL,
  `result_value`    DECIMAL(18,6)     DEFAULT NULL,
  `spec_limit`      VARCHAR(100)      DEFAULT NULL,
  `initiated_by`    BIGINT            NOT NULL,
  `initiated_date`  DATETIME          NOT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'INITIATED',
  `phase`           VARCHAR(30)       NOT NULL DEFAULT 'LEVEL_1',
  `description`     TEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`incident_id`),
  UNIQUE KEY `uq_oos_number` (`incident_number`),
  KEY `idx_oos_sample` (`sample_id`),
  KEY `idx_oos_status` (`status`),
  KEY `idx_oos_phase` (`phase`),
  CONSTRAINT `fk_oos_sample` FOREIGN KEY (`sample_id`) REFERENCES `test_samples` (`sample_id`),
  CONSTRAINT `fk_oos_sample_param` FOREIGN KEY (`sample_parameter_id`) REFERENCES `sample_parameters` (`id`),
  CONSTRAINT `fk_oos_test_result` FOREIGN KEY (`test_result_id`) REFERENCES `test_results` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_oos_initiated` FOREIGN KEY (`initiated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oos_investigations` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_id`     BIGINT            NOT NULL,
  `investigator`    BIGINT            NOT NULL,
  `phase`           VARCHAR(30)       NOT NULL,
  `findings`        TEXT,
  `conclusion`      VARCHAR(50)       DEFAULT NULL,
  `action_taken`    TEXT,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'IN_PROGRESS',
  `started_date`    DATETIME          NOT NULL,
  `completed_date`  DATETIME          DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_oinv_incident` (`incident_id`),
  KEY `idx_oinv_investigator` (`investigator`),
  CONSTRAINT `fk_oinv_incident` FOREIGN KEY (`incident_id`) REFERENCES `oos_incidents` (`incident_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_oinv_investigator` FOREIGN KEY (`investigator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oos_reviews` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_id`     BIGINT            NOT NULL,
  `reviewer_id`     BIGINT            NOT NULL,
  `action_taken`    VARCHAR(30)       NOT NULL,
  `comments`        TEXT,
  `reviewed_date`   DATETIME          NOT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_orev_incident` (`incident_id`),
  CONSTRAINT `fk_orev_incident` FOREIGN KEY (`incident_id`) REFERENCES `oos_incidents` (`incident_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orev_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oot_incidents` (
  `incident_id`     BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_number` VARCHAR(50)       NOT NULL,
  `sample_id`       BIGINT            NOT NULL,
  `sample_parameter_id` BIGINT        NOT NULL,
  `test_result_id`  BIGINT            DEFAULT NULL,
  `result_value`    DECIMAL(18,6)     DEFAULT NULL,
  `target_value`    DECIMAL(18,6)     DEFAULT NULL,
  `deviation`       DECIMAL(18,6)     DEFAULT NULL,
  `initiated_by`    BIGINT            NOT NULL,
  `initiated_date`  DATETIME          NOT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'INITIATED',
  `phase`           VARCHAR(30)       NOT NULL DEFAULT 'LEVEL_1',
  `description`     TEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`incident_id`),
  UNIQUE KEY `uq_oot_number` (`incident_number`),
  KEY `idx_oot_sample` (`sample_id`),
  KEY `idx_oot_status` (`status`),
  CONSTRAINT `fk_oot_sample` FOREIGN KEY (`sample_id`) REFERENCES `test_samples` (`sample_id`),
  CONSTRAINT `fk_oot_sample_param` FOREIGN KEY (`sample_parameter_id`) REFERENCES `sample_parameters` (`id`),
  CONSTRAINT `fk_oot_test_result` FOREIGN KEY (`test_result_id`) REFERENCES `test_results` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_oot_initiated` FOREIGN KEY (`initiated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oot_investigations` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_id`     BIGINT            NOT NULL,
  `investigator`    BIGINT            NOT NULL,
  `phase`           VARCHAR(30)       NOT NULL,
  `findings`        TEXT,
  `conclusion`      VARCHAR(50)       DEFAULT NULL,
  `action_taken`    TEXT,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'IN_PROGRESS',
  `started_date`    DATETIME          NOT NULL,
  `completed_date`  DATETIME          DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tinv_incident` (`incident_id`),
  CONSTRAINT `fk_tinv_incident` FOREIGN KEY (`incident_id`) REFERENCES `oot_incidents` (`incident_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tinv_investigator` FOREIGN KEY (`investigator`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oot_reviews` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `incident_id`     BIGINT            NOT NULL,
  `reviewer_id`     BIGINT            NOT NULL,
  `action_taken`    VARCHAR(30)       NOT NULL,
  `comments`        TEXT,
  `reviewed_date`   DATETIME          NOT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trev_incident` (`incident_id`),
  CONSTRAINT `fk_trev_incident` FOREIGN KEY (`incident_id`) REFERENCES `oot_incidents` (`incident_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trev_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 7. Master Data Lookup Tables
-- ============================================================

CREATE TABLE `test_parameters` (
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `parameter_name`  VARCHAR(255)      NOT NULL,
  `category`        VARCHAR(100)      DEFAULT NULL,
  `unit`            VARCHAR(30)       DEFAULT NULL,
  `spec_min`        DECIMAL(18,6)     DEFAULT NULL,
  `spec_max`        DECIMAL(18,6)     DEFAULT NULL,
  `method`          VARCHAR(255)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`parameter_code`),
  UNIQUE KEY `uq_tp_name` (`parameter_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sample_types` (
  `type_code`       VARCHAR(50)       NOT NULL,
  `type_name`       VARCHAR(200)      NOT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `retention_days`  INT               DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instruments` (
  `instrument_id`   VARCHAR(50)       NOT NULL,
  `instrument_name` VARCHAR(255)      NOT NULL,
  `model`           VARCHAR(255)      DEFAULT NULL,
  `serial_number`   VARCHAR(100)      DEFAULT NULL,
  `location`        VARCHAR(255)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE','CALIBRATION_DUE','OUT_OF_SERVICE') NOT NULL DEFAULT 'ACTIVE',
  `last_calibration_date` DATE DEFAULT NULL,
  `next_calibration_date` DATE DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`instrument_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `departments` (
  `department_id`   VARCHAR(20)       NOT NULL,
  `department_name` VARCHAR(200)      NOT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `designations` (
  `designation_id`  VARCHAR(20)       NOT NULL,
  `designation_name` VARCHAR(200)     NOT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`designation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `specification_limits` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `product_name`    VARCHAR(255)      DEFAULT NULL,
  `spec_min`        DECIMAL(18,6)     DEFAULT NULL,
  `spec_max`        DECIMAL(18,6)     DEFAULT NULL,
  `target_value`    DECIMAL(18,6)     DEFAULT NULL,
  `tolerance`       DECIMAL(18,6)     DEFAULT NULL,
  `method`          VARCHAR(255)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sl_parameter` (`parameter_code`),
  CONSTRAINT `fk_sl_parameter` FOREIGN KEY (`parameter_code`) REFERENCES `test_parameters` (`parameter_code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `trf_templates` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `template_name`   VARCHAR(255)      NOT NULL,
  `template_code`   VARCHAR(50)       NOT NULL,
  `description`     TEXT,
  `content_html`    LONGTEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_trf_tmpl_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `certificate_templates` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `template_name`   VARCHAR(255)      NOT NULL,
  `template_code`   VARCHAR(50)       NOT NULL,
  `description`     TEXT,
  `content_html`    LONGTEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cert_tmpl_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `retention_periods` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `sample_type`     VARCHAR(100)      NOT NULL,
  `retention_days`  INT               NOT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `data_review_criteria` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `criteria_name`   VARCHAR(255)      NOT NULL,
  `criteria_value`  TEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `environmental_parameters` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_name`  VARCHAR(255)      NOT NULL,
  `unit`            VARCHAR(30)       DEFAULT NULL,
  `min_value`       DECIMAL(18,4)     DEFAULT NULL,
  `max_value`       DECIMAL(18,4)     DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reference_standards` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `standard_code`   VARCHAR(50)       NOT NULL,
  `standard_name`   VARCHAR(255)      NOT NULL,
  `source`          VARCHAR(255)      DEFAULT NULL,
  `expiry_date`     DATE              DEFAULT NULL,
  `potency`         DECIMAL(18,6)     DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rs_code` (`standard_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reagents` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `reagent_code`    VARCHAR(50)       NOT NULL,
  `reagent_name`    VARCHAR(255)      NOT NULL,
  `concentration`   VARCHAR(100)      DEFAULT NULL,
  `expiry_date`     DATE              DEFAULT NULL,
  `storage_condition` VARCHAR(100)   DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reagent_code` (`reagent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `equipment` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `equipment_code`  VARCHAR(50)       NOT NULL,
  `equipment_name`  VARCHAR(255)      NOT NULL,
  `location`        VARCHAR(255)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_equipment_code` (`equipment_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reference_books` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `book_code`       VARCHAR(50)       NOT NULL,
  `title`           VARCHAR(500)      NOT NULL,
  `edition`         VARCHAR(50)       DEFAULT NULL,
  `publisher`       VARCHAR(255)      DEFAULT NULL,
  `year`            INT               DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_book_code` (`book_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `training_records` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT            NOT NULL,
  `training_name`   VARCHAR(255)      NOT NULL,
  `training_date`   DATE              NOT NULL,
  `trainer`         VARCHAR(255)      DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'COMPLETED',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tr_user` (`user_id`),
  CONSTRAINT `fk_tr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `shift_assignments` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT            NOT NULL,
  `shift_name`      VARCHAR(50)       NOT NULL,
  `start_time`      TIME              NOT NULL,
  `end_time`        TIME              NOT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sa_user` (`user_id`),
  CONSTRAINT `fk_sa_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `oos_escalation_rules` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `phase`           VARCHAR(30)       NOT NULL,
  `escalation_role_code` VARCHAR(40)  NOT NULL,
  `sla_hours`       INT               DEFAULT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `data_integrity_checks` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `check_name`      VARCHAR(255)      NOT NULL,
  `check_type`      VARCHAR(50)       NOT NULL,
  `check_expression` TEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sample_storage_conditions` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `condition_name`  VARCHAR(100)      NOT NULL,
  `temperature_min` DECIMAL(6,2)      DEFAULT NULL,
  `temperature_max` DECIMAL(6,2)      DEFAULT NULL,
  `humidity_min`    DECIMAL(5,2)      DEFAULT NULL,
  `humidity_max`    DECIMAL(5,2)      DEFAULT NULL,
  `description`     VARCHAR(500)      DEFAULT NULL,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `result_templates` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `template_name`   VARCHAR(255)      NOT NULL,
  `template_content` LONGTEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_templates` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `template_code`   VARCHAR(50)       NOT NULL,
  `template_name`   VARCHAR(255)      NOT NULL,
  `subject`         VARCHAR(500)      DEFAULT NULL,
  `body`            LONGTEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_nt_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `report_templates` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `template_code`   VARCHAR(50)       NOT NULL,
  `template_name`   VARCHAR(255)      NOT NULL,
  `description`     TEXT,
  `content_html`    LONGTEXT,
  `status`          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rt_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `method_validation_records` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_code`  VARCHAR(50)       NOT NULL,
  `method_name`     VARCHAR(255)      NOT NULL,
  `validation_date` DATE              NOT NULL,
  `validated_by`    BIGINT            DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'PENDING',
  `report_content`  LONGTEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mvr_parameter` (`parameter_code`),
  CONSTRAINT `fk_mvr_validated` FOREIGN KEY (`validated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `calibration_schedules` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `instrument_id`   VARCHAR(50)       NOT NULL,
  `scheduled_date`  DATE              NOT NULL,
  `scheduled_by`    BIGINT            DEFAULT NULL,
  `status`          VARCHAR(30)       NOT NULL DEFAULT 'SCHEDULED',
  `completed_date`  DATE              DEFAULT NULL,
  `completed_by`    BIGINT            DEFAULT NULL,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cs_instrument` (`instrument_id`),
  CONSTRAINT `fk_cs_scheduled` FOREIGN KEY (`scheduled_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_cs_completed` FOREIGN KEY (`completed_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `environmental_logs` (
  `id`              BIGINT            NOT NULL AUTO_INCREMENT,
  `parameter_name`  VARCHAR(100)      NOT NULL,
  `value`           DECIMAL(18,4)     NOT NULL,
  `location`        VARCHAR(255)      DEFAULT NULL,
  `recorded_by`     BIGINT            DEFAULT NULL,
  `recorded_at`     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_el_parameter` (`parameter_name`),
  KEY `idx_el_recorded_at` (`recorded_at`),
  CONSTRAINT `fk_el_recorded` FOREIGN KEY (`recorded_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 8. Audit Trail
-- ============================================================

CREATE TABLE `review_audit_log` (
  `log_id`          BIGINT            NOT NULL AUTO_INCREMENT,
  `entity_type`     VARCHAR(50)       NOT NULL,
  `entity_id`       BIGINT            NOT NULL,
  `user_id`         BIGINT            NOT NULL,
  `action`          VARCHAR(50)       NOT NULL,
  `old_value`       JSON              DEFAULT NULL,
  `new_value`       JSON              DEFAULT NULL,
  `comments`        TEXT,
  `created_at`      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_ral_entity` (`entity_type`, `entity_id`),
  KEY `idx_ral_user` (`user_id`),
  KEY `idx_ral_action` (`action`),
  CONSTRAINT `fk_ral_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
