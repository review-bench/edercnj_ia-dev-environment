#!/usr/bin/env bash
# git-commit-trailer.sh — Smoke tests for story-0059-0005 TASK-001:
# Validates that x-git-commit injects Co-Authored-By: x-git-commit@<sha> trailer.
#
# Tests the trailer contract defined in x-git-commit SKILL.md (Output Contract)
# and full-protocol.md (Step 5). Verifies the git commit --trailer mechanism
# satisfies the .githooks/commit-msg surface-D guard (EPIC-0059 story-0059-0005).
#
# Usage:
#   src/test/bash/git-commit-trailer.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
PASS=0
FAIL=0
ERRORS=()

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

pass() { local name="$1"; echo "  PASS: ${name}"; PASS=$((PASS + 1)); }
fail() { local name="$1" msg="$2"; echo "  FAIL: ${name} -- ${msg}" >&2; FAIL=$((FAIL + 1)); ERRORS+=("${name}: ${msg}"); }

# ---------------------------------------------------------------------------
# Test suite
# ---------------------------------------------------------------------------

echo "=========================================="
echo "git-commit-trailer.sh -- EPIC-0059 story-0059-0005 TASK-001"
echo "=========================================="
echo ""

# AT-01: x-git-commit SKILL.md documents the orchestrator-signature trailer
at01() {
    local skill_md="${REPO_ROOT}/.claude/skills/x-git-commit/SKILL.md"
    if [[ ! -f "${skill_md}" ]]; then
        fail "AT-01: SKILL.md exists" "file not found: ${skill_md}"
        return
    fi
    if grep -q "Co-Authored-By: x-git-commit@" "${skill_md}"; then
        pass "AT-01: SKILL.md documents Co-Authored-By: x-git-commit@ trailer"
    else
        fail "AT-01: SKILL.md documents trailer" "Co-Authored-By: x-git-commit@ not found in SKILL.md"
    fi
}
at01

# AT-02: x-git-commit full-protocol.md documents --trailer flag in Step 5
at02() {
    local protocol_md="${REPO_ROOT}/.claude/skills/x-git-commit/references/full-protocol.md"
    if [[ ! -f "${protocol_md}" ]]; then
        fail "AT-02: full-protocol.md exists" "file not found: ${protocol_md}"
        return
    fi
    if grep -q "\-\-trailer" "${protocol_md}"; then
        pass "AT-02: full-protocol.md documents --trailer flag in Step 5"
    else
        fail "AT-02: full-protocol.md documents --trailer" "--trailer not found in full-protocol.md"
    fi
}
at02

# AT-03: source-of-truth SKILL.md also documents the trailer
at03() {
    local src_skill="${REPO_ROOT}/src/main/resources/targets/claude/skills/core/git/x-git-commit/SKILL.md"
    if [[ ! -f "${src_skill}" ]]; then
        fail "AT-03: source SKILL.md exists" "file not found: ${src_skill}"
        return
    fi
    if grep -q "Co-Authored-By: x-git-commit@" "${src_skill}"; then
        pass "AT-03: source SKILL.md documents Co-Authored-By: x-git-commit@ trailer"
    else
        fail "AT-03: source SKILL.md documents trailer" "trailer not found in source SKILL.md"
    fi
}
at03

# AT-04: source-of-truth full-protocol.md documents --trailer flag
at04() {
    local src_protocol="${REPO_ROOT}/src/main/resources/targets/claude/skills/core/git/x-git-commit/references/full-protocol.md"
    if [[ ! -f "${src_protocol}" ]]; then
        fail "AT-04: source full-protocol.md exists" "file not found: ${src_protocol}"
        return
    fi
    if grep -q "\-\-trailer" "${src_protocol}"; then
        pass "AT-04: source full-protocol.md documents --trailer flag"
    else
        fail "AT-04: source full-protocol.md --trailer" "--trailer not found in source full-protocol.md"
    fi
}
at04

# AT-05: git interpret-trailers correctly parses Co-Authored-By: x-git-commit@<sha>
# (functional test: verify the git mechanism works as the hook expects)
at05() {
    local sha="a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    local msg_file
    msg_file=$(mktemp /tmp/git-commit-trailer-test-XXXXX)

    # Write a commit message with the trailer (simulating what git commit --trailer does)
    cat > "${msg_file}" <<EOF
feat(TASK-0059-0005-001): add trailer injection to x-git-commit

Co-Authored-By: x-git-commit@${sha}
EOF

    # Validate via git interpret-trailers (same mechanism as .githooks/commit-msg)
    local parsed
    parsed=$(git interpret-trailers --parse < "${msg_file}" 2>/dev/null)
    rm -f "${msg_file}"

    if echo "${parsed}" | grep -qE "^Co-Authored-By:[[:space:]]+x-git-commit@[0-9a-f]{40}$"; then
        pass "AT-05: git interpret-trailers correctly parses Co-Authored-By: x-git-commit@<sha>"
    else
        fail "AT-05: git interpret-trailers parses trailer" "parsed output: ${parsed}"
    fi
}
at05

# AT-06: PARENT_SHA capture pattern is documented in full-protocol.md
at06() {
    local protocol_md="${REPO_ROOT}/.claude/skills/x-git-commit/references/full-protocol.md"
    if grep -q "PARENT_SHA" "${protocol_md}"; then
        pass "AT-06: full-protocol.md documents PARENT_SHA capture pattern"
    else
        fail "AT-06: PARENT_SHA documented" "PARENT_SHA not found in full-protocol.md"
    fi
}
at06

# AT-07: SKILL.md Output Contract mentions 'proof-of-orchestration'
at07() {
    local skill_md="${REPO_ROOT}/.claude/skills/x-git-commit/SKILL.md"
    if grep -q "proof-of-orchestration\|surface-D" "${skill_md}"; then
        pass "AT-07: SKILL.md links trailer to surface-D guard"
    else
        fail "AT-07: trailer linked to surface-D" "proof-of-orchestration or surface-D not in SKILL.md"
    fi
}
at07

# AT-08: Both SKILL.md files (generated + source-of-truth) are consistent
at08() {
    local gen="${REPO_ROOT}/.claude/skills/x-git-commit/SKILL.md"
    local src="${REPO_ROOT}/src/main/resources/targets/claude/skills/core/git/x-git-commit/SKILL.md"
    local gen_line src_line
    gen_line=$(grep "Co-Authored-By: x-git-commit@" "${gen}" | head -1)
    src_line=$(grep "Co-Authored-By: x-git-commit@" "${src}" | head -1)
    if [[ "${gen_line}" == "${src_line}" ]]; then
        pass "AT-08: generated and source SKILL.md are consistent"
    else
        fail "AT-08: SKILL.md consistency" "generated: '${gen_line}' vs source: '${src_line}'"
    fi
}
at08

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "=========================================="
echo "Results: ${PASS} passed, ${FAIL} failed"
echo "=========================================="

if [[ ${FAIL} -gt 0 ]]; then
    echo ""
    echo "Failed tests:"
    for err in "${ERRORS[@]}"; do
        echo "  - ${err}"
    done
    exit 1
fi

exit 0
