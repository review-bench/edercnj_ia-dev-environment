# Implementation Plan — story-0076-0001

**Story:** Gramática verb-first e ADR do rename  
**Epic:** EPIC-0076  
**Date:** 2026-05-03

## Scope

Create ADR-0029 (Verb-First Skill Naming Convention) documenting the canonical grammar, exception criteria, and hard-cut strategy for EPIC-0076.

## Files Changed

- `docs/adr/ADR-0029-verb-first-skill-naming.md` — created
- `docs/adr/README.md` — added ADR-0029 entry
- `ai/epics/epic-0076-verb-first-skill-naming-refactor/story-0076-0001.md` — status update

## ADR Decisions

- Grammar: `x-<verb>-<object>` (public), `x-internal-<verb>-<object>` (internal), `x-lib-<verb>-<object>` (lib)
- No aliases / no deprecation window — hard-cut per cluster
- Exceptions: skills already verb-first (`x-setup-env`, `x-setup-stack`, `x-release`, `x-review-pr`) retain names
- Knowledge packs (`*-kp`) excluded from scope
- CI guard via `scripts/audit-legacy-skill-names.sh` (delivered by story-0076-0007)

## DoD Check

- [x] ADR created at canonical path `docs/adr/ADR-0029-verb-first-skill-naming.md`
- [x] ADR README updated with new entry
- [x] Grammar (public/internal/lib) documented
- [x] Exception criteria documented
- [x] ADR supersedes naming decisions of ADR-0003
