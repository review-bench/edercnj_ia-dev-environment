---
name: audit-gate-lifecycle
description: Full audit gate lifecycle reference — 5-camada taxonomy, naming conventions, exit codes, self-check contract, catalog-before-add
requires-capabilities: []
---
# Audit Gate Lifecycle — Full Reference

> **Introduced by:** EPIC-0058. **Extended by:** EPIC-0061 (Camada 0).
> **ADR:** ADR-0015, ADR-0017.
> **Full reference (decision tree, Camada 0 details, naming, exit codes, self-check template):**
> `Read .claude/knowledge/governance/audit-gate-lifecycle.md`

## Taxonomy (5 Camadas)

| Camada | Layer | Location | Mode |
| :--- | :--- | :--- | :--- |
| **0** | Local Hooks Preventivos | `.claude/hooks/verify-*.sh`, `enforce-*.sh` | **Preventive** |
| **1** | Normative | `.claude/rules/*.md`, `CLAUDE.md` | Normative |
| **2** | CI Script | `scripts/audit-*.sh` | Detectivo |
| **3** | Java Test | `*AuditTest.java` / `*Lint.java` | Detectivo |
| **4** | CI Workflow | `.github/workflows/*.yml` | Detectivo |

## Naming Summary

- Hook: `verify-{subject}.sh` (read-only) / `enforce-{subject}.sh` (blocking) / `session-{subject}.sh`
- CI script: `audit-{subject}.sh` — prefix `audit-` is mandatory
- Java test: `{Subject}AuditTest.java` or `{Subject}Lint.java`

## Exit Codes (CI Scripts)

| Code | Named constant | Meaning |
| :--- | :--- | :--- |
| 0 | — | Success |
| 1 | `<RULE>_VIOLATION` | Violation detected |
| 2 | `OPERATIONAL_ERROR` | Prerequisite missing |
| 3 | `BASELINE_CORRUPT` | Baseline/exemption corrupt |

## `--self-check` Contract

Every `audit-*.sh` MUST implement `--self-check` (exit 0 = valid; exit 2 = broken prerequisite).
The CI workflow runs `--self-check` for all scripts before the main audit matrix.

## Catalog-before-Add

No gate may be introduced in any Rule, ADR, or SKILL.md without a simultaneous entry in `docs/audit-gates-catalog.md`.

## Forbidden

- CI script without `audit-` prefix
- Exit codes outside 0–3 without a rule amendment
- Adding a gate to any Rule without a catalog entry
- Implementing a Hook gate for a check that only needs to run on PR (wrong layer)
- Omitting `--self-check` from a CI script
- Using prefix `verify-` for a CI script or `audit-` for a Hook
