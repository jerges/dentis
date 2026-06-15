--liquibase formatted sql

--changeset dentis:003-demo-data context:demo,qa
INSERT INTO clinics (id, name, nif, address, city, province, phone, email, active)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Clínica Dental Demo',
    'J-00000000-0',
    'Av. Principal 1, Piso 1, Consultorio 1',
    'Caracas',
    'Distrito Capital',
    '+58-212-0000000',
    'demo@dentis.com.ve',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, username, email, password, role, full_name, staff_type, clinic_id, active)
VALUES (
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'demo',
    'demo@dentis.com.ve',
    '$2a$12$W0IlLwCqO4xL46nU6L6gdONRs.AC66QBuenySRdKHq9Zso/1q89GW',
    'ADMIN',
    'Usuario Demo',
    'ADMINISTRATIVE',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

--rollback DELETE FROM users WHERE username = 'demo';
--rollback DELETE FROM clinics WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';


--changeset dentis:003-demo-extended context:demo,qa
-- ── Dentista demo ────────────────────────────────────────────────────────────
INSERT INTO users (id, username, email, password, role, full_name, staff_type, clinic_id, active)
VALUES (
    'c3d4e5f6-a7b8-9012-cdef-012345678902',
    'dra.garcia',
    'garcia@dentis.com.ve',
    '$2a$12$W0IlLwCqO4xL46nU6L6gdONRs.AC66QBuenySRdKHq9Zso/1q89GW',
    'USER',
    'Dra. María García',
    'DENTIST',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

-- ── Paciente demo ─────────────────────────────────────────────────────────────
INSERT INTO patients (id, first_name, last_name, id_document, birth_date, sex, gender,
    email, phone_number, city, state, clinic_id, active, created_at)
VALUES (
    'd4e5f6a7-b8c9-0123-def0-123456789012',
    'Carlos',
    'Rodríguez',
    'V-18450321',
    '1985-07-14',
    'MALE',
    'MALE',
    'carlos.rodriguez@email.com',
    '+58-414-1234567',
    'Caracas',
    'Distrito Capital',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    TRUE,
    NOW()
)
ON CONFLICT (id_document) DO NOTHING;

-- ── Aranceles demo ───────────────────────────────────────────────────────────
INSERT INTO tariffs (id, code, name, category, base_price, discount_allowed, active)
VALUES
    ('e1f2a3b4-c5d6-789a-4567-890123456789', 'LIM-001', 'Limpieza Dental Profesional',         'GENERAL_DENTISTRY', 45.00,  TRUE, TRUE),
    ('f2a3b4c5-d6e7-89ab-5678-901234567890', 'REST-002', 'Restauración con Resina Clase II',    'GENERAL_DENTISTRY', 120.00, TRUE, TRUE),
    ('a3b4c5d6-e7f8-9abc-6789-012345678901', 'END-003',  'Tratamiento de Conducto Unirradicular', 'ENDODONTICS',    280.00, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- ── Citas demo ────────────────────────────────────────────────────────────────
INSERT INTO appointments (id, patient_id, dentist_id, start_date_time, end_date_time,
    status, consultation_reason, created_at)
VALUES
    (
        'b4c5d6e7-f8a9-abcd-7890-123456789012',
        'd4e5f6a7-b8c9-0123-def0-123456789012',
        'c3d4e5f6-a7b8-9012-cdef-012345678902',
        (CURRENT_DATE + INTERVAL '7 days')::TIMESTAMP + TIME '10:00:00',
        (CURRENT_DATE + INTERVAL '7 days')::TIMESTAMP + TIME '10:45:00',
        'SCHEDULED',
        'Control periódico y limpieza dental',
        NOW()
    ),
    (
        'c5d6e7f8-a9b0-bcde-8901-234567890123',
        'd4e5f6a7-b8c9-0123-def0-123456789012',
        'c3d4e5f6-a7b8-9012-cdef-012345678902',
        (CURRENT_DATE - INTERVAL '10 days')::TIMESTAMP + TIME '15:00:00',
        (CURRENT_DATE - INTERVAL '10 days')::TIMESTAMP + TIME '15:30:00',
        'COMPLETED',
        'Restauración pieza 16',
        NOW() - INTERVAL '12 days'
    )
ON CONFLICT (id) DO NOTHING;

-- ── Historia clínica demo ─────────────────────────────────────────────────────
INSERT INTO clinical_records (id, patient_id, dentition_type, created_at, updated_at)
VALUES (
    'e5f6a7b8-c9d0-1234-ef01-234567890123',
    'd4e5f6a7-b8c9-0123-def0-123456789012',
    'PERMANENT',
    NOW() - INTERVAL '10 days',
    NOW()
)
ON CONFLICT (patient_id) DO NOTHING;

-- ── Odontograma demo ─────────────────────────────────────────────────────────
INSERT INTO odontogram_teeth (id, clinical_record_id, tooth_number, condition, affected_surfaces, notes)
VALUES
    ('f6a7b8c9-d0e1-2345-f012-345678901234', 'e5f6a7b8-c9d0-1234-ef01-234567890123', 16, 'RESTORED',  'OCCLUSAL,DISTAL', 'Restauración con resina compuesta'),
    ('a7b8c9d0-e1f2-3456-0123-456789012345', 'e5f6a7b8-c9d0-1234-ef01-234567890123', 36, 'CARIES',    'OCCLUSAL',        NULL),
    ('b8c9d0e1-f2a3-4567-1234-567890123456', 'e5f6a7b8-c9d0-1234-ef01-234567890123', 18, 'EXTRACTED', NULL,              'Extracción previa por impacción')
ON CONFLICT (clinical_record_id, tooth_number) DO NOTHING;

-- ── Evolución clínica demo ────────────────────────────────────────────────────
INSERT INTO clinical_evolutions (id, clinical_record_id, dentist_id, description, findings, treatment, recorded_at)
VALUES (
    'c9d0e1f2-a3b4-5678-2345-678901234567',
    'e5f6a7b8-c9d0-1234-ef01-234567890123',
    'c3d4e5f6-a7b8-9012-cdef-012345678902',
    'Paciente acude por dolor espontáneo en zona posterior inferior izquierda.',
    'Caries profunda en pieza 36 con compromiso pulpar. Percusión positiva. Rx: imagen periapical.',
    'Se realizó apertura cameral y trepanación. Se indica tratamiento de conducto en siguiente sesión.',
    NOW() - INTERVAL '10 days'
)
ON CONFLICT (id) DO NOTHING;

-- ── Diagnóstico demo ─────────────────────────────────────────────────────────
INSERT INTO diagnoses (id, clinical_record_id, code, description, diagnosed_at, dentist_id)
VALUES (
    'd0e1f2a3-b4c5-6789-3456-789012345678',
    'e5f6a7b8-c9d0-1234-ef01-234567890123',
    'K04.0',
    'Pulpitis irreversible pieza 36',
    CURRENT_DATE - INTERVAL '10 days',
    'c3d4e5f6-a7b8-9012-cdef-012345678902'
)
ON CONFLICT (id) DO NOTHING;

-- ── Plan de tratamiento demo ──────────────────────────────────────────────────
INSERT INTO treatment_plans (id, clinical_record_id, dentist_id, title, description, status, start_date, estimated_end_date)
VALUES (
    'e1f2a3b4-c5d6-7890-4567-890123456789',
    'e5f6a7b8-c9d0-1234-ef01-234567890123',
    'c3d4e5f6-a7b8-9012-cdef-012345678902',
    'Rehabilitación zona posterior inferior',
    'Tratamiento de conducto en pieza 36 + restauración final. Control en 3 meses.',
    'IN_PROGRESS',
    CURRENT_DATE - INTERVAL '10 days',
    CURRENT_DATE + INTERVAL '30 days'
)
ON CONFLICT (id) DO NOTHING;

-- ── Presupuesto demo ──────────────────────────────────────────────────────────
INSERT INTO budgets (id, patient_id, dentist_id, status, notes, created_at, approved_at)
VALUES (
    'f2a3b4c5-d6e7-8901-5678-901234567890',
    'd4e5f6a7-b8c9-0123-def0-123456789012',
    'c3d4e5f6-a7b8-9012-cdef-012345678902',
    'APPROVED',
    'Presupuesto aprobado en consulta. Paciente confirma inicio de tratamiento.',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO budget_items (id, budget_id, tariff_id, description, quantity, unit_price, discount_percentage)
VALUES
    (
        'a1b2c3d4-e5f6-7890-abcd-111111111111',
        'f2a3b4c5-d6e7-8901-5678-901234567890',
        'a3b4c5d6-e7f8-9abc-6789-012345678901',
        'Tratamiento de Conducto Unirradicular pieza 36',
        1, 280.00, 0.00
    ),
    (
        'a1b2c3d4-e5f6-7890-abcd-222222222222',
        'f2a3b4c5-d6e7-8901-5678-901234567890',
        'f2a3b4c5-d6e7-89ab-5678-901234567890',
        'Restauración con Resina Clase II pieza 36',
        1, 120.00, 0.00
    )
ON CONFLICT (id) DO NOTHING;

-- ── Pago demo ─────────────────────────────────────────────────────────────────
INSERT INTO payments (id, patient_id, budget_id, amount, payment_method, notes, paid_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-333333333333',
    'd4e5f6a7-b8c9-0123-def0-123456789012',
    'f2a3b4c5-d6e7-8901-5678-901234567890',
    200.00,
    'CASH',
    'Abono inicial del tratamiento.',
    NOW() - INTERVAL '10 days'
)
ON CONFLICT (id) DO NOTHING;

--rollback DELETE FROM payments WHERE id = 'a1b2c3d4-e5f6-7890-abcd-333333333333';
--rollback DELETE FROM budget_items WHERE budget_id = 'f2a3b4c5-d6e7-8901-5678-901234567890';
--rollback DELETE FROM budgets WHERE id = 'f2a3b4c5-d6e7-8901-5678-901234567890';
--rollback DELETE FROM treatment_plans WHERE id = 'e1f2a3b4-c5d6-7890-4567-890123456789';
--rollback DELETE FROM diagnoses WHERE id = 'd0e1f2a3-b4c5-6789-3456-789012345678';
--rollback DELETE FROM clinical_evolutions WHERE id = 'c9d0e1f2-a3b4-5678-2345-678901234567';
--rollback DELETE FROM odontogram_teeth WHERE clinical_record_id = 'e5f6a7b8-c9d0-1234-ef01-234567890123';
--rollback DELETE FROM clinical_records WHERE id = 'e5f6a7b8-c9d0-1234-ef01-234567890123';
--rollback DELETE FROM appointments WHERE patient_id = 'd4e5f6a7-b8c9-0123-def0-123456789012';
--rollback DELETE FROM tariffs WHERE code IN ('LIM-001','REST-002','END-003');
--rollback DELETE FROM patients WHERE id = 'd4e5f6a7-b8c9-0123-def0-123456789012';
--rollback DELETE FROM users WHERE username = 'dra.garcia';
--rollback DELETE FROM users WHERE username = 'demo';
--rollback DELETE FROM clinics WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
