# Task Breakdown — story-0077-0002

**Story:** story-0077-0002 — ai/products/ Structure + ProductNumbering + Whitelist  
**Scope:** STANDARD  
**Tasks:** 3

---

## TASK-0077-0002-001: ai/products/ Directory Scaffold

**Branch:** `feat/task-0077-0002-001-products-dir-scaffold`  
**Size:** S  
**Dependencies:** none

### Files

```
write:
  - ai/products/.gitkeep
  - ai/products/product-0000/_PRODUCT.md
  - ai/products/product-0000/capabilities/.gitkeep
  - ai/products/product-0000/features/.gitkeep
  - ai/products/product-0000/stories/.gitkeep
```

### Acceptance Criteria

- `ai/products/` directory tracked via `.gitkeep`
- `product-0000/` sentinel directory with 3 child subdirs
- `_PRODUCT.md` contains template format documentation

---

## TASK-0077-0002-002: ProductNumbering + CommitPathWhitelist Domain Records

**Branch:** `feat/task-0077-0002-002-domain-records`  
**Size:** M  
**Dependencies:** TASK-0077-0002-001

### Files

```
write:
  - src/main/java/dev/iadev/domain/products/ProductNumbering.java
  - src/main/java/dev/iadev/domain/products/CommitPathWhitelist.java
  - src/test/java/dev/iadev/domain/products/ProductNumberingTest.java
  - src/test/java/dev/iadev/domain/products/CommitPathWhitelistTest.java
```

### Acceptance Criteria

- `ProductNumbering.of(1).formatted()` → `"product-0001"`
- `ProductNumbering.of(9999).next()` → `IllegalStateException`
- `CommitPathWhitelist.standard().isAllowed("ai/products/x")` → `true`
- `CommitPathWhitelist.standard().isAllowed("src/main/x")` → `false`
- All 27 unit tests pass; coverage ≥ 95% line / ≥ 90% branch
- Zero external imports in domain/products/ classes (Rule 04)

---

## TASK-0077-0002-003: x-commit-planning Whitelist Fix

**Branch:** `feat/task-0077-0002-003-commit-planning-whitelist`  
**Size:** S  
**Dependencies:** TASK-0077-0002-002

### Files

```
edit:
  - src/main/resources/targets/claude/skills/core/git/x-commit-planning/SKILL.md
```

### Acceptance Criteria

- `ai/epics/` added to allowed path prefixes
- `ai/products/` added to allowed path prefixes
- `ai/memory/` added to allowed path prefixes
- `ai/releases/` added to allowed path prefixes
- Existing `plans/` and `.claude/templates/` entries preserved
- No other changes to SKILL.md

---

## Story File Footprint Summary

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
