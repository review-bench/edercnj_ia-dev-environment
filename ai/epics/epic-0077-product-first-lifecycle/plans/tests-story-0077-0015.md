# Test Plan — story-0077-0015

## Unit Tests

### C4CodeLevelValidatorTest
- `validate_withEmptyClasses_returnsViolation`
- `validate_withNoDomainClass_returnsViolation`
- `validate_withOutwardDependencyDomainToAdapter_returnsViolation`
- `validate_withValidHexagonalClasses_returnsOk`
- `validate_withApplicationDependingOnAdapter_returnsViolation`
- `codeEntry_withBlankClassName_throwsException`

### TaskC4CodePlannerTest
- `planCode_mermaid_returnsClassDiagram`
- `planCode_plantuml_returnsPackageDiagram`
- `planCode_nullTaskId_throwsException`
- `planCode_emptyClasses_throwsException`
- `planCode_htmlEscapingInTaskId`
- `planCode_levelIsCode`

### C4CodeRendererTest
- `render_returnsContent`
- `render_wrongLevel_throwsException`
- `renderHeader_containsLevelAndFormat`

## Smoke Test
- `TaskC4CodeSmokeTest` — 6 E2E scenarios covering validator + planner + renderer for both formats
