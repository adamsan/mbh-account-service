CREATE TABLE IF NOT EXISTS transactions (
    uuid UUID PRIMARY KEY,
    account_number DECIMAL(24,0) NOT NULL,
    type TINYINT NOT NULL,
    amount BIGINT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transaction_account_number ON transactions (account_number);
ALTER TABLE transactions ADD CONSTRAINT FK_account_number FOREIGN KEY (account_number) REFERENCES accounts(account_number);