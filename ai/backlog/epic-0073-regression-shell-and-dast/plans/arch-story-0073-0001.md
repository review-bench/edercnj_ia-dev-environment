# Architecture Plan — story-0073-0001

**Story:** Schema YAML `quality.{regression,dast}` + capabilities + ADR
**Epic:** EPIC-0073

---

## Design Decisions

### Extend QualityConfig (not new class)
EPIC-0072 introduced `QualityConfig` as the canonical quality-gate aggregate. EPIC-0073 adds two sub-records:
- `RegressionConfig` — regression shell gate (enabled, mode, scenariosFile)
- `DastConfig` — DAST gate (enabled, tierPr, tierNightly, target, zap, nuclei)

Both follow the same pattern: record + `DEFAULT` constant + `fromMap()` factory + compact constructor.

### Capability Atomic Files (D-R7)
5 atomic YAML files in `capabilities/quality/regression/` and `capabilities/quality/dast/`.
Atomic granularity allows skill frontmatter to declare `requires-capabilities: [quality.dast.zap-active]`
for active-scan-specific logic.

### Production Target Guard (D-R9)
`DastConfig.fromMap()` throws `ConfigValidationException("DAST_TARGET_PRODUCTION_FORBIDDEN")` when
`quality.dast.target = production`. Defense in depth: also validated in `ScriptsAssembler` (future story).

### Nuclei Version Pinning (D-R8)
`ZapConfig.fromMap()` validates `nuclei.templates-version` against regex `v\d+(\.\d+)?(\.\d+)?`.
Rejects `latest`, `master`, `HEAD`. Default: `v9.x`.

---

## No Rule Required (D-R3)
Rules 06 (Security Baseline), 24 (Execution Integrity), 26 (Audit Gate Lifecycle), and 28
(Capability Frontmatter) already cover all invariants introduced by EPIC-0073. The DAST gate
is a CI workflow concern (Rule 26), not a normative LLM-session concern. ADR-0026 + capability
families are sufficient.
