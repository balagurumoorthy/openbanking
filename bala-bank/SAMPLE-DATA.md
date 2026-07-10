# Bala Bank — Sample Dataset

Seed data for local testing, loaded by Flyway at startup:
`V1__schema.sql` + `V2__seed.sql` (core AIS), `V3__aisp_extended.sql` (extended AISP),
`V4__pis_cbpii_events.sql` (PISP/CBPII/Events). H2 in-memory by default.

## Customers & login (consent-auth)

| Customer | Username / password | Accounts |
|----------|--------------------|----------|
| Alice    | `alice` / `pw`     | `GB-ALICE-001` (current), `GB-ALICE-002` (savings) |
| Bob      | `bob` / `pw`       | `GB-BOB-001` (current) |

## Core AIS (V2)

- **Accounts**: `GB-ALICE-001`, `GB-ALICE-002`, `GB-BOB-001`.
- **Balances**: e.g. `GB-ALICE-001` → 2540.18 GBP (InterimAvailable).
- **Transactions**: coffee / salary / groceries entries per account.

## Extended AISP (V3) — one+ row per account

| Resource | Endpoint | Sample ids |
|----------|----------|-----------|
| Beneficiaries | `GET /aisp/accounts/{id}/beneficiaries`, `/aisp/beneficiaries` | `bene-alice-1/2`, `bene-bob-1` |
| Direct debits | `GET /aisp/accounts/{id}/direct-debits` | `dd-alice-1/2`, `dd-bob-1` |
| Standing orders | `GET /aisp/accounts/{id}/standing-orders` | `so-alice-1/2`, `so-bob-1` |
| Scheduled payments | `GET /aisp/accounts/{id}/scheduled-payments` | `sp-alice-1/2`, `sp-bob-1` |
| Statements | `GET /aisp/accounts/{id}/statements` | `stmt-alice-1/2`, `stmt-bob-1` |
| Party | `GET /aisp/accounts/{id}/party`, `/aisp/party` | `party-alice-1/2`, `party-bob-1` |
| Products | `GET /aisp/products`, `/aisp/accounts/{id}/product` | `prod-current-1`, `prod-saver-1` |
| Offers | `GET /aisp/offers`, `/aisp/accounts/{id}/offers` | `offer-alice-1/2`, `offer-bob-1` |
| Account-access consent | `GET`/`DELETE /aisp/account-access-consents/{id}` | ASPSP-side consent record |

Each read enforces the matching OBIE permission (`ReadBeneficiariesDetail`,
`ReadDirectDebits`, `ReadStandingOrdersDetail`, `ReadScheduledPaymentsDetail`,
`ReadStatementsDetail`, `ReadParty`/`ReadPartyPSU`, `ReadProducts`, `ReadOffers`)
and, where account-scoped, that the account was consented.

## PISP / CBPII / Events (V4)

| Area | Endpoints | Seed id |
|------|-----------|---------|
| Domestic scheduled payments | `POST/GET /pisp/domestic-scheduled-payment-consents[/{id}]`, `.../domestic-scheduled-payments[/{id}]` | consent `spcon-seed-0001` (debtor `GB-ALICE-001`) |
| Domestic standing orders | `POST/GET /pisp/domestic-standing-order-consents[/{id}]`, `.../domestic-standing-orders[/{id}]` | consent `socon-seed-0001` |
| International payments | `POST/GET /pisp/international-payment-consents[/{id}]`, `.../international-payments[/{id}]` | consent `ipcon-seed-0001` |
| Payment details / funds check | `GET /pisp/domestic-payments/{id}/payment-details`, `.../domestic-payment-consents/{id}/funds-confirmation` | derived from existing payment state |
| CBPII | `POST/GET/DELETE /cbpii/funds-confirmation-consents[/{id}]`, `POST /cbpii/funds-confirmations` | consent `fccon-seed-0001` |
| Event Notification | `POST/GET/PUT/DELETE /event-subscriptions[/{id}]`, `.../callback-urls[/{id}]`, `POST /events` | subscription `evtsub-seed-0001` |

PISP endpoints require the `payments` scope; CBPII requires `fundsconfirmations`
(see the note in `FundsConfirmationResource` — consent-auth issues `payments` for a
blank-permission consent today; a `fundsconfirmations`-scoped token is a follow-up).

## Ready-made scenarios

1. **AIS read** — `alice`/`pw`, consent `GB-ALICE-001` with all Read permissions →
   accounts, balances, transactions, beneficiaries, standing-orders, statements, etc.
2. **Scoped denial** — grant only `ReadAccountsDetail` → `…/balances` returns 403.
3. **Account outside consent** — consent `GB-ALICE-001` only, request `GB-ALICE-002` → 403.
4. **Payment** — MohanaTPP `/pay` → authorise (`payments`) → execute → status
   `AcceptedSettlementInProcess`.
5. **Revocation** — `DELETE /account-access-consents/{id}` then re-exchange the code → 400 `consent_revoked`.
