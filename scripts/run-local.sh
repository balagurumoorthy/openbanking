#!/usr/bin/env bash
# Run the platform locally in JVM mode (no minikube, no Docker).
# Starts consent-auth, bala-bank and mohana-tpp as background Quarkus dev processes.
# For local testing MohanaTPP calls bala-bank directly (the ASPSP still validates the
# JWT/scope/consent); the APISIX layer is exercised via scripts/bring-up.sh on minikube.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p logs

PRIV="$ROOT/certs/jwt-signing-pkcs8.key"
PUB="$ROOT/certs/jwt-signing.pub"

echo "==> [1/4] Generate PKI (if missing)"
[ -f "$PUB" ] || ./scripts/gen-certs.sh

start() {
  local module="$1"; shift
  echo "==> starting ${module} ..."
  ( cd "$module" && env "$@" mvn -q quarkus:dev ) > "logs/${module}.log" 2>&1 &
  echo "$!" > "logs/${module}.pid"
}

echo "==> [2/4] consent-auth on :8081"
start consent-auth OB_JWT_PRIVATE_KEY="$PRIV" OB_JWT_PUBLIC_KEY="$PUB" OB_ISSUER_BASE="http://localhost:8081"

echo "==> [3/4] bala-bank on :8082"
start bala-bank OB_JWT_PUBLIC_KEY="$PUB"

echo "==> [4/4] mohana-tpp on :8080 (gateway -> bala-bank direct)"
start mohana-tpp AUTH_BASE="http://localhost:8081" AUTH_PUBLIC_BASE="http://localhost:8081" \
                 GATEWAY_BASE="http://localhost:8082" TPP_REDIRECT_URI="http://localhost:8080/callback"

echo
echo "Waiting for services to come up (tail logs/*.log to watch)..."
for url in http://localhost:8081/jwks http://localhost:8082/q/health/ready http://localhost:8080/; do
  for i in $(seq 1 60); do
    if curl -sf "$url" >/dev/null 2>&1; then echo "  ready: $url"; break; fi
    sleep 2
  done
done

cat <<EOF

All services up:
  MohanaTPP        http://localhost:8080
  Bala Bank login  http://localhost:8081/authorize?...   (entered via MohanaTPP)
  Bala Bank API    http://localhost:8082

Open http://localhost:8080 and click "Connect your Bala Bank account".
Stop everything with: ./scripts/stop-local.sh
EOF
