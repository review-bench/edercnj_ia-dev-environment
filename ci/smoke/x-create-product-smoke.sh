#!/usr/bin/env bash
# Smoke test for x-create-product CLI pipeline.
# Usage: ./ci/smoke/x-create-product-smoke.sh [--idempotency]
# Exit 0 = pass; exit 1 = fail.
set -euo pipefail

FIXTURE="src/test/resources/fixtures/ideation/analytics-platform.md"
OUTPUT_DIR=$(mktemp -d)
PRODUCT_ID="product-smoke-0001"
JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | head -1)

cleanup() { rm -rf "$OUTPUT_DIR"; }
trap cleanup EXIT

fail() { echo "SMOKE FAIL: $1" >&2; exit 1; }

[[ -f "$FIXTURE" ]] || fail "Fixture not found: $FIXTURE"
[[ -f "$JAR" ]] || fail "JAR not found — run mvn package first"

# First run — should create artifacts
java -jar "$JAR" x-create-product \
    --ideation-file "$FIXTURE" \
    --output-dir "$OUTPUT_DIR" \
    --product-id "$PRODUCT_ID" 2>/dev/null || true

PRODUCT_FILE="$OUTPUT_DIR/${PRODUCT_ID}-product.json"
CAP_FILE="$OUTPUT_DIR/${PRODUCT_ID}-capability-c1-stub.json"

[[ -f "$PRODUCT_FILE" ]] || fail "Product artifact not written: $PRODUCT_FILE"
[[ -f "$CAP_FILE" ]] || fail "Capability stub not written: $CAP_FILE"

grep -q '"productId"' "$PRODUCT_FILE" || fail "Product artifact missing productId field"
grep -q '"idempotencyHash"' "$PRODUCT_FILE" || fail "Product artifact missing idempotencyHash field"

# Idempotency check — second run must not modify existing artifacts
if [[ "${1:-}" == "--idempotency" ]]; then
    MTIME1=$(stat -f "%m" "$PRODUCT_FILE" 2>/dev/null || stat -c "%Y" "$PRODUCT_FILE")
    sleep 1
    java -jar "$JAR" x-create-product \
        --ideation-file "$FIXTURE" \
        --output-dir "$OUTPUT_DIR" \
        --product-id "$PRODUCT_ID" 2>/dev/null || true
    MTIME2=$(stat -f "%m" "$PRODUCT_FILE" 2>/dev/null || stat -c "%Y" "$PRODUCT_FILE")
    [[ "$MTIME1" == "$MTIME2" ]] || fail "Idempotency failed: file was re-written on second run"
    echo "SMOKE PASS (idempotency verified)"
else
    echo "SMOKE PASS"
fi
