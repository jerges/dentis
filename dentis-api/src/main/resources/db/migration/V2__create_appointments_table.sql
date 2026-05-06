CREATE TABLE appointments (
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

CREATE INDEX idx_appointment_dentist_date ON appointments(dentist_id, start_date_time);
CREATE INDEX idx_appointment_patient ON appointments(patient_id);
