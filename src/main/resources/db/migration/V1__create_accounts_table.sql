CREATE TABLE IF NOT EXISTS accounts (
    account_number DECIMAL(24,0),
    account_holder_name VARCHAR(255),
    is_deleted BOOLEAN,
    PRIMARY KEY (account_number)
);

CREATE SEQUENCE IF NOT EXISTS accounts_seq;

CREATE INDEX IF NOT EXISTS idx_accountholdername ON accounts (account_holder_name);
CREATE INDEX IF NOT EXISTS idx_isdeleted ON accounts (is_deleted);
