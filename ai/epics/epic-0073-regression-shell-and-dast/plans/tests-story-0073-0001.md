# Test Plan — story-0073-0001

**Story:** Schema YAML + capabilities + ADR (EPIC-0073)

---

## Unit Tests

### QualityConfig — RegressionConfig

| Scenario | Input | Expected |
|----------|-------|----------|
| Happy: full regression config | `quality.regression.enabled=true, mode=service` | RegressionConfig populated |
| Happy: self mode | `quality.regression.mode=self` | mode=SELF |
| Default: absent block, interfaces declared | `quality:` absent | enabled=true (default), mode=SERVICE |
| Default: no interfaces | `quality:` absent, interfaces empty | enabled=false |
| Boundary: custom scenarios-file | `scenarios-file=custom/path.yaml` | scenariosFile set |

### QualityConfig — DastConfig

| Scenario | Input | Expected |
|----------|-------|----------|
| Happy: full dast config | `quality.dast.enabled=true, tier-pr=smoke` | DastConfig enabled=true |
| Boundary: target=production | `quality.dast.target=production` | ConfigValidationException DAST_TARGET_PRODUCTION_FORBIDDEN |
| Boundary: nuclei.templates-version=latest | `templates-version=latest` | ConfigValidationException invalid pinning |
| Boundary: nuclei.templates-version=v9.x | `templates-version=v9.x` | accepted |
| Boundary: nuclei.templates-version=v9.0.0 | `templates-version=v9.0.0` | accepted |
| Default: absent block | `quality:` absent | enabled=false, all defaults |
| Default: absent nuclei.templates-version | omitted | `v9.x` |

---

## Integration Tests

- `mvn verify` passes with 5 new capability YAML files present
- `ConfigLoaderTest` covers happy + boundary scenarios above
- No regression in existing `Epic0072*` tests
