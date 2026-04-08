CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(200) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    account_id UUID         NOT NULL UNIQUE REFERENCES accounts(id),
    pin_hash   VARCHAR(64),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_account_id ON users(account_id);
