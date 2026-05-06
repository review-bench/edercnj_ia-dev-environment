# Test Plan — story-0077-0005

**Story:** _TEMPLATE-PRODUCT.md (8 seções, RNFs Root)

## Unit Tests — RNFRootValidatorTest

| ID | Scenario | Expected |
| :--- | :--- | :--- |
| T1 | Complete product with 10+ RNF categories | passes |
| T2 | Missing SECURITY mandatory category | fails with specific error |
| T3 | Missing PERFORMANCE mandatory category | fails with specific error |
| T4 | Less than 6 mandatory categories present | fails with count error |
| T5 | Product with all 6 mandatory categories but no optionals | passes |
| T6 | Multiple missing mandatory categories | reports all errors |
| T7 | Product with null/blank RNF description | fails with content error |

## Integration Tests — RNFInheritanceUseCaseIT

| ID | Scenario | Expected |
| :--- | :--- | :--- |
| IT1 | Capability inherits all product RNFs | effective RNFs = product RNFs |
| IT2 | Capability marks SECURITY as no-relax | SECURITY cannot be overridden |
| IT3 | Cascade: product → capability → feature | all levels inherit consistently |

## Smoke Tests

| ID | Script | Expected |
| :--- | :--- | :--- |
| S1 | product-template-smoke.sh (--self-check) | exit 0 |
| S2 | product-template-smoke.sh on example-product-saas.md | PASS, all sections present |

## Coverage

- New domain classes: ≥95% line, ≥90% branch
- Integration test covers inheritance cascade
