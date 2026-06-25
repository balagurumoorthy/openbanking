## ADDED Requirements

### Requirement: OBIE Account Information (AISP) coverage
Bala Bank SHALL implement the full UK Open Banking Account Information API (Read/Write v3.1) endpoint surface so any AISP operation in the standard can be exercised locally.

#### Scenario: Account access consent endpoints
- **WHEN** a TPP calls the account-access-consents resource (`POST`, `GET {ConsentId}`, `DELETE {ConsentId}`)
- **THEN** Bala Bank creates, returns, and revokes the consent per the OBIE schema and status model

#### Scenario: Full AIS resource surface available
- **WHEN** a TPP calls any standard AIS resource — accounts, balances, transactions, beneficiaries, direct-debits, standing-orders, products, offers, party/parties, scheduled-payments, statements
- **THEN** Bala Bank returns HTTP 200 with OBIE-schema-conformant bodies for the resources permitted by the consent

#### Scenario: Bulk and per-account access
- **WHEN** a TPP requests a bulk resource (e.g. `GET /transactions`) and a per-account resource (e.g. `GET /accounts/{AccountId}/transactions`)
- **THEN** both return data limited to the consented accounts and permissions

### Requirement: OBIE Payment Initiation (PISP) coverage
Bala Bank SHALL implement the full UK Open Banking Payment Initiation API surface across the standard payment types.

#### Scenario: Domestic payment types
- **WHEN** a TPP uses domestic-payment-consents/domestic-payments, domestic-scheduled-payment-consents/domestic-scheduled-payments, and domestic-standing-order-consents/domestic-standing-orders
- **THEN** Bala Bank creates the consent, supports authorization, and executes/records each payment type per the OBIE status model

#### Scenario: International payment types
- **WHEN** a TPP uses international-payment, international-scheduled-payment, and international-standing-order consent/execution resources
- **THEN** Bala Bank processes them with currency/exchange-rate fields per the OBIE schema

#### Scenario: File payments and funds confirmation
- **WHEN** a TPP uses file-payment-consents/file-payments or calls the PISP `funds-confirmation` resource on a payment consent
- **THEN** Bala Bank returns conformant responses including the payment/funds-availability status

#### Scenario: Payment details and status retrieval
- **WHEN** a TPP calls `GET {PaymentId}` or `GET {PaymentId}/payment-details`
- **THEN** Bala Bank returns the current payment status and detail per the OBIE schema

### Requirement: OBIE Confirmation of Funds (CBPII) coverage
Bala Bank SHALL implement the Confirmation of Funds API for CBPII clients.

#### Scenario: Funds confirmation consent and check
- **WHEN** a CBPII creates a funds-confirmation-consent, authorizes it, then calls `POST /funds-confirmations`
- **THEN** Bala Bank returns whether sufficient funds are available for the requested amount without exposing balances

### Requirement: OBIE Event Notification coverage
Bala Bank SHALL support the Open Banking event notification mechanisms for resource and aggregated polling/push.

#### Scenario: Callback URL and event subscription
- **WHEN** a TPP registers a callback-url or event-subscription
- **THEN** Bala Bank stores it and returns the conformant resource

#### Scenario: Event polling
- **WHEN** a TPP calls `POST /events` (aggregated polling)
- **THEN** Bala Bank returns queued event notifications (e.g. resource-update, consent-revoked) in the OBIE event format

### Requirement: Standard conformance and error model
Bala Bank SHALL align request/response schemas, headers, pagination, and error bodies to the OBIE standard.

#### Scenario: Required headers and metadata
- **WHEN** a TPP sends standard headers (`x-fapi-interaction-id`, `x-fapi-financial-id`, `Authorization`) and the response is built
- **THEN** Bala Bank echoes/sets the required headers and includes OBIE `Meta` and `Links` (pagination) in collection responses

#### Scenario: Conformant error response
- **WHEN** a request is invalid or unauthorized
- **THEN** Bala Bank returns the OBIE error structure (`Code`, `Id`, `Message`, `Errors[]` with `ErrorCode`)

### Requirement: Comprehensive sample dataset
Bala Bank SHALL seed a comprehensive sample dataset covering every supported resource so all endpoints are demonstrable locally without external data.

#### Scenario: Seed data covers all AIS resources
- **WHEN** the platform starts
- **THEN** seeded data includes multiple customers, multiple accounts per customer, balances, transactions, beneficiaries, standing orders, direct debits, scheduled payments, statements, products, offers, and party records

#### Scenario: Seed data supports payment and funds flows
- **WHEN** payment, scheduled, standing-order, international, and funds-confirmation flows are exercised
- **THEN** seeded source accounts have sufficient balances and the required reference data to complete each flow

#### Scenario: Sample data is documented and reproducible
- **WHEN** an operator inspects the project
- **THEN** the sample dataset (customers, credentials, account ids, scenarios) is documented so test flows can be run deterministically
