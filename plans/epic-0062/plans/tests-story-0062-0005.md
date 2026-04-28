---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0005
epic-id: EPIC-0062
---
# Test Plan — story-0062-0005

**Story:** story-0062-0005 — Update Java assemblers + FileCategorizer for v4 layout
**Status:** DONE

## Test Results

- `DocsAdrAssemblerTest` — 17 tests GREEN (updated to `docs/adr/` paths)
- `DocsContributingAssemblerTest` — 13 tests GREEN (updated to `docs/specs/_templates/` paths)
- `ReleaseChecklistAssemblerTest` — 25 tests GREEN (updated to `ai/releases/` paths)
- `SloSliTemplateAssemblerTest` — tests GREEN (updated to `governance/slo-sli/` paths)
- `DataMigrationPlanAssemblerTest` — tests GREEN (updated to `governance/migrations/` paths)
- `FileCategorizerTest` — all GREEN (v3 `adr/` and `specs/` removed from rules)
- `CliDisplayTest` — fixed 2 tests using old `adr/` and `specs/` paths → now uses v4 equivalents
- `GoldenFileTest` — all profiles GREEN with regenerated fixtures
- **Total: 3992 tests, 0 failures**
