# Bala Bank Open Banking Platform

A runnable UK Open Banking (OBIE) reference platform on **Java 21 + Quarkus + GraalVM native**,
deployable locally on **minikube**.

| Module        | Role                                                            | Port |
|---------------|----------------------------------------------------------------|------|
| `common`      | Shared OBIE DTOs, enums, JWT claim constants                    | —    |
| `bala-bank`   | ASPSP — AIS/PIS Open Banking APIs + sample data                | 8082 |
| `consent-auth`| OAuth2/OIDC authorization server + login & consent UI          | 8081 |
| `mohana-tpp`  | TPP client app (consent flow + accounts/balances dashboard)    | 8080 |
| `admin-portal`| TPP plan/usage portal over the APISIX Admin API + Redis        | 8090 |
| `gateway`     | APISIX (JWT, scope, mTLS, tiered rate-limit) — etcd + Redis    | 9080 |
| `e2e-tests`   | Cucumber (BDD) end-to-end suite                                 | —    |

## Quick start (minikube)

```bash
mvn -N wrapper:wrapper          # one-time: generate ./mvnw used by the Docker builds
./scripts/bring-up.sh           # certs -> minikube -> build -> load -> deploy
```

Then open:
- MohanaTPP — http://localhost:30080
- Bala Bank login & consent — http://localhost:30081
- APISIX gateway — http://localhost:30090

See [WORKFLOW.md](openspec/changes/implement-openbanking-platform/WORKFLOW.md) for the full
step-by-step flow and sample data.

## Local dev (JVM mode, no native build)

```bash
./scripts/gen-certs.sh
(cd consent-auth && OB_JWT_PRIVATE_KEY=../certs/jwt-signing-pkcs8.key OB_JWT_PUBLIC_KEY=../certs/jwt-signing.pub mvn quarkus:dev)
(cd bala-bank   && OB_JWT_PUBLIC_KEY=../certs/jwt-signing.pub mvn quarkus:dev)
(cd mohana-tpp  && mvn quarkus:dev)
```

## End-to-end BDD tests

```bash
mvn -pl e2e-tests -Pe2e test \
  -Dauth.base=http://localhost:30081 \
  -Dgateway.base=http://localhost:30090
```

Report: `e2e-tests/target/cucumber-report.html`.

## APISIX gateway + tiered plans (local, Podman)

Rate-limit tiers (**silver 5 / gold 20 / diamond 100** req/min) are enforced **in APISIX**,
not the ASPSP: each TPP is a `jwt-auth` consumer (keyed on the token's `client_id`), assigned
to a `consumer-group` whose `limit-count` (Redis) holds the allowance. Upgrading = moving the
consumer to another group via the Admin API.

```bash
./scripts/gen-ob-pki.sh                # OBIE PKI (root/issuing CA, OBWAC/OBSEAL, JWT keys)
./scripts/run-apisix-podman.sh         # etcd + Redis + APISIX, then Admin API bootstrap
(cd admin-portal && mvn quarkus:dev)   # plan/usage portal at http://localhost:8090
```

### Admin portal access

Open **http://localhost:8090** in a browser — **no login is required**. It is an unauthenticated
local-dev tool that manages a single TPP consumer (the APISIX consumer set by
`PORTAL_CONSUMER`, default `mohana_tpp`). It shows that consumer's live usage (from APISIX's
Redis counters) and Silver/Gold/Diamond upgrade buttons.

Prerequisites for the page to load data:
- The gateway stack is up (`./scripts/run-apisix-podman.sh`) so the **Admin API** (`:9180`) and
  **Redis** (`:6380`) are reachable, and the bootstrap has created the `mohana_tpp` consumer.
- consent-auth + bala-bank are running, and you have driven at least one request through the
  gateway (so a usage counter exists).

> Not authenticated by design — it talks to the APISIX Admin API using the admin key
> (`APISIX_ADMIN_KEY`, default `ob-admin-key-0001`). Do **not** expose it outside localhost.
> Adding a login (e.g. basic auth or an OIDC gate) is a follow-up if this ever leaves local dev.

### How plan switching works (request routed via APISIX)

The tier is **server-side state on the APISIX consumer**, not something baked into the route or
the TPP's token. Three pieces hold it:

- **Consumer** `mohana_tpp` — has a `jwt-auth` credential and a `group_id` pointing at a tier.
- **Consumer-groups = tiers** — `silver`/`gold`/`diamond`, each carrying the `limit-count`
  plugin that *is* the allowance (`count` 5/20/100, `time_window` 60s, `policy` redis,
  `key` = `consumer_name`). The limit lives on the **group**, not the route or consumer.
- **Routes** — the AIS/PIS routes enable only `jwt-auth`; they define **no** rate limit.

**Per request:**
```
TPP → APISIX route (/open-banking/v3.1/aisp/*)
  ├─ jwt-auth: validate signature, read the `key` claim ("mohana-tpp") → resolve consumer mohana_tpp
  ├─ consumer.group_id = silver → APISIX merges that group's plugins onto THIS request (live, per-request)
  ├─ limit-count: key = consumer_name, counter in Redis, count=5 / 60s window
  │     within allowance → 200 (X-RateLimit-Remaining decrements) ; over → 429
  └─ proxy to bala-bank
```
The `group_id` is resolved **at request time**, so nothing is baked into the route.

**Upgrade = one Admin API write** that changes only `group_id` on the consumer (done by the admin
portal):
```java
body.put("group_id", "gold");                 // silver → gold
apisix.put("/apisix/admin/consumers", body);  // persisted to etcd
```
APISIX watches etcd and refreshes its in-memory consumer config within ~milliseconds — **no route
change, no restart, no redeploy**. The very next request for `mohana_tpp` resolves `group_id=gold`
and picks up the gold group's `limit-count` (20/min). The TPP's bearer **token is unchanged**; the
same token now gets the new allowance.

**Counter subtlety:** each group's `limit-count` is a distinct Redis counter keyed on
`consumer_name`. On silver→gold the gold counter starts fresh against 20; the old silver key lingers
until its 60s TTL expires (harmless). That's why the portal's `currentRemaining()` ignores stale
keys where `remaining > limit` — right after an upgrade both a silver(≤5) and gold(≤20) key can
briefly exist. Net effect: if silver was exhausted (429), an upgrade to gold restores headroom on
the next request.

## End-to-end flow (happy path)

```
MohanaTPP /connect ─▶ consent-auth /authorize (login + pick permissions/accounts)
   └─▶ approve ─▶ redirect with code ─▶ /token (signed JWT: consent id + scopes + client_id)
        └─▶ MohanaTPP /dashboard ─▶ APISIX (jwt-auth, scope, limit-count) ─▶ bala-bank AIS
             └─▶ renders each consented account + its balance
```

## OBIE API conformance

The AIS surface emits **true OBIE JSON**: PascalCase fields, the nested
`{"Data":{"Account"|"Balance"|"Transaction":[...]},"Links":{},"Meta":{}}` envelope, the nested
`Account[]` identifier block, string `Amount`, and the `Code/Id/Message/Errors[].ErrorCode`
error model. See [MANUAL-TEST.md](MANUAL-TEST.md) for sample bodies.

## Sample customers

| Customer | Login        | Accounts                                    |
|----------|--------------|---------------------------------------------|
| alice    | `alice`/`pw` | GB-ALICE-001 (current), GB-ALICE-002 (savings) |
| bob      | `bob`/`pw`   | GB-BOB-001 (current)                        |

## Status — completed so far

- ✅ **Consent & login** (consent-auth): OAuth2 `/authorize` + `/token` (signed JWT, JWKS),
  custom consent UI to narrow permissions and pick accounts, sample customers seeded.
- ✅ **ASPSP** (bala-bank): AIS (accounts, balances, transactions) with per-request consent +
  scope enforcement and OBIE error bodies; PIS domestic-payment consent + execution; JWKS validation.
- ✅ **TPP** (mohana-tpp): consent initiation, callback/token exchange, and an accounts +
  **balances dashboard** consumed via the gateway.
- ✅ **APISIX gateway** (Podman): routes/upstreams, jwt-auth against the JWKS, per-route scope,
  and **tiered limit-count** (silver/gold/diamond → 429 over allowance), verified end-to-end.
- ✅ **Tiered plans & admin portal**: APISIX-native enforcement + thin portal for live usage and
  tier upgrade via the Admin API.
- ✅ **OBIE JSON conformance** for the AIS surface (PascalCase, nested `Data`, string `Amount`).
- ✅ **OBIE PKI**: scripted root/issuing CA, OBWAC (transport) + OBSEAL (signing) certs, JWT keys.
- ✅ **Local deployment**: minikube manifests + bring-up script; Cucumber BDD E2E (happy path + 401/deny).

**Not yet done:** consent revocation/expiry lifecycle; gateway mTLS termination wired into the
Podman setup (task 4.4); the broader OBIE surface (beneficiaries, standing-orders, direct-debits,
CBPII funds-confirmation, event notification, `x-fapi-*` headers, pagination — task 6); PIS payment
UI in the TPP; native-image build + in-cluster minikube run verification. See
[tasks.md](openspec/changes/implement-openbanking-platform/tasks.md) for the full checklist.
