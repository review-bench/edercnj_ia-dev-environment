---
name: doc-freshness-gate
description: Full documentation freshness gate reference — targets, YAML block, staleness definition, enforcement layers
requires-capabilities: []
---
# Documentation Freshness Gate — Full Reference

> **Introduced by:** EPIC-0071
> **ADR:** ADR-0024
> **Capability:** `governance.doc-as-dod`

## Documentation Targets (Stack-Aware)

| Target | Always included | Conditional on |
| :--- | :--- | :--- |
| `readme` | yes | — |
| `adr` | yes | — |
| `openapi` | no | `interfaces[].spec` contains `openapi` |
| `asyncapi` | no | `interfaces[].broker` is not empty |
| `grpc-proto` | no | `interfaces[].spec` contains `proto` |
| `skill-docs` | no | `.claude/skills/` directory exists |
| `system-architecture` | no | `docs/architecture/system.md` exists |

## `documentation` YAML Block

```yaml
documentation:
  targets:
    - readme
    - openapi
    - adr
  freshness-window-hours: 0
```

| Field | Default | Semantics |
| :--- | :--- | :--- |
| `targets` | `[]` (auto-detect) | Explicit override of target list |
| `freshness-window-hours` | `0` | Grace period in hours; `0` = immediate gate |

## Freshness Definition

A documentation target is **stale** when:

- Target file does not exist at the expected path
- Target file's `git diff --stat HEAD~1` shows no changes, but implementation files changed
- For OpenAPI/AsyncAPI: new endpoint/event declared in source but not in spec
- For ADR: `## Decision Rationale` references `ADR-XXXX` but the file does not exist
- For skill-docs: modified SKILL.md lacks `## Triggers` or `## Examples`

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` | `git push`, `gh pr create` | Blocks if evidence absent |
| **1 — Normative** | This rule + CLAUDE.md | Every conversation | — |
| **2 — CI Script** | `audit-doc-freshness.sh` | PR open/sync | `DOC_FRESHNESS_VIOLATION` |
| **3 — Java Test** | `Epic0071DocAsDoDSmokeIT` | `mvn verify` | JUnit assertion failure |

## Mandatory Invocation in `x-implement-story`

Phase 3 MUST invoke both:

```
Skill(skill: "x-generate-docs", model: "sonnet", args: "<STORY-ID> --target-stack-aware")  [required]
Skill(skill: "x-validate-docs", model: "sonnet", args: "<STORY-ID>")                        [required]
```

Evidence artifact: `ai/epics/epic-XXXX/reports/doc-validate-report-STORY-ID.md`.

## `--skip-doc` Constraint

Permitted exclusively inside `## Recovery` blocks. Occurrence outside → `BYPASS_FLAG_VIOLATION` in CI.

## Forbidden

- Invoking `x-implement-story` without subsequent `x-validate-docs` in Phase 3
- Using `--skip-doc` outside a `## Recovery` block
- Declaring a target not in the canonical list above
