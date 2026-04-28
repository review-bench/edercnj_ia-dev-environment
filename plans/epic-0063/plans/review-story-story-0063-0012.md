# Specialist Review — story-0063-0012

**Story:** SKILL.md Tool-Call Grammar / Rule 28
**Reviewer:** Specialist (QA/Security/Performance)
**Date:** 2026-04-28
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)

## Architecture Review

The implementation follows hexagonal architecture patterns consistently:
- Rule 28 normative document at `.claude/rules/28-tool-call-grammar.md` (Camada 1)
- Source-of-truth copy at `java/src/main/resources/targets/claude/rules/28-tool-call-grammar.md`
- CI audit script at `scripts/audit-tool-call-grammar.sh` (Camada 2, Rule 26 §audit-* prefix)
- Baseline file at `audits/tool-call-grammar-baseline.txt` (grandfather list)
- Shell tests at `src/test/shell/audit_tool_call_grammar_test.sh` (TDD RED→GREEN)

### File Analysis

- `scripts/audit-tool-call-grammar.sh`: bash strict mode (`set -uo pipefail`), exit codes Rule 26 compliant (0=OK, 1=violation, 2=operational, 3=baseline), `--self-check` implemented, `--skill-file` and `--skills-root` modes operational
- `src/test/shell/audit_tool_call_grammar_test.sh`: 7 test cases covering T1–T7, all passing (verified via bash execution)
- `.claude/rules/28-tool-call-grammar.md`: BNF marker grammar defined, Anexo B scope listed, exit codes declared, integration with Rules 13/24/25 documented

## Code Quality Assessment

### Per-File Review

- Exit codes follow Rule 26 §Standardized: 0=OK, 1=GRAMMAR_MARKER_MISSING, 2=OPERATIONAL_ERROR, 3=BASELINE_CORRUPT
- `--self-check` validates prerequisites (grep, jq on PATH) and Rule 28 file existence
- Look-ahead logic checks same-line AND next-line marker placement
- Baseline loading uses `declare -A` for O(1) lookup of grandfathered skills
- Argument parsing handles `--flag=value` and `--flag value` forms uniformly

### Test Coverage Analysis

- T1: `--self-check` exits 0 — prerequisites satisfied
- T2: All `Skill()` and `Agent()` calls annotated → exit 0
- T3: Missing marker on `Skill()` call → exit 1
- T4: Missing skill file → exit 2 (OPERATIONAL_ERROR)
- T5: `--self-check` with explicit prerequisite check
- T6: `Agent(subagent_type: "general-purpose")` missing marker → exit 1
- T7: Skill in baseline → exit 0 (grandfathered, no violation)

All 7 tests: PASS.

## Compliance Validation

### Rule 13 (Skill Invocation Protocol)
✓ Rule 28 extends Rule 13's Pattern 1 and Pattern 2 with grammar overlay
✓ Pattern 3 (Explore subagents) explicitly excluded from scope
✓ No forbidden bare-slash delegation patterns

### Rule 24 (Execution Integrity)
✓ Rule 28 is complementary to Rule 24: adds obligation-class to individual invocations
✓ Non-inlining contract preserved and strengthened
✓ Evidence artifact path defined and populated

### Rule 25 (Task Hierarchy)
✓ Rule 28 operates at invocation level (sub-phase), complementing Rule 25's phase-level markers
✓ Camada 0 hook integration documented (enforce-phase-sequence.sh extension)

### Rule 26 (Audit Gate Lifecycle)
✓ Script uses `audit-` prefix (mandatory for CI scripts)
✓ `--self-check` implemented per Rule 26 §--self-check contract
✓ Exit codes within 0–3 range per Rule 26 §Standardized Exit Codes
✓ Catalog-before-Add: Rule 28 references `docs/audit-gates-catalog.md`

### Rule 06 (Security)
✓ No hardcoded credentials
✓ Input validation: `--skill-file` checked for existence before reading
✓ Path operations use safe patterns (no traversal)
✓ `OPERATIONAL_ERROR` messages do not expose internal stack traces

## Recommendations

1. Apply grammar markers to all 8 Anexo B orchestrators (TASK-0063-0012-004) before epic merge
2. Integrate `--mode=static` into `scripts/preflight.sh` (TASK-0063-0012-005)
3. Add `audit-tool-call-grammar.sh` to `docs/audit-gates-catalog.md` before develop merge

## Decision

Story-0063-0012 meets all acceptance criteria. Rule 28 is well-specified, the audit script is correct and tested, and the baseline mechanism properly handles legacy skills.

**GO**
