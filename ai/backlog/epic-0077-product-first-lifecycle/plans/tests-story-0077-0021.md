# Test Plan — story-0077-0021

## Acceptance Criteria (from story)

```gherkin
Cenário: x-create-product rejeita produto com < 10 RNF categorias
  DADO que PM tenta criar produto com apenas 8 RNF categories
  QUANDO x-create-product valida
  ENTÃO erro "Minimum 10 RNF categories required (6 mandatory + 4 optional minimum)"
  E produto não é criado até PM adiciona 2+ mais categorias
```

## Unit Tests — RNFRootValidatorTest

| Test method | Scenario | Expected |
|-------------|----------|----------|
| `validate_with9Categories_failsWithMinTotalError` | 9 RNF roots (6 mandatory + 3 optional) | failure, error contains "Minimum 10 RNF categories required" |
| `validate_with10Categories_passes` | 10 RNF roots (6 mandatory + 4 optional) | success |
| `validate_with12Categories_passes` | 12 RNF roots (all categories) | success |
| `validate_with5MandatoryCategories_failsBothChecks` | Only 5 mandatory categories | failure; both mandatory-missing AND total-count errors |

## Integration Tests — CreateProductOrchestrationUseCaseIT

| Test method | Scenario | Expected |
|-------------|----------|----------|
| `execute_whenProductHasLessThan10Rnf_returnsFailure` | Stub transformer producing 8 RNF roots | `result.successful() == false` |
| `execute_whenProductHasLessThan10Rnf_containsMinCategoryError` | Same as above | `result.validationErrors()` contains "Minimum 10" |
| `execute_withValidIdeation_resultContainsTwelveOrMoreRnfRoots` | Existing test (preserved) | passes with ≥12 |

## Smoke Test
No separate smoke test needed — existing `XCreateProductE2ETest` + new IT coverage satisfies gate.

## Coverage Target
Line ≥ 95%, Branch ≥ 90% — all new branches in `validateTotalCount` covered.
