## Context

This is a greenfield reference implementation of the UK Open Banking ecosystem, runnable locally on minikube. It models three independent parties plus a gateway:

- **Bala Bank (ASPSP)** — owns customer accounts and exposes Open Banking Account Information (AISP) and Payment Initiation (PISP) APIs.
- **Consent & Login app** — Bala Bank's customer-facing OAuth2/OIDC authorization server + consent UI. Users authenticate and choose exactly which permissions/accounts a TPP may access.
- **MohanaTPP** — a Third Party Provider client that drives the consent flow and consumes Bala Bank's APIs.
- **APISIX** — API gateway in front of the ASPSP APIs, enforcing routing, JWT/token validation, mTLS, and rate limiting.

All services target **Java 21 + Quarkus + GraalVM native image** for fast startup and small container footprint, and ship as container images deployed to minikube via Helm/Kustomize. The scope is a faithful-but-simplified flow suitable for local demonstration and testing — not a production-certified FAPI deployment.

## Goals / Non-Goals

**Goals:**
- Demonstrate the full Open Banking authorization code flow: TPP → consent → user permission selection → token issuance → scoped API access → payment initiation.
- Run the entire platform locally on minikube with a single documented bring-up and an end-to-end smoke test.
- Use a single coherent stack (Java + Quarkus + GraalVM native) across all custom services.
- Enforce security at the gateway (APISIX): JWT validation, scope checks, mTLS, rate limits.
- Keep capabilities cleanly separated so each service is independently buildable/testable.

**Non-Goals:**
- Full UK Open Banking / FAPI certification or directory (OBIE) integration.
- Production-grade key management, HSMs, or real bank settlement.
- Complete coverage of every Open Banking endpoint — only the core AIS (accounts, balances, transactions) and PIS (domestic payment) subset.
- Multi-tenant / horizontal-scale concerns beyond what minikube needs.

## Decisions

### Stack: Quarkus over Micronaut/Spring Boot
Quarkus has first-class GraalVM native-image support, fast dev loop, and built-in OIDC + REST + Kubernetes/Helm tooling. Spring Boot native is heavier and slower to build native images; Micronaut is viable but Quarkus's OIDC and Dev Services ecosystem fits the auth-heavy flow better.

### Authorization server: dedicated Consent & Login app (Keycloak-backed or Quarkus + SmallRye JWT)
The consent app is its own service to keep the "user choosing permissions" concern isolated. Decision: build a **custom Quarkus OAuth2/OIDC authorization endpoint + consent UI** that persists a `Consent` record (requested vs. granted scopes/accounts) and mints JWT access tokens via SmallRye JWT. Alternative considered: embed Keycloak — rejected for the demo because the per-account consent selection UX is custom to Open Banking and awkward to model purely in Keycloak; however Keycloak remains a fallback for raw user authentication if needed.

### Gateway: APISIX with openid-connect / jwt-auth + limit-req plugins
APISIX fronts the ASPSP. It validates the bearer JWT (jwt-auth/openid-connect plugin against the consent app's JWKS), enforces required scopes per route, terminates mTLS from the TPP, and applies `limit-req`. Routes/upstreams are declarative (APISIX standalone YAML or admin API), version-controlled, and applied at deploy time. Alternative considered: Kong — APISIX chosen for lighter footprint and native YAML/declarative config friendly to minikube.

### Consent model
A `Consent` is created when MohanaTPP initiates (intent) and references requested permissions (e.g., `ReadAccountsDetail`, `ReadBalances`, `ReadTransactionsDetail`) and/or a payment intent. The user, during login, may narrow the granted set and pick which accounts are shared. The issued access token's claims encode the consent id + granted scopes; the ASPSP authorizes each resource request against that consent.

### Data
Bala Bank seeds sample customers, accounts, balances, and transactions in H2 (in-memory, default) with an option to switch to Postgres. Keeps the demo self-contained while allowing a realistic persistence path.

### Deployment
Each service has a multi-stage Dockerfile producing a GraalVM native binary in a distroless/ubi-micro image. Quarkus Helm/Kustomize manifests deploy bala-bank, consent-auth, mohana-tpp, and APISIX. minikube uses NodePort/Ingress; an end-to-end smoke job runs the full scenario.

### End-to-end testing: Cucumber (BDD) over plain integration tests
The end-to-end flow is specified and executed as **Cucumber-JVM** (Gherkin `.feature` files) driving the deployed platform. Each spec scenario (login → permission/account selection → token → AIS read → payment) maps to a Gherkin scenario, keeping tests readable and traceable to requirements. Step definitions use **REST Assured** for HTTP/JSON assertions and a lightweight headless browser/HTTP client for the redirect-based consent UI. Alternative considered: Karate — rejected to keep a single JVM/Java toolchain consistent with the services; plain JUnit integration tests are retained per-service for native-image validation, with Cucumber reserved for cross-service E2E. The Cucumber suite runs both locally (against `minikube`/port-forward) and as a Kubernetes Job in-cluster.

### Tech stack summary
- **Language/runtime**: Java 21, GraalVM native-image.
- **Framework**: Quarkus (RESTEasy Reactive, SmallRye OIDC/JWT, Hibernate ORM with Panache, Qute templates for the consent UI).
- **Datastore**: H2 (in-memory, default) / PostgreSQL (optional), Flyway for schema + seed data.
- **API gateway**: APISIX (standalone/declarative YAML) with jwt-auth/openid-connect, limit-req, and mTLS.
- **Build/packaging**: Maven, Quarkus container-image + Jib/Docker multi-stage, distroless/ubi-micro base.
- **Orchestration**: minikube, Helm/Kustomize, kubectl.
- **Testing**: JUnit 5 + Quarkus Test (unit/native per service), REST Assured (API assertions), **Cucumber-JVM + Gherkin** (cross-service E2E/BDD), WireMock for external stubs where needed.
- **Security/PKI**: OpenSSL/`step` scripted self-signed CA, JKS/PKCS12 keystores, OAuth2 authorization-code + JWT/JWKS.
- **CI/tooling**: Make/shell bring-up scripts, GitHub Actions (native build + Cucumber E2E) optional.

## Risks / Trade-offs

- **GraalVM native-image reflection/serialization gaps** → Use Quarkus extensions (which register reflection automatically) and add `reflection-config`/`@RegisterForReflection` where needed; validate with native integration tests.
- **mTLS + JWT complexity locally** → Provide a scripted self-signed CA and pre-generated certs/keystores; allow toggling mTLS off via config for first bring-up.
- **APISIX declarative config drift** → Keep gateway config in-repo as the single source of truth, applied idempotently at deploy.
- **Native build times slow the dev loop** → Develop in JVM mode (Quarkus dev), build native only for CI/minikube images.
- **Simplified FAPI flow may diverge from real OBIE behavior** → Explicitly documented as non-goal; keep token/consent structure close enough to be educational.
- **minikube resource pressure** (4 services + gateway + DB) → Document minimum resources; allow H2 in-memory and disabling optional components.

## Migration Plan

Greenfield — no migration. Bring-up order: (1) build native images, (2) `minikube start` + load images, (3) deploy datastore + consent-auth, (4) deploy bala-bank behind APISIX, (5) deploy mohana-tpp, (6) run end-to-end smoke. Rollback = `helm uninstall` / delete namespace.

## Open Questions

- Custom Quarkus authorization server vs. Keycloak for the final consent app — start custom, keep Keycloak as documented fallback.
- H2 vs. Postgres as the default for the demo (default H2 for simplicity).
- Whether to expose APISIX via Ingress or NodePort on minikube (default NodePort, Ingress optional).
