-- ============================================================
-- PaySphere
-- Add payment tracking to salary_history
-- Migration: V2__add_salary_payment_tracking.sql
-- ============================================================

ALTER TABLE salary_history
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN paid_at TIMESTAMP;

ALTER TABLE salary_history
    ADD CONSTRAINT chk_salary_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID'));

CREATE INDEX idx_salary_payment_status
    ON salary_history(payment_status);
