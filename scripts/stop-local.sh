#!/usr/bin/env bash
# Stops the local JVM dev processes started by run-local.sh.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

for module in consent-auth bala-bank mohana-tpp; do
  pidfile="logs/${module}.pid"
  if [ -f "$pidfile" ]; then
    pid="$(cat "$pidfile")"
    echo "==> stopping ${module} (pid ${pid})"
    # kill the dev process and its child JVM
    kill "$pid" 2>/dev/null || true
    pkill -P "$pid" 2>/dev/null || true
    rm -f "$pidfile"
  fi
done
# Fallback: kill any lingering quarkus:dev for these modules
pkill -f "quarkus:dev" 2>/dev/null || true
echo "==> stopped."
