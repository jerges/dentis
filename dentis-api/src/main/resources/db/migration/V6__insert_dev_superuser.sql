-- Dev superuser: jbello / Admin@2026!
-- BCrypt hash (strength 12) of "Admin@2026!"
INSERT INTO users (id, username, email, password, role, full_name)
VALUES (
    uuid_generate_v4(),
    'jbello',
    'jbello@dentis.dev',
    '$2a$12$9/y9SvYLWpJPUt20mp/j/.2cqORjnqA5MEop4Ct30MtR9MQrPAwpm',
    'ADMIN',
    'J. Bello (Dev Admin)'
)
ON CONFLICT (username) DO NOTHING;

