# Test Plan — story-0077-0002

**Story:** story-0077-0002 — ai/products/ Structure + ProductNumbering + Whitelist  
**Scope:** STANDARD

---

## Test Strategy

### Unit Tests — ProductNumbering

| Test | Scenario | Expected |
| :--- | :--- | :--- |
| `of_validSequence_returnsNumbering` | `ProductNumbering.of(1)` | `sequence=1` |
| `of_null_throwsIllegalArgument` | null sequence | `IllegalArgumentException` |
| `of_zero_throwsIllegalArgument` | sequence=0 | `IllegalArgumentException` |
| `of_negativeSequence_throwsIllegalArgument` | sequence=-1 | `IllegalArgumentException` |
| `of_maxSequence_returnsNumbering` | sequence=9999 | `sequence=9999` |
| `of_overMaxSequence_throwsIllegalArgument` | sequence=10000 | `IllegalArgumentException` |
| `formatted_sequence1_returnsProductId` | `of(1).formatted()` | `"product-0001"` |
| `formatted_sequence10_returnsProductId` | `of(10).formatted()` | `"product-0010"` |
| `formatted_sequence100_returnsProductId` | `of(100).formatted()` | `"product-0100"` |
| `formatted_sequence9999_returnsProductId` | `of(9999).formatted()` | `"product-9999"` |
| `next_sequence1_returns2` | `of(1).next()` | `sequence=2` |
| `next_sequence9999_throwsIllegalState` | `of(9999).next()` | `IllegalStateException` (overflow) |
| `equality_sameSequence_equal` | `of(1).equals(of(1))` | `true` |
| `equality_differentSequence_notEqual` | `of(1).equals(of(2))` | `false` |

### Unit Tests — CommitPathWhitelist

| Test | Scenario | Expected |
| :--- | :--- | :--- |
| `standard_containsAllExpectedPrefixes` | `standard()` | 6 prefixes present |
| `isAllowed_plansPath_returnsTrue` | `"plans/arch.md"` | `true` |
| `isAllowed_aiEpicsPath_returnsTrue` | `"ai/epics/epic-0077/..."` | `true` |
| `isAllowed_aiProductsPath_returnsTrue` | `"ai/products/product-0001/..."` | `true` |
| `isAllowed_aiMemoryPath_returnsTrue` | `"ai/memory/epic-0077-summary.md"` | `true` |
| `isAllowed_aiReleasesPath_returnsTrue` | `"ai/releases/release-state-5.2.0.json"` | `true` |
| `isAllowed_claudeTemplatesPath_returnsTrue` | `".claude/templates/FOO.md"` | `true` |
| `isAllowed_srcMainPath_returnsFalse` | `"src/main/java/..."` | `false` |
| `isAllowed_emptyPath_returnsFalse` | `""` | `false` |
| `isAllowed_nullPath_throwsIllegalArgument` | `null` | `IllegalArgumentException` |
| `of_nullPrefixes_throwsIllegalArgument` | `CommitPathWhitelist.of(null)` | `IllegalArgumentException` |
| `of_defensiveCopy_mutationDoesNotAffect` | mutate source set | whitelist unchanged |
| `equality_samePrefixes_equal` | two identical whitelists | `equals = true` |

## TDD Order (TPP — Simple → Complex)

1. RED: `of_null_throwsIllegalArgument` → GREEN: validate null → REFACTOR: extract validation
2. RED: `of_validSequence_returnsNumbering` → GREEN: constructor → REFACTOR: record
3. RED: `formatted_sequence1_returnsProductId` → GREEN: String.format → REFACTOR: none
4. RED: `next_sequence1_returns2` → GREEN: return of(sequence+1) → REFACTOR: none
5. RED: `next_sequence9999_throwsIllegalState` → GREEN: overflow guard → REFACTOR: none
6. RED: `standard_containsAllExpectedPrefixes` → GREEN: Set.of(6 prefixes) → REFACTOR: constant
7. RED: `isAllowed_plansPath_returnsTrue` → GREEN: startsWith loop → REFACTOR: stream

## Coverage Target

| Class | Line | Branch |
| :--- | :--- | :--- |
| `ProductNumbering` | ≥ 95% | ≥ 90% |
| `CommitPathWhitelist` | ≥ 95% | ≥ 90% |

## Test File

- `src/test/java/dev/iadev/domain/products/ProductNumberingTest.java`
- `src/test/java/dev/iadev/domain/products/CommitPathWhitelistTest.java`
