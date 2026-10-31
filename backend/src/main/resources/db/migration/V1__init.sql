CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_users(id),
  account_number VARCHAR(24) NOT NULL UNIQUE,
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING','SAVINGS')),
  currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency IN ('USD','EUR','GBP')),
  balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX accounts_user_idx ON accounts(user_id);
CREATE TABLE beneficiaries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_users(id),
  name VARCHAR(120) NOT NULL,
  destination_account VARCHAR(24) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, destination_account)
);
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id UUID NOT NULL REFERENCES accounts(id),
  type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
  amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  currency CHAR(3) NOT NULL,
  description VARCHAR(255) NOT NULL,
  reference VARCHAR(80) NOT NULL,
  counterparty_account VARCHAR(24),
  status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX transactions_account_date_idx ON transactions(account_id, created_at DESC);
CREATE TABLE idempotency_records (
  user_id UUID NOT NULL REFERENCES app_users(id),
  idempotency_key VARCHAR(100) NOT NULL,
  response JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id, idempotency_key)
);
CREATE TABLE audit_events (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES app_users(id),
  action VARCHAR(80) NOT NULL,
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
