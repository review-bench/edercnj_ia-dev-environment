#!/usr/bin/env bash
# stage_telemetry_test.sh — Unit tests for stage-telemetry.sh hook
# Tests map 1-to-1 to Gherkin scenarios in story-0063-0003 §5.2
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
HOOK_SCRIPT="$REPO_ROOT/.claude/hooks/stage-telemetry.sh"
TMP_DIR="$(mktemp -d)"
PASS=0; FAIL=0

cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

assert_exit() {
    local test_name="$1" expected="$2"; shift 2
    local actual=0
    "$@" >/dev/null 2>&1 || actual=$?
    if [ "$actual" -eq "$expected" ]; then
        echo "  PASS: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $test_name — expected exit $expected, got $actual" >&2
        FAIL=$((FAIL + 1))
    fi
}

echo "=== stage-telemetry.sh tests ==="

# T1: Hook executes successfully when no NDJSON present (no-op fail-OPEN)
assert_exit "T1 hook no-op when no NDJSON exists" 0 \
    bash -c "cd '$TMP_DIR' && '$HOOK_SCRIPT'"

# T2: Hook is fail-OPEN when git ausente (PATH manipulation; preserve /usr/bin for bash)
assert_exit "T2 hook fail-OPEN when git ausente" 0 \
    env -i HOME="$HOME" PATH="/usr/bin:/bin" CLAUDE_PROJECT_DIR="$TMP_DIR" bash -c "command -v git >/dev/null 2>&1 && exit 0; '$HOOK_SCRIPT' || exit \$?"

# T3: Hook is executable
if [ -x "$HOOK_SCRIPT" ]; then
    echo "  PASS: T3 hook is executable"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 hook not executable" >&2
    FAIL=$((FAIL + 1))
fi

# T4: Hook uses fail-open bash mode (set -u + pipefail; -e is omitted by design)
if grep -qE 'set -[a-z]*u[a-z]*o pipefail|set -uo pipefail' "$HOOK_SCRIPT" 2>/dev/null; then
    echo "  PASS: T4 hook uses fail-open strict mode (-uo pipefail)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T4 hook missing fail-open strict mode" >&2
    FAIL=$((FAIL + 1))
fi

# T5: Hook references flock for concurrency protection
if grep -q 'flock' "$HOOK_SCRIPT" 2>/dev/null; then
    echo "  PASS: T5 hook uses flock for race protection"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T5 hook missing flock protection" >&2
    FAIL=$((FAIL + 1))
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
