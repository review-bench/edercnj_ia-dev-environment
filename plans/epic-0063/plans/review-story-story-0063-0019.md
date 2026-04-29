# Specialist Review — story-0063-0019

**Story:** NDJSON Integrity Hash Chain
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Reviewer:** Specialist Review (QA + Security)
**Date:** 2026-04-28
**Verdict:** GO

## QA Review

### Test Coverage
- 5 shell tests covering all primary scenarios (self-check, init, missing file, unknown flag, verify)
- RED → GREEN TDD cycle verified (tests written before implementation, all failed at 127, all pass after)
- T2 validates --init on empty NDJSON (edge case: empty chain)
- T5 validates full init → verify round-trip on single-line NDJSON

### Test Quality
- Tests are isolated: each uses its own TMP_DIR and independent STATE_DIR
- Cleanup via `trap cleanup EXIT` — no test pollution
- assert_exit helper is consistent with other audit test files in the project

### Acceptance Criteria Coverage
- AC1 (--self-check): T1 covers sha256sum/shasum availability check
- AC2 (--init on empty NDJSON): T2 covers genesis chain initialization
- AC3 (missing NDJSON): T3 covers OPERATIONAL_ERROR exit 2
- AC4 (unknown flag): T4 covers OPERATIONAL_ERROR exit 2
- AC5 (--verify with valid NDJSON): T5 covers end-to-end round-trip

## Security Review

### Hash Chain Algorithm
- Rolling SHA-256 chain: hash[N] = SHA-256(hash[N-1] + line[N]) — correctly detects injection at any position
- Genesis: SHA-256("" + line[0]) for first event — deterministic empty-anchor behavior
- Empty file: resolves to SHA-256("") genesis — consistent with --verify on same empty file

### Portability
- Uses `sha256sum` (Linux) with fallback to `shasum -a 256` (macOS) — correct cross-platform approach
- `--self-check` validates tool availability before any audit operation

### Tamper Detection
- Any line insertion, deletion, or modification changes the rolling hash — integrity is guaranteed
- Line count stored in anchor alongside hash — protects against appending that happens to not shift prior hashes (impossible for SHA-256 chain but adds defense-in-depth)

### Path Handling
- No path traversal risk: NDJSON_FILE validated for existence only (no normalized path injection)
- STATE_DIR and ANCHOR_FILE are operator-controlled (not user-input derived in normal use)

### Strict Mode
- `set -uo pipefail` enforced — unbound variables and pipe failures caught
- Known exception: `|| true` on jq calls in verify mode is deliberate (fallback to grep-based parsing)

## Findings

| Severity | Finding | Status |
|---|---|---|
| INFO | jq dependency optional (falls back to grep-based parsing) | Accepted — portability trade-off, documented in script |
| INFO | No locking on anchor file (concurrent --init could race) | Accepted — concurrent invocation not in scope for CI audit use |

## Verdict

**GO** — Script is correct, portable, secure, and well-tested. Ready for merge.
