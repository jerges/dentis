-- ============================================================
-- V7: Multi-clinic support
-- ============================================================

-- 1. Create clinics table
CREATE TABLE clinics (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(200) NOT NULL,
    nif        VARCHAR(20),
    address    VARCHAR(300),
    city       VARCHAR(100),
    province   VARCHAR(100),
    zip_code   VARCHAR(10),
    phone      VARCHAR(20),
    email      VARCHAR(150),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_clinic_name ON clinics(name);

-- 2. Add clinic_id to users (nullable: SUPER_ADMIN has no clinic)
ALTER TABLE users ADD COLUMN clinic_id UUID REFERENCES clinics(id);
CREATE INDEX idx_user_clinic ON users(clinic_id);

-- 3. Add clinic_id to patients (scoped to clinic)
ALTER TABLE patients ADD COLUMN clinic_id UUID REFERENCES clinics(id);
CREATE INDEX idx_patient_clinic ON patients(clinic_id);

-- 4. Migrate existing roles to new model
--    ADMIN stays as ADMIN (clinic admin)
--    DENTIST -> MEDICO
--    RECEPTIONIST -> MEDICO
UPDATE users SET role = 'MEDICO' WHERE role IN ('DENTIST', 'RECEPTIONIST');

-- 5. Promote jbello to SUPER_ADMIN
UPDATE users SET role = 'SUPER_ADMIN' WHERE username = 'jbello';

-- 6. Update default role constraint
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'MEDICO';

