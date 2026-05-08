# Implementation Plan — story-0077-0001

**Story:** Rule 19 update + 5 Product-First capabilities + ADR  
**Epic:** EPIC-0077  
**Phase:** 1B — Implementation  
**Scope:** STANDARD

---

## Tasks

### TASK-0077-0001-001: 5 Product-First capability YAML files

**Branch:** `feat/task-0077-0001-001-product-first-capabilities`

**Deliverables:**
- `capabilities/product-first/product-capability-transformation.yaml`
- `capabilities/product-first/c4-model-mandatory.yaml`
- `capabilities/product-first/rnf-validation.yaml`
- `capabilities/product-first/pentest-always-on.yaml`
- `capabilities/product-first/story-planning-v5.yaml`
- `capabilities/_index.yaml` — add `product-first` category

**Implementation notes:**
- Follow format of `capabilities/governance/ai-memory.yaml` (id, name, description, category, universal, requires, excludes)
- All 5 capabilities have `category: product-first`
- `universal: false` for all (opt-in via project YAML)
- No circular dependencies

### TASK-0077-0001-002: domain/products/ Java records

**Branch:** `feat/task-0077-0001-002-products-domain`

**Deliverables:**
- `src/main/java/dev/iadev/domain/products/ProductId.java` (record)
- `src/main/java/dev/iadev/domain/products/ProductStatus.java` (enum)
- `src/main/java/dev/iadev/domain/products/Product.java` (record)
- `src/test/java/dev/iadev/domain/products/ProductTest.java`

**Implementation notes:**
- `ProductId` — record wrapping String; validates non-blank; slug format `[a-z][a-z0-9-]*`
- `ProductStatus` — enum: `ACTIVE`, `DEPRECATED`, `DRAFT`
- `Product` — record with `ProductId id, String name, ProductStatus status`
- All Java standard library only (domain purity — Rule 04)

### TASK-0077-0001-003: domain/capabilities/ Java records + ADR-0031

**Branch:** `feat/task-0077-0001-003-capabilities-domain-adr`

**Deliverables:**
- `src/main/java/dev/iadev/domain/capabilities/CapabilityId.java` (record)
- `src/main/java/dev/iadev/domain/capabilities/Capability.java` (record)
- `src/test/java/dev/iadev/domain/capabilities/CapabilityTest.java`
- `docs/adr/ADR-0031-product-first-capability-registration.md`
- `docs/adr/README.md` — add ADR-0031 entry

**Implementation notes:**
- `CapabilityId` — record wrapping String; validates slug format (distinct from EPIC-0064's `domain/capability/CapabilityId`)
- `Capability` — record: `CapabilityId id, String name, String description`
- ADR-0031 documents the Product-First capability registration decision

### TASK-0077-0001-004: mvn test verification

**Branch:** same as TASK-0077-0001-003 or separate

**Deliverables:**
- mvn test: BUILD SUCCESS
- All new tests pass
- Execution-state.json: story-0077-0001 → done
