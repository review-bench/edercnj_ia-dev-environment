# Architecture Plan — story-0077-0004

**Story:** story-0077-0004 — _TEMPLATE-IDEATION.md (7 seções)  
**Phase:** Phase 1 (Product-First Lifecycle)  
**Architect:** Padrões / Hexagonal

---

## 1. Context

Story-0077-0004 creates the `_TEMPLATE-IDEATION.md` with 7 canonical sections — the entry-point template for the Product-First Lifecycle. It also introduces Java domain classes that validate ideation documents, enabling downstream stories (story-0077-0009: x-create-product) to reliably process ideation input.

**Scope classification:** STANDARD — markdown templates + Java domain/application classes + unit tests + smoke CI script.

---

## 2. Current State Analysis

| Component | State | Action |
|-----------|-------|--------|
| `ai/templates/` directory | Does not exist | Create |
| `ai/examples/` directory | Does not exist | Create |
| `dev.iadev.domain.ideation` package | Does not exist | Create |
| `dev.iadev.application.ideation` package | Does not exist | Create |
| Ideation validation use case | Does not exist | Implement |

---

## 3. Target Architecture

### 3.1 Dependency Direction

```
adapter.inbound (CLI) → application.ideation → domain.ideation
                                                      ↑
                                                  (IdeationPort)
```

**Domain:** `dev.iadev.domain.ideation` — pure Java, no external deps.  
**Application:** `dev.iadev.application.ideation` — orchestrates validation.  
**Templates:** `ai/templates/` — workspace Markdown artifacts used by AI agents.

### 3.2 New Files

| File | Layer | Description |
|------|-------|-------------|
| `ai/templates/_TEMPLATE-IDEATION.md` | Workspace | 7-section canonical template |
| `ai/examples/example-ideation-ecommerce.md` | Workspace | E-commerce pilot ideation |
| `ai/examples/example-ideation-saas.md` | Workspace | SaaS pilot ideation |
| `ai/examples/pilot-ideation-001.md` | Workspace | Pilot 1: verified against schema |
| `ai/examples/pilot-ideation-002.md` | Workspace | Pilot 2: verified against schema |
| `ai/examples/pilot-ideation-003.md` | Workspace | Pilot 3: verified against schema |
| `src/main/java/dev/iadev/domain/ideation/IdeationSection.java` | Domain | Enum of 7 sections |
| `src/main/java/dev/iadev/domain/ideation/IdeationTemplate.java` | Domain | Value object representing template |
| `src/main/java/dev/iadev/domain/ideation/IdeationValidator.java` | Domain | Validation rules (section presence, field types) |
| `src/main/java/dev/iadev/domain/ideation/IdeationValidationResult.java` | Domain | Result VO: passed/failed + error list |
| `src/main/java/dev/iadev/application/ideation/IdeationValidationUseCase.java` | Application | Orchestrates parse + validate |
| `src/test/java/dev/iadev/domain/ideation/IdeationValidatorTest.java` | Test | Unit tests (7+ scenarios) |
| `ci/smoke/ideation-template-smoke.sh` | CI | Smoke: validates 3 pilot ideations pass |

### 3.3 IdeationSection Enum

```java
public enum IdeationSection {
    VISION_AND_SCOPE(1, "Visão & Escopo", true),
    STAKEHOLDERS(2, "Stakeholders & Personas", true),
    BUSINESS_REQUIREMENTS(3, "Requisitos de Negócio", true),
    CONSTRAINTS(4, "Restrições & Assunções", true),
    SUCCESS_CRITERIA(5, "Critérios de Sucesso", true),
    RISKS(6, "Riscos & Mitigação", true),
    ROADMAP(7, "Roadmap", true);
    // number, title, required
}
```

### 3.4 Invariants

- `IdeationTemplate` is a value object — immutable, no setters.
- Validation is stateless in `IdeationValidator` — no side effects.
- `IdeationValidationResult` carries all errors; does not throw.
- Domain never imports adapter or framework code (Rule 04).

---

## 4. File Footprint

```
write:
  - ai/templates/_TEMPLATE-IDEATION.md
  - ai/examples/example-ideation-ecommerce.md
  - ai/examples/example-ideation-saas.md
  - ai/examples/pilot-ideation-001.md
  - ai/examples/pilot-ideation-002.md
  - ai/examples/pilot-ideation-003.md
  - src/main/java/dev/iadev/domain/ideation/IdeationSection.java
  - src/main/java/dev/iadev/domain/ideation/IdeationTemplate.java
  - src/main/java/dev/iadev/domain/ideation/IdeationValidator.java
  - src/main/java/dev/iadev/domain/ideation/IdeationValidationResult.java
  - src/main/java/dev/iadev/application/ideation/IdeationValidationUseCase.java
  - src/test/java/dev/iadev/domain/ideation/IdeationValidatorTest.java
  - ci/smoke/ideation-template-smoke.sh
read:
  - ai/examples/ (existing ideations)
  - story-0077-0004.md
regen: []
```
