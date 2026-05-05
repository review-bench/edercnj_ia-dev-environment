# Deprecations

This file tracks skills, flags, and APIs that have been removed or renamed.
Ordered by removal date (most recent first).

---

## Removed in EPIC-0076 (Verb-First Skill Naming Refactor)

### `x-feature-create` → `x-create-feature`

| Field | Value |
| :--- | :--- |
| Old name | `x-feature-create` |
| New name | `x-create-feature` |
| Reason | Verb-first naming convention (EPIC-0076 ADR-0003) |
| Removed in | EPIC-0076 (released in v5.x) |
| Introduced by | EPIC-0065 (Feature Creation Chain) |
| Coordinated with | EPIC-0077 story-0077-0003 |

**Migration:** Replace all references to `x-feature-create` with `x-create-feature`. Skill arguments and behavior are unchanged — only the name changed. See [migration guide](docs/migration/x-feature-create-to-create-feature.md).

### `x-feature-ideate` → `x-ideate-feature`

| Field | Value |
| :--- | :--- |
| Old name | `x-feature-ideate` |
| New name | `x-ideate-feature` |
| Reason | Verb-first naming convention (EPIC-0076 ADR-0003) |
| Removed in | EPIC-0076 (released in v5.x) |

**Migration:** Replace `x-feature-ideate` with `x-ideate-feature`.

---

## Removed in earlier epics

### `x-epic-decompose` → `x-create-feature`

Replaced by EPIC-0065 (Feature Creation Chain). `x-create-feature` supersedes it with worktree isolation, consolidated docs/ branch commits, and auto-merged PR. See CHANGELOG.md §EPIC-0065.

### `x-epic-create`, `x-epic-map`, `x-story-create` → internal only

Hard-cut to `x-internal-create-epic`, `x-internal-map-epic`, `x-internal-create-story` by EPIC-0065. No longer user-invocable. Delegated internally by `x-create-feature`.

---

## Deprecated flags (within deprecation window)

### `--non-interactive` on `x-implement-story`, `x-implement-epic`, `x-release`

**Status:** Deprecated — use default (no flag) instead.  
**Reason:** EPIC-0061 flipped non-interactive as the default; `--non-interactive` is now a no-op that emits a warning. Removed in 2 releases after EPIC-0061.  
**Migration:** Remove `--non-interactive` flag; non-interactive is now the default behavior.
