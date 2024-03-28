CREATE TABLE transactions (
    uuid UUID PRIMARY KEY,
    account_number DECIMAL NOT NULL,
    type INT NOT NULL,
    amount BIGINT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transaction_account_number ON transactions (account_number);
ALTER TABLE transactions ADD CONSTRAINT FK_account_number FOREIGN KEY (account_number) REFERENCES accounts(account_number);