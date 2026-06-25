## ADDED Requirements

### Requirement: Account Information API
Bala Bank SHALL expose an Open Banking Account Information API (AIS, v3.1 subset) that returns accounts, balances, and transactions for the customer associated with a valid consent.

#### Scenario: List consented accounts
- **WHEN** a TPP calls `GET /open-banking/v3.1/aisp/accounts` with a valid access token whose consent grants `ReadAccountsDetail`
- **THEN** Bala Bank returns HTTP 200 with only the accounts the user selected during consent

#### Scenario: Retrieve account balances
- **WHEN** a TPP calls `GET /open-banking/v3.1/aisp/accounts/{accountId}/balances` with a token granting `ReadBalances` for that account
- **THEN** Bala Bank returns HTTP 200 with the account's balance amounts and currency

#### Scenario: Retrieve account transactions
- **WHEN** a TPP calls `GET /open-banking/v3.1/aisp/accounts/{accountId}/transactions` with a token granting `ReadTransactionsDetail`
- **THEN** Bala Bank returns HTTP 200 with the transaction list for that account

#### Scenario: Access outside granted consent is rejected
- **WHEN** a TPP requests an account or permission not included in the consent
- **THEN** Bala Bank returns HTTP 403 with an Open Banking error body and discloses no account data

### Requirement: Payment Initiation API
Bala Bank SHALL expose an Open Banking Payment Initiation API (PIS, v3.1 subset) that creates a domestic payment consent and executes a payment once authorized.

#### Scenario: Create domestic payment consent
- **WHEN** a TPP calls `POST /open-banking/v3.1/pisp/domestic-payment-consents` with payment details
- **THEN** Bala Bank creates a payment consent in `AwaitingAuthorisation` status and returns its `ConsentId`

#### Scenario: Execute authorized payment
- **WHEN** a TPP calls `POST /open-banking/v3.1/pisp/domestic-payments` referencing an authorized payment consent
- **THEN** Bala Bank records the payment, returns status `AcceptedSettlementInProcess`, and debits the source account in the sample ledger

#### Scenario: Reject unauthorized payment
- **WHEN** a TPP attempts to execute a payment whose consent is not in an authorized state
- **THEN** Bala Bank returns HTTP 400/403 and does not move funds

### Requirement: Consent enforcement
Bala Bank SHALL authorize every resource request against the consent encoded in the access token before returning data.

#### Scenario: Token without required scope
- **WHEN** a request arrives with a token lacking the scope required for the endpoint
- **THEN** Bala Bank returns HTTP 403 and logs the denied access

#### Scenario: Expired or revoked consent
- **WHEN** a request references a consent that is expired or revoked
- **THEN** Bala Bank returns HTTP 403 with an `invalid_consent` error

### Requirement: Sample account data
Bala Bank SHALL seed sample customers, accounts, balances, and transactions so the platform is demonstrable without external data.

#### Scenario: Seed data available at startup
- **WHEN** Bala Bank starts
- **THEN** at least one customer with multiple accounts, balances, and transactions is available for the consent and API flows
