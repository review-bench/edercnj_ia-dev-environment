---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0005
epic-id: EPIC-0062
---
# Implementation Plan — story-0062-0005

**Story:** story-0062-0005 — Update Java assemblers + FileCategorizer for v4 layout
**Status:** DONE

## Tasks Executed

| Task | Description | Status |
| :--- | :--- | :--- |
| task-0062-0005-001 | Update 5 Java assemblers to emit v4 paths | DONE |
| task-0062-0005-002 | Add constants to PathResolver | DONE |
| task-0062-0005-003 | Update FileCategorizer for v4 paths | DONE |
| task-0062-0005-004 | Remove transitional symlinks adr/ and specs/ | DONE |
| task-0062-0005-005 | Regenerate golden fixtures (9 profiles + platform) | DONE |
| task-0062-0005-006 | mvn test GREEN (3992 tests, 0 failures) | DONE |

## Files Modified

- `java/src/main/java/dev/iadev/application/assembler/DocsAdrAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/DocsContributingAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/ReleaseChecklistAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/SloSliTemplateAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/DataMigrationPlanAssembler.java`
- `java/src/main/java/dev/iadev/util/PathResolver.java`
- `java/src/main/java/dev/iadev/cli/FileCategorizer.java`
- `java/src/test/resources/golden/**` (9 profiles regenerated)
