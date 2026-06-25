#!/usr/bin/env bash
# Generates a local self-signed CA plus service certs/keystores for the Open Banking platform.
# Outputs to ./certs. Safe to re-run (idempotent-ish: regenerates everything).
set -euo pipefail

# Git Bash on Windows rewrites /CN=.. subjects into paths; disable that conversion.
export MSYS_NO_PATHCONV=1

OUT="${1:-certs}"
DAYS=825
PASS="${KEYSTORE_PASSWORD:-changeit}"

mkdir -p "$OUT"
cd "$OUT"

echo "==> Generating root CA"
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days "$DAYS" \
  -subj "/C=GB/O=BalaBank/CN=BalaBank Local Root CA" -out ca.crt

gen_cert() {
  local name="$1" cn="$2"
  echo "==> Generating cert for ${name} (CN=${cn})"
  openssl genrsa -out "${name}.key" 2048
  openssl req -new -key "${name}.key" -subj "/C=GB/O=BalaBank/CN=${cn}" -out "${name}.csr"
  openssl x509 -req -in "${name}.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days "$DAYS" -sha256 -out "${name}.crt"
  # PKCS12 keystore for the JVM / mTLS client
  openssl pkcs12 -export -in "${name}.crt" -inkey "${name}.key" -certfile ca.crt \
    -name "${name}" -out "${name}.p12" -passout "pass:${PASS}"
}

# Gateway server cert (APISIX), TPP client cert (mTLS to gateway)
gen_cert apisix      "apisix-gateway.balabank.local"
gen_cert mohana-tpp  "mohana-tpp.balabank.local"

echo "==> Generating JWT signing keypair for consent-auth"
openssl genrsa -out jwt-signing.key 2048
# SmallRye JWT signing requires PKCS#8
openssl pkcs8 -topk8 -nocrypt -in jwt-signing.key -out jwt-signing-pkcs8.key
openssl rsa -in jwt-signing.key -pubout -out jwt-signing.pub

echo "==> Done. Artifacts in ./${OUT}:"
ls -1
echo "Keystore password: ${PASS}"
