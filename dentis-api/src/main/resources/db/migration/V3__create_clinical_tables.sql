CREATE TABLE clinical_records (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_clinical_record_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE TABLE odontogram_teeth (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    tooth_number      INT NOT NULL,
    condition         VARCHAR(30) NOT NULL DEFAULT 'HEALTHY',
    affected_surfaces VARCHAR(200),
    notes             TEXT,
    CONSTRAINT fk_tooth_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id),
    CONSTRAINT uq_tooth_per_record UNIQUE (clinical_record_id, tooth_number)
);

CREATE TABLE clinical_evolutions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    dentist_id        UUID NOT NULL,
    description       TEXT NOT NULL,
    findings          TEXT,
    treatment         TEXT,
    recorded_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_evolution_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE diagnoses (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    code              VARCHAR(20),
    description       TEXT NOT NULL,
    diagnosed_at      DATE NOT NULL DEFAULT CURRENT_DATE,
    dentist_id        UUID NOT NULL,
    CONSTRAINT fk_diagnosis_clinical_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE treatment_plans (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clinical_record_id UUID NOT NULL,
    dentist_id        UUID NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
    budget_id         UUID,
    start_date        DATE,
    estimated_end_date DATE,
    CONSTRAINT fk_treatment_plan_record FOREIGN KEY (clinical_record_id) REFERENCES clinical_records(id)
);

CREATE TABLE treatment_procedures (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    treatment_plan_id    UUID NOT NULL,
    description          TEXT NOT NULL,
    tooth_number         INT,
    performed            BOOLEAN NOT NULL DEFAULT FALSE,
    performed_at         TIMESTAMP,
    performed_by_dentist_id UUID,
    budget_item_id       UUID,
    CONSTRAINT fk_procedure_treatment_plan FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plans(id)
);
