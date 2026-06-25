# Bala Bank Open Banking Platform

A runnable UK Open Banking (OBIE) reference platform on **Java 21 + Quarkus + GraalVM native**,
deployable locally on **minikube**.

| Module        | Role                                                            | Port |
|---------------|----------------------------------------------------------------|------|
| `common`      | Shared OBIE DTOs, enums, JWT claim constants                    | —    |
| `bala-bank`   | ASPSP — AIS/PIS Open Banking APIs + sample data                | 8082 |
| `consent-auth`| OAuth2/OIDC authorization server + login & consent UI          | 8081 |
| `mohana-tpp`  | TPP client app (consent flow + API consumption)                | 8080 |
| `gateway`     | APISIX declarative config (JWT, scope, mTLS, rate-limit)       | 9080 |
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

## Sample customers

| Customer | Login        | Accounts                                    |
|----------|--------------|---------------------------------------------|
| alice    | `alice`/`pw` | GB-ALICE-001 (current), GB-ALICE-002 (savings) |
| bob      | `bob`/`pw`   | GB-BOB-001 (current)                        |
