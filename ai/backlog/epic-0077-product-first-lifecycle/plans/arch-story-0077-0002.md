# Architecture Plan — story-0077-0002

**Story:** story-0077-0002 — ai/products/ Structure + ProductNumbering + Whitelist  
**Scope:** STANDARD  
**Planning Mode:** PRE_PLANNED

## Architecture Decisions

### 1. `ai/products/` Directory Structure

The `ai/products/` directory mirrors `ai/epics/` — a well-known filesystem layout for
product-level artifacts. Each product gets a `product-NNNN/` subdirectory with three
standard child directories: `capabilities/`, `features/`, `stories/`.

`product-0000/` is a reserved template directory (never used for a real product).

### 2. `ProductNumbering` — Domain Value Object

`ProductNumbering` is a simple value object representing the next sequential product ID.
It wraps an integer sequence (1–9999) and produces formatted string IDs (`product-0001`).

Package: `domain/products/` (extends the package authorized by ADR-0030).
Zero external dependencies (Rule 04 §Domain Purity).

### 3. x-commit-planning Whitelist Fix

The `x-commit-planning` skill's whitelist currently only allows `plans/` and `.claude/templates/`.
The v4 layout uses `ai/epics/` paths, which are rejected. This story adds:
- `ai/epics/` — fixes existing v4 gap (memory note: project_x-planning-commit_v4_gap)
- `ai/products/` — new product artifact directory
- `ai/memory/` — AI memory layer artifacts
- `ai/releases/` — release state files

### 4. `CommitPathWhitelist` — Domain Record

A pure domain record holding the set of allowed path prefixes for commit operations.
Used by the generator when assembling x-commit-planning skill configuration.
Package: `domain/products/`.

## File Footprint

```
write:
  - ai/products/.gitkeep
  - ai/products/product-0000/_PRODUCT.md
  - ai/products/product-0000/capabilities/.gitkeep
  - ai/products/product-0000/features/.gitkeep
  - ai/products/product-0000/stories/.gitkeep
  - src/main/java/dev/iadev/domain/products/ProductNumbering.java
  - src/main/java/dev/iadev/domain/products/CommitPathWhitelist.java
  - src/test/java/dev/iadev/domain/products/ProductNumberingTest.java
  - src/test/java/dev/iadev/domain/products/CommitPathWhitelistTest.java
edit:
  - src/main/resources/targets/claude/skills/core/git/x-commit-planning/SKILL.md
read:
  - src/main/resources/targets/claude/skills/core/git/x-commit-planning/SKILL.md
```
