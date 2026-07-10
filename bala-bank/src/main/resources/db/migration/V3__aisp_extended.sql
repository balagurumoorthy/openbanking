-- Extended AISP resources: beneficiaries, direct debits, standing orders,
-- scheduled payments, statements, party, products, offers, account-access-consents.

CREATE TABLE beneficiary (
    id                        BIGINT PRIMARY KEY,
    account_id                VARCHAR(64) NOT NULL,
    beneficiary_id            VARCHAR(64) NOT NULL,
    account_identification    VARCHAR(64) NOT NULL,
    account_name              VARCHAR(128) NOT NULL,
    account_sort_code         VARCHAR(16),
    reference                 VARCHAR(64)
);

CREATE TABLE direct_debit (
    id                       BIGINT PRIMARY KEY,
    account_id               VARCHAR(64) NOT NULL,
    direct_debit_id          VARCHAR(64) NOT NULL,
    frequency                VARCHAR(32) NOT NULL,
    status                   VARCHAR(16) NOT NULL,
    previous_payment_amount  DECIMAL(18,2),
    previous_payment_currency VARCHAR(3),
    previous_payment_date    TIMESTAMP WITH TIME ZONE,
    name                     VARCHAR(128)
);

CREATE TABLE standing_order (
    id                    BIGINT PRIMARY KEY,
    account_id            VARCHAR(64) NOT NULL,
    standing_order_id     VARCHAR(64) NOT NULL,
    frequency             VARCHAR(32) NOT NULL,
    status                VARCHAR(32) NOT NULL,
    next_payment_amount   DECIMAL(18,2),
    next_payment_currency VARCHAR(3),
    next_payment_date     TIMESTAMP WITH TIME ZONE,
    final_payment_amount  DECIMAL(18,2),
    final_payment_currency VARCHAR(3),
    creditor_identification VARCHAR(64),
    creditor_name         VARCHAR(128)
);

CREATE TABLE scheduled_payment (
    id                        BIGINT PRIMARY KEY,
    account_id                VARCHAR(64) NOT NULL,
    scheduled_payment_id      VARCHAR(64) NOT NULL,
    scheduled_payment_date    TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_type            VARCHAR(32) NOT NULL,
    amount                    DECIMAL(18,2) NOT NULL,
    currency                  VARCHAR(3) NOT NULL,
    creditor_identification   VARCHAR(64),
    creditor_name             VARCHAR(128),
    reference                 VARCHAR(64)
);

CREATE TABLE statement (
    id                     BIGINT PRIMARY KEY,
    account_id             VARCHAR(64) NOT NULL,
    statement_id           VARCHAR(64) NOT NULL,
    statement_type         VARCHAR(32) NOT NULL,
    start_date_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    creation_date_time     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE party (
    id                BIGINT PRIMARY KEY,
    customer_id       VARCHAR(64) NOT NULL,
    account_id        VARCHAR(64),
    party_id          VARCHAR(64) NOT NULL,
    party_number      VARCHAR(64),
    full_legal_name   VARCHAR(128) NOT NULL,
    party_type        VARCHAR(32) NOT NULL,
    email_address     VARCHAR(128),
    phone_number      VARCHAR(32)
);

CREATE TABLE product (
    id              BIGINT PRIMARY KEY,
    account_id      VARCHAR(64),
    product_id       VARCHAR(64) NOT NULL,
    product_name     VARCHAR(128) NOT NULL,
    product_type     VARCHAR(64) NOT NULL,
    marketing_state  VARCHAR(32)
);

CREATE TABLE offer (
    id              BIGINT PRIMARY KEY,
    account_id       VARCHAR(64) NOT NULL,
    offer_id         VARCHAR(64) NOT NULL,
    offer_type       VARCHAR(64) NOT NULL,
    description      VARCHAR(256),
    amount           DECIMAL(18,2),
    currency         VARCHAR(3),
    start_date_time  TIMESTAMP WITH TIME ZONE,
    end_date_time    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE account_access_consent (
    consent_id     VARCHAR(64) PRIMARY KEY,
    customer_id    VARCHAR(64) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    permissions    VARCHAR(2048) NOT NULL,
    expiration_date_time     TIMESTAMP WITH TIME ZONE,
    transaction_from_date_time TIMESTAMP WITH TIME ZONE,
    transaction_to_date_time   TIMESTAMP WITH TIME ZONE,
    creation_date_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    status_update_date_time  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE SEQUENCE beneficiary_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE direct_debit_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE standing_order_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE scheduled_payment_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE statement_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE party_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE product_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE offer_seq START WITH 100 INCREMENT BY 1;

-- Beneficiaries
INSERT INTO beneficiary (id, account_id, beneficiary_id, account_identification, account_name, account_sort_code, reference) VALUES
 (1, 'GB-ALICE-001', 'bene-alice-1', '11223344556677', 'Landlord Ltd',   '112233', 'RENT'),
 (2, 'GB-ALICE-002', 'bene-alice-2', '22334455667788', 'Alice Current',  '223344', 'SAVE'),
 (3, 'GB-BOB-001',   'bene-bob-1',   '33445566778899', 'Bob Landlord',   '334455', 'RENT');

-- Direct debits
INSERT INTO direct_debit (id, account_id, direct_debit_id, frequency, status, previous_payment_amount, previous_payment_currency, previous_payment_date, name) VALUES
 (1, 'GB-ALICE-001', 'dd-alice-1', 'MNTH', 'Active', 45.00, 'GBP', '2026-05-01T09:00:00Z', 'Gym Membership'),
 (2, 'GB-ALICE-002', 'dd-alice-2', 'MNTH', 'Active', 12.99, 'GBP', '2026-05-05T09:00:00Z', 'Streaming Service'),
 (3, 'GB-BOB-001',   'dd-bob-1',   'MNTH', 'Active', 60.00, 'GBP', '2026-05-03T09:00:00Z', 'Phone Contract');

-- Standing orders
INSERT INTO standing_order (id, account_id, standing_order_id, frequency, status, next_payment_amount, next_payment_currency, next_payment_date, final_payment_amount, final_payment_currency, creditor_identification, creditor_name) VALUES
 (1, 'GB-ALICE-001', 'so-alice-1', 'MNTH', 'Active', 500.00, 'GBP', '2026-07-01T09:00:00Z', NULL, NULL, '99887766554433', 'Alice Savings Transfer'),
 (2, 'GB-ALICE-002', 'so-alice-2', 'WEEK', 'Active', 25.00,  'GBP', '2026-07-05T09:00:00Z', NULL, NULL, '99887766554400', 'Charity Weekly'),
 (3, 'GB-BOB-001',   'so-bob-1',   'MNTH', 'Active', 300.00, 'GBP', '2026-07-01T09:00:00Z', NULL, NULL, '99887766554455', 'Bob Rent Standing Order');

-- Scheduled payments
INSERT INTO scheduled_payment (id, account_id, scheduled_payment_id, scheduled_payment_date, scheduled_type, amount, currency, creditor_identification, creditor_name, reference) VALUES
 (1, 'GB-ALICE-001', 'sp-alice-1', '2026-07-15T09:00:00Z', 'Arranged', 150.00, 'GBP', '11119922334455', 'Utility Co', 'ELECTRICITY'),
 (2, 'GB-ALICE-002', 'sp-alice-2', '2026-07-20T09:00:00Z', 'Arranged', 75.00,  'GBP', '22229922334455', 'Water Co',   'WATER'),
 (3, 'GB-BOB-001',   'sp-bob-1',   '2026-07-18T09:00:00Z', 'Arranged', 200.00, 'GBP', '33339922334455', 'Council Tax', 'COUNCIL TAX');

-- Statements
INSERT INTO statement (id, account_id, statement_id, statement_type, start_date_time, end_date_time, creation_date_time) VALUES
 (1, 'GB-ALICE-001', 'stmt-alice-1', 'AccountStatement', '2026-05-01T00:00:00Z', '2026-05-31T23:59:59Z', '2026-06-01T06:00:00Z'),
 (2, 'GB-ALICE-002', 'stmt-alice-2', 'AccountStatement', '2026-05-01T00:00:00Z', '2026-05-31T23:59:59Z', '2026-06-01T06:00:00Z'),
 (3, 'GB-BOB-001',   'stmt-bob-1',   'AccountStatement', '2026-05-01T00:00:00Z', '2026-05-31T23:59:59Z', '2026-06-01T06:00:00Z');

-- Party
INSERT INTO party (id, customer_id, account_id, party_id, party_number, full_legal_name, party_type, email_address, phone_number) VALUES
 (1, 'alice', 'GB-ALICE-001', 'party-alice-1', 'PN-ALICE-1', 'Alice Anderson', 'Sole', 'alice@example.com', '+447700900001'),
 (2, 'alice', 'GB-ALICE-002', 'party-alice-2', 'PN-ALICE-2', 'Alice Anderson', 'Sole', 'alice@example.com', '+447700900001'),
 (3, 'bob',   'GB-BOB-001',   'party-bob-1',   'PN-BOB-1',   'Bob Baker',      'Sole', 'bob@example.com',   '+447700900002');

-- Products (not account-scoped, marketing catalogue; also account-scoped variant)
INSERT INTO product (id, account_id, product_id, product_name, product_type, marketing_state) VALUES
 (1, NULL,           'prod-current-1', 'Everyday Current Account', 'PersonalCurrentAccount', 'Active'),
 (2, NULL,           'prod-saver-1',   'Rainy Day Saver',          'PersonalSavingsAccount', 'Active'),
 (3, 'GB-ALICE-001',  'prod-current-1', 'Everyday Current Account', 'PersonalCurrentAccount', 'Active'),
 (4, 'GB-ALICE-002',  'prod-saver-1',   'Rainy Day Saver',          'PersonalSavingsAccount', 'Active'),
 (5, 'GB-BOB-001',    'prod-current-1', 'Everyday Current Account', 'PersonalCurrentAccount', 'Active');

-- Offers
INSERT INTO offer (id, account_id, offer_id, offer_type, description, amount, currency, start_date_time, end_date_time) VALUES
 (1, 'GB-ALICE-001', 'offer-alice-1', 'Loan',      '3.9% APR Personal Loan pre-approved', 5000.00, 'GBP', '2026-06-01T00:00:00Z', '2026-12-31T23:59:59Z'),
 (2, 'GB-ALICE-002', 'offer-alice-2', 'Savings',   'Boosted 4.5% AER on Rainy Day Saver',  NULL,   'GBP', '2026-06-01T00:00:00Z', '2026-12-31T23:59:59Z'),
 (3, 'GB-BOB-001',   'offer-bob-1',   'Overdraft', 'Fee-free arranged overdraft offer',    500.00, 'GBP', '2026-06-01T00:00:00Z', '2026-12-31T23:59:59Z');

-- Account access consents (ASPSP side records)
INSERT INTO account_access_consent (consent_id, customer_id, status, permissions, expiration_date_time, transaction_from_date_time, transaction_to_date_time, creation_date_time, status_update_date_time) VALUES
 ('aac-alice-1', 'alice', 'Authorised', 'ReadAccountsDetail,ReadBalances,ReadTransactionsDetail,ReadBeneficiariesDetail,ReadDirectDebits,ReadStandingOrdersDetail,ReadScheduledPaymentsDetail,ReadStatementsDetail,ReadParty,ReadPartyPSU,ReadProducts,ReadOffers', '2026-12-31T23:59:59Z', '2026-01-01T00:00:00Z', '2026-12-31T23:59:59Z', '2026-06-01T09:00:00Z', '2026-06-01T09:00:00Z'),
 ('aac-bob-1',   'bob',   'Authorised', 'ReadAccountsDetail,ReadBalances,ReadTransactionsDetail,ReadBeneficiariesDetail,ReadDirectDebits,ReadStandingOrdersDetail,ReadScheduledPaymentsDetail,ReadStatementsDetail,ReadParty,ReadPartyPSU,ReadProducts,ReadOffers', '2026-12-31T23:59:59Z', '2026-01-01T00:00:00Z', '2026-12-31T23:59:59Z', '2026-06-01T09:00:00Z', '2026-06-01T09:00:00Z');
