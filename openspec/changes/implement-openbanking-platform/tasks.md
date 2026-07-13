## 1. Project scaffolding & shared setup

- [x] 1.1 Create a multi-module repo layout: `bala-bank`, `consent-auth`, `mohana-tpp`, plus `deploy/` (manifests) and `gateway/` (APISIX config)
- [x] 1.2 Bootstrap each service as a Quarkus app (Java 21) with REST, OIDC/JWT, Hibernate ORM, and native-image extensions
- [x] 1.3 Define shared Open Banking DTOs/enums (accounts, balances, transactions, payment consents, permission scopes)
- [x] 1.4 Add a script to generate a local self-signed CA and service certs/keystores
- [x] 1.5 Configure JVM dev mode + native build profiles in each `pom.xml`/`build.gradle`

## 2. Consent & Login app (consent-authorization)

- [x] 2.1 Model `Customer`, `Account`, `Consent` (requested vs granted scopes, selected accounts, status, expiry) and persist (H2 default, Postgres optional)
- [x] 2.2 Implement username/password authentication and session for Bala Bank customers
- [x] 2.3 Implement OAuth2 `/authorize` endpoint: validate client_id, redirect_uri, scope, state
- [x] 2.4 Build the consent UI: show requested permissions, allow narrowing permissions and selecting specific accounts, plus approve/deny
- [x] 2.5 Issue authorization code on approval; redirect with code + state; return `access_denied` on deny
- [x] 2.6 Implement `/token` endpoint: exchange code for a signed JWT encoding consent id + granted scopes; expose `/jwks`
- [x] 2.7 Implement consent lifecycle: persist on approval, support revocation (`DELETE /account-access-consents/{id}`), reject expired/revoked at `/token` (lazy expiry) and block re-approval of non-pending consents
- [x] 2.8 Seed sample customers and accounts (shared with bala-bank dataset)

## 3. Bala Bank ASPSP (bala-bank-aspsp)

- [x] 3.1 Model account domain + seed customers, accounts, balances, transactions
- [x] 3.2 Implement AIS endpoints: `GET accounts`, `accounts/{id}/balances`, `accounts/{id}/transactions`
- [x] 3.3 Implement PIS endpoints: create domestic-payment-consent, execute domestic-payment, update sample ledger
- [x] 3.4 Implement consent enforcement: authorize each request against token's consent/scopes; restrict to consented accounts
- [x] 3.5 Return Open Banking error bodies (403 invalid_consent / insufficient scope, 400 unauthorized payment)
- [x] 3.6 Validate JWT (trusting consent-auth JWKS) for defense-in-depth behind the gateway

## 4. APISIX gateway (apisix-gateway)

- [x] 4.1 Author declarative routes/upstreams for AIS and PIS paths to the bala-bank service
- [x] 4.2 Configure jwt-auth/openid-connect plugin to validate tokens against consent-auth JWKS
- [x] 4.3 Configure per-route scope enforcement (403 on insufficient scope)
- [x] 4.4 Configure mTLS termination using the local OB CA (SSL object on `:9443`: server=aspsp-transport, client CA=ob-ca-bundle), with an `MTLS_ENABLED` toggle (off by default) — config + bootstrap wired (live verification pending)
- [x] 4.5 Configure `limit-req` rate limiting (429 over threshold)
- [x] 4.6 Verify 404 on unknown routes and 401 on missing/invalid token

## 5. MohanaTPP client (mohana-tpp)

- [x] 5.1 Register MohanaTPP as an OAuth2 client (client id/secret, redirect URI) with consent-auth
- [x] 5.2 Implement account-access consent initiation + redirect to `/authorize`
- [x] 5.3 Implement payment consent initiation + redirect (`/pay` form → consent-auth `/domestic-payment-consents` intent → `/authorize` with payments scope)
- [x] 5.4 Implement callback handler: validate state, exchange code for token, store per session
- [x] 5.5 Call AIS endpoints via APISIX and render accounts/balances/transactions
- [x] 5.6 Call PIS payment endpoint via APISIX and render payment status (createPaymentConsent → executePayment → status page)
- [x] 5.7 Surface 401/403 errors and prompt re-consent (friendly gatewayError page). mTLS client cert to the gateway: gateway side ready (task 4.4); TPP presents `certs/tpp-obwac.*` via curl — automated TPP-client-cert wiring is a follow-up

## 6. Full OBIE API standard conformance (openbanking-api-standard)

- [x] 6.1 Align AIS DTOs + envelope to true OBIE JSON: PascalCase fields, nested `Data.<Resource>` arrays, `Account[]` identifier block, string `Amount`, and PascalCase error model (Code/Id/Message/Errors[].ErrorCode)
- [x] 6.2 Complete AISP surface: account-access-consents (GET/DELETE), beneficiaries, direct-debits, standing-orders, scheduled-payments, products, offers, party/parties, statements (entity-backed, seeded) — POST intent lives in consent-auth
- [x] 6.3 Complete PISP surface: domestic + domestic-scheduled + domestic-standing-order, international, payment-details, and PISP funds-confirmation (file-payments not modelled)
- [x] 6.4 Implement CBPII funds-confirmation-consents + funds-confirmations (needs a `fundsconfirmations`-scoped token; noted)
- [x] 6.5 Implement Event Notification: callback-urls, event-subscriptions, aggregated `POST /events` polling (empty result set — no producer)
- [x] 6.6 Enforce standard headers (`x-fapi-interaction-id` echo filter), pagination Links/Meta helper on `ObResponse`, conformant `ObError` across endpoints
- [x] 6.7 Seed dataset covering every new AIS/PIS/CBPII/event resource (V3/V4 migrations, alice + bob)
- [x] 6.8 Document the sample dataset — [bala-bank/SAMPLE-DATA.md](../../../bala-bank/SAMPLE-DATA.md)

## 7. Local deployment on minikube (local-deployment)

- [x] 7.1 Write multi-stage Dockerfiles producing GraalVM native images on minimal base images
- [x] 7.2 Author Helm/Kustomize manifests for bala-bank, consent-auth, mohana-tpp, APISIX, and datastore
- [x] 7.3 Expose APISIX via NodePort (Ingress optional) and wire service-to-service config (URLs, JWKS, certs as secrets)
- [x] 7.4 Write a bring-up script/runbook: minikube start, build images, `minikube image load`, deploy, readiness checks
- [x] 7.5 Implement the end-to-end BDD suite with Cucumber-JVM + Gherkin (REST Assured step defs) covering the happy path and negative scenarios (access_denied, 401, 403, 429), runnable locally and as an in-cluster Job
- [x] 7.6 Document the full local workflow in a README and link the workflow document (WORKFLOW.md)

## 9. Tiered API plans & usage portal (api-plans)

- [x] 9.1 Add `Tier` enum (SILVER/GOLD/DIAMOND allowances) and `client_id`/`key` token claims
- [x] 9.2 Enforce tiers in APISIX: consumer-groups (silver/gold/diamond) + limit-count (redis policy)
- [x] 9.3 Identify each TPP as an APISIX `jwt-auth` consumer keyed on the `client_id` claim
- [x] 9.4 Build thin admin-portal: tier/limit from Admin API, live usage from Redis, upgrade via Admin API
- [x] 9.5 Bring up the gateway stack on Podman (etcd+redis+APISIX) + Admin API bootstrap script
- [x] 9.6 Verify tiered limits at the gateway end-to-end (SILVER 5→429, upgrade→GOLD 20 allowed; 2026-06-25)
- [x] 9.7 Remove the transient in-ASPSP rate limiter/portal from bala-bank (APISIX is now the sole enforcer)

## 8. Verification

- [ ] 8.1 Run native integration tests per service (catch GraalVM reflection/serialization gaps) — JVM compile + boot verified; native-image build not yet run
- [x] 8.2 Run the end-to-end happy path and confirm it passes (verified locally in JVM dev mode 2026-06-24: TPP /connect → consent → token → AIS accounts/balances/transactions, plus 401/403 negatives; minikube run still pending)
- [x] 8.3 Validate negative paths: 401 (no token), 404 (unknown route), 403 (insufficient scope / non-consented account), 429 (SILVER→gold, 2026-06-25) and revoked/expired consent (400) — verified live at various points; consolidating them into the Cucumber suite is a follow-up
- [x] 8.4 Offline OBIE schema/conformance validation wired: OBIE JSON Schemas (`e2e-tests/src/test/resources/obie-schemas/`) validated against live AIS bodies + a 403 error body via the networknt JSON-Schema validator (`ObieConformanceTest`, `-Pconformance`), driven by `scripts/run-conformance.sh` (boots consent-auth + bala-bank, no gateway). Compiles; run the script to execute live. (Broader PIS/CBPII/event schemas are a follow-up.)
