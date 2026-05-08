# Implementation Plan — story-0077-0000

**Story:** ADR Amendment — Rule 14 Extension for Product-First Runtime Domain

## Tasks

### TASK-0077-0000-001: Create ADR for Rule 14 Extension
- Create `docs/adr/ADR-0030-rule14-product-first-domain.md`
- Edit `docs/adr/README.md` — add ADR-0030 entry

### TASK-0077-0000-002: Amend Rule 14 with Product-First Extension
- Edit `src/main/resources/targets/claude/rules/14-project-scope.md` (source of truth)
- Edit `CHANGELOG.md` — add entry under [Added]
- Mirror to `.claude/rules/14-project-scope.md` (generated, not committed)

## Validation
- `audit-doc-freshness.sh`: ADR present in docs/adr/
- `docs/adr/README.md`: indexing ADR-0030
- Rule 14: contains `## Product-First Domain Extension (EPIC-0077)` with 4 authorized packages
