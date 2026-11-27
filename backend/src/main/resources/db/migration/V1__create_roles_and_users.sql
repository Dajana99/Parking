-- FR-01, FR-03: uloge i korisnici (spec. odeljak 5, tabele roles i users)

CREATE TABLE roles (
    role_id   BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO roles (role_name) VALUES ('VOZAC'), ('OPERATER'), ('ADMINISTRATOR');

CREATE TABLE users (
    user_id       BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id       BIGINT       NOT NULL REFERENCES roles (role_id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role_id ON users (role_id);
