# Test Plan — story-0077-0004

**Story:** story-0077-0004 — _TEMPLATE-IDEATION.md (7 seções)

---

## Test Matrix

| ID | Type | Scenario | Expected |
|----|------|----------|----------|
| T1 | Unit | Complete ideation (all 7 sections, required fields filled) | `passed=true`, 0 errors |
| T2 | Unit | Missing section 5 (Critérios de Sucesso) | `passed=false`, error "Section 5 missing: CRITÉRIOS_DE_SUCESSO" |
| T3 | Unit | Empty title in section 1 | `passed=false`, error "Section 1 title must not be blank" |
| T4 | Unit | Fewer than 5 business requirements (section 3) | `passed=false`, error "Section 3: minimum 5 requirements required" |
| T5 | Unit | 100+ stakeholders in section 2 | `passed=true` — boundary case, large list still valid |
| T6 | Unit | All 7 sections present, required content | `passed=true`, no errors |
| T7 | Unit | Multiple violations (missing section 6 + empty title) | `passed=false`, 2 errors returned |
| T8 | Smoke | Pilot-001 (FinTech) validated by smoke script | Exit 0, "PASS" for all 7 sections |
| T9 | Smoke | Pilot-002 (Healthcare) validated by smoke script | Exit 0, "PASS" |
| T10 | Smoke | Pilot-003 (EdTech) validated by smoke script | Exit 0, "PASS" |
| D1 | Doc | `_TEMPLATE-IDEATION.md` has exactly 7 section headings | Section count = 7 |
| D2 | Doc | Example `ecommerce` ideation has all 7 sections | All headings present |
| D3 | Doc | Example `saas` ideation has all 7 sections | All headings present |

---

## Coverage Requirements

| Metric | Target |
|--------|--------|
| Line coverage | ≥ 95% |
| Branch coverage | ≥ 90% |

---

## Test Class

**`dev.iadev.domain.ideation.IdeationValidatorTest`**

```java
class IdeationValidatorTest {
    // T1: completeIdeation_allSections_passes
    // T2: missingSection5_reportsSpecificError
    // T3: emptyTitle_reportsError
    // T4: belowMinimumRequirements_failsCount
    // T5: largeStakeholderList_stillValid
    // T6: allSectionsPresent_noErrors (alias of T1 from use-case perspective)
    // T7: multipleViolations_reportsAll
}
```

---

## Smoke Script Contract

`ci/smoke/ideation-template-smoke.sh` — exit 0 = PASS, 1 = FAIL

Checks per pilot file:
1. Section 1 heading present (`## 1. Visão`)
2. Section 2 heading present (`## 2. Stakeholders`)
3. Section 3 heading present (`## 3. Requisitos`)
4. Section 4 heading present (`## 4. Restrições`)
5. Section 5 heading present (`## 5. Critérios`)
6. Section 6 heading present (`## 6. Riscos`)
7. Section 7 heading present (`## 7. Roadmap`)
