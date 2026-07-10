#!/usr/bin/env bash
# Configures the running APISIX (Admin API) for the Open Banking platform:
#   - upstream  -> host-run bala-bank
#   - consumer-groups silver/gold/diamond, each with a redis-backed limit-count (the COUNT + LIMIT)
#   - consumer  mohana_tpp via jwt-auth (RS256), assigned to the silver group
#   - routes    for the AIS/PIS surface, protected by jwt-auth
#   - (optional, task 4.4) ssl object on 9443 enforcing mTLS with the TPP's OBWAC cert
# Tier upgrades are then just: move the consumer to another group via the Admin API.
set -euo pipefail
export MSYS_NO_PATHCONV=1   # stop Git Bash from rewriting the /apisix/admin/... paths below
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADMIN="${APISIX_ADMIN:-http://localhost:9180}"
KEY="${APISIX_ADMIN_KEY:-ob-admin-key-0001}"
HOST_ALIAS="${HOST_ALIAS:-host.containers.internal}"   # podman host alias
PUBKEY_FILE="${PUBKEY_FILE:-$ROOT/certs/jwt-signing.pub}"
PRIVKEY_FILE="${PRIVKEY_FILE:-$ROOT/certs/jwt-signing.key}"   # PKCS1; jwt-auth RS256 schema requires it

# mTLS toggle (task 4.4): off by default so a first bring-up — before any TPP has
# been issued/trusts an OBWAC cert — doesn't require one. When "true", this script
# also pushes an SSL object (server cert = aspsp-transport, trusted client CA =
# ob-ca-bundle.crt) to APISIX, which turns on client-cert enforcement on the 9443
# listener declared in gateway/apisix/podman/config.yaml.
MTLS_ENABLED="${MTLS_ENABLED:-false}"
ASPSP_CERT_FILE="${ASPSP_CERT_FILE:-$ROOT/certs/aspsp-transport-chain.crt}"   # leaf + issuing CA
ASPSP_KEY_FILE="${ASPSP_KEY_FILE:-$ROOT/certs/aspsp-transport.key}"
CLIENT_CA_FILE="${CLIENT_CA_FILE:-$ROOT/certs/ob-ca-bundle.crt}"              # issuing + root CA

put() { curl -s -X PUT "$ADMIN$1" -H "X-API-KEY: $KEY" -H 'Content-Type: application/json' -d "$2" -o /dev/null -w "PUT $1 -> %{http_code}\n"; }

# JSON-escape the PEM public key for the jwt-auth consumer.
PUBKEY_JSON=$(python -c "import json,sys; print(json.dumps(open(sys.argv[1]).read()))" "$PUBKEY_FILE")
PRIVKEY_JSON=$(python -c "import json,sys; print(json.dumps(open(sys.argv[1]).read()))" "$PRIVKEY_FILE")

echo "==> upstream: bala-bank"
put /apisix/admin/upstreams/bala-bank "{\"type\":\"roundrobin\",\"nodes\":{\"${HOST_ALIAS}:8082\":1}}"

echo "==> consumer-groups (tiers) with redis-backed limit-count"
for grp in "silver:5" "gold:20" "diamond:100"; do
  name="${grp%%:*}"; count="${grp##*:}"
  put "/apisix/admin/consumer_groups/${name}" "{
    \"plugins\": { \"limit-count\": {
      \"count\": ${count}, \"time_window\": 60, \"rejected_code\": 429,
      \"policy\": \"redis\", \"redis_host\": \"redis\", \"redis_port\": 6379,
      \"key_type\": \"var\", \"key\": \"consumer_name\",
      \"show_limit_quota_header\": true
    } }
  }"
done

echo "==> consumer: mohana_tpp (jwt-auth RS256), tier=silver"
put /apisix/admin/consumers "{
  \"username\": \"mohana_tpp\",
  \"group_id\": \"silver\",
  \"plugins\": { \"jwt-auth\": {
    \"key\": \"mohana-tpp\", \"algorithm\": \"RS256\",
    \"public_key\": ${PUBKEY_JSON}, \"private_key\": ${PRIVKEY_JSON}
  } }
}"

echo "==> routes: AIS + PIS, protected by jwt-auth"
put /apisix/admin/routes/aisp "{
  \"uri\": \"/open-banking/v3.1/aisp/*\",
  \"upstream_id\": \"bala-bank\",
  \"plugins\": { \"jwt-auth\": {} }
}"
put /apisix/admin/routes/pisp "{
  \"uri\": \"/open-banking/v3.1/pisp/*\",
  \"upstream_id\": \"bala-bank\",
  \"plugins\": { \"jwt-auth\": {} }
}"

# --- mTLS (task 4.4): only when toggled on ------------------------------------
# Push an SSL object: server cert = aspsp-transport (full chain), trusted client
# CA = ob-ca-bundle.crt, and client.depth so APISIX enforces that the TPP presents
# an OBWAC transport cert issued by the OB CA on the 9443 listener. When
# MTLS_ENABLED is not "true" no SSL object is created, so 9443 has no cert and the
# plain-HTTP 9080 happy path is unaffected.
if [ "$MTLS_ENABLED" = "true" ]; then
  echo "==> mTLS: pushing SSL object (server=aspsp-transport, client CA=ob-ca-bundle) -> :9443"
  CERT_JSON=$(python -c "import json,sys; print(json.dumps(open(sys.argv[1]).read()))" "$ASPSP_CERT_FILE")
  KEY_JSON=$(python -c "import json,sys; print(json.dumps(open(sys.argv[1]).read()))" "$ASPSP_KEY_FILE")
  CA_JSON=$(python -c "import json,sys; print(json.dumps(open(sys.argv[1]).read()))" "$CLIENT_CA_FILE")
  put /apisix/admin/ssls/ob-mtls "{
    \"snis\": [\"localhost\", \"host.docker.internal\", \"aspsp.balabank.local\"],
    \"cert\": ${CERT_JSON},
    \"key\": ${KEY_JSON},
    \"client\": { \"ca\": ${CA_JSON}, \"depth\": 2 }
  }"
  echo "    TPP must now present its OBWAC cert on :9443, e.g."
  echo "    curl --cacert certs/ob-ca-bundle.crt --cert certs/tpp-obwac-chain.crt --key certs/tpp-obwac.key \\"
  echo "         https://localhost:9443/open-banking/v3.1/aisp/accounts -H \"Authorization: Bearer \$TOKEN\""
else
  echo "==> mTLS: disabled (MTLS_ENABLED!=true); :9443 has no cert, use plain http://localhost:9080"
fi

echo "==> Done. APISIX gateway ready on http://localhost:9080"
