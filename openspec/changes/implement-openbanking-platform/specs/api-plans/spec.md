## ADDED Requirements

### Requirement: Tiered API plans
The platform SHALL support TPP API plan tiers — SILVER, GOLD, DIAMOND — each granting a
distinct per-minute request allowance, with each TPP assigned to exactly one tier.

#### Scenario: Default tier
- **WHEN** a TPP (e.g. MohanaTPP) is registered without an explicit plan
- **THEN** it is assigned the SILVER tier

#### Scenario: Tier allowances
- **WHEN** the tiers are configured
- **THEN** SILVER allows 5, GOLD allows 20, and DIAMOND allows 100 requests per minute

### Requirement: Tiered rate limiting
The platform SHALL enforce the requesting TPP's tier allowance on the Open Banking API and
reject requests beyond it.

#### Scenario: Within allowance
- **WHEN** a TPP issues requests within its tier's per-minute allowance
- **THEN** each request returns normally with `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers

#### Scenario: Over allowance
- **WHEN** a TPP exceeds its tier's per-minute allowance
- **THEN** the platform returns HTTP 429 with an OBIE error (`UK.OBIE.Rate.LimitExceeded`) and a `Retry-After` header

#### Scenario: Identity for metering
- **WHEN** a metered request arrives
- **THEN** the TPP is identified from the access token's `client_id` claim and counted against that client

### Requirement: Usage portal and upgrade
The platform SHALL provide a UI where a TPP can view its current usage against its tier
allowance and upgrade to a higher tier to increase its traffic limit.

#### Scenario: View usage
- **WHEN** the TPP opens the developer portal
- **THEN** it shows the current tier, used vs allowed requests in the active window, and remaining allowance, refreshed live

#### Scenario: Upgrade increases the limit
- **WHEN** the TPP upgrades from a lower tier to a higher one
- **THEN** the new (higher) allowance takes effect immediately for subsequent requests

#### Scenario: Gateway parity
- **WHEN** the platform runs behind the APISIX gateway
- **THEN** equivalent tiered limits are enforced at the gateway via consumer-groups with limit-count
