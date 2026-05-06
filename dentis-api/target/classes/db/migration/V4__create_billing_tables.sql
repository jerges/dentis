CREATE TABLE tariffs (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code             VARCHAR(30) NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    category         VARCHAR(30) NOT NULL,
    base_price       NUMERIC(15,2) NOT NULL,
    discount_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    active           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_tariff_category ON tariffs(category);
CREATE INDEX idx_tariff_code ON tariffs(code);

CREATE TABLE budgets (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id       UUID NOT NULL,
    treatment_plan_id UUID,
    dentist_id       UUID NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at      TIMESTAMP,
    CONSTRAINT fk_budget_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE TABLE budget_items (
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

CREATE TABLE payments (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    patient_id       UUID NOT NULL,
    budget_id        UUID NOT NULL,
    amount           NUMERIC(15,2) NOT NULL,
    payment_method   VARCHAR(30) NOT NULL,
    invoice_reference VARCHAR(100),
    notes            TEXT,
    paid_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_payment_budget FOREIGN KEY (budget_id) REFERENCES budgets(id)
);

CREATE INDEX idx_payment_budget ON payments(budget_id);
CREATE INDEX idx_payment_patient ON payments(patient_id);
