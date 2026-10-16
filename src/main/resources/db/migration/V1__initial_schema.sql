-- V1: Initial schema — shared infrastructure table
-- Creates ar_sequence_tracker for AR number generation (Phase 1 dependency)

CREATE TABLE IF NOT EXISTS ar_sequence_tracker (
    calendar_year VARCHAR(4) PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0
);

-- Seed initial entry for current year (2026)
-- MySQL: ON DUPLICATE KEY UPDATE instead of ON CONFLICT
INSERT INTO ar_sequence_tracker (calendar_year, last_sequence)
VALUES ('26', 0)
ON DUPLICATE KEY UPDATE calendar_year = VALUES(calendar_year);
