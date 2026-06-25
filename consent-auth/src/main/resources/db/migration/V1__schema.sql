CREATE TABLE customer (
    customer_id  VARCHAR(64) PRIMARY KEY,
    username     VARCHAR(64) NOT NULL UNIQUE,
    password     VARCHAR(128) NOT NULL,
    display_name VARCHAR(128)
);

CREATE TABLE registered_client (
    client_id      VARCHAR(64) PRIMARY KEY,
    client_secret  VARCHAR(128) NOT NULL,
    redirect_uri   VARCHAR(256) NOT NULL,
    allowed_scopes VARCHAR(256) NOT NULL
);

CREATE TABLE consent (
    consent_id            VARCHAR(64) PRIMARY KEY,
    client_id             VARCHAR(64) NOT NULL,
    customer_id           VARCHAR(64),
    requested_permissions VARCHAR(2000),
    granted_permissions   VARCHAR(2000),
    granted_accounts      VARCHAR(2000),
    status                VARCHAR(32) NOT NULL,
    expires_at            TIMESTAMP
);

CREATE TABLE auth_code (
    code         VARCHAR(64) PRIMARY KEY,
    client_id    VARCHAR(64) NOT NULL,
    consent_id   VARCHAR(64) NOT NULL,
    customer_id  VARCHAR(64) NOT NULL,
    redirect_uri VARCHAR(256) NOT NULL,
    expires_at   TIMESTAMP,
    used         BOOLEAN DEFAULT FALSE
);
