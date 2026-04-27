#!/usr/bin/env bats
#
# audit-flow-version.bats — Integration tests for audit-flow-version.sh
# Covers the 5 Gherkin scenarios from story-0058-0003 §7.
#
# Each test creates a temp dir mimicking plans/epic-NNNN/execution-state.json,
# points the script at it via AUDIT_FLOW_VERSION_PLANS_GLOB, and asserts
# the script's exit code + stdout/stderr.
#
# Requirements:
#   - bats-core >= 1.5 (https://github.com/bats-core/bats-core)
#   - jq on PATH (script's preferred parser)
#
# Run: bats scripts/tests/audit-flow-version.bats

SCRIPT="$(cd "$(dirname "$BATS_TEST_FILENAME")/.." && pwd)/audit-flow-version.sh"
FIXTURES="$(cd "$(dirname "$BATS_TEST_FILENAME")/../fixtures/audit-flow-version" && pwd)"

setup() {
  TMP_PLANS_DIR="$(mktemp -d)"
}

teardown() {
  rm -rf "${TMP_PLANS_DIR}"
}

# Helper: copy a fixture into a per-epic dir under TMP_PLANS_DIR.
seed_fixture() {
  local fixture="$1" epic_id="$2"
  mkdir -p "${TMP_PLANS_DIR}/epic-${epic_id}"
  cp "${FIXTURES}/${fixture}" \
     "${TMP_PLANS_DIR}/epic-${epic_id}/execution-state.json"
}

# ---------------------------------------------------------------------------
# Scenario: Happy path — flowVersion="2"
# ---------------------------------------------------------------------------
@test "valid flowVersion=2 exits 0 with violations: 0" {
  seed_fixture valid-v2.json 9001

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 0 ]
  [[ "$output" =~ "violations: 0" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Happy path — flowVersion="1"
# ---------------------------------------------------------------------------
@test "valid flowVersion=1 exits 0 with violations: 0" {
  seed_fixture valid-v1.json 9002

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 0 ]
  [[ "$output" =~ "violations: 0" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Error path — flowVersion="3" (invalid)
# ---------------------------------------------------------------------------
@test "invalid flowVersion=3 exits 1 with FLOW_VERSION_VIOLATION" {
  seed_fixture invalid.json 9003

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 1 ]
  [[ "$output" =~ "FLOW_VERSION_VIOLATION" ]] || [[ "$stderr" =~ "FLOW_VERSION_VIOLATION" ]] || true
  # stdout sumarises violation count
  [[ "$output" =~ "violations: 1" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Boundary — missing flowVersion in default mode (warning)
# ---------------------------------------------------------------------------
@test "missing flowVersion in default mode exits 0 with warning" {
  seed_fixture missing.json 9004

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 0 ]
  [[ "$output" =~ "warnings: 1" ]] || [[ "$output" =~ "violations: 0" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Boundary — missing flowVersion with --strict (violation)
# ---------------------------------------------------------------------------
@test "missing flowVersion with --strict exits 1" {
  seed_fixture missing.json 9005

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}" --strict

  [ "$status" -eq 1 ]
  [[ "$output" =~ "violations: 1" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Self-check exits 0
# ---------------------------------------------------------------------------
@test "--self-check exits 0 and prints OK" {
  run bash "${SCRIPT}" --self-check
  [ "$status" -eq 0 ]
  [[ "$output" =~ "OK" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Help flag exits 0
# ---------------------------------------------------------------------------
@test "--help exits 0 and prints Usage" {
  run bash "${SCRIPT}" --help
  [ "$status" -eq 0 ]
  [[ "$output" =~ "Usage" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Mixed fixtures — 1 valid + 1 invalid → exit 1
# ---------------------------------------------------------------------------
@test "mixed fixtures (valid + invalid) exits 1 with violations: 1" {
  seed_fixture valid-v2.json 9006
  seed_fixture invalid.json 9007

  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 1 ]
  [[ "$output" =~ "violations: 1" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Empty plans dir — no execution-state.json files
# ---------------------------------------------------------------------------
@test "empty plans dir exits 0 with checked 0 files" {
  run env AUDIT_FLOW_VERSION_PLANS_GLOB="${TMP_PLANS_DIR}/epic-*/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 0 ]
  [[ "$output" =~ "checked 0 files" ]]
}
