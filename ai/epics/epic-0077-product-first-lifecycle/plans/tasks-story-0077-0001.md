# Task Breakdown — story-0077-0001

**Story:** Rule 19 update + 5 Product-First capabilities + ADR  
**Epic:** EPIC-0077

---

## Tasks

| Task | Description | Size | Branch |
| :--- | :--- | :--- | :--- |
| TASK-0077-0001-001 | 5 Product-First capability YAML files + _index.yaml update | S | `feat/task-0077-0001-001-product-first-capabilities` |
| TASK-0077-0001-002 | domain/products/ Java records (Product, ProductId, ProductStatus) + tests | M | `feat/task-0077-0001-002-products-domain` |
| TASK-0077-0001-003 | domain/capabilities/ Java records (Capability, CapabilityId) + ADR-0031 | M | `feat/task-0077-0001-003-capabilities-domain-adr` |

## File Footprint

```
write:
  - capabilities/product-first/product-capability-transformation.yaml
  - capabilities/product-first/c4-model-mandatory.yaml
  - capabilities/product-first/rnf-validation.yaml
  - capabilities/product-first/pentest-always-on.yaml
  - capabilities/product-first/story-planning-v5.yaml
  - capabilities/_index.yaml
  - src/main/java/dev/iadev/domain/products/ProductId.java
  - src/main/java/dev/iadev/domain/products/ProductStatus.java
  - src/main/java/dev/iadev/domain/products/Product.java
  - src/main/java/dev/iadev/domain/capabilities/CapabilityId.java
  - src/main/java/dev/iadev/domain/capabilities/Capability.java
  - src/test/java/dev/iadev/domain/products/ProductTest.java
  - src/test/java/dev/iadev/domain/capabilities/CapabilityTest.java
  - docs/adr/ADR-0031-product-first-capability-registration.md
  - docs/adr/README.md
read:
  - capabilities/governance/ai-memory.yaml
  - capabilities/_index.yaml
  - src/main/java/dev/iadev/domain/capability/CapabilityId.java
```
