CREATE TABLE account (
    account_id       VARCHAR(64) PRIMARY KEY,
    customer_id      VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    currency         VARCHAR(3)  NOT NULL,
    account_type     VARCHAR(32),
    account_sub_type VARCHAR(32),
    nickname         VARCHAR(128),
    identification   VARCHAR(64),
    name             VARCHAR(128)
);

CREATE TABLE balance (
    id                     BIGINT PRIMARY KEY,
    account_id             VARCHAR(64) NOT NULL,
    amount                 DECIMAL(18,2) NOT NULL,
    currency               VARCHAR(3) NOT NULL,
    credit_debit_indicator VARCHAR(8) NOT NULL,
    type                   VARCHAR(32) NOT NULL,
    date_time              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE transaction (
    id                      BIGINT PRIMARY KEY,
    account_id              VARCHAR(64) NOT NULL,
    credit_debit_indicator  VARCHAR(8) NOT NULL,
    status                  VARCHAR(16) NOT NULL,
    amount                  DECIMAL(18,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    booking_date_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    transaction_information VARCHAR(256)
);

CREATE TABLE payment_consent (
    consent_id              VARCHAR(64) PRIMARY KEY,
    debtor_account_id       VARCHAR(64),
    creditor_identification VARCHAR(64),
    creditor_name           VARCHAR(128),
    amount                  DECIMAL(18,2),
    currency                VARCHAR(3),
    reference               VARCHAR(128),
    status                  VARCHAR(32) NOT NULL
);

CREATE TABLE payment (
    payment_id VARCHAR(64) PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL,
    status     VARCHAR(40) NOT NULL
);

CREATE SEQUENCE balance_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE transaction_seq START WITH 100 INCREMENT BY 1;
