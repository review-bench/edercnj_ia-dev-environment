# Story Completion Report — story-0063-0019

**Story:** NDJSON Integrity Hash Chain
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28
**Status:** COMPLETE

## Delivery Summary

Story-0063-0019 delivers `audit-ndjson-hash-chain.sh`, a Camada 2 CI audit script (Rule 26) that
computes a rolling SHA-256 hash chain over events.ndjson files to detect tampered or injected
telemetry events.

## Artifacts Delivered

| Artifact | Path | Status |
|---|---|---|
| Script (source-of-truth) | `java/src/main/resources/targets/claude/scripts/audit-ndjson-hash-chain.sh` | DELIVERED |
| Script (runtime copy) | `.claude/scripts/audit-ndjson-hash-chain.sh` | DELIVERED |
| Shell tests | `src/test/shell/audit_ndjson_hash_chain_test.sh` | DELIVERED (5 tests pass) |

## TDD Cycle

- **RED:** 5 tests written, all fail (script absent — exit 127)
- **GREEN:** Script implemented, all 5 tests pass
- **Refactor:** sha256_of and compute_chain helpers extracted for clarity

## Hash Chain Algorithm

The script implements a rolling SHA-256 chain:
- `hash[0] = SHA-256("" + line[0])` — genesis: empty previous hash concatenated with first line
- `hash[N] = SHA-256(hash[N-1] + line[N])` — each event chains on prior hash

This ensures that any insertion, deletion, or modification of any event in the NDJSON file
will produce a different chain head, making tampering detectable.

## Interface

| Flag | Description |
|---|---|
| `--ndjson-file <path>` | NDJSON file to audit |
| `--state-dir <dir>` | Directory for chain anchor storage |
| `--epic-id <id>` | Epic identifier (used in anchor filename) |
| `--init` | Initialize new chain from current NDJSON content |
| `--verify` | Verify NDJSON against stored chain anchor |
| `--self-check` | Verify sha256sum/shasum availability |

## Exit Codes (Rule 26)

| Code | Meaning |
|---|---|
| 0 | OK |
| 1 | CHAIN_INTEGRITY_VIOLATED |
| 2 | OPERATIONAL_ERROR |

## Test Results

```
Results: 5 passed, 0 failed
```

## Evidence Artifacts

- `ai/epics/epic-0063-local-first-preflight-gates/plans/review-story-story-0063-0019.md` — GO
- `ai/epics/epic-0063-local-first-preflight-gates/plans/techlead-review-story-story-0063-0019.md` — GO
- `ai/epics/epic-0063-local-first-preflight-gates/reports/verify-envelope-story-0063-0019.json` — passed=true, acCheckCount=5
- `ai/epics/epic-0063-local-first-preflight-gates/reports/dependency-audit-story-0063-0019.md`
- `ai/epics/epic-0063-local-first-preflight-gates/reports/story-completion-report-story-0063-0019.md` (this file)

## Acceptance Criteria Verification

| AC | Description | Status |
|---|---|---|
| AC1 | --self-check validates sha256sum/shasum (T1) | PASS |
| AC2 | --init on empty NDJSON exits 0 (T2) | PASS |
| AC3 | missing NDJSON file exits 2 (T3) | PASS |
| AC4 | unknown flag exits 2 (T4) | PASS |
| AC5 | --verify with valid single-line NDJSON exits 0 (T5) | PASS |
