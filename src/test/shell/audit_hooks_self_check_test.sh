#!/usr/bin/env bash
# audit_hooks_self_check_test.sh — Unit tests for audit-hooks-self-check.sh
# TDD RED→GREEN: tests verify the script's contract per story-0063-0018.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-hooks-self-check.sh"
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

echo "=== audit-hooks-self-check.sh tests ==="

# T1: --self-check exits 0 (structural validation of the audit script itself)
assert_exit "T1 --self-check exits 0" 0 \
    "$AUDIT_SCRIPT" --self-check

# T2: audit script file is executable
if [ -x "$AUDIT_SCRIPT" ]; then
    echo "  PASS: T2 audit script is executable"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T2 audit script is not executable: $AUDIT_SCRIPT" >&2
    FAIL=$((FAIL + 1))
fi

# T3: audit script references --self-check in its source
if grep -q -- '--self-check' "$AUDIT_SCRIPT" 2>/dev/null; then
    echo "  PASS: T3 audit script references --self-check"
    PASS=$((PASS + 1))
else
    echo "  FAIL: T3 audit script does not reference --self-check" >&2
    FAIL=$((FAIL + 1))
fi

# T4: missing settings.json → OPERATIONAL_ERROR → exit 2
# Create a fake settings.json-less environment by pointing SETTINGS_FILE to missing path.
# We achieve this by calling self_check with a modified SETTINGS_FILE via a temp wrapper.
FAKE_SETTINGS="$TMP_DIR/nonexistent-settings.json"
WRAPPER="$TMP_DIR/wrapper_missing_settings.sh"
cat > "$WRAPPER" <<-'WRAPPER_EOF'
#!/usr/bin/env bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
AUDIT_SCRIPT="$REPO_ROOT/.claude/scripts/audit-hooks-self-check.sh"
# Override SETTINGS_FILE by exporting before running is not possible since the
# script resolves it internally. Instead verify that an empty/nonexistent path
# causes jq to fail (exit 2). We test via --self-check with a patched path.
# For this test we verify the script's exit when run in a dir without settings.json.
# We use a subshell with HOME overridden to prevent unintended config discovery.
"$AUDIT_SCRIPT" --self-check
WRAPPER_EOF
chmod +x "$WRAPPER"

# The real T4 test: run the audit script with an artificially absent settings.json.
# We simulate this by creating a temporary directory without .claude/settings.json
# and running the script with REPO_ROOT pointing there.
FAKE_REPO="$TMP_DIR/fake-repo"
mkdir -p "$FAKE_REPO/.claude/hooks"
# No settings.json created — script should exit 2 from self_check
PATCHED_SCRIPT="$TMP_DIR/patched_audit.sh"
cp "$AUDIT_SCRIPT" "$PATCHED_SCRIPT"
chmod +x "$PATCHED_SCRIPT"
# Replace REPO_ROOT resolution with FAKE_REPO for this test
sed "s|REPO_ROOT=\"\$(cd -- \"\$(dirname -- \"\${BASH_SOURCE\[0\]}\")/..\"|REPO_ROOT=\"${FAKE_REPO}\" #|g" \
    "$AUDIT_SCRIPT" > "$PATCHED_SCRIPT" 2>/dev/null || cp "$AUDIT_SCRIPT" "$PATCHED_SCRIPT"
chmod +x "$PATCHED_SCRIPT"

# Run --self-check against the fake repo (no settings.json) — should exit 2
assert_exit "T4 missing settings.json causes self-check exit 2" 2 \
    "$PATCHED_SCRIPT" --self-check

# T5: all hooks registered in the real settings.json report --self-check OK
# This passes only if every registered .sh hook exists, is executable,
# references --self-check, and exits 0 when called with --self-check.
# We run the full audit script (not --self-check mode) and expect exit 0.
assert_exit "T5 all registered hooks report --self-check OK (exit 0)" 0 \
    "$AUDIT_SCRIPT"

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
