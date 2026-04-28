# Audit Gates Catalog — {{STACK}} Stack

Stack: {{STACK}}
Total Audits: {{TOTAL_AUDITS}}
Generated: automatically by `ia-dev-env generate`

This catalog lists exactly the audit scripts delivered to `.claude/scripts/` for this project.
Each audit runs as part of the CI governance pipeline via `bash .claude/scripts/audit-all.sh`.

---

## Audit Inventory

{{#each audit}}
### {{audit.name}}

| Field | Value |
| :--- | :--- |
| **Category** | {{audit.category}} |
| **Validates** | {{audit.validates}} |
| **Guarantees** | {{audit.guarantees}} |
| **Rule Anchor** | {{audit.ruleAnchor}} |

**Exit Codes:**

| Code | Constant | Meaning |
| :--- | :--- | :--- |
{{#each audit.exitCodes}}
| {{exitCode.code}} | `{{exitCode.constant}}` | {{exitCode.meaning}} |
{{/each}}

---
{{/each}}

## Notes

{{#if isDefaultStack}}
> ℹ Runtime audits require stack-specific knowledge — provide custom templates under
> `targets/claude/scripts/{stack}/` to enable runtime audit coverage for this stack.
{{/if}}

## Running All Audits

```bash
bash .claude/scripts/audit-all.sh
```

**Exit codes:** `0` = all pass | `1` = at least one failure | `2` = operational error
