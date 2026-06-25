#!/usr/bin/env bash
# Builds GraalVM native container images for each service using its Dockerfile.native.
set -euo pipefail
TAG="${IMAGE_TAG:-1.0.0-SNAPSHOT}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

for svc in bala-bank consent-auth mohana-tpp; do
  echo "==> Building native image: ${svc}:${TAG}"
  docker build -f "${svc}/src/main/docker/Dockerfile.native" -t "${svc}:${TAG}" .
done
echo "==> Done."
