---
generated-by: x-internal-story-build-plan@1bb391629efbed76158e524466d207669d540e94
story-id: story-0062-0005
epic-id: EPIC-0062
---
# arch — story-0062-0005

**Story:** story-0062-0005 — Update Java assemblers + FileCategorizer for v4 layout
**Scope:** STANDARD (5 assemblers + FileCategorizer + golden regen + symlink removal)
**Status:** DONE

## Architecture Decisions

- `DocsAdrAssembler` now emits to `docs/adr/` (v4 canonical path)
- `DocsContributingAssembler` now emits to `docs/specs/_templates/` (v4 specs path)
- `ReleaseChecklistAssembler` now emits to `ai/releases/` (v4 releases path)
- `SloSliTemplateAssembler` now emits to `governance/slo-sli/` (v4 governance path)
- `DataMigrationPlanAssembler` now emits to `governance/migrations/` (v4 governance path)
- `PathResolver` gains 4 public string constants: `DOCS_ADR_DIR`, `DOCS_SPECS_DIR`, `GOVERNANCE_BASELINES_DIR`, `AI_RELEASES_DIR`
- `FileCategorizer` drops v3 `adr/` and `specs/` entries; v4 entries (`docs/adr/`, `docs/specs/`) already present from EPIC-0060
- Transitional symlinks `adr → docs/adr` and `specs → docs/specs` removed from repository root
- 11 golden profiles regenerated to reflect new output paths
