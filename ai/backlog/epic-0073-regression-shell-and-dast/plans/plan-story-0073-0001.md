# Implementation Plan — story-0073-0001

**Story:** Schema YAML `quality.{regression,dast}` + sub-records Java + capabilities families + ADR
**Epic:** EPIC-0073
**Scope:** STANDARD
**ADR Number:** 0026 (verified: next free slot after ADR-0025; palpite 0022 was taken)
**EPIC-0072 Status:** merged → extending existing `QualityConfig`
**D-R3 Decision:** dispensar Rule nova (Rules 06/24/26/28 cover all invariants)

---

## Dependency Analysis

- QualityConfig.java exists (EPIC-0072) → extend with `RegressionConfig` + `DastConfig`
- capabilities/quality/{performance,mutation,contract}/ exist → add regression/ + dast/
- Governance.fromMap() already calls ProjectConfig.parseQuality() → no change needed there
- MapHelper utilities (optionalBoolean, optionalString, optionalMap, optionalInt) all available

---

## File Footprint

**write:**
- `capabilities/quality/regression/self.yaml` (NEW)
- `capabilities/quality/regression/service.yaml` (NEW)
- `capabilities/quality/dast/zap-passive.yaml` (NEW)
- `capabilities/quality/dast/zap-active.yaml` (NEW)
- `capabilities/quality/dast/nuclei.yaml` (NEW)
- `capabilities/_index.yaml` (MODIFY — append 5 IDs to quality category)
- `src/main/java/dev/iadev/domain/model/QualityConfig.java` (MODIFY — add RegressionConfig + DastConfig)
- `src/test/java/dev/iadev/config/ConfigLoaderTest.java` (MODIFY — add quality.regression + quality.dast test cases)
- `docs/adr/ADR-0026-regression-shell-and-dast.md` (NEW)
- `docs/audit-gates-catalog.md` (MODIFY — append reserved entry for audit-regression-shell.sh)

**read:** Governance.java, MapHelper.java, QualityConfig.java, ConfigLoader.java

---

## Task Sequence

1. **task-0073-0001-001** — Verify ADR number (0026 confirmed)
2. **task-0073-0001-002** — D-R3 decision: dispensar Rule (confirmed)
3. **task-0073-0001-003** — Create capabilities/quality/regression/{self,service}.yaml
4. **task-0073-0001-004** — Create capabilities/quality/dast/{zap-passive,zap-active,nuclei}.yaml
5. **task-0073-0001-005** — Update capabilities/_index.yaml
6. **task-0073-0001-006** — Extend QualityConfig.java with RegressionConfig + DastConfig
7. **task-0073-0001-007** — Add unit tests for new configs + boundary (target=production rejected)
8. **task-0073-0001-008** — Create ADR-0026-regression-shell-and-dast.md
9. **task-0073-0001-009** — Update docs/audit-gates-catalog.md (reserve entry)

---

## Key Decisions

- **DAST target=production**: throw ConfigValidationException with message `DAST_TARGET_PRODUCTION_FORBIDDEN`
- **Nuclei pinning**: regex validation `v\d+(\.\d+)?` (allows `v9.x` or `v9.0.0`); reject `latest`/`master`/`HEAD`
- **RegressionConfig.enabled default**: `true` when `interfaces[]` is declared; `false` otherwise
- **DastConfig.enabled default**: `false` (opt-in explicit per Rule 19)
