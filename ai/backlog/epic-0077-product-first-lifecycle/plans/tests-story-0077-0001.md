# Test Plan — story-0077-0001

**Story:** Rule 19 update + 5 Product-First capabilities + ADR  
**Epic:** EPIC-0077  
**Phase:** 1C — Test Plan

---

## Test Scenarios

### ProductId

| Test | Type | Priority |
| :--- | :--- | :--- |
| `of_validSlug_returnsProductId` | Unit | HIGH |
| `of_blankSlug_throwsIllegalArgument` | Unit | HIGH |
| `of_slugWithUpperCase_throwsIllegalArgument` | Unit | MEDIUM |
| `of_slugStartingWithDigit_throwsIllegalArgument` | Unit | MEDIUM |
| `equals_sameSlug_true` | Unit | HIGH |
| `equals_differentSlug_false` | Unit | MEDIUM |
| `toString_returnsSlug` | Unit | LOW |

### ProductStatus

| Test | Type | Priority |
| :--- | :--- | :--- |
| `values_containsActiveDeprecatedDraft` | Unit | HIGH |
| `valueOf_validName_returnsStatus` | Unit | HIGH |

### Product

| Test | Type | Priority |
| :--- | :--- | :--- |
| `of_validArgs_returnsProduct` | Unit | HIGH |
| `of_nullId_throwsNullPointer` | Unit | HIGH |
| `of_nullName_throwsNullPointer` | Unit | HIGH |
| `of_nullStatus_throwsNullPointer` | Unit | HIGH |
| `id_returnsProductId` | Unit | MEDIUM |
| `name_returnsName` | Unit | MEDIUM |
| `status_returnsStatus` | Unit | MEDIUM |

### domain/capabilities/CapabilityId

| Test | Type | Priority |
| :--- | :--- | :--- |
| `of_validSlug_returnsCapabilityId` | Unit | HIGH |
| `of_blank_throwsIllegalArgument` | Unit | HIGH |
| `equals_sameId_true` | Unit | HIGH |
| `toString_returnsValue` | Unit | LOW |

### domain/capabilities/Capability

| Test | Type | Priority |
| :--- | :--- | :--- |
| `of_validArgs_returnsCapability` | Unit | HIGH |
| `of_nullId_throws` | Unit | HIGH |
| `of_nullName_throws` | Unit | HIGH |

## Coverage Target

- Line: ≥ 95% on new classes
- Branch: ≥ 90% on new classes
- All new tests follow `methodName_scenario_expectedBehavior` naming (Rule 05)
