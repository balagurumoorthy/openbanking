#!/usr/bin/env bash
# End-to-end local bring-up: certs -> minikube -> build -> load -> deploy.
set -euo pipefail
TAG="${IMAGE_TAG:-1.0.0-SNAPSHOT}"
NS="${NAMESPACE:-openbanking}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> [1/6] Generate PKI"
[ -f certs/jwt-signing.pub ] || ./scripts/gen-certs.sh

echo "==> [2/6] Start minikube"
minikube status >/dev/null 2>&1 || minikube start --memory=6g --cpus=4

echo "==> [3/6] Build native images"
IMAGE_TAG="$TAG" ./scripts/build-images.sh

echo "==> [4/6] Load images into minikube"
for svc in bala-bank consent-auth mohana-tpp; do
  minikube image load "${svc}:${TAG}"
done

echo "==> [5/6] Create namespace + PKI secret"
kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"
kubectl -n "$NS" delete secret ob-pki >/dev/null 2>&1 || true
kubectl -n "$NS" create secret generic ob-pki \
  --from-file=jwt-signing.pub=certs/jwt-signing.pub \
  --from-file=jwt-signing-pkcs8.key=certs/jwt-signing-pkcs8.key

echo "==> [6/6] Deploy with Helm"
helm upgrade --install openbanking ./deploy/helm -n "$NS" --set imageTag="$TAG"
kubectl -n "$NS" rollout status deploy --timeout=180s

echo "==> Up. Endpoints:"
echo "    MohanaTPP:    http://localhost:30080  (minikube service mohana-tpp -n $NS)"
echo "    Consent/Login http://localhost:30081"
echo "    APISIX:       http://localhost:30090"
