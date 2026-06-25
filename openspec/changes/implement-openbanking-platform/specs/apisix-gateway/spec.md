## ADDED Requirements

### Requirement: API routing
APISIX SHALL route Open Banking API traffic to the Bala Bank ASPSP upstream based on declarative, version-controlled configuration.

#### Scenario: Route AIS and PIS paths
- **WHEN** a request matches `/open-banking/v3.1/aisp/*` or `/open-banking/v3.1/pisp/*`
- **THEN** APISIX forwards it to the bala-bank upstream service

#### Scenario: Unknown route rejected
- **WHEN** a request targets a path with no configured route
- **THEN** APISIX returns HTTP 404 and does not reach any upstream

### Requirement: Token validation
APISIX SHALL validate the bearer JWT on protected routes against the consent app's JWKS before forwarding.

#### Scenario: Valid token forwarded
- **WHEN** a request carries a JWT signed by the consent app with a valid signature and expiry
- **THEN** APISIX accepts it and forwards the request to the upstream

#### Scenario: Missing or invalid token rejected
- **WHEN** a request has no token, an expired token, or an invalid signature
- **THEN** APISIX returns HTTP 401 and does not forward the request

### Requirement: Scope enforcement
APISIX SHALL enforce that the token carries the scope required by the matched route.

#### Scenario: Insufficient scope blocked
- **WHEN** a token lacks the scope required for the requested endpoint
- **THEN** APISIX returns HTTP 403 before the request reaches the ASPSP

### Requirement: mTLS termination
APISIX SHALL support mutual TLS from the TPP using the local self-signed CA, configurable on or off for first bring-up.

#### Scenario: mTLS enabled accepts trusted client cert
- **WHEN** mTLS is enabled and the TPP presents a client certificate signed by the trusted CA
- **THEN** APISIX completes the handshake and processes the request

#### Scenario: mTLS enabled rejects untrusted cert
- **WHEN** mTLS is enabled and the client presents no certificate or an untrusted one
- **THEN** APISIX terminates the connection

### Requirement: Rate limiting
APISIX SHALL apply rate limiting to protect the ASPSP upstream.

#### Scenario: Within limit
- **WHEN** request rate from a client is under the configured threshold
- **THEN** requests are forwarded normally

#### Scenario: Over limit
- **WHEN** a client exceeds the configured rate threshold
- **THEN** APISIX returns HTTP 429
