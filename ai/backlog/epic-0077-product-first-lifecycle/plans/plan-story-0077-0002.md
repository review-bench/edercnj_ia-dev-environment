# Implementation Plan — story-0077-0002

**Story:** story-0077-0002 — ai/products/ Structure + ProductNumbering + Whitelist  
**Scope:** STANDARD  
**Planning Mode:** PRE_PLANNED

---

## Overview

Three deliverables:
1. `ai/products/` directory scaffold — template directories and `.gitkeep` files
2. `ProductNumbering` + `CommitPathWhitelist` — domain value objects in `domain/products/`
3. x-commit-planning SKILL.md whitelist fix — add `ai/epics/`, `ai/products/`, `ai/memory/`, `ai/releases/`

## Implementation Sequence

### Phase A: Directory Scaffold (TASK-0077-0002-001)

Create the `ai/products/` canonical directory layout:

```
ai/products/
├── .gitkeep
└── product-0000/
    ├── _PRODUCT.md          # template/sentinel for structure
    ├── capabilities/
    │   └── .gitkeep
    ├── features/
    │   └── .gitkeep
    └── stories/
        └── .gitkeep
```

`product-0000` is the reserved template directory — never assigned to a real product.
`_PRODUCT.md` documents the template format expected by `x-create-product`.

### Phase B: Domain Records (TASK-0077-0002-002)

**`ProductNumbering`** — value object in `domain/products/`:
- Wraps an integer `sequence` (1–9999)
- `formatted()` returns `"product-NNNN"` zero-padded to 4 digits
- `next()` returns a new `ProductNumbering` with `sequence + 1`
- Validates range at construction (1–9999)
- Zero external dependencies (Rule 04 §Domain Purity)

**`CommitPathWhitelist`** — domain record in `domain/products/`:
- Immutable `Set<String>` of allowed path prefixes
- `isAllowed(String path)` returns true if any prefix matches
- Factory method `standard()` returns the set: `plans/`, `.claude/templates/`, `ai/epics/`, `ai/products/`, `ai/memory/`, `ai/releases/`
- Defensive copy on construction

Tests: TDD Red→Green→Refactor; coverage ≥ 95% line / ≥ 90% branch.

### Phase C: Whitelist Fix (TASK-0077-0002-003)

Edit `src/main/resources/targets/claude/skills/core/git/x-commit-planning/SKILL.md`:

Find the current whitelist validation block and add 4 new allowed prefixes:
- `ai/epics/` — fixes existing v4 layout gap
- `ai/products/` — new product artifact directory
- `ai/memory/` — AI memory layer artifacts
- `ai/releases/` — release state files

The edit must preserve the existing `plans/` and `.claude/templates/` entries.

## Layer Mapping

| Deliverable | Layer | Package |
| :--- | :--- | :--- |
| ai/products/ scaffold | Filesystem artifact | — |
| ProductNumbering | Domain | `domain/products/` |
| CommitPathWhitelist | Domain | `domain/products/` |
| SKILL.md edit | Resource file | `resources/targets/claude/skills/core/git/` |

## Dependency Order

```
TASK-001 (scaffold) → TASK-002 (domain records) → TASK-003 (SKILL.md edit)
```

TASK-003 depends on TASK-002 only logically (CommitPathWhitelist defines the canonical set that the SKILL.md whitelist must reflect).

## Exit Criteria

- `ai/products/` directory exists with sentinel `product-0000/`
- `ProductNumbering.of(1).formatted()` returns `"product-0001"`
- `CommitPathWhitelist.standard().isAllowed("ai/products/product-0001/foo")` returns `true`
- x-commit-planning SKILL.md allows `ai/epics/`, `ai/products/`, `ai/memory/`, `ai/releases/`
- `mvn test` green; coverage ≥ 95% line / ≥ 90% branch
