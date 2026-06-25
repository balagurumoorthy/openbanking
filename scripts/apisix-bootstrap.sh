#!/usr/bin/env bash
# Configures the running APISIX (Admin API) for the Open Banking platform:
#   - upstream  -> host-run bala-bank
#   - consumer-groups silver/gold/diamond, each with a redis-backed limit-count (the COUNT + LIMIT)
#   - consumer  mohana_tpp via jwt-auth (RS256), assigned to the silver group
#   - routes    for the AIS/PIS surface, protected by jwt-auth
# Tier upgrades are then just: move the consumer to another group via the Admin API.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADMIN="${APISIX_ADMIN:-http://localhost:9180}"
KEY="${APISIX_ADMIN_KEY:-ob-admin-key-0001}"
HOST_ALIAS="${HOST_ALIAS:-host.containers.internal}"   # podman host alias
PUBKEY_FILE="${PUBKEY_FILE:-$ROOT/certs/jwt-signing.pub}"
PRIVKEY_FILE="${PRIVKEY_FILE:-$ROOT/certs/jwt-signing.key}"   # PKCS1; jwt-auth RS256 schema requires it

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

echo "==> Done. APISIX gateway ready on http://localhost:9080"
