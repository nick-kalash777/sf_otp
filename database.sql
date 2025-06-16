CREATE TYPE otp_status AS ENUM ('ACTIVE', 'EXPIRED', 'USED');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    login         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    is_admin      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Таблица конфигурации OTP-кодов (не более одной строки)

CREATE TABLE otp_config (
    id            INT         PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    code_length   INT         NOT NULL DEFAULT 6,
    code_ttl      INT    NOT NULL DEFAULT 5,
    max_attempts  INT         NOT NULL DEFAULT 3,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Таблица OTP-кодов
CREATE TABLE otp_codes (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id  UUID         NOT NULL,
    code          VARCHAR(64)  NOT NULL,
    status        otp_status   NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ
);

-- Индексы для ускорения поиска по operation_id и статусам
CREATE INDEX idx_otp_codes_operation ON otp_codes(operation_id);
CREATE INDEX idx_otp_codes_status    ON otp_codes(status);