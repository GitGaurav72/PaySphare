-- ============================================================
-- PayVista - Employee Salary Management System
-- Initial Database Schema
-- Database: PostgreSQL
-- Migration: V1__create_initial_schema.sql
-- ============================================================


-- ============================================================
-- 1. COUNTRIES
-- ============================================================

CREATE TABLE countries (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    country_code    VARCHAR(10) NOT NULL,
    currency_code   VARCHAR(10) NOT NULL,

    CONSTRAINT uk_country_name
        UNIQUE (name),

    CONSTRAINT uk_country_code
        UNIQUE (country_code)
);


-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================

CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),

    CONSTRAINT uk_department_name
        UNIQUE (name)
);


-- ============================================================
-- 3. DESIGNATIONS
-- ============================================================

CREATE TABLE designations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,

    CONSTRAINT uk_designation_name
        UNIQUE (name)
);


-- ============================================================
-- 4. HR USERS
--    Application users who manage employee/salary data
-- ============================================================

CREATE TABLE hr_users (
    id              BIGSERIAL PRIMARY KEY,

    name            VARCHAR(150) NOT NULL,

    email           VARCHAR(255) NOT NULL,

    password_hash   VARCHAR(255) NOT NULL,

    role            VARCHAR(50) NOT NULL DEFAULT 'HR_MANAGER',

    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_hr_user_email
        UNIQUE (email),

    CONSTRAINT chk_hr_user_role
        CHECK (role IN ('HR_ADMIN', 'HR_MANAGER', 'HR_VIEWER')),

    CONSTRAINT chk_hr_user_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);


-- ============================================================
-- 5. EMPLOYEES
-- ============================================================

CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,

    employee_code       VARCHAR(50) NOT NULL,

    first_name          VARCHAR(100) NOT NULL,

    last_name           VARCHAR(100) NOT NULL,

    email               VARCHAR(255) NOT NULL,

    country_id          BIGINT NOT NULL,

    department_id       BIGINT NOT NULL,

    designation_id      BIGINT NOT NULL,

    joining_date        DATE NOT NULL,

    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_code
        UNIQUE (employee_code),

    CONSTRAINT uk_employee_email
        UNIQUE (email),

    CONSTRAINT fk_employee_country
        FOREIGN KEY (country_id)
        REFERENCES countries(id),

    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id),

    CONSTRAINT fk_employee_designation
        FOREIGN KEY (designation_id)
        REFERENCES designations(id),

    CONSTRAINT chk_employee_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'))
);


-- ============================================================
-- 6. SALARY HISTORY
-- ============================================================

CREATE TABLE salary_history (
    id                  BIGSERIAL PRIMARY KEY,

    employee_id         BIGINT NOT NULL,

    currency_code       VARCHAR(10) NOT NULL,

    base_salary         NUMERIC(15, 2) NOT NULL,

    bonus               NUMERIC(15, 2) NOT NULL DEFAULT 0,

    effective_from      DATE NOT NULL,

    effective_to        DATE,

    created_by          BIGINT NOT NULL,

    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_salary_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_salary_created_by
        FOREIGN KEY (created_by)
        REFERENCES hr_users(id),

    CONSTRAINT chk_salary_base
        CHECK (base_salary >= 0),

    CONSTRAINT chk_salary_bonus
        CHECK (bonus >= 0),

    CONSTRAINT chk_salary_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        )
);


-- ============================================================
-- 7. INDEXES
-- ============================================================


-- ----------------------------
-- Employee indexes
-- ----------------------------

CREATE INDEX idx_employee_country
    ON employees(country_id);

CREATE INDEX idx_employee_department
    ON employees(department_id);

CREATE INDEX idx_employee_designation
    ON employees(designation_id);

CREATE INDEX idx_employee_status
    ON employees(status);

CREATE INDEX idx_employee_name
    ON employees(last_name, first_name);


-- ----------------------------
-- Salary indexes
-- ----------------------------

CREATE INDEX idx_salary_employee
    ON salary_history(employee_id);

CREATE INDEX idx_salary_effective_from
    ON salary_history(effective_from);

CREATE INDEX idx_salary_current
    ON salary_history(employee_id, effective_to);


-- ============================================================
-- 8. SEED MASTER DATA
-- ============================================================


-- Countries

INSERT INTO countries
    (name, country_code, currency_code)
VALUES
    ('India', 'IN', 'INR'),
    ('United States', 'US', 'USD'),
    ('United Kingdom', 'GB', 'GBP'),
    ('Germany', 'DE', 'EUR'),
    ('Singapore', 'SG', 'SGD'),
    ('Australia', 'AU', 'AUD'),
    ('Canada', 'CA', 'CAD'),
    ('United Arab Emirates', 'AE', 'AED');


-- Departments

INSERT INTO departments
    (name, description)
VALUES
    ('Engineering', 'Software engineering and technology'),
    ('Human Resources', 'People and HR operations'),
    ('Finance', 'Finance and accounting'),
    ('Sales', 'Sales and business development'),
    ('Marketing', 'Marketing and brand management'),
    ('Operations', 'Business and operational management'),
    ('Information Technology', 'IT infrastructure and support'),
    ('Legal', 'Legal and compliance'),
    ('Product', 'Product management and strategy'),
    ('Customer Support', 'Customer support and service');


-- Designations

INSERT INTO designations
    (name)
VALUES
    ('Software Engineer'),
    ('Senior Software Engineer'),
    ('Lead Software Engineer'),
    ('Engineering Manager'),
    ('Product Manager'),
    ('Senior Product Manager'),
    ('HR Manager'),
    ('HR Executive'),
    ('Financial Analyst'),
    ('Finance Manager'),
    ('Sales Executive'),
    ('Sales Manager'),
    ('Marketing Executive'),
    ('Marketing Manager'),
    ('Operations Manager'),
    ('IT Administrator'),
    ('Business Analyst'),
    ('Legal Counsel'),
    ('Customer Support Executive'),
    ('Director');


-- ============================================================
-- END OF INITIAL SCHEMA
-- ============================================================
