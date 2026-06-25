## ADDED Requirements

### Requirement: Native container images
Each custom service (bala-bank, consent-auth, mohana-tpp) SHALL build a GraalVM native-image container via a multi-stage Dockerfile.

#### Scenario: Native image builds
- **WHEN** the build pipeline runs for a service
- **THEN** it produces a native binary packaged in a minimal base image that starts in sub-second time

#### Scenario: Image loadable into minikube
- **WHEN** images are built
- **THEN** they can be loaded into the minikube cluster (e.g. `minikube image load`) without a remote registry

### Requirement: minikube deployment manifests
The platform SHALL provide Helm/Kustomize manifests deploying all components into minikube.

#### Scenario: Full deploy
- **WHEN** an operator applies the manifests against a running minikube
- **THEN** bala-bank, consent-auth, mohana-tpp, APISIX, and the datastore are deployed and reach a ready state

#### Scenario: Gateway exposed
- **WHEN** the deployment completes
- **THEN** APISIX is reachable from the host (NodePort or Ingress) and routes to the ASPSP

### Requirement: Documented bring-up
The platform SHALL include documented, repeatable steps to start minikube, build/load images, deploy, and reach the apps.

#### Scenario: Operator follows the runbook
- **WHEN** an operator follows the documented steps from a clean machine with minikube and Docker
- **THEN** the full platform comes up and MohanaTPP and the consent app are reachable from the browser

### Requirement: End-to-end BDD test suite
The platform SHALL provide an automated end-to-end test suite written as BDD (Cucumber-JVM / Gherkin) feature files exercising the full consent → data-access → payment scenario against the deployed platform.

#### Scenario: Gherkin feature describes the happy path
- **WHEN** the E2E suite is inspected
- **THEN** a Gherkin `.feature` file describes the flow in Given/When/Then steps (TPP initiates consent, user logs in, user selects permissions/accounts, token issued, AIS data read, payment initiated)

#### Scenario: Happy path passes
- **WHEN** the Cucumber suite runs against the deployed platform (local port-forward or in-cluster Job)
- **THEN** every step passes — login, permission/account selection, token issuance, an AIS data read, and a payment initiation — asserting expected results at each step

#### Scenario: Negative scenarios covered
- **WHEN** the suite runs
- **THEN** it includes Gherkin scenarios for denied consent (`access_denied`), insufficient scope (403), invalid/missing token (401), and rate limiting (429)

#### Scenario: Failure surfaces clearly
- **WHEN** any step of a scenario fails
- **THEN** Cucumber reports the failing step and the relevant HTTP status/response in the test output and generated report
