#!/bin/bash
# =====================================================================
# 📊 Generate and Open Allure Report
# =====================================================================
# Usage:
#   ./scripts/generate-allure-report.sh           # Generate HTML report
#   ./scripts/generate-allure-report.sh --serve    # Open in browser
#   ./scripts/generate-allure-report.sh --clean    # Clean and regenerate
# =====================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

MODE="${1:-report}"

case "$MODE" in
    --serve|-s)
        echo "📊 Generating and serving Allure report..."
        mvn allure:serve
        ;;
    --clean|-c)
        echo "🧹 Cleaning and regenerating Allure report..."
        rm -rf allure-results allure-report
        mvn allure:report
        echo "✅ Report generated at: target/site/allure-maven-plugin/index.html"
        ;;
    --help|-h)
        echo "Usage: ./scripts/generate-allure-report.sh [--serve|--clean]"
        exit 0
        ;;
    *)
        echo "📊 Generating Allure report..."
        mvn allure:report
        echo "✅ Report generated at: target/site/allure-maven-plugin/index.html"
        echo "   Open in browser: open target/site/allure-maven-plugin/index.html"
        ;;
esac
