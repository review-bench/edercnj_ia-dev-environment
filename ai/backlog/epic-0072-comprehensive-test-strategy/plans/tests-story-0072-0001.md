# Test Plan — story-0072-0001

## Test Scenarios (from AC)

### Happy Path
- `QualityConfig` loaded from YAML with all 3 sub-blocks → all fields populated correctly
- `QualityConfig` accessible via `ProjectConfig.quality()` from a full config map
- Golden file for java-spring profile passes after quality block added

### Degenerate (defaults)
- YAML without `quality:` block → `QualityConfig.DEFAULT` with all `enabled=false`
- YAML with partial `quality:` → missing sub-blocks use defaults
- `ConfigLoader.loadConfig()` with no quality block → no exception

### Error/Boundary
- Capability YAML missing required fields → schema validation fails
- ADR number collision detection (checked via task-001)

### Performance
- Parse latency < 100ms for full quality block (verified in unit test with `Instant`)

### Security
- Capability YAML with path-traversal `id` field (`../../etc/passwd`) → schema validation rejects
- QualityConfig parsing via SafeConstructor (already in ConfigLoader) → no arbitrary class instantiation

## Test Classes

| Class | What |
| :--- | :--- |
| `QualityConfigTest` | `fromMap()` all variants: full, partial, absent, malformed |
| `GovernanceQualityTest` | Governance record with quality field; integration with ProjectConfig |
| `QualityConfigDefaultsTest` | All `enabled=false` defaults when block absent |
| `QualityConfigParseLatencyTest` | Parse latency < 100ms |
| `CapabilityYamlSchemaTest` | (extends existing) validates new quality/* YAMLs against schema |

## Coverage Target
≥ 95% line, ≥ 90% branch on new classes.
