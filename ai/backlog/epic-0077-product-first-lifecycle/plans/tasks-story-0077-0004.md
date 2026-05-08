# Task Breakdown — story-0077-0004

**Story:** story-0077-0004 — _TEMPLATE-IDEATION.md (7 seções)

---

## Tasks

| Task | Layer | Size | Deps | Branch |
|------|-------|------|------|--------|
| TASK-0077-0004-001 | Infrastructure/Doc | M | — | `feat/task-0077-0004-001-ideation-template` |
| TASK-0077-0004-002 | Domain/Application | M | 001 | `feat/task-0077-0004-002-ideation-validator` |
| TASK-0077-0004-003 | Test/CI | S | 002 | `feat/task-0077-0004-003-pilot-ideations` |

---

### TASK-0077-0004-001: Template estrutura + exemplos

**Files written:**
- `ai/templates/_TEMPLATE-IDEATION.md`
- `ai/examples/example-ideation-ecommerce.md`
- `ai/examples/example-ideation-saas.md`

**Acceptance criteria:**
- [ ] Template has exactly 7 sections with numbered headings
- [ ] Each section has instruction block + required fields
- [ ] 2 complete example ideations with realistic content
- [ ] Valid Markdown syntax (no broken headings or tables)

---

### TASK-0077-0004-002: Validador schema + integration

**Files written:**
- `src/main/java/dev/iadev/domain/ideation/IdeationSection.java`
- `src/main/java/dev/iadev/domain/ideation/IdeationTemplate.java`
- `src/main/java/dev/iadev/domain/ideation/IdeationValidator.java`
- `src/main/java/dev/iadev/domain/ideation/IdeationValidationResult.java`
- `src/main/java/dev/iadev/application/ideation/IdeationValidationUseCase.java`
- `src/test/java/dev/iadev/domain/ideation/IdeationValidatorTest.java`

**Acceptance criteria:**
- [ ] 7 unit test scenarios passing (T1-T7)
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%
- [ ] Domain classes have zero external dependencies (only java.util, java.lang)
- [ ] IdeationValidationResult is immutable record

---

### TASK-0077-0004-003: Pilot ideações + smoke test

**Files written:**
- `ai/examples/pilot-ideation-001.md`
- `ai/examples/pilot-ideation-002.md`
- `ai/examples/pilot-ideation-003.md`
- `ci/smoke/ideation-template-smoke.sh`

**Acceptance criteria:**
- [ ] 3 pilot ideations each with all 7 sections
- [ ] Smoke script exits 0 (PASS) for all 3 pilots
- [ ] Smoke script follows Rule 26 exit codes (0=OK, 1=violation, 2=error)
- [ ] Full test suite: `mvn test` → 0 failures

## File Footprint Summary

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
  - story-0077-0004.md
regen: []
```
