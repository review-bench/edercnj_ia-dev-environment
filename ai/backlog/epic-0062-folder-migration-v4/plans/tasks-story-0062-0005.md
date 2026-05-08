---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0005
epic-id: EPIC-0062
---
# Task Breakdown — story-0062-0005

| Task | Description | Status |
| :--- | :--- | :--- |
| task-0062-0005-001 | Update 5 assemblers (DocsAdrAssembler, DocsContributingAssembler, ReleaseChecklistAssembler, SloSliTemplateAssembler, DataMigrationPlanAssembler) to v4 output paths | DONE |
| task-0062-0005-002 | Add DOCS_ADR_DIR, DOCS_SPECS_DIR, GOVERNANCE_BASELINES_DIR, AI_RELEASES_DIR constants to PathResolver | DONE |
| task-0062-0005-003 | Remove v3 `adr/` and `specs/` entries from FileCategorizer | DONE |
| task-0062-0005-004 | Remove transitional symlinks adr/ and specs/ from repo root via git rm -r | DONE |
| task-0062-0005-005 | Regenerate 9+1 golden profiles via GoldenFileRegenerator | DONE |
| task-0062-0005-006 | mvn test — 3992 tests GREEN, 0 failures | DONE |
| task-0062-0005-007 | Update unit tests for 5 assemblers + CliDisplayTest | DONE |
