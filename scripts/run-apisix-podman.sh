#!/usr/bin/env bash
# Brings up the APISIX gateway stack on Podman (etcd + redis + APISIX, traditional mode)
# and configures it via the Admin API. APISIX is then the sole rate limiter:
#   - tiers      = consumer-groups silver/gold/diamond (limit-count, redis policy)
#   - counting   = APISIX limit-count counters in redis
#   - identity   = jwt-auth consumer keyed on the token's client_id claim
# Upgrades happen by moving the consumer between groups (Admin API) — see the admin-portal.
#
# Requires: podman (machine running), certs from scripts/gen-ob-pki.sh, and the host-run
# consent-auth (:8081) + bala-bank (:8082).
#
# mTLS toggle (task 4.4): set MTLS_ENABLED=true to require the TPP's OBWAC client
# cert on the 9443 listener (in addition to plain 9080). Defaults to off/false so a
# first bring-up (before certs are provisioned or a client trusts them) just works.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="$PATH:/c/Program Files/RedHat/Podman"
NET=ob
MTLS_ENABLED="${MTLS_ENABLED:-false}"

command -v podman >/dev/null 2>&1 || { echo "ERROR: podman not found."; exit 1; }

# Host IP reachable from the podman machine (Windows host = the machine's default gateway).
HOST_IP="${HOST_IP:-$(podman machine ssh "ip route | awk '/default/ {print \$3; exit}'" 2>/dev/null | tr -d '\r')}"
echo "==> host IP (for upstream): ${HOST_IP}"

podman network exists "$NET" 2>/dev/null || podman network create "$NET"

echo "==> etcd"
podman rm -f ob-etcd >/dev/null 2>&1 || true
podman run -d --name ob-etcd --network "$NET" --network-alias etcd \
  quay.io/coreos/etcd:v3.5.12 \
  etcd --name etcd0 --advertise-client-urls http://etcd:2379 --listen-client-urls http://0.0.0.0:2379 >/dev/null

echo "==> redis (published on host 6380 to avoid a local Redis/Memurai on 6379)"
podman rm -f ob-redis >/dev/null 2>&1 || true
podman run -d --name ob-redis --network "$NET" --network-alias redis -p 6380:6379 \
  docker.io/library/redis:7-alpine >/dev/null

echo "==> APISIX (traditional mode, Admin API enabled)"
podman rm -f ob-apisix >/dev/null 2>&1 || true
# 9443 (mTLS listener) is always published — it's inert/unreachable-by-TLS unless
# apisix-bootstrap.sh has pushed an SSL object (which only happens when
# MTLS_ENABLED=true). The certs/ dir is always mounted read-only so the bootstrap
# script's Admin API calls (curl reading PEMs off disk) and any future
# static-cert approach both have the PKI available inside the container.
podman run -d --name ob-apisix --network "$NET" -p 9080:9080 -p 9180:9180 -p 9443:9443 \
  -v "$ROOT/gateway/apisix/podman/config.yaml:/usr/local/apisix/conf/config.yaml:ro" \
  -v "$ROOT/certs:/usr/local/apisix/conf/ob-certs:ro" \
  docker.io/apache/apisix:3.9.1-debian >/dev/null

echo "==> waiting for Admin API..."
for i in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:9180/apisix/admin/routes -H 'X-API-KEY: ob-admin-key-0001' || true)
  [ "$code" = "200" ] && { echo "  admin up"; break; }
  sleep 2
done

echo "==> bootstrapping routes / consumer-groups / consumer (MTLS_ENABLED=${MTLS_ENABLED})"
HOST_ALIAS="$HOST_IP" MTLS_ENABLED="$MTLS_ENABLED" bash "$ROOT/scripts/apisix-bootstrap.sh"

cat <<EOF

APISIX gateway ready:
  Gateway   : http://localhost:9080   (jwt-auth + tiered limit-count)
  Admin API : http://localhost:9180   (key: ob-admin-key-0001)
  Redis     : localhost:6380          (limit-count counters)
EOF
if [ "$MTLS_ENABLED" = "true" ]; then
cat <<EOF
  mTLS      : https://localhost:9443  (requires TPP OBWAC client cert; see certs/tpp-obwac.*)
EOF
else
cat <<EOF
  mTLS      : disabled (set MTLS_ENABLED=true to require client certs on :9443)
EOF
fi
cat <<EOF

Start the admin portal:  (cd admin-portal && mvn quarkus:dev)  -> http://localhost:8090
Stop the stack:          podman rm -f ob-apisix ob-etcd ob-redis
EOF
