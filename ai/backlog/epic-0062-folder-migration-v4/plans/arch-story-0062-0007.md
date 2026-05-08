---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0007
epic-id: EPIC-0062
---

# Architecture Plan — story-0062-0007

## Summary

This story updates 6-7 rule source files under `java/src/main/resources/targets/claude/rules/`
to replace outdated path prefixes (`audits/`, bare `adr/`, bare `specs/`) with the canonical
v4-layout paths (`governance/baselines/`, `docs/adr/`, `docs/specs/`), then regenerates
11 golden fixture profiles.

## Impacted Components

- **Source-of-truth rules** (read-only text, no compiled classes):
  - `05-quality-gates.md` — `adr/` → `docs/adr/`
  - `24-execution-integrity.md` — `audits/` → `governance/baselines/` (3 occurrences)
  - `25-task-hierarchy.md` — `audits/` → `governance/baselines/` (3 occurrences)
  - `26-audit-gate-lifecycle.md` — `audits/` → `governance/baselines/` (2 occurrences)
  - `27-zero-bypass-lifecycle.md` — `audits/` → `governance/baselines/` (4 occurrences)

- **Generated outputs** (regenerated via Maven, not edited directly):
  - `.claude/rules/` — flat copy of source-of-truth rules
  - `src/test/resources/golden/**` — 11 fixture profiles

## Architectural Decision

Path references in rules must match the v4 layout introduced by EPIC-0060.
The `governance/baselines/` directory is the v4 canonical location for baseline files
(previously `audits/`). ADR documents moved from `adr/` to `docs/adr/`. No Java classes
are added or modified; this is a pure documentation update with golden fixture regen.

## Risk

Low. Changes are restricted to Markdown text in rule files and their generated copies.
Golden fixture regen is deterministic via `GoldenFileRegenerator`.
