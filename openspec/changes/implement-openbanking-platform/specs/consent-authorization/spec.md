## ADDED Requirements

### Requirement: User authentication
The Consent & Login app SHALL authenticate Bala Bank customers before any consent decision.

#### Scenario: Successful login
- **WHEN** a user submits valid Bala Bank credentials on the login page
- **THEN** the app establishes an authenticated session and proceeds to the consent screen

#### Scenario: Invalid credentials
- **WHEN** a user submits invalid credentials
- **THEN** the app rejects the login, shows an error, and does not create a session

### Requirement: OAuth2 authorization code flow
The Consent & Login app SHALL act as an OAuth2/OIDC authorization server implementing the authorization code flow for registered TPP clients.

#### Scenario: Authorization request from TPP
- **WHEN** a TPP redirects the user to `/authorize` with a valid `client_id`, `redirect_uri`, `scope`, `state`, and consent/intent reference
- **THEN** the app validates the client and redirect URI and presents the login + consent screen

#### Scenario: Authorization code issued after consent
- **WHEN** an authenticated user approves the consent
- **THEN** the app redirects to the TPP's `redirect_uri` with an authorization `code` and the original `state`

#### Scenario: Token exchange
- **WHEN** the TPP exchanges the authorization `code` at `/token` with valid client credentials
- **THEN** the app returns a signed JWT access token whose claims encode the consent id and granted scopes, and exposes a JWKS endpoint for verification

#### Scenario: Invalid client or redirect URI
- **WHEN** an authorization request uses an unregistered client or mismatched redirect URI
- **THEN** the app rejects the request and does not present a consent screen

### Requirement: Permission and account selection
The Consent & Login app SHALL let the user review the TPP's requested permissions and select which permissions and which accounts to grant.

#### Scenario: User narrows requested permissions
- **WHEN** the consent screen shows the TPP's requested permissions and the user deselects some
- **THEN** the resulting grant contains only the permissions the user kept

#### Scenario: User selects specific accounts
- **WHEN** the user picks a subset of their accounts to share
- **THEN** only the selected accounts are bound to the consent and accessible to the TPP

#### Scenario: User denies consent
- **WHEN** the user rejects the consent request
- **THEN** the app redirects to the TPP with an `access_denied` error and grants nothing

### Requirement: Consent lifecycle
The Consent & Login app SHALL persist consent records and support their lifecycle (authorized, expired, revoked).

#### Scenario: Consent persisted on approval
- **WHEN** a user approves a consent
- **THEN** a consent record is stored with the granted scopes, selected accounts, status `Authorised`, and an expiry

#### Scenario: Consent revocation
- **WHEN** a user revokes a previously granted consent
- **THEN** the consent status becomes `Revoked` and tokens bound to it are no longer accepted
