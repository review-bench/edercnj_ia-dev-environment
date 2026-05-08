# Implementation Plan — story-0076-0002

**Story:** Inventário e matriz canônica pós-EPIC-0075  
**Epic:** EPIC-0076  
**Date:** 2026-05-03

## Scope

Audit actual skill catalog on disk against SPEC v1.1 matrix, reconcile discrepancies, and produce SPEC v1.2 as the definitive canonical reference for Phase 2 renames.

## Audit Findings

### On-disk catalog (2026-05-03)
- Total skill directories: 107 unique skill names
- Source of truth: `src/main/resources/targets/claude/skills/**`

### Skills on disk but NOT in SPEC v1.1 (2 missing)
| Skill | Category | Added to SPEC v1.2 section |
|-------|---------|--------------------------|
| `x-arch-system-update` | plan | 6.1 |
| `x-pentest-dynamic` | security | 6.7 |

### Skills in SPEC but NOT on disk (17 phantom — skip rename)
| Skill | Reason |
|-------|--------|
| `x-epic-create` | Converted to internal by EPIC-0065 |
| `x-story-create` | Converted to internal by EPIC-0065 |
| `x-epic-decompose` | Removed hard-cut by EPIC-0065 |
| `x-epic-map` | Converted to internal by EPIC-0065 |
| `x-test-property` | Not materialized by predecessor epics |
| `x-test-quality` | Not materialized |
| `x-test-regression-service` | Not materialized |
| `x-test-regression-self` | Not materialized |
| `x-doc-generate-v2` | Not materialized |
| `x-pr-body-render` | Not materialized by EPIC-0066 |
| `x-license-check` | Not materialized |
| `x-dep-validate-with-policy` | Not materialized |
| `x-internal-pr-body-render-backlog` | Not materialized |
| `x-internal-pr-body-render-impl` | Not materialized |
| `x-internal-pr-backlog-render` | Not materialized |
| `x-internal-doc-generate-step` | Not materialized |
| `x-internal-doc-validate-step` | Not materialized |

## Files Changed

- `docs/specs/SPEC-verb-first-skill-naming-v1.md` — bumped to v1.2, added 2 skills, marked 17 phantom

## DoD Check

- [x] SPEC updated to v1.2 as canonical reference
- [x] All skills on disk covered by the matrix (100%)
- [x] Knowledge packs explicitly excluded
- [x] Skills already verb-first annotated as "sem renome"
- [x] Phantom skills annotated with rationale
- [x] Matrix validated against `find src/main/resources/targets/claude/skills -type d -name "x-*"` 
