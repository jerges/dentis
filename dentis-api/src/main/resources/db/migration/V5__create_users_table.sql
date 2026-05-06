CREATE TABLE users (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(30) NOT NULL DEFAULT 'RECEPTIONIST',
    full_name  VARCHAR(200) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_email ON users(email);

-- Default admin user (password: Admin@2024! - bcrypt encoded)
INSERT INTO users (id, username, email, password, role, full_name)
VALUES (
    uuid_generate_v4(),
    'admin',
    'admin@dentis.com',
    '$2a$12$9Y4RXZE2mNEU3dSKzO1p2.E9TZpRcbH8.WVJBl3tIKNFXPjXoHI5a',
    'ADMIN',
    'System Administrator'
);
