# Architecture Plan — story-0067-0001 (Foundation: JSON Schema + Templates)

**Story:** story-0067-0001
**Epic:** EPIC-0067 — Review YAML Frontmatter
**Date:** 2026-04-29
**Scope:** SIMPLE

## Component Overview

Three new/modified artifacts:

1. **`governance/schemas/review-frontmatter-1.0.json`** — JSON Schema Draft 2020-12. Source of truth for the `review-frontmatter-1.0` contract. Used by `audit-review-frontmatter.sh` (story-0067-0004) for validation.

2. **`src/main/resources/shared/templates/_TEMPLATE-SPECIALIST-REVIEW.md`** — Modified to prepend YAML frontmatter block with handlebars placeholders. Body prose preserved intact below the closing `---` delimiter.

3. **`src/main/resources/shared/templates/_TEMPLATE-TECH-LEAD-REVIEW.md`** — Same modification pattern; uses `x-review-pr@` prefix and `checklist` field instead of `reviewers`.

## Dependency Direction

```
ReviewFrontmatterSchemaTest
    ↓ reads
governance/schemas/review-frontmatter-1.0.json

PlanTemplatesAssembler (existing, unchanged)
    ↓ copies
_TEMPLATE-SPECIALIST-REVIEW.md + _TEMPLATE-TECH-LEAD-REVIEW.md
    ↓ propagates to
.claude/templates/ + golden fixtures (auto-regen)
```

## Layer Placement

| Artifact | Layer | Notes |
| :--- | :--- | :--- |
| `governance/schemas/review-frontmatter-1.0.json` | Governance | Static JSON Schema; no Java dependency |
| `_TEMPLATE-SPECIALIST-REVIEW.md` | Resources (shared/templates) | Handlebars `{{...}}` resolved by skills at runtime |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | Resources (shared/templates) | Same |
| `ReviewFrontmatterSchemaTest.java` | Test | `src/test/java/dev/iadev/governance/` package |

## Decision

**No new Java production classes required.** Schema is a static JSON file; templates are static Markdown with handlebars. Test validates schema structure only. `PlanTemplatesAssembler` already handles template propagation — no assembler change in this story.
