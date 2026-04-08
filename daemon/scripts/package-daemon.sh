#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DAEMON_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${DAEMON_DIR}/dist"

cd "${DAEMON_DIR}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven(mvn) is not installed or not in PATH."
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] Java is not installed or not in PATH."
  exit 1
fi

echo "[INFO] Cleaning previous build outputs..."
rm -rf target
mkdir -p "${DIST_DIR}"

echo "[INFO] Running tests..."
mvn test

echo "[INFO] Packaging daemon jar..."
mvn package -DskipTests

JAR_PATH="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1)"

if [[ -z "${JAR_PATH}" ]]; then
  echo "[ERROR] No packaged jar found in target/."
  exit 1
fi

ARTIFACT_NAME="daemon-app.jar"
cp "${JAR_PATH}" "${DIST_DIR}/${ARTIFACT_NAME}"

cat > "${DIST_DIR}/run-daemon.sh" <<'RUNEOF'
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec java -jar "${SCRIPT_DIR}/daemon-app.jar"
RUNEOF
chmod +x "${DIST_DIR}/run-daemon.sh"

echo "[INFO] Package completed."
echo "[INFO] Output jar: ${DIST_DIR}/${ARTIFACT_NAME}"
echo "[INFO] Run script : ${DIST_DIR}/run-daemon.sh"
