## ADDED Requirements

### Requirement: TPP client registration
MohanaTPP SHALL be registered as an OAuth2 client with Bala Bank's consent app, holding a client id, secret/credentials, and registered redirect URI.

#### Scenario: Registered client initiates flow
- **WHEN** MohanaTPP starts the consent flow using its registered credentials
- **THEN** Bala Bank's authorization server accepts the request as a known client

### Requirement: Consent initiation
MohanaTPP SHALL create an account-access or payment intent and redirect the user to Bala Bank's authorization endpoint with the requested permissions.

#### Scenario: Account access consent initiated
- **WHEN** a user clicks "Connect bank account" in MohanaTPP
- **THEN** MohanaTPP creates an account-access intent, then redirects the user to Bala Bank's `/authorize` with the requested permissions, `state`, and `redirect_uri`

#### Scenario: Payment consent initiated
- **WHEN** a user initiates a payment via MohanaTPP
- **THEN** MohanaTPP creates a domestic payment consent at Bala Bank and redirects the user to authorize it

### Requirement: Redirect handling and token exchange
MohanaTPP SHALL handle the authorization redirect, validate `state`, and exchange the authorization code for an access token.

#### Scenario: Successful callback
- **WHEN** Bala Bank redirects back with a valid `code` and matching `state`
- **THEN** MohanaTPP exchanges the code at the token endpoint and stores the resulting access token for the session

#### Scenario: State mismatch rejected
- **WHEN** the callback `state` does not match the value MohanaTPP sent
- **THEN** MohanaTPP aborts the flow and does not request a token

### Requirement: API consumption
MohanaTPP SHALL call Bala Bank's Open Banking APIs through the APISIX gateway using the access token.

#### Scenario: Display account data
- **WHEN** MohanaTPP holds a valid token granting account permissions
- **THEN** it calls the AIS endpoints via the gateway and displays the user's accounts, balances, and transactions

#### Scenario: Initiate payment
- **WHEN** MohanaTPP holds an authorized payment consent token
- **THEN** it calls the PIS payment endpoint via the gateway and shows the returned payment status

#### Scenario: Access denied surfaced
- **WHEN** the gateway or ASPSP returns 401/403
- **THEN** MohanaTPP surfaces an error and prompts the user to re-consent rather than retrying blindly
