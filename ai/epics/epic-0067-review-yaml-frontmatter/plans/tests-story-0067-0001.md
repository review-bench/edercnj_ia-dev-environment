# Test Plan — story-0067-0001

**Story:** story-0067-0001
**Scope:** SIMPLE

## Acceptance Tests (Outer Loop)

1. `ReviewFrontmatterSchemaTest.schemaFile_exists_returnsTrue` — schema file present at expected path
2. `ReviewFrontmatterSchemaTest.schemaFile_isValidJson_parsesWithoutException` — JSON valid
3. `ReviewFrontmatterSchemaTest.schemaFile_hasMetaSchemaFields_allPresent` — `$schema`, `$id`, `required`, `properties`
4. `ReviewFrontmatterSchemaTest.requiredArray_hasAllTenFields_matchesSpec` — 10 required fields
5. `ReviewFrontmatterSchemaTest.generatedByPattern_restrictsToReviewSkills_patternCorrect` — regex contains `^(x-review|x-review-pr)@`
6. `GoldenFileTest` GREEN for all 20 template golden files (handled by existing test infrastructure)

## Unit Tests (Inner Loop — TPP Order)

| # | Transform | Test | Expected |
| :--- | :--- | :--- | :--- |
| 1 | nil | Schema absent → FileNotFound | assertion error with path |
| 2 | constant | Schema present → parse succeeds | no exception |
| 3 | scalar | `required` has 10 entries | size == 10 |
| 4 | collection | All 10 field names present | containsAll(...) |
| 5 | expression | `generated-by` pattern starts with `^(x-review|x-review-pr)@` | assertTrue |
