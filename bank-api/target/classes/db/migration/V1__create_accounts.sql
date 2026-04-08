CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    balance NUMERIC(19,2) NOT NULL CHECK (balance >= 0),
    frozen BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_type ON accounts(type);
