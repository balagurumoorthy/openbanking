#!/usr/bin/env bash
# Offline OBIE conformance check (task 8.4): boots consent-auth (:8081) + bala-bank (:8082)
# in JVM dev mode, runs the JSON-Schema conformance test, then tears them down. No gateway/Podman.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/OpenJDK/jdk-21.0.2}"
export PATH="$JAVA_HOME/bin:$PATH"
CERTS="$ROOT/certs"
mkdir -p "$ROOT/logs"

[ -f "$CERTS/jwt-signing.pub" ] || { echo "ERROR: certs missing — run scripts/gen-ob-pki.sh first"; exit 1; }

echo "==> starting consent-auth (:8081) + bala-bank (:8082)"
( cd "$ROOT/consent-auth" && OB_JWT_PRIVATE_KEY="$CERTS/jwt-signing-pkcs8.key" OB_JWT_PUBLIC_KEY="$CERTS/jwt-signing.pub" \
    mvn -q quarkus:dev -Dquarkus.http.port=8081 > "$ROOT/logs/conf-consent-auth.log" 2>&1 & )
( cd "$ROOT/bala-bank" && OB_JWT_PUBLIC_KEY="$CERTS/jwt-signing.pub" \
    mvn -q quarkus:dev -Dquarkus.http.port=8082 > "$ROOT/logs/conf-bala-bank.log" 2>&1 & )

ready() { curl -s -o /dev/null -w "%{http_code}" "$1" 2>/dev/null; }
echo "==> waiting for readiness..."
ok=0
for i in $(seq 1 60); do
  if [ "$(ready http://localhost:8081/jwks)" = "200" ] && [ "$(ready http://localhost:8082/q/health/ready)" = "200" ]; then
    echo "    both up after ${i}0s"; ok=1; break
  fi
  sleep 10
done

rc=1
if [ "$ok" = "1" ]; then
  echo "==> running OBIE conformance test"
  ( cd "$ROOT" && mvn -pl e2e-tests -Pconformance test )
  rc=$?
else
  echo "ERROR: services did not become ready; see logs/conf-*.log"
fi

echo "==> stopping services"
for p in $(powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Process -Filter \"name='java.exe' OR name='java.EXE'\" | Where-Object { \$_.CommandLine -like '*consent-auth*' -or \$_.CommandLine -like '*bala-bank*' }).ProcessId" 2>/dev/null | tr -d '\r'); do
  [ -n "$p" ] && taskkill.exe //PID "$p" //F >/dev/null 2>&1
done
exit $rc
