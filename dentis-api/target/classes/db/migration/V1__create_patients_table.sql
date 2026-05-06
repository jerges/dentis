CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE patients (
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
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP
);

CREATE INDEX idx_patient_id_document ON patients(id_document);
CREATE INDEX idx_patient_name ON patients(first_name, last_name);
