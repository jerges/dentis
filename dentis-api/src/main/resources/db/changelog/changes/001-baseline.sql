--liquibase formatted sql

--changeset dentis:001-baseline
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS clinics (
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

CREATE INDEX IF NOT EXISTS idx_clinic_name ON clinics(name);

CREATE TABLE IF NOT EXISTS patients (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    id_document                 VARCHAR(20) NOT NULL UNIQUE,
    birth_date                  DATE NOT NULL,
    sex                         VARCHAR(20) NOT NULL,
    gender                      VARCHAR(20) NOT NULL,
    social_name                 VARCHAR(150),
    email                       VARCHAR(150),
    phone_number                VARCHAR(20),
    alternative_phone           VARCHAR(20),
    street                      VARCHAR(200),
    city                        VARCHAR(100),
    state                       VARCHAR(100),
    zip_code                    VARCHAR(10),
    representative_full_name    VARCHAR(200),
    representative_id_document  VARCHAR(20),
    representative_relationship VARCHAR(50),
    representative_phone        VARCHAR(20),
    clinic_id                   UUID REFERENCES clinics(id),
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_id_document ON patients(id_document);
CREATE INDEX IF NOT EXISTS idx_patient_name ON patients(first_name, last_name);
CREATE INDEX IF NOT EXISTS idx_patient_clinic ON patients(clinic_id);

CREATE TABLE IF NOT EXISTS users (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(30) NOT NULL DEFAULT 'USER',
    full_name  VARCHAR(200) NOT NULL,
    staff_type VARCHAR(30) NOT NULL DEFAULT 'ADMINISTRATIVE',
    clinic_id  UUID REFERENCES clinics(id),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_clinic ON users(clinic_id);

CREATE TABLE IF NOT EXISTS appointments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id          UUID NOT NULL,
    dentist_id          UUID NOT NULL,
    start_date_time     TIMESTAMP NOT NULL,
    end_date_time       TIMESTAMP NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    consultation_reason VARCHAR(500),
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE INDEX IF NOT EXISTS idx_appointment_dentist_date ON appointments(dentist_id, start_date_time);
CREATE INDEX IF NOT EXISTS idx_appointment_patient ON appointments(patient_id);

CREATE TABLE IF NOT EXISTS clinical_records (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_clinical_record_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE TABLE IF NOT EXISTS odontogram_teeth (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    tooth_number       INT NOT NULL,
    condition          VARCHAR(30) NOT NULL DEFAULT 'HEALTHY',
    affected_surfaces  VARCHAR(200),
    notes              TEXT,
    CONSTRAINT fk_tooth_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id),
    CONSTRAINT uq_tooth_per_record UNIQUE (clinical_record_id, tooth_number)
);

CREATE TABLE IF NOT EXISTS clinical_evolutions (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    dentist_id         UUID NOT NULL,
    description        TEXT NOT NULL,
    findings           TEXT,
    treatment          TEXT,
    recorded_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_evolution_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE IF NOT EXISTS diagnoses (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    code               VARCHAR(20),
    description        TEXT NOT NULL,
    diagnosed_at       DATE NOT NULL DEFAULT CURRENT_DATE,
    dentist_id         UUID NOT NULL,
    CONSTRAINT fk_diagnosis_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE IF NOT EXISTS treatment_plans (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    dentist_id         UUID NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
    budget_id          UUID,
    start_date         DATE,
    estimated_end_date DATE,
    CONSTRAINT fk_treatment_plan_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE IF NOT EXISTS treatment_procedures (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    treatment_plan_id       UUID NOT NULL,
    description             TEXT NOT NULL,
    tooth_number            INT,
    performed               BOOLEAN NOT NULL DEFAULT FALSE,
    performed_at            TIMESTAMP,
    performed_by_dentist_id UUID,
    budget_item_id          UUID,
    CONSTRAINT fk_procedure_treatment_plan FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plans(id)
);

CREATE TABLE IF NOT EXISTS tariffs (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code             VARCHAR(30) NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    category         VARCHAR(30) NOT NULL,
    base_price       NUMERIC(15,2) NOT NULL,
    discount_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    active           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_tariff_category ON tariffs(category);
CREATE INDEX IF NOT EXISTS idx_tariff_code ON tariffs(code);

CREATE TABLE IF NOT EXISTS budgets (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id        UUID NOT NULL,
    treatment_plan_id UUID,
    dentist_id        UUID NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at       TIMESTAMP,
    CONSTRAINT fk_budget_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE TABLE IF NOT EXISTS budget_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    budget_id           UUID NOT NULL,
    tariff_id           UUID NOT NULL,
    description         VARCHAR(500) NOT NULL,
    quantity            INT NOT NULL DEFAULT 1,
    unit_price          NUMERIC(15,2) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    performed           BOOLEAN NOT NULL DEFAULT FALSE,
    performed_at        TIMESTAMP,
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_budget_item_budget FOREIGN KEY (budget_id) REFERENCES budgets(id),
    CONSTRAINT fk_budget_item_tariff FOREIGN KEY (tariff_id) REFERENCES tariffs(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id        UUID NOT NULL,
    budget_id         UUID NOT NULL,
    amount            NUMERIC(15,2) NOT NULL,
    payment_method    VARCHAR(30) NOT NULL,
    invoice_reference VARCHAR(100),
    notes             TEXT,
    paid_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_payment_budget FOREIGN KEY (budget_id) REFERENCES budgets(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_budget ON payments(budget_id);
CREATE INDEX IF NOT EXISTS idx_payment_patient ON payments(patient_id);

INSERT INTO users (id, username, email, password, role, full_name, staff_type)
VALUES (
    uuid_generate_v4(),
    'admin',
    'admin@dentis.com',
    '$2a$12$9Y4RXZE2mNEU3dSKzO1p2.E9TZpRcbH8.WVJBl3tIKNFXPjXoHI5a',
    'ADMIN',
    'System Administrator',
    'ADMINISTRATIVE'
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, email, password, role, full_name, staff_type)
VALUES (
    uuid_generate_v4(),
    'jbello',
    'jbello@dentis.dev',
    '$2a$12$9/y9SvYLWpJPUt20mp/j/.2cqORjnqA5MEop4Ct30MtR9MQrPAwpm',
    'SUPER_ADMIN',
    'J. Bello (Dev Admin)',
    'ADMINISTRATIVE'
)
ON CONFLICT (username) DO NOTHING;

--rollback DROP TABLE IF EXISTS payments, budget_items, budgets, tariffs, treatment_procedures, treatment_plans, diagnoses, clinical_evolutions, odontogram_teeth, clinical_records, appointments, users, patients, clinics CASCADE;
