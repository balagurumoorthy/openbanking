## Why

Third Party Providers (TPPs) need a standards-based, secure way to access bank account and payment data on behalf of consenting customers. We need a runnable reference platform that demonstrates the full UK Open Banking flow — a bank (ASPSP) exposing Open Banking APIs through an API gateway, a customer-facing consent/login app where users grant scoped permissions, and a TPP client app that consumes those APIs — all built on a fast-startup, low-footprint Java + GraalVM stack and deployable locally on minikube for end-to-end testing.

## What Changes

- Introduce **Bala Bank**, an ASPSP that exposes Open Banking Read/Write APIs (Account Information + Payment Initiation) backed by sample account data.
- Provide **full UK Open Banking (OBIE) API standard conformance** across AISP, PISP, CBPII (Confirmation of Funds), and Event Notification surfaces, with a comprehensive seeded sample dataset so every endpoint is testable locally.
- Introduce **MohanaTPP**, a TPP client application that registers with Bala Bank, initiates consent, and calls the Open Banking APIs (fetch accounts/balances/transactions, initiate a payment).
- Introduce a **Bala Bank Consent & Login app** — a separate customer-facing web app where a bank user authenticates and selects/approves the specific permissions (scopes/accounts) requested by MohanaTPP, producing an authorization grant.
- Introduce **APISIX** as the API gateway fronting Bala Bank's Open Banking APIs, enforcing routing, OAuth2/token validation, mTLS termination, and rate limiting.
- Provide an **OAuth2/OIDC authorization server** issuing scoped access tokens tied to a consent (FAPI-style flow, simplified for local use).
- Provide **minikube deployment** manifests (Helm/Kustomize) to run the whole platform locally and execute an end-to-end consent → data-access → payment scenario.
- Standardize all services on **Java + GraalVM native image** (Quarkus) for fast startup and small container images.

## Capabilities

### New Capabilities
- `bala-bank-aspsp`: Bala Bank's Open Banking API resource server — Account Information (accounts, balances, transactions) and Payment Initiation endpoints, plus the backing account domain model.
- `consent-authorization`: OAuth2/OIDC authorization server and the customer-facing login + consent UI where users authenticate and select the permissions/accounts granted to a TPP.
- `mohana-tpp`: The MohanaTPP client application — dynamic/static client registration, consent initiation, redirect handling, token exchange, and API consumption.
- `apisix-gateway`: APISIX gateway configuration fronting the ASPSP APIs — routes, upstreams, token/JWT validation, mTLS, and rate-limiting policies.
- `local-deployment`: minikube-based local deployment of all components and the end-to-end test scenario (Helm/Kustomize manifests, container build, smoke test).
- `api-plans`: Tiered TPP API plans (SILVER/GOLD/DIAMOND) enforced in APISIX via jwt-auth consumers + consumer-groups + limit-count (Redis), returning 429 over the allowance, with a thin admin portal to view live usage and upgrade tier (Admin API consumer-group move).
- `openbanking-api-standard`: Full UK Open Banking (OBIE) API standard conformance for Bala Bank — complete Account Information (AISP), Payment Initiation (PISP), Confirmation of Funds (CBPII), and Event Notification surfaces aligned to the OBIE Read/Write spec, plus a comprehensive seeded sample dataset to exercise every endpoint locally.

### Modified Capabilities
<!-- None — greenfield platform. -->

## Impact

- **New services/repos**: `bala-bank` (ASPSP), `consent-auth` (auth server + consent UI), `mohana-tpp` (TPP client). All Quarkus + GraalVM native.
- **Infrastructure**: APISIX (gateway), an OAuth2/OIDC authorization server, sample datastore (in-memory/H2 or Postgres), minikube cluster.
- **APIs**: UK Open Banking Read/Write API surface (Account Information v3.1, Payment Initiation v3.1, simplified).
- **Dependencies**: Quarkus, GraalVM native-image, APISIX, Helm/Kustomize, Docker, kubectl/minikube.
- **Security**: OAuth2 authorization code flow with scoped consent, JWT validation at the gateway, mTLS between TPP and gateway (self-signed certs for local use).
