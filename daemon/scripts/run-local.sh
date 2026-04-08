#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DAEMON_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${DAEMON_DIR}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven(mvn) is not installed or not in PATH."
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java is not installed or not in PATH."
  exit 1
fi

echo "[INFO] Running tests before local execution..."
mvn test

echo "[INFO] Packaging daemon jar..."
mvn package -DskipTests

JAR_PATH="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1)"

if [[ -z "${JAR_PATH}" ]]; then
  echo "[ERROR] No runnable jar found in target/."
  exit 1
fi

echo "[INFO] Starting daemon locally: ${JAR_PATH}"
exec java -jar "${JAR_PATH}"
