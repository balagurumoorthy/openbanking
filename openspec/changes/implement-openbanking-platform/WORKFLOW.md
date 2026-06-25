# Open Banking Platform — End-to-End Workflow

This document walks through the full lifecycle: building and running the platform on minikube,
registering the MohanaTPP client, and testing the complete Open Banking flow (consent →
data access → payment) using BDD (Cucumber).

## Components

| Component       | Role                                                        | Stack                          |
|-----------------|-------------------------------------------------------------|--------------------------------|
| **bala-bank**   | ASPSP — exposes OBIE AIS/PIS/CBPII/event APIs               | Quarkus + GraalVM native       |
| **consent-auth**| OAuth2/OIDC authorization server + customer login & consent | Quarkus + GraalVM native       |
| **mohana-tpp**  | TPP client app (initiates consent, consumes APIs)           | Quarkus + GraalVM native       |
| **APISIX**      | API gateway fronting bala-bank (JWT, scopes, mTLS, rate-limit) | APISIX (declarative YAML)    |
| **datastore**   | Sample data (customers, accounts, transactions, …)          | H2 (default) / PostgreSQL      |

## Prerequisites

- JDK 21 + GraalVM (native-image), Maven
- Docker
- minikube + kubectl
- Helm (or Kustomize)
- OpenSSL (for the local self-signed CA)

---

## Step 1 — Generate local PKI

```bash
./scripts/gen-certs.sh        # creates self-signed CA + service certs/keystores
```

Produces the CA, TPP client cert (for mTLS to APISIX), and JWT signing keys for consent-auth.
For the very first bring-up you may disable mTLS (`MTLS_ENABLED=false`).

## Step 2 — Build native images

```bash
./mvnw -pl bala-bank,consent-auth,mohana-tpp -Pnative package
./scripts/build-images.sh     # multi-stage Docker build → native images
```

> Tip: develop in JVM mode (`./mvnw quarkus:dev`) and only build native for minikube/CI.

## Step 3 — Start minikube and load images

```bash
minikube start --memory=6g --cpus=4
./scripts/load-images.sh      # minikube image load for each service
```

## Step 4 — Deploy the platform

```bash
helm install openbanking ./deploy/helm -n openbanking --create-namespace
kubectl rollout status -n openbanking deploy --timeout=180s
```

This deploys datastore, consent-auth, bala-bank, APISIX, and mohana-tpp. Flyway seeds the
comprehensive sample dataset on startup.

## Step 5 — Get endpoints

```bash
minikube service list -n openbanking
# or port-forward:
kubectl -n openbanking port-forward svc/mohana-tpp 8080:8080 &
kubectl -n openbanking port-forward svc/consent-auth 8081:8080 &
kubectl -n openbanking port-forward svc/apisix-gateway 9080:9080 &
```

| URL                        | What                                   |
|----------------------------|----------------------------------------|
| http://localhost:8080      | MohanaTPP app                          |
| http://localhost:8081      | Bala Bank login & consent screen       |
| http://localhost:9080      | APISIX gateway (Open Banking APIs)     |

## Step 6 — Register the TPP (MohanaTPP)

MohanaTPP is pre-registered with consent-auth via seed config. To inspect/register:

```bash
# View seeded client registration
kubectl -n openbanking get configmap mohana-tpp-client -o yaml
```

Registration record contains: `client_id`, `client_secret`, `redirect_uri`
(`http://localhost:8080/callback`), and allowed scopes. To register a different TPP, add an
entry to `deploy/seed/clients.yaml` and redeploy.

## Step 7 — Run the consent flow (manual, in browser)

1. Open MohanaTPP at http://localhost:8080 and click **"Connect bank account"**.
2. MohanaTPP creates an account-access intent and redirects you to Bala Bank's `/authorize`.
3. Log in with a seeded customer (see [Sample Data](#sample-data)).
4. On the consent screen, review the requested permissions, **deselect any you don't want**,
   and **pick which accounts** to share. Click **Approve**.
5. Bala Bank redirects back to MohanaTPP with an authorization `code`; MohanaTPP exchanges it
   for a JWT access token.
6. MohanaTPP calls the AIS APIs through APISIX and displays accounts, balances, transactions.
7. Initiate a payment in MohanaTPP → authorize it → see the returned payment status.

## Step 8 — Test the flow with BDD (Cucumber)

The end-to-end flow is codified as Gherkin feature files and runs with Cucumber-JVM.

```bash
# Run against the deployed platform (uses port-forwarded URLs or in-cluster service DNS)
./mvnw -pl e2e-tests test -Pe2e \
  -Dtpp.base=http://localhost:8080 \
  -Dauth.base=http://localhost:8081 \
  -Dgateway.base=http://localhost:9080
```

Or run in-cluster as a Kubernetes Job:

```bash
kubectl -n openbanking apply -f deploy/e2e-job.yaml
kubectl -n openbanking logs job/e2e-cucumber -f
```

### Example feature

```gherkin
Feature: Account information consent and access

  Scenario: User grants scoped consent and TPP reads account data
    Given MohanaTPP is a registered TPP client
    And customer "alice" has accounts at Bala Bank
    When MohanaTPP initiates an account-access consent for "ReadAccountsDetail, ReadBalances, ReadTransactionsDetail"
    And customer "alice" logs in and approves only "ReadAccountsDetail, ReadBalances" for account "GB-ALICE-001"
    Then MohanaTPP receives an access token bound to that consent
    And a request to the AIS accounts endpoint via APISIX returns account "GB-ALICE-001"
    And a request for transactions returns 403 because the scope was not granted

  Scenario: Initiate a domestic payment
    Given MohanaTPP has an authorized domestic-payment consent for customer "alice"
    When MohanaTPP submits the domestic payment via APISIX
    Then the payment status is "AcceptedSettlementInProcess"
```

Negative scenarios covered: denied consent (`access_denied`), missing/invalid token (401),
insufficient scope (403), and rate limiting (429).

## Step 9 — Tear down

```bash
helm uninstall openbanking -n openbanking
minikube stop   # or: minikube delete
```

---

## Sample Data

Seeded on startup (see `deploy/seed/` and Flyway migrations). Documented for deterministic tests.

| Customer | Login         | Accounts                          | Notes                                  |
|----------|---------------|-----------------------------------|----------------------------------------|
| alice    | `alice` / `pw`| GB-ALICE-001 (current), GB-ALICE-002 (savings) | Balances + transactions, beneficiaries, standing orders |
| bob      | `bob` / `pw`  | GB-BOB-001 (current)              | Funds available for payment flows      |

Each account carries balances, transactions, direct debits, standing orders, scheduled
payments, statements, and party records so every OBIE AIS/PIS/CBPII endpoint is exercisable.

## Flow at a glance

```
MohanaTPP ──(1) create intent──▶ consent-auth
   │  ◀─(2) redirect to /authorize──┘
   ▼
Browser ──(3) login + select permissions/accounts──▶ consent-auth ──(4) code──▶ MohanaTPP
                                                                                   │
MohanaTPP ──(5) code→token──▶ consent-auth (JWT + JWKS)                            │
   │                                                                               │
   └──(6) API calls w/ JWT ──▶ APISIX ──(JWT/scope/mTLS/rate-limit)──▶ bala-bank ──┘
```
