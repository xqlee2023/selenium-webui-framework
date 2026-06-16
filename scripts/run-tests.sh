#!/bin/bash
# =====================================================================
# 🚀 Selenium WebUI Test Runner Script
# =====================================================================
# Usage:
#   ./scripts/run-tests.sh                    # Default (dev, chrome)
#   ./scripts/run-tests.sh qa firefox         # QA env, Firefox
#   ./scripts/run-tests.sh prod chrome 8      # Prod, Chrome, 8 threads
#   ./scripts/run-tests.sh dev chrome 2 smoke # Dev, Chrome, 2 threads, smoke tests
# =====================================================================

set -euo pipefail

ENV="${1:-dev}"
BROWSER="${2:-chrome}"
PARALLEL="${3:-2}"
GROUP="${4:-}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "╔═══════════════════════════════════════╗"
echo "║  🚀 Selenium WebUI Test Runner       ║"
echo "╠═══════════════════════════════════════╣"
echo "║  Environment:  ${ENV}"
echo "║  Browser:      ${BROWSER}"
echo "║  Parallel:     ${PARALLEL}"
echo "║  Group:        ${GROUP:-all tests}"
echo "╚═══════════════════════════════════════╝"

cd "$PROJECT_DIR"

# Build Maven command
MAVEN_CMD="mvn clean test -P${ENV} -Dbrowser=${BROWSER} -Dparallel.count=${PARALLEL}"

if [ -n "$GROUP" ]; then
    MAVEN_CMD="${MAVEN_CMD} -Dgroups=${GROUP}"
fi

# Set environment variables
export TEST_ENV="${ENV}"
export BROWSER="${BROWSER}"

echo "📋 Running: ${MAVEN_CMD}"
echo ""

# Execute
eval "${MAVEN_CMD}"
EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Tests failed (exit code: ${EXIT_CODE})"
fi

exit ${EXIT_CODE}
