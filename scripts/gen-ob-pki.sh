#!/usr/bin/env bash
# Emulates the UK Open Banking (OBIE) PKI for local testing.
#
# Hierarchy (mirrors the OB Directory model):
#   OB Root CA
#     └── OB Issuing CA  (intermediate)
#           ├── ASPSP transport cert      (server TLS for the APISIX gateway)
#           ├── TPP OBWAC transport cert   (client cert for mTLS — QWAC equivalent)
#           └── TPP OBSEAL signing cert    (message/JWS signing — QSEAL equivalent)
#
# Each leaf subject carries an organizationIdentifier (2.5.4.97) like the real OB
# PSD2 certs (e.g. PSDGB-OB-Unknown0015800001...). Full ETSI QCStatements/role OIDs
# are simplified for a local emulation.
set -euo pipefail
export MSYS_NO_PATHCONV=1   # stop Git Bash from rewriting openssl -subj/DNs into paths

OUT="${1:-certs}"
DAYS=825
PASS="${KEYSTORE_PASSWORD:-changeit}"
ASPSP_ORGID="PSDGB-OB-Unknown0015800001ABALA"   # Bala Bank (ASPSP)
TPP_ORGID="PSDGB-OB-Unknown0015800002BMOHANA"   # MohanaTPP (TPP)

rm -rf "$OUT"; mkdir -p "$OUT"; cd "$OUT"

# ----- helper: write an openssl extension file -----
ext_ca() {
cat > "$1.ext" <<EOF
basicConstraints = critical, CA:TRUE${2:+, pathlen:0}
keyUsage = critical, keyCertSign, cRLSign
subjectKeyIdentifier = hash
EOF
}

# =====================================================================
echo "==> OB Root CA"
openssl genrsa -out ob-root-ca.key 4096
ext_ca ob-root-ca
openssl req -new -key ob-root-ca.key \
  -subj "/C=GB/O=OpenBanking/OU=OB Directory/CN=OB Root CA" -out ob-root-ca.csr
openssl x509 -req -in ob-root-ca.csr -signkey ob-root-ca.key -sha256 -days 3650 \
  -extfile ob-root-ca.ext -out ob-root-ca.crt

echo "==> OB Issuing CA (intermediate)"
openssl genrsa -out ob-issuing-ca.key 4096
ext_ca ob-issuing-ca pathlen0
openssl req -new -key ob-issuing-ca.key \
  -subj "/C=GB/O=OpenBanking/OU=OB Directory/CN=OB Issuing CA" -out ob-issuing-ca.csr
openssl x509 -req -in ob-issuing-ca.csr -CA ob-root-ca.crt -CAkey ob-root-ca.key \
  -CAcreateserial -days 1825 -sha256 -extfile ob-issuing-ca.ext -out ob-issuing-ca.crt

# CA chain bundle (issuing + root) — truststore for mTLS verification
cat ob-issuing-ca.crt ob-root-ca.crt > ob-ca-bundle.crt

# ----- helper: issue a leaf cert from the issuing CA -----
# args: name  subject  eku  san(optional)
issue_leaf() {
  local name="$1" subj="$2" eku="$3" san="${4:-}"
  echo "==> Leaf: ${name}"
  openssl genrsa -out "${name}.key" 2048
  openssl req -new -key "${name}.key" -subj "$subj" -out "${name}.csr"
  {
    echo "basicConstraints = critical, CA:FALSE"
    echo "keyUsage = critical, digitalSignature${eku:+, keyEncipherment}"
    [ -n "$eku" ] && echo "extendedKeyUsage = ${eku}"
    echo "subjectKeyIdentifier = hash"
    echo "authorityKeyIdentifier = keyid,issuer"
    [ -n "$san" ] && echo "subjectAltName = ${san}"
  } > "${name}.ext"
  openssl x509 -req -in "${name}.csr" -CA ob-issuing-ca.crt -CAkey ob-issuing-ca.key \
    -CAcreateserial -days "$DAYS" -sha256 -extfile "${name}.ext" -out "${name}.crt"
  # full chain for serving / presenting
  cat "${name}.crt" ob-issuing-ca.crt > "${name}-chain.crt"
}

# ASPSP transport (server) cert — used by the APISIX gateway TLS listener
issue_leaf aspsp-transport \
  "/C=GB/O=Bala Bank/OU=ASPSP/organizationIdentifier=${ASPSP_ORGID}/CN=apisix-gateway.balabank.local" \
  "serverAuth" \
  "DNS:apisix-gateway.balabank.local,DNS:apisix-gateway,DNS:localhost,DNS:host.docker.internal,IP:127.0.0.1"

# TPP OBWAC transport (client) cert — presented by MohanaTPP for mTLS
issue_leaf tpp-obwac \
  "/C=GB/O=MohanaTPP Ltd/OU=TPP/organizationIdentifier=${TPP_ORGID}/CN=mohana-tpp.balabank.local" \
  "clientAuth, serverAuth" \
  "DNS:mohana-tpp.balabank.local,DNS:mohana-tpp"

# TPP OBSEAL signing cert — for JWS message signatures (x-jws-signature)
issue_leaf tpp-obseal \
  "/C=GB/O=MohanaTPP Ltd/OU=TPP/organizationIdentifier=${TPP_ORGID}/CN=${TPP_ORGID}" \
  "" ""

# ----- JVM keystores/truststores (PKCS12) -----
echo "==> Building PKCS12 keystores (password: ${PASS})"
# TPP client keystore (transport key+chain) for mTLS from MohanaTPP
openssl pkcs12 -export -in tpp-obwac-chain.crt -inkey tpp-obwac.key -certfile ob-ca-bundle.crt \
  -name "tpp-obwac" -out tpp-obwac.p12 -passout "pass:${PASS}"
# ASPSP/gateway server keystore
openssl pkcs12 -export -in aspsp-transport-chain.crt -inkey aspsp-transport.key -certfile ob-ca-bundle.crt \
  -name "aspsp-transport" -out aspsp-transport.p12 -passout "pass:${PASS}"
# Truststore (CA bundle) as PKCS12 with no key — import the CA bundle
keytool -importcert -noprompt -alias ob-root -file ob-root-ca.crt \
  -keystore ob-truststore.p12 -storetype PKCS12 -storepass "${PASS}" 2>/dev/null || true
keytool -importcert -noprompt -alias ob-issuing -file ob-issuing-ca.crt \
  -keystore ob-truststore.p12 -storetype PKCS12 -storepass "${PASS}" 2>/dev/null || true

# ----- consent-auth JWT signing keypair (token signing) -----
echo "==> JWT signing keypair for consent-auth"
openssl genrsa -out jwt-signing.key 2048
openssl pkcs8 -topk8 -nocrypt -in jwt-signing.key -out jwt-signing-pkcs8.key
openssl rsa -in jwt-signing.key -pubout -out jwt-signing.pub

echo
echo "==> Verifying chains"
openssl verify -CAfile ob-root-ca.crt ob-issuing-ca.crt
openssl verify -CAfile ob-ca-bundle.crt aspsp-transport.crt
openssl verify -CAfile ob-ca-bundle.crt tpp-obwac.crt
openssl verify -CAfile ob-ca-bundle.crt tpp-obseal.crt

echo
echo "==> Done. Key artifacts in ./${OUT}:"
echo "    ob-root-ca.crt / ob-issuing-ca.crt / ob-ca-bundle.crt   (CA hierarchy + truststore)"
echo "    aspsp-transport.*   gateway server TLS (mTLS)"
echo "    tpp-obwac.*         TPP client transport cert (mTLS)    keystore: tpp-obwac.p12"
echo "    tpp-obseal.*        TPP signing cert (JWS)"
echo "    jwt-signing*        consent-auth token signing"
echo "    keystore/truststore password: ${PASS}"
