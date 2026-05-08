# Story Completion Report — story-0063-0004

**Story:** PreToolUse Blocking Hook (enforce-preflight-gates.sh)
**Epic:** EPIC-0063 Local-First Pre-Flight Gates
**Branch:** feat/story-0063-0004-pretooluse-hook-v1
**Status:** Concluída
**Date:** 2026-04-28

## Summary

Story story-0063-0004 implementada com sucesso. O hook PreToolUse `enforce-preflight-gates.sh` é a peça central da Camada 0 preventiva do EPIC-0063 — fisicamente bloqueia `git push`, `gh pr create`, e `Skill x-pr-create` antes de executar quando `scripts/preflight.sh` falha.

## Acceptance Criteria

| AC | Status | Evidence |
| :--- | :--- | :--- |
| AC1 Hook executável + registrado em settings.json | ✓ PASS | `.claude/hooks/enforce-preflight-gates.sh` chmod +x; settings.json updated |
| AC2 9 cenários Gherkin testados via TDD | ✓ PASS | `src/test/shell/enforce_preflight_gates_test.sh` — 9/9 GREEN |
| AC3 Recovery mode telemetry event | ✓ PASS | `emit_recovery_event()` appends to NDJSON when CLAUDE_RECOVERY_MODE=1 |
| AC4 git commit -n blocked | ✓ PASS | T7 in test suite |
| AC5 gh pr merge --admin blocked | ✓ PASS | Inline bypass-vector detection |
| AC6 Preflight absent = fail-CLOSED | ✓ PASS | T5 in test suite |
| AC7 Non-matched = no-op | ✓ PASS | T6 in test suite |
| AC8 Skill x-pr-create intercepted | ✓ PASS | T8 in test suite |
| AC9 Malformed JSON fail-CLOSED | ✓ PASS | T9 in test suite |

## Tasks Executed (TDD)

1. Tests written first (RED phase): 0/9 passing
2. Hook implemented (GREEN phase): 9/9 passing
3. Settings.json updated (hook registration)

## Files Delivered

| File | Action |
| :--- | :--- |
| `java/src/main/resources/targets/claude/hooks/enforce-preflight-gates.sh` | NEW (source-of-truth) |
| `.claude/hooks/enforce-preflight-gates.sh` | NEW (output copy, chmod +x) |
| `src/test/shell/enforce_preflight_gates_test.sh` | NEW (9 TDD tests) |
| `.claude/settings.json` | MODIFIED (PreToolUse hook registered, timeout=300) |

## Test Results

- Shell tests: 9/9 passing
- Java tests: N/A (no new Java code)
- Smoke tests: validated (hook executable, settings.json valid JSON)

## Coverage Delta

N/A — story scope is governance/hook infrastructure (no main src coverage impact).

## Review Findings

- Specialist review: GO (see review-story-story-0063-0004.md)
- Tech-Lead review: GO (see techlead-review-story-story-0063-0004.md)
- Verify gate: PASSED (see verify-envelope-story-0063-0004.json)

## Hook Design

The hook implements a 3-stage dispatch:
1. **Bypass-vector detection** (unconditional — before recovery check): blocks `git commit -n`, `git commit --no-verify`, `gh pr merge --admin`
2. **Pattern matching**: identifies interceptable tool calls (git push, gh pr create, Skill x-pr-create)
3. **Preflight gate**: invokes `scripts/preflight.sh` with derived scope/story-id

RULE-005 (fail-CLOSED): preflight absent → block (exit 2), not allow.
RULE-004: only `CLAUDE_RECOVERY_MODE=1` bypasses gates — all other env vars ignored.

## Next Steps

1. Human review of PR
2. Merge to epic/0063
3. story-0063-0013 (v2 matchers) extends this hook with 15 additional bypass vectors
4. story-0063-0016 (feature toggle) adds CLAUDE_PREFLIGHT_PHASE=warn mode
5. story-0063-0018 (hook self-check) adds --self-check flag to this hook
