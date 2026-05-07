# Rule {{RULE_NUMBER}} — {{RULE_NAME}}

> **Related:** Rule {{RELATED_RULE_1}} ({{RELATED_RULE_1_NAME}}), Rule {{RELATED_RULE_2}} ({{RELATED_RULE_2_NAME}}).
> **Introduced by:** {{EPIC_ID}} ({{EPIC_NAME}}).
> **ADR:** [ADR-{{ADR_NUMBER}} — {{ADR_NAME}}](../../docs/adr/ADR-{{ADR_NUMBER}}-{{ADR_SLUG}}.md).
> **Capability:** `{{CAPABILITY_ID}}` (`capabilities/{{CAPABILITY_PATH}}.yaml`).

## Purpose

{{ONE_PARAGRAPH_WHAT_AND_WHY}}

## Invariants

- MUST {{INVARIANT_1}}.
- MUST NOT {{INVARIANT_2}}.
- NEVER {{INVARIANT_3}}.

## Forbidden

- {{FORBIDDEN_1}}.
- {{FORBIDDEN_2}}.

## Enforcement

| Camada | Mechanism | Trigger | Exit |
| :--- | :--- | :--- | :--- |
| **0 — PreToolUse** | `{{HOOK_SCRIPT}}` | {{HOOK_TRIGGER}} | {{HOOK_EXIT}} |
| **1 — Normative** | This rule + CLAUDE.md | Every conversation | — |
| **2 — CI Script** | `{{AUDIT_SCRIPT}}` | PR open/sync to `develop` or `epic/*` | 1 `{{VIOLATION_CODE}}` |
| **3 — Java Test** | `{{JAVA_TEST_CLASS}}` | `mvn verify` | JUnit assertion failure |

## Reference

> **ADR:** [ADR-{{ADR_NUMBER}}](../../docs/adr/ADR-{{ADR_NUMBER}}-{{ADR_SLUG}}.md) · **KP:** [`knowledge/{{KP_PATH}}/index.md`](../../.claude/knowledge/{{KP_PATH}}/index.md) · **Catalogado em:** [`docs/audit-gates-catalog.md`](../../docs/audit-gates-catalog.md)
