# Architecture Plan — story-0064-0008

**Status:** Concluída
**Story:** story-0064-0008 — CHANGELOG seed `[Unreleased] [Breaking]`
**Epic:** EPIC-0064
**Scope:** SIMPLE
**Author:** x-arch-plan (inline, SIMPLE scope)

## 1. Context

CHANGELOG.md is the single artifact. No new domain classes, no assembler changes, no
schema changes. The change is a documentation-only seed entry for EPIC-0064.

## 2. Packages Touched

| Layer | Package | Change |
|---|---|---|
| — | CHANGELOG.md (root) | Add `[Unreleased] ### Breaking` seed entry |

All other layers: —

## 3. Architecture Decision

No ADR required. The `[Unreleased]` `### Breaking` entry is a changelog convention
(Keep a Changelog). It does not introduce any new classes, interfaces, or configuration.

## 4. Rationale

Early disclosure of EPIC-0064's breaking nature (schema YAML v2→v3.0) via CHANGELOG
ensures downstream consumers of `ia-dev-env` can track the approaching major bump without
waiting for Phase 7 (story-0064-0705) when the version is finalized.

## 5. Implementation Order

1. Verify `## [Unreleased]` section exists in CHANGELOG.md.
2. Under `[Unreleased]`, verify `### Breaking` subsection exists with EPIC-0064 entry.
3. If absent: add ~10 lines. If present: confirm text satisfies all AC.

## 6. File Footprint

```
write:
  - CHANGELOG.md
read:
  - docs/adr/ADR-0016-capability-driven-composition.md
regen: []
```
