#!/usr/bin/env bash
# Runs APISIX locally in Docker, fronting host-run bala-bank, with:
#   - JWT validation via consent-auth JWKS (openid-connect plugin)
#   - per-route scope enforcement + rate limiting
#   - mTLS on :9443 requiring a TPP OBWAC transport cert signed by the OB CA
#
# Prereqs: Docker; certs generated via scripts/gen-ob-pki.sh; consent-auth + bala-bank
# running on the host. IMPORTANT: start consent-auth with
#   OB_ISSUER_BASE=http://host.docker.internal:8081
# so its discovery doc advertises URLs the APISIX container can reach.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

command -v docker >/dev/null 2>&1 || { echo "ERROR: docker not found. Install Docker Desktop."; exit 1; }
[ -f certs/aspsp-transport-chain.crt ] || { echo "ERROR: run scripts/gen-ob-pki.sh first."; exit 1; }

GEN="gateway/apisix/local/apisix.yaml"
TMPL="gateway/apisix/local/routes.tmpl.yaml"

# Indent a PEM file by 6 spaces to nest under the YAML '|' block scalars.
indent() { sed 's/^/      /' "$1"; }

echo "==> Rendering ${GEN} with inlined PEM bodies"
awk -v cert="$(indent certs/aspsp-transport-chain.crt)" \
    -v key="$(indent certs/aspsp-transport.key)" \
    -v ca="$(indent certs/ob-ca-bundle.crt)" '
  /__ASPSP_CERT__/ { print cert; next }
  /__ASPSP_KEY__/  { print key;  next }
  /__CA_BUNDLE__/  { print ca;   next }
  { print }
' "$TMPL" > "$GEN"

echo "==> Starting APISIX (apache/apisix:3.9.1-debian)"
docker rm -f ob-apisix >/dev/null 2>&1 || true
docker run -d --name ob-apisix \
  --add-host host.docker.internal:host-gateway \
  -p 9080:9080 -p 9443:9443 \
  -v "$ROOT/gateway/apisix/local/config.yaml:/usr/local/apisix/conf/config.yaml:ro" \
  -v "$ROOT/$GEN:/usr/local/apisix/conf/apisix.yaml:ro" \
  apache/apisix:3.9.1-debian

echo "==> Waiting for APISIX..."
for i in $(seq 1 20); do
  if curl -sf -o /dev/null http://localhost:9080/open-banking/v3.1/aisp/accounts; then break; fi
  # 401 (no token) also means it's up:
  code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9080/open-banking/v3.1/aisp/accounts || true)
  [ "$code" = "401" ] && { echo "  up (401 as expected without token)"; break; }
  sleep 2
done

cat <<EOF

APISIX is up:
  Plain  HTTP : http://localhost:9080   (JWT required)
  mTLS   HTTPS: https://localhost:9443   (TPP transport cert + JWT required)

Test through the gateway (plain, JWT only) — get a token first via MANUAL-TEST.md, then:
  curl http://localhost:9080/open-banking/v3.1/aisp/accounts -H "Authorization: Bearer \$TOKEN"

Test mTLS on :9443 (presents the TPP OBWAC client cert):
  curl https://localhost:9443/open-banking/v3.1/aisp/accounts \\
       --cacert certs/ob-ca-bundle.crt \\
       --cert certs/tpp-obwac.crt --key certs/tpp-obwac.key \\
       -H "Authorization: Bearer \$TOKEN"

Logs: docker logs -f ob-apisix      Stop: docker rm -f ob-apisix
EOF
