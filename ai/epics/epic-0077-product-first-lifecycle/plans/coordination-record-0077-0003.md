# Coordination Record — story-0077-0003

**Date:** 2026-05-04  
**Parties:** EPIC-0065 (Feature Creation Chain), EPIC-0076 (Verb-First Naming Refactor), EPIC-0077 (Product-First Lifecycle)

---

## Context

story-0077-0003 was scoped to coordinate a naming conflict between:

- **EPIC-0065's** feature-creation skill (originally named `x-feature-create`)
- **EPIC-0077's** planned skill to create a Product Feature from a Product Capability (which would conflict if also named `x-feature-create` or `x-create-feature`)

## Outcome

**EPIC-0076 (Verb-First Skill Naming Refactor) resolved the conflict before this story executed.**

EPIC-0076 renamed `x-feature-create → x-create-feature` as part of its verb-first naming pass. This rename:

1. Puts the verb first (`create`) consistent with all other skills in the catalog
2. Distinguishes EPIC-0065's skill (`x-create-feature` = create feature from spec/ideation file) from any future EPIC-0077 skill

## EPIC-0077 Naming Decision

EPIC-0077's "create a Product Feature from a Product Capability" concept will use a DISTINCT name when implemented (later stories), chosen to not conflict with `x-create-feature`. Candidate names (to be finalized in the relevant story):

- `x-create-product-feature` — explicitly scoped to product domain
- `x-link-capability-feature` — describes the relationship being created

## Verification Status

As of 2026-05-04:

- `x-feature-create` directory: **absent** from `.claude/skills/` ✓
- `x-create-feature` directory: **present** at `.claude/skills/x-create-feature/` ✓
- Legacy references in active skill paths: **0** (verified by `audit-skill-references.sh`) ✓

## Cross-Reference

| Epic | Status | Relevance |
| :--- | :--- | :--- |
| EPIC-0065 | Concluded | Created `x-create-feature` |
| EPIC-0076 | Concluded | Renamed `x-feature-create → x-create-feature` |
| EPIC-0077 | In Progress | Acknowledges rename; will use distinct name for Feature-from-Capability |
