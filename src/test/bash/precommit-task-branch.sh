#!/usr/bin/env bash
# precommit-task-branch.sh — Smoke tests for story-0059-0005 TASK-002:
# Validates the .githooks/commit-msg Guard 2 (surface-D) for feat/task-* branches.
#
# Tests the branch-pattern detection and trailer validation in the commit-msg hook.
# The hook must:
#   - Allow commits on non-task branches (no trailer required)
#   - Block commits on feat/task-* branches without Co-Authored-By: x-git-commit@<sha>
#   - Allow commits on feat/task-* branches WITH the correct trailer
#   - Not be bypassable via CLAUDE_SKIP_AUDIT or similar (only CLAUDE_TASK_BRANCH_HOOK_DISABLED=1)
#
# Usage:
#   src/test/bash/precommit-task-branch.sh
#
# Exit codes:
#   0 — all tests passed
#   1 — one or more tests failed

set -u

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
HOOK="${REPO_ROOT}/.githooks/commit-msg"
PASS=0
FAIL=0
ERRORS=()
TEMP_FILES=()

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

pass() { local name="$1"; echo "  PASS: ${name}"; PASS=$((PASS + 1)); }
fail() { local name="$1" msg="$2"; echo "  FAIL: ${name} -- ${msg}" >&2; FAIL=$((FAIL + 1)); ERRORS+=("${name}: ${msg}"); }

# Create a temp commit-msg file and register for cleanup
make_commit_msg() {
    local content="$1"
    local tmpfile
    tmpfile=$(mktemp /tmp/commit-msg-test-XXXXX)
    TEMP_FILES+=("${tmpfile}")
    printf '%s' "${content}" > "${tmpfile}"
    echo "${tmpfile}"
}

cleanup() {
    for f in "${TEMP_FILES[@]+"${TEMP_FILES[@]}"}"; do
        rm -f "${f}"
    done
}
trap cleanup EXIT

# Run the hook with a given branch (mocked via GIT_BRANCH_OVERRIDE) and commit msg file.
# Because the hook reads `git symbolic-ref --short HEAD` we simulate by running in a
# subshell and using a git-worktree trick. Instead, we mock by testing the hook logic
# directly: we extract the branch-detection logic and test it standalone.
run_hook_with_branch() {
    local branch="$1"
    local commit_msg_file="$2"
    local result

    # We need to mock git symbolic-ref. Use a wrapper script.
    local mock_dir
    mock_dir=$(mktemp -d /tmp/git-mock-XXXXX)
    TEMP_FILES+=("${mock_dir}/git")
    cat > "${mock_dir}/git" <<MOCK_EOF
#!/usr/bin/env bash
if [[ "\${1:-}" == "symbolic-ref" ]] && [[ "\${2:-}" == "--short" ]] && [[ "\${3:-}" == "HEAD" ]]; then
    echo "${branch}"
    exit 0
fi
# Fall through to real git for other commands
exec /usr/bin/git "\$@"
MOCK_EOF
    chmod +x "${mock_dir}/git"

    result=$(PATH="${mock_dir}:${PATH}" bash "${HOOK}" "${commit_msg_file}" 2>&1; echo "exit:$?")
    rm -rf "${mock_dir}"
    echo "${result}"
}

run_hook_exitcode_with_branch() {
    local branch="$1"
    local commit_msg_file="$2"
    local result
    result=$(run_hook_with_branch "${branch}" "${commit_msg_file}")
    echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://'
}

# ---------------------------------------------------------------------------
# Test suite
# ---------------------------------------------------------------------------

echo "=============================================="
echo "precommit-task-branch.sh -- EPIC-0059 story-0059-0005 TASK-002"
echo "=============================================="
echo ""

# Verify hook exists and is executable
if [[ ! -f "${HOOK}" ]]; then
    echo "FATAL: hook not found: ${HOOK}" >&2
    exit 2
fi

# AT-01: Hook is executable
at01() {
    if [[ -x "${HOOK}" ]]; then
        pass "AT-01: .githooks/commit-msg is executable"
    else
        fail "AT-01: hook executable" "${HOOK} is not executable"
    fi
}
at01

# AT-02: Non-task branch (feat/story-*) with no trailer → exit 0
at02() {
    local msg_file
    msg_file=$(make_commit_msg "feat(STORY-0059-0005): some commit on story branch")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "feat/story-0059-0005-description" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-02: feat/story-* branch with no trailer → exit 0 (exempt)"
    else
        fail "AT-02: feat/story-* exempt" "got exit ${exitcode}"
    fi
}
at02

# AT-03: Non-task branch (fix/*) with no trailer → exit 0
at03() {
    local msg_file
    msg_file=$(make_commit_msg "fix: some bugfix commit")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "fix/typo-in-readme" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-03: fix/* branch with no trailer → exit 0 (exempt)"
    else
        fail "AT-03: fix/* exempt" "got exit ${exitcode}"
    fi
}
at03

# AT-04: Non-task branch (chore/*) with no trailer → exit 0
at04() {
    local msg_file
    msg_file=$(make_commit_msg "chore: some chore commit")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "chore/cleanup" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-04: chore/* branch with no trailer → exit 0 (exempt)"
    else
        fail "AT-04: chore/* exempt" "got exit ${exitcode}"
    fi
}
at04

# AT-05: feat/task-* branch with NO trailer → exit 1 BLOCKED
at05() {
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): manual commit without trailer")
    local result
    result=$(run_hook_with_branch "feat/task-0059-0005-002-precommit-task-branch" "${msg_file}")
    local exitcode
    exitcode=$(echo "${result}" | grep "exit:" | tail -1 | sed 's/exit://')
    if [[ "${exitcode}" == "1" ]] && echo "${result}" | grep -q "BLOCKED"; then
        pass "AT-05: feat/task-* branch without trailer → exit 1 BLOCKED"
    else
        fail "AT-05: task branch blocked" "exitcode=${exitcode}, output=${result}"
    fi
}
at05

# AT-06: feat/task-* branch with CORRECT trailer → exit 0
at06() {
    local sha="a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): commit via x-git-commit

Co-Authored-By: x-git-commit@${sha}")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "feat/task-0059-0005-002-precommit-task-branch" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-06: feat/task-* branch WITH correct trailer → exit 0"
    else
        fail "AT-06: task branch with trailer passes" "got exit ${exitcode}"
    fi
}
at06

# AT-07: feat/task-* branch with MALFORMED trailer (wrong key) → exit 1
at07() {
    local sha="a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): commit with wrong trailer key

Co-Authored-By: x-internal-status-update@${sha}")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "feat/task-0059-0005-002-precommit-task-branch" "${msg_file}")
    if [[ "${exitcode}" == "1" ]]; then
        pass "AT-07: feat/task-* branch with wrong trailer key → exit 1"
    else
        fail "AT-07: malformed trailer rejected" "got exit ${exitcode}"
    fi
}
at07

# AT-08: feat/task-* branch with MALFORMED SHA (not 40 hex) → exit 1
at08() {
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): commit with short SHA trailer

Co-Authored-By: x-git-commit@abc123")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "feat/task-0059-0005-002-precommit-task-branch" "${msg_file}")
    if [[ "${exitcode}" == "1" ]]; then
        pass "AT-08: feat/task-* branch with malformed SHA trailer → exit 1"
    else
        fail "AT-08: short SHA rejected" "got exit ${exitcode}"
    fi
}
at08

# AT-09: CLAUDE_TASK_BRANCH_HOOK_DISABLED=1 bypasses Guard 2 → exit 0
at09() {
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): commit with bypass env")
    local exitcode
    exitcode=$(CLAUDE_TASK_BRANCH_HOOK_DISABLED=1 bash "${HOOK}" "${msg_file}" 2>/dev/null; echo $?)
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-09: CLAUDE_TASK_BRANCH_HOOK_DISABLED=1 bypasses Guard 2 → exit 0"
    else
        fail "AT-09: env bypass" "got exit ${exitcode}"
    fi
}
at09

# AT-10: CLAUDE_SKIP_AUDIT=1 does NOT bypass Guard 2 (only CLAUDE_TASK_BRANCH_HOOK_DISABLED)
at10() {
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): commit on task branch no trailer")
    local exitcode
    exitcode=$(CLAUDE_SKIP_AUDIT=1 run_hook_exitcode_with_branch "feat/task-0059-0005-002-precommit-task-branch" "${msg_file}")
    if [[ "${exitcode}" == "1" ]]; then
        pass "AT-10: CLAUDE_SKIP_AUDIT=1 does NOT bypass Guard 2 → still exit 1"
    else
        fail "AT-10: CLAUDE_SKIP_AUDIT bypass ignored" "got exit ${exitcode}"
    fi
}
at10

# AT-11: Branch name pattern validates correctly — partial matches exempt
# e.g. "feat/task" (without the 4-digit numbers) should be exempt
at11() {
    local msg_file
    msg_file=$(make_commit_msg "chore: commit on vague task branch without numbers")
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "feat/task-cleanup" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-11: 'feat/task-cleanup' (no XXXX-YYYY-NNN pattern) → exit 0 (exempt)"
    else
        fail "AT-11: non-canonical task branch exempt" "got exit ${exitcode}"
    fi
}
at11

# AT-12: Error message mentions x-git-commit-version for user orientation
at12() {
    local msg_file
    msg_file=$(make_commit_msg "feat(TASK-0059-0005-002): direct commit no trailer")
    local result
    result=$(run_hook_with_branch "feat/task-0059-0005-002-test" "${msg_file}")
    if echo "${result}" | grep -q "x-git-commit-version\|x-git-commit"; then
        pass "AT-12: error message references x-git-commit for user orientation"
    else
        fail "AT-12: error message mentions x-git-commit" "output: ${result}"
    fi
}
at12

# AT-13: Guard 1 still works (regression test — execution-state.json protection intact)
# Run on a non-task branch (mocked) to isolate Guard 1 behavior.
# Since no execution-state.json is staged in the test context, Guard 1 should exit 0.
at13() {
    local msg_file
    msg_file=$(make_commit_msg "chore: some commit on develop without x-internal-status-update trailer")
    # Use branch mocking + disable Guard 2 to isolate Guard 1 behavior
    # (Guard 2 is only relevant on task branches; this tests Guard 1 in isolation)
    local exitcode
    exitcode=$(run_hook_exitcode_with_branch "develop" "${msg_file}")
    if [[ "${exitcode}" == "0" ]]; then
        pass "AT-13: Guard 1 regression — non-task branch, no staged execution-state.json → exit 0"
    else
        fail "AT-13: Guard 1 regression" "got exit ${exitcode}"
    fi
}
at13

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "=============================================="
echo "Results: ${PASS} passed, ${FAIL} failed"
echo "=============================================="

if [[ ${FAIL} -gt 0 ]]; then
    echo ""
    echo "Failed tests:"
    for err in "${ERRORS[@]}"; do
        echo "  - ${err}"
    done
    exit 1
fi

exit 0
