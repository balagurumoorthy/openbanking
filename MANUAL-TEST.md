# Manual Test Guide (local, JVM mode)

Prerequisites: **JDK 21** and **Maven** on your PATH. (`java -version`, `mvn -version`)

## 1. Start the services

**Windows (PowerShell):**
```powershell
.\scripts\run-local.ps1
```
Opens 3 windows (consent-auth :8081, bala-bank :8082, mohana-tpp :8080). Wait until each
prints `Listening on: http://0.0.0.0:80xx`.

**Git Bash / Linux / macOS:**
```bash
./scripts/run-local.sh      # runs in background; logs in ./logs
```

First start is slow (Maven downloads Quarkus). Health checks:
```bash
curl http://localhost:8081/jwks                 # consent-auth keys
curl http://localhost:8082/q/health/ready        # bala-bank ready
curl http://localhost:8080/                      # MohanaTPP home
```

---

## 2. Test via the browser (full flow)

1. Open **http://localhost:8080** → click **"Connect your Bala Bank account"**.
2. You're redirected to Bala Bank's consent screen (served by consent-auth :8081).
3. Sign in: **username `alice`, password `pw`**.
4. Leave the permissions checked, keep **GB-ALICE-001** ticked, click **Approve**.
5. You're redirected back to MohanaTPP, which exchanges the code and shows the
   **account dashboard** — each consented account (only `GB-ALICE-001`) with its
   **balance** fetched live via the gateway. Raw JSON is still at
   `/accounts?s=<session>` and `/accounts/GB-ALICE-001/balances?s=<session>`.

Try the negative path: repeat and click **Deny** → MohanaTPP shows "Consent denied (access_denied)".

---

## 3. Test via curl (step by step)

```bash
AUTH=http://localhost:8081
API=http://localhost:8082

# 3a. TPP registers an account-access intent -> get a ConsentId
CONSENT=$(curl -s -X POST $AUTH/account-access-consents \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"mohana-tpp","permissions":["ReadAccountsDetail","ReadBalances","ReadTransactionsDetail"]}' \
  | grep -o 'acon-[0-9a-f-]*' | head -1)
echo "ConsentId=$CONSENT"

# 3b. User approves (login + permission/account selection). Capture the redirect 'code'.
CODE=$(curl -s -i -X POST $AUTH/authorize/decision \
  --data-urlencode "client_id=mohana-tpp" \
  --data-urlencode "redirect_uri=http://localhost:8080/callback" \
  --data-urlencode "state=s1" \
  --data-urlencode "consent_id=$CONSENT" \
  --data-urlencode "username=alice" \
  --data-urlencode "password=pw" \
  --data-urlencode "decision=approve" \
  --data-urlencode "permissions=ReadAccountsDetail" \
  --data-urlencode "permissions=ReadBalances" \
  --data-urlencode "permissions=ReadTransactionsDetail" \
  --data-urlencode "accounts=GB-ALICE-001" \
  | grep -i '^location:' | grep -o 'code=[^&]*' | cut -d= -f2)
echo "Code=$CODE"

# 3c. Exchange the code for an access token
TOKEN=$(curl -s -X POST $AUTH/token \
  --data-urlencode "grant_type=authorization_code" \
  --data-urlencode "code=$CODE" \
  --data-urlencode "redirect_uri=http://localhost:8080/callback" \
  --data-urlencode "client_id=mohana-tpp" \
  --data-urlencode "client_secret=mohana-secret" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
echo "Token=${TOKEN:0:30}..."

# 3d. Call the Open Banking AIS endpoints with the token
curl -s $API/open-banking/v3.1/aisp/accounts -H "Authorization: Bearer $TOKEN"
echo
curl -s $API/open-banking/v3.1/aisp/accounts/GB-ALICE-001/balances -H "Authorization: Bearer $TOKEN"
echo
curl -s $API/open-banking/v3.1/aisp/accounts/GB-ALICE-001/transactions -H "Authorization: Bearer $TOKEN"
```

### Expected results (true OBIE envelope — PascalCase, nested `Data.<Resource>`)
- `accounts` → `{"Data":{"Account":[{"AccountId":"GB-ALICE-001","Currency":"GBP","AccountType":"Personal","AccountSubType":"CurrentAccount","Account":[{"SchemeName":"UK.OBIE.SortCodeAccountNumber","Identification":"...","Name":"..."}]}]},"Links":{"Self":...},"Meta":{"TotalPages":1}}` — GB-ALICE-001 only.
- `balances` → `{"Data":{"Balance":[{"AccountId":"GB-ALICE-001","Amount":{"Amount":"2540.18","Currency":"GBP"},"CreditDebitIndicator":"Credit","Type":"InterimAvailable",...}]},...}`.
- `transactions` → `{"Data":{"Transaction":[...]}}` (coffee/salary/groceries). Note `Amount.Amount` is a **string** per the standard.

---

## 4. Negative checks (security working)

```bash
# No token -> 401 Unauthorized
curl -i -s $API/open-banking/v3.1/aisp/accounts | head -1

# Token but request a NON-consented account -> 403 with OBIE error
curl -s $API/open-banking/v3.1/aisp/accounts/GB-ALICE-002/balances \
  -H "Authorization: Bearer $TOKEN"

# Permission not granted (e.g. consent only for ReadAccountsDetail) -> 403 on transactions
```

| Check                                   | Expected |
|-----------------------------------------|----------|
| Missing/invalid token                   | 401      |
| Account outside consent                 | 403 (OBIE `ConsentMismatch`) |
| Permission not granted                  | 403      |
| Payment with un-authorised consent      | 403 / 400 |

---

## 4b. APISIX gateway: tiered rate limits + admin portal (silver / gold / diamond)

Tiering is enforced **in APISIX**, not the ASPSP. APISIX identifies each TPP as a `jwt-auth`
consumer (keyed on the token's `client_id`), assigns it to a consumer-group
(silver=5 / gold=20 / diamond=100 req/min), and counts with `limit-count` (Redis). Upgrading =
moving the consumer to another group via the Admin API — done by the thin **admin portal**.

### Bring up the gateway (Podman)
```bash
./scripts/gen-ob-pki.sh                # OBIE certs/keys (if not already generated)
./scripts/run-apisix-podman.sh         # etcd + redis + APISIX, then Admin API bootstrap
(cd admin-portal && mvn quarkus:dev)   # admin portal on http://localhost:8090
```
Requires Podman (the machine running) plus the host-run consent-auth + bala-bank.

### Admin portal UI
Open **http://localhost:8090** — live usage bar (polls every 2s; tier+limit from the APISIX
Admin API, usage from APISIX's Redis counters) and Silver/Gold/Diamond upgrade buttons.

### Prove it on the command line (get `$TOKEN` from section 3)
```bash
GW=http://localhost:9080; PORTAL=http://localhost:8090
# SILVER allows 5/min; the 6th returns 429 (enforced by APISIX limit-count)
for n in $(seq 1 7); do
  curl -s -o /dev/null -w "req $n -> %{http_code}\n" \
    $GW/open-banking/v3.1/aisp/accounts -H "Authorization: Bearer $TOKEN"
done
curl -s $PORTAL/api/state                       # {tier:SILVER, used:5, limit:5, ...}

# Upgrade to GOLD (Admin API consumer group move) -> allowance rises to 20/min
curl -s -X POST $PORTAL/upgrade --data-urlencode tier=gold
curl -s $PORTAL/api/state                        # {tier:GOLD, limit:20, ...}
```
Gateway responses carry `X-RateLimit-Limit/Remaining/Reset`. The config that drives this is in
[gateway/apisix/podman/config.yaml](gateway/apisix/podman/config.yaml) and applied by
[scripts/apisix-bootstrap.sh](scripts/apisix-bootstrap.sh).

## 5. Stop

- **PowerShell:** close the 3 service windows.
- **Bash:** `./scripts/stop-local.sh`

> Note: This local mode talks to bala-bank directly. To exercise the **APISIX gateway**
> (rate-limiting 429, mTLS, gateway-side JWT validation), use the minikube path:
> `./scripts/bring-up.sh` (see [WORKFLOW.md](openspec/changes/implement-openbanking-platform/WORKFLOW.md)).
