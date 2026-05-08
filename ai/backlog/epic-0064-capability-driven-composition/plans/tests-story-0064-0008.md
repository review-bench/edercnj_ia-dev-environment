# Test Plan — story-0064-0008

**Status:** Concluída
**Story:** story-0064-0008 — CHANGELOG seed `[Unreleased] [Breaking]`
**Epic:** EPIC-0064
**Scope:** SIMPLE

## Test Strategy

No Java unit tests required — CHANGELOG.md is a documentation file, not a code artifact.

Acceptance criteria are verified via manual grep inspection (part of story verification):

| Check | Command | Expected |
|---|---|---|
| `[Unreleased]` section | `grep -c "## \[Unreleased\]" CHANGELOG.md` | ≥ 1 |
| `### Breaking` present | `grep -c "### Breaking" CHANGELOG.md` | ≥ 1 |
| EPIC-0064 mention | `grep -c "EPIC-0064" CHANGELOG.md` | ≥ 1 |
| v3.0 schema mention | `grep -c "v3.0" CHANGELOG.md` | ≥ 1 |

## Regression Guard

The existing `LifecycleIntegrityAuditTest` does NOT scan CHANGELOG.md (it only scans
`plans/epic-XXXX/` planning artifact markdown files). No regression risk from this
story on the Java audit suite.

## Coverage Impact

0% coverage delta — no production Java code changed.
