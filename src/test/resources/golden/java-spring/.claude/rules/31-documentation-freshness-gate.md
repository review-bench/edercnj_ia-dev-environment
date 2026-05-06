---
requires-capabilities: []
---
# Rule 31 — Documentation Freshness Gate

> **Related:** Rule 04 (Architecture Summary), Rule 22 (Skill Visibility), Rule 24 (Execution Integrity), Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle), Rule 30 (Value-Driven Templates).
> **Introduced by:** EPIC-0071 (Documentation as DoD).
> **Cross-link:** Rule 30 (Value-Driven Templates, EPIC-0070) — complementary; governs template structure; Rule 31 governs temporal enforcement of doc updates.
> **ADR:** [ADR-0024 — Documentation Freshness Gate](../../docs/adr/ADR-0024-documentation-freshness-gate.md).
> **Capability:** `governance.doc-as-dod` (`capabilities/governance/doc-as-dod.yaml`).

## Purpose

Documentation that is not enforced as a gate becomes documentation that does not exist. Before EPIC-0071, the `x-generate-docs` skill was invoked "if time permits" in Phase 3 of `x-implement-story`, and no CI check verified that README, OpenAPI specs, ADRs, or skill-docs were kept current. The result: silent accumulation of documentation debt at a rate proportional to the velocity of code changes.

Rule 31 makes documentation updates a **blocking gate** in the story lifecycle:

1. **`x-validate-docs`** (EPIC-0071 story-0071-0002) verifies freshness of all configured documentation targets for the project's stack. Invocation is **MANDATORY** (Rule 24) in Phase 3 of `x-implement-story`.
2. **`x-generate-docs` v2** (EPIC-0071 story-0071-0003) updates documentation in stack-aware mode before the validate gate runs.
3. **`audit-doc-freshness.sh`** (EPIC-0071 story-0071-0005) is the Camada 2 CI gate that verifies the same targets on every PR to `develop` or `epic/*`.
4. The `--skip-doc` flag is removed from `x-implement-story`; it is only permitted inside `## Recovery` blocks of that skill.

## Documentation Targets

The set of targets is **stack-aware** — derived from the project YAML `documentation.targets` block or auto-detected from `interfaces[]` declarations when the block is absent:

| Target | Always included | Conditional on |
| :--- | :--- | :--- |
| `readme` | yes | — |
| `adr` | yes | — |
| `openapi` | no | `interfaces[].spec` contains `openapi` |
| `asyncapi` | no | `interfaces[].broker` is not empty |
| `grpc-proto` | no | `interfaces[].spec` contains `proto` |
| `skill-docs` | no | `.claude/skills/` directory exists at runtime |
| `system-architecture` | no | `docs/architecture/system.md` exists at runtime (EPIC-0070) |

When `documentation.targets` is explicitly configured in the project YAML, the declared list overrides auto-detection.

## `documentation` YAML Block

Projects configure the gate via the `documentation` block in the project YAML (parsed by `DocumentationConfig.java`, EPIC-0071 story-0071-0001):

```yaml
documentation:
  targets:
    - readme
    - openapi
    - adr
  freshness-window-hours: 0
```

| Field | Type | Default | Semantics |
| :--- | :--- | :--- | :--- |
| `targets` | string[] | `[]` (auto-detect) | Explicit override of target list; empty = auto-detect |
| `freshness-window-hours` | integer | `0` | Grace period in hours; `0` = immediate gate |

## Freshness Definition

A documentation target is **stale** when:

- The target file does not exist at the expected path.
- The target file's `git diff --stat HEAD~1` shows no changes, but one or more implementation files in the same PR have been added/modified.
- For OpenAPI/AsyncAPI: a new endpoint/event is declared in source code but not reflected in the spec.
- For ADR: a `## Decision Rationale` block in a story v2 markdown references `ADR-XXXX` but the ADR file does not exist under `docs/adr/`.
- For skill-docs: a modified or new SKILL.md lacks `## Triggers` or `## Examples`.

When `freshness-window-hours > 0`, a target is stale only if the grace period has elapsed since the code change was committed. Default `0` = any PR with code changes without a corresponding doc change is blocked immediately.

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `enforce-preflight-gates.sh` (EPIC-0063) | `git push`, `gh pr create`, `Skill x-create-pr` | Blocks if `x-validate-docs` evidence absent |
| **1 — Normative** | This rule + CLAUDE.md (EPIC-0071) | Every conversation | — |
| **2 — CI Script** | `audit-doc-freshness.sh` | PR open/sync to `develop` or `epic/*` | 1 `DOC_FRESHNESS_VIOLATION` |
| **3 — Java Test** | `Epic0071DocAsDoDSmokeIT` | `mvn verify` | JUnit assertion failure |

## Mandatory Invocation in `x-implement-story`

Phase 3 of `x-implement-story` MUST invoke both:

```
Skill(skill: "x-generate-docs", model: "sonnet", args: "<STORY-ID> --target-stack-aware")  [required]
Skill(skill: "x-validate-docs", model: "sonnet", args: "<STORY-ID>")                        [required]
```

Silent omission is a `PROTOCOL_VIOLATION` under Rule 24. The evidence artifact produced by `x-validate-docs` is `ai/epics/epic-XXXX/reports/doc-validate-report-STORY-ID.md` (template `_TEMPLATE-DOC-VALIDATE-REPORT.md`, delivered in story-0071-0002).

## `--skip-doc` Constraint

The `--skip-doc` flag on `x-implement-story` is permitted exclusively inside `## Recovery` blocks of the calling skill. Occurrence outside a Recovery block is caught by `scripts/audit-bypass-flags.sh` (Rule 45) and fails the CI build with `BYPASS_FLAG_VIOLATION`.

## Forbidden

- Invoking `x-implement-story` without a subsequent `x-validate-docs` call in Phase 3 (blocked at Camada 0 and Camada 2).
- Maintaining documentation targets outside the `documentation.targets` YAML block or auto-detection (no per-story override mechanism).
- Using `--skip-doc` outside a `## Recovery` block.
- Declaring a target in `documentation.targets` that is not in the canonical list above.

## Audit

`audit-doc-freshness.sh --self-check` MUST verify:
1. This rule file (`31-documentation-freshness-gate.md`) exists.
2. `capabilities/governance/doc-as-dod.yaml` exists and is valid against `governance/schemas/capabilities-1.0.json`.
3. `x-validate-docs/SKILL.md` is registered and references `RULE-031`.

Failure → `RULE_31_ENFORCEMENT_BROKEN`.

---

> **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)