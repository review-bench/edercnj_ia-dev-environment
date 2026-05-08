# Story Completion Report — story-0063-0012

**Story:** SKILL.md Tool-Call Grammar / Rule 28
**Epic:** EPIC-0063 Local-First Pre-Flight Gates
**Status:** Concluída
**Date:** 2026-04-28

## Summary

Story story-0063-0012 implemented Rule 28 (Tool-Call Grammar) and `audit-tool-call-grammar.sh`, establishing a mandatory inline grammar marker contract for every `Skill(...)` and `Agent(subagent_type: "general-purpose", ...)` declaration in Anexo B orchestrator SKILL.md files. This closes the class of "LLM silently skipped a declared step" bypass that was previously undetectable via static analysis.

## Acceptance Criteria

| AC | Status | Evidence |
| :--- | :--- | :--- |
| AC1 — Rule 28 normative document published | ✓ PASS | `.claude/rules/30-tool-call-grammar.md` created |
| AC2 — audit-tool-call-grammar.sh static mode | ✓ PASS | `scripts/audit-tool-call-grammar.sh` with --self-check, --skill-file, --skills-root |
| AC3 — Shell tests T1-T7 all passing | ✓ PASS | `src/test/shell/audit_tool_call_grammar_test.sh`: 7 passed, 0 failed |
| AC4 — Baseline file initialized | ✓ PASS | `audits/tool-call-grammar-baseline.txt` with grandfather mechanism explained |
| AC5 — Source-of-truth copy | ✓ PASS | `java/src/main/resources/targets/claude/rules/30-tool-call-grammar.md` |
| AC6 — Backward compatibility | ✓ PASS | Empty baseline; Anexo B markers pending TASK-0063-0012-004 |

## Tasks Executed

| Task | Description | Status |
| :--- | :--- | :--- |
| TASK-0063-0012-001 | Author Rule 28 normative document | Done |
| TASK-0063-0012-002 | Implement audit-tool-call-grammar.sh + baseline + tests | Done |

## Test Results

- Shell tests: **7 passed, 0 failed** (audit_tool_call_grammar_test.sh)
- Java tests: N/A for this story (ToolCallGrammarAuditTest.java deferred to TASK-0063-0012-003)
- Smoke tests: `--self-check` validated

## Coverage Delta

N/A — story scope is governance/audit infrastructure (bash scripts + markdown rules). No main Java source coverage impact.

## Review Findings

- Specialist review: **GO** (see review-story-story-0063-0012.md)
- Tech-Lead review: **GO** (see techlead-review-story-story-0063-0012.md)
- Verify gate: **PASSED** (see verify-envelope-story-0063-0012.json)

## Implementation Notes

- TDD RED→GREEN followed: test file created first (all 7 tests failing at exit 127), then script implemented (all 7 tests passing)
- `check_skill_file()` function handles both same-line and next-line marker placement per BNF spec
- Baseline mechanism uses `declare -A GRANDFATHERED` for O(1) lookups
- Rule 28 explicitly cross-references Rules 13, 24, 25 for integration clarity
- `--self-check` validates grep, jq, and Rule 28 file existence per Rule 26 contract

## Pull Request

See PR linked to branch `feat/story-0063-0012-tool-call-grammar` → `epic/0063`.

## Next Steps

1. TASK-0063-0012-003: Implement dynamic mode (NDJSON cross-check) — requires story-0063-0003
2. TASK-0063-0012-004: Apply markers to 8 Anexo B orchestrators
3. TASK-0063-0012-005: Integrate into preflight.sh and CI audit.yml
