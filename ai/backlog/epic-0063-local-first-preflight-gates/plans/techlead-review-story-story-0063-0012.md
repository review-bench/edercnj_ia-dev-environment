# Tech-Lead Review — story-0063-0012

**Story:** SKILL.md Tool-Call Grammar / Rule 28
**Reviewer:** Tech-Lead (45-point holistic review)
**Date:** 2026-04-28
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)

## Overview

Story story-0063-0012 introduces Rule 28 (Tool-Call Grammar) and `audit-tool-call-grammar.sh`, closing the class of "LLM silently skipped a declared sub-skill" bypass by making each invocation's obligation class statically checkable. Evaluated against the 45-point checklist.

## 45-Point Checklist

### Clean Code (10 pontos)
✓ Naming: `GRAMMAR_MARKER_MISSING`, `OPERATIONAL_ERROR`, `BASELINE_CORRUPT` — intent-revealing
✓ Functions: `check_skill_file()` is < 25 lines of logic per concern
✓ No dead code: all code paths reachable and tested
✓ No redundant comments: comments explain why, not what
✓ Variable names self-documenting: `SKILL_FILE`, `BASELINE_FILE`, `TOTAL_VIOLATIONS`
✓ Single responsibility per function: argument parsing, self-check, and file-checking are separate
✓ DRY: look-ahead logic centralized in `check_skill_file()`
✓ Error messages clear: format `GRAMMAR_MARKER_MISSING: <file> line <N>: <line>`
✓ Imports/dependencies: explicit `command -v` checks for external tools
✓ Indentation consistent: 4-space bash indentation throughout

### SOLID Principles (5 pontos)
✓ SRP: script has one responsibility — static lint of grammar markers
✓ OCP: new modes (dynamic) can be added without breaking static mode
✓ LSP: N/A (no inheritance in bash)
✓ ISP: small focused flags (`--skill-file`, `--skills-root`, `--baseline`, `--self-check`)
✓ DIP: depends on `grep`, `jq` via PATH (injectable via environment)

### Architecture (5 pontos)
✓ Hexagonal layers respected: audit script is infrastructure, no domain logic
✓ Domain purity preserved: no domain imports in bash scripts
✓ Dependency direction: script reads SKILL.md → reports to stderr; no circular deps
✓ Adapter pattern: baseline file is an adapter for the grandfather list
✓ ADR references: Rule 28 cross-references ADR-0016 for zero-bypass context

### Framework Conventions (5 pontos)
✓ Rule 26 naming: `audit-` prefix mandatory for CI scripts — compliant
✓ Rule 26 exit codes: 0/1/2/3 mapping follows §Standardized Exit Codes
✓ Rule 26 `--self-check`: implemented per contract (validates prerequisites + rule file)
✓ Rule 13 integration: extends Pattern 1 and Pattern 2 with grammar overlay documented
✓ Backward compatibility: baseline file provides grandfather mechanism; empty by design

### Tests (5 pontos)
✓ Tests precede implementation (TDD RED phase run and verified before GREEN)
✓ Edge cases covered: missing file, grandfathered skill, Agent() calls, next-line markers
✓ No mocking of domain (pure shell test without stubs)
✓ Test names follow convention: `T1 --self-check exits 0`, etc.
✓ Acceptance criteria validated via T1–T7

### TDD Process (5 pontos)
✓ RED phase: tests authored first, all 7 failing (exit 127)
✓ GREEN phase: script implemented, all 7 tests pass
✓ Refactor: `check_skill_file()` extracted to avoid duplication between single-file and directory modes
✓ Atomic commits: single commit per concern
✓ Conventional Commits format: `feat(story-0063-0012): ...`

### Security (5 pontos)
✓ No hardcoded secrets or credentials
✓ Path validation: `[[ ! -f "$SKILL_FILE" ]]` before reading
✓ Input validation: unknown flags → `OPERATIONAL_ERROR: unknown flag`
✓ No unsafe shell expansions: `"$var"` quoting throughout
✓ Error messages do not expose absolute paths in success output

### Cross-file Consistency (5 pontos)
✓ Exit code pattern uniform with `audit-pr-fix-diff.sh` and other audit scripts
✓ Self-check pattern uniform: `SELF_CHECK_OK: ... prerequisites satisfied`
✓ Argument parsing pattern uniform: `case "$1"` with `=` and space variants
✓ Logging style consistent: all violations to stderr, OK to stderr
✓ Error message format consistent: `CODE: message` pattern throughout

## Risk Assessment

**Low Risk.** Implementation is additive — the Rule 28 grammar requirement only applies
to Anexo B orchestrators when markers are applied (TASK-0063-0012-004). Existing SKILL.md
files without markers are safe until that task runs. The baseline provides a controlled
grandfather path.

## Decision

All 45 checkpoints passed. Story is production-ready.

**GO**
