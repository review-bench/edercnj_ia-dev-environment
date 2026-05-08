# Implementation Plan — story-0067-0001 (Foundation: JSON Schema + Templates)

**Story:** story-0067-0001
**Epic:** EPIC-0067
**Scope:** SIMPLE
**PlanningMode:** PRE_PLANNED (story spec provides full contracts)

## Tasks

### TASK-0067-0001-001 — Create `governance/schemas/review-frontmatter-1.0.json`

**File:** `governance/schemas/review-frontmatter-1.0.json` (NEW)
**Branch:** `feat/task-0067-0001-001-schema-create`

JSON Schema Draft 2020-12 with 10 required fields + 2 optional. Exact content from story §3.1.

### TASK-0067-0001-002 — Modify `_TEMPLATE-SPECIALIST-REVIEW.md`

**File:** `src/main/resources/shared/templates/_TEMPLATE-SPECIALIST-REVIEW.md` (MODIFIED)
**Branch:** `feat/task-0067-0001-002-template-specialist`

Prepend `<!-- template-version: 1.0 -->` + YAML frontmatter block at top. Preserve existing prose body.

### TASK-0067-0001-003 — Modify `_TEMPLATE-TECH-LEAD-REVIEW.md`

**File:** `src/main/resources/shared/templates/_TEMPLATE-TECH-LEAD-REVIEW.md` (MODIFIED)
**Branch:** `feat/task-0067-0001-003-template-techlead`

Same pattern; uses `x-review-pr@`, `score-max: 55`, `checklist` instead of `reviewers`.

### TASK-0067-0001-004 — Create `ReviewFrontmatterSchemaTest.java`

**File:** `src/test/java/dev/iadev/governance/ReviewFrontmatterSchemaTest.java` (NEW)
**Branch:** `feat/task-0067-0001-004-schema-test`

JUnit 5 test. Validates schema existence, JSON validity, presence of required meta-schema fields, and 10 required field names.

### TASK-0067-0001-005 — Regenerate golden files + verify GREEN

**Branch:** `feat/task-0067-0001-005-regen-goldens`

Run `mvn test` to trigger `GoldenFileRegenerator`. Commit 20 updated golden files (10 profiles × 2 templates).

## Execution Order

001 → 002 → 003 → 004 → 005 (sequential — each depends on 001 for schema existence validation in test)
