-- PISP breadth (6.3): scheduled payments, standing orders, international payments.
CREATE TABLE scheduled_payment_consent (
    consent_id                    VARCHAR(64) PRIMARY KEY,
    debtor_account_id             VARCHAR(64),
    creditor_identification       VARCHAR(64),
    creditor_name                 VARCHAR(128),
    amount                        DECIMAL(18,2),
    currency                      VARCHAR(3),
    reference                     VARCHAR(128),
    requested_execution_date_time TIMESTAMP WITH TIME ZONE,
    status                        VARCHAR(32) NOT NULL
);

CREATE TABLE domestic_scheduled_payment (
    scheduled_payment_id VARCHAR(64) PRIMARY KEY,
    consent_id            VARCHAR(64) NOT NULL,
    status                VARCHAR(40) NOT NULL
);

CREATE TABLE standing_order_consent (
    consent_id              VARCHAR(64) PRIMARY KEY,
    debtor_account_id       VARCHAR(64),
    creditor_identification VARCHAR(64),
    creditor_name           VARCHAR(128),
    first_payment_amount    DECIMAL(18,2),
    currency                VARCHAR(3),
    reference               VARCHAR(128),
    frequency               VARCHAR(64),
    number_of_payments      INTEGER,
    status                  VARCHAR(32) NOT NULL
);

CREATE TABLE domestic_standing_order (
    standing_order_id VARCHAR(64) PRIMARY KEY,
    consent_id        VARCHAR(64) NOT NULL,
    status            VARCHAR(40) NOT NULL
);

CREATE TABLE international_payment_consent (
    consent_id                       VARCHAR(64) PRIMARY KEY,
    debtor_account_id                VARCHAR(64),
    creditor_account_identification  VARCHAR(64),
    creditor_account_name            VARCHAR(128),
    creditor_agent_bic               VARCHAR(32),
    instructed_amount                DECIMAL(18,2),
    instructed_currency              VARCHAR(3),
    reference                        VARCHAR(128),
    status                           VARCHAR(32) NOT NULL
);

CREATE TABLE international_payment (
    international_payment_id VARCHAR(64) PRIMARY KEY,
    consent_id                VARCHAR(64) NOT NULL,
    status                    VARCHAR(40) NOT NULL
);

-- CBPII (6.4): funds confirmation consents.
CREATE TABLE funds_confirmation_consent (
    consent_id           VARCHAR(64) PRIMARY KEY,
    debtor_account_id    VARCHAR(64),
    expiration_date_time TIMESTAMP WITH TIME ZONE,
    status               VARCHAR(32) NOT NULL
);

-- Event Notification (6.5): subscriptions and callback URLs.
CREATE TABLE event_subscription (
    subscription_id VARCHAR(64) PRIMARY KEY,
    client_id       VARCHAR(64) NOT NULL,
    callback_url    VARCHAR(256),
    event_types     VARCHAR(512),
    version         VARCHAR(8)
);

CREATE TABLE callback_url (
    callback_url_id VARCHAR(64) PRIMARY KEY,
    client_id       VARCHAR(64) NOT NULL,
    url             VARCHAR(256),
    version         VARCHAR(8)
);

-- Seed: one representative row per new resource, debtor GB-ALICE-001.
INSERT INTO scheduled_payment_consent
 (consent_id, debtor_account_id, creditor_identification, creditor_name, amount, currency, reference, requested_execution_date_time, status)
VALUES
 ('spcon-seed-0001', 'GB-ALICE-001', '80200112345678', 'Landlord Ltd', 850.00, 'GBP', 'RENT JULY', '2026-07-01T09:00:00Z', 'AUTHORISED');

INSERT INTO standing_order_consent
 (consent_id, debtor_account_id, creditor_identification, creditor_name, first_payment_amount, currency, reference, frequency, number_of_payments, status)
VALUES
 ('socon-seed-0001', 'GB-ALICE-001', '80200187654321', 'Gym Membership', 45.00, 'GBP', 'GYM', 'EvryMnth', 12, 'AUTHORISED');

INSERT INTO international_payment_consent
 (consent_id, debtor_account_id, creditor_account_identification, creditor_account_name, creditor_agent_bic, instructed_amount, instructed_currency, reference, status)
VALUES
 ('ipcon-seed-0001', 'GB-ALICE-001', 'FR7630006000011234567890189', 'Cousin Dupont', 'BNPAFRPPXXX', 200.00, 'EUR', 'HOLIDAY GIFT', 'AUTHORISED');

INSERT INTO funds_confirmation_consent
 (consent_id, debtor_account_id, expiration_date_time, status)
VALUES
 ('fccon-seed-0001', 'GB-ALICE-001', '2026-12-31T23:59:59Z', 'AUTHORISED');

INSERT INTO event_subscription
 (subscription_id, client_id, callback_url, event_types, version)
VALUES
 ('evtsub-seed-0001', 'default-client', 'https://tpp.example.com/callback', 'urn:uk:org:openbanking:events:resource-update', '3.1');
