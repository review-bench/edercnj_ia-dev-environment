# Story Completion Report — story-0063-0014

**Story:** Sub-Skill Wave Dispatch Audit
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28
**Status:** COMPLETE

## Delivery Summary

Story-0063-0014 delivers `audit-wave-dispatch.sh`, a Camada 2 CI audit script (Rule 26) that
validates that when x-story-implement Phase 1 dispatches parallel planning agents (Batch A),
the telemetry NDJSON shows subagent.start events for the expected wave members within a
30-second clustering window. Detects cases where agents were supposed to run in parallel
but were instead run serially or skipped.

## Artifacts Delivered

| Artifact | Path | Status |
|---|---|---|
| Script (source-of-truth) | `java/src/main/resources/targets/claude/scripts/audit-wave-dispatch.sh` | DELIVERED |
| Script (runtime copy) | `.claude/scripts/audit-wave-dispatch.sh` | DELIVERED |
| Shell tests | `src/test/shell/audit_wave_dispatch_test.sh` | DELIVERED (6 tests pass) |

## TDD Cycle

- **RED:** 6 tests written, all fail (script absent — exit 127)
- **GREEN:** Script implemented, all 6 tests pass
- **Refactor:** ts_to_epoch helper extracted for cross-platform portability; wave counting loop isolated

## Wave Dispatch Detection Algorithm

The script implements a two-phase validation:

1. **Count check:** extract subagent.start events filtered by storyId from NDJSON;
   if count < --expected-wave-size → WAVE_DISPATCH_INCOMPLETE (exit 1)

2. **Clustering check:** convert timestamps to epoch seconds for the first N start events;
   if max(ts) - min(ts) >= 30s → agents dispatched serially → WAVE_DISPATCH_INCOMPLETE (exit 1)

Exit 0 only when count ≥ expected AND spread < 30s (parallel window).

## Exit Code Compliance (Rule 26)

| Exit | Code | Condition |
|---|---|---|
| 0 | OK | Wave dispatched with ≥ expected agents, all within 30s window |
| 1 | WAVE_DISPATCH_INCOMPLETE | Fewer agents than expected OR serial dispatch detected |
| 2 | OPERATIONAL_ERROR | Missing file, missing jq, unknown flag |

## Test Results

```
=== audit-wave-dispatch.sh tests ===
  PASS: T1 --self-check ok
  PASS: T2 3 clustered subagent.start events exits 0
  PASS: T3 0 subagent.start events exits 1
  PASS: T4 missing NDJSON file exits 2
  PASS: T5 unknown flag exits 2
  PASS: T6 3 serial subagent.start events exits 1

Results: 6 passed, 0 failed
```
