ALTER TABLE accounts DROP CONSTRAINT IF EXISTS accounts_currency_check;
UPDATE accounts SET currency = 'INR' WHERE currency <> 'INR';
UPDATE transactions SET currency = 'INR' WHERE currency <> 'INR';
ALTER TABLE accounts ADD CONSTRAINT accounts_currency_check
  CHECK (currency IN ('INR'));
ALTER TABLE accounts ALTER COLUMN currency SET DEFAULT 'INR';
