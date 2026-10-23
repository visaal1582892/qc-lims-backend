-- ============================================================
-- QC LIMS — Initial Schema (IAM + Audit-Ready)
-- MySQL 8.0+ / InnoDB / utf8mb4
-- ============================================================

-- ============================================================
-- 1. IAM (Identity & Access Management)
-- ============================================================

CREATE TABLE `rights` (
  `right_id`   VARCHAR(100) NOT NULL,  -- MUST start with 'ROLE_'
  `right_name` VARCHAR(150) NOT NULL,
  `description` TEXT,
  `status`     VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50),
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(50),
  PRIMARY KEY (`right_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rights_log` (
  `log_id`     BIGINT AUTO_INCREMENT PRIMARY KEY,
  `right_id`   VARCHAR(100),
  `right_name` VARCHAR(150),
  `description` TEXT,
  `status`     VARCHAR(20),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `role_id`    BIGINT AUTO_INCREMENT,
  `role_code`  VARCHAR(50) NOT NULL,
  `role_name`  VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status`     VARCHAR(20) DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50),
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(50),
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uq_roles_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles_log` (
  `log_id`     BIGINT AUTO_INCREMENT PRIMARY KEY,
  `role_id`    BIGINT,
  `role_code`  VARCHAR(50),
  `role_name`  VARCHAR(100),
  `status`     VARCHAR(20),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id`       BIGINT AUTO_INCREMENT,
  `employee_id`   VARCHAR(50) NOT NULL,
  `full_name`     VARCHAR(150) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `status`        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at`    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by`    VARCHAR(50),
  `updated_at`    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by`    VARCHAR(50),
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `unique_emp_status` (`employee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users_log` (
  `log_id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`      BIGINT,
  `employee_id`  VARCHAR(50),
  `full_name`    VARCHAR(150),
  `password_hash` VARCHAR(255),
  `status`       VARCHAR(20),
  `created_at`   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by`   VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_rights` (
  `role_id`    BIGINT NOT NULL,
  `right_id`   VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50),
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(50),
  PRIMARY KEY (`role_id`, `right_id`),
  CONSTRAINT `fk_rr_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rr_right` FOREIGN KEY (`right_id`) REFERENCES `rights` (`right_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `role_rights_log` (
  `log_id`     BIGINT AUTO_INCREMENT PRIMARY KEY,
  `role_id`    BIGINT,
  `right_id`   VARCHAR(100),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_roles` (
  `user_id`    BIGINT NOT NULL,
  `role_id`    BIGINT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50),
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(50),
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_roles_log` (
  `log_id`     BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`    BIGINT,
  `role_id`    BIGINT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
