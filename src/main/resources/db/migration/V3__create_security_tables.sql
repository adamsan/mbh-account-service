create TABLE IF NOT EXISTS security_request (
    callbackUUID UUID PRIMARY KEY,
    account_number DECIMAL(24,0) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL
);

alter table security_request add CONSTRAINT FK_security_request_account_number FOREIGN KEY (account_number) REFERENCES accounts(account_number);

create TABLE IF NOT EXISTS security_response (
    account_number DECIMAL(24,0),
    is_security_check_success BOOLEAN,
    PRIMARY KEY (account_number)
);

alter table security_response add CONSTRAINT FK_security_response_account_number FOREIGN KEY (account_number) REFERENCES accounts(account_number);
