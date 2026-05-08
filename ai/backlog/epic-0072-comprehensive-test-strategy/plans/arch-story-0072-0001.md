# Architecture Plan — story-0072-0001

## Context
EPIC-0072 Comprehensive Test Strategy — Foundation story.
Delivers `QualityConfig` schema, 13 capability YAMLs, and ADR-0025.

## Decision: Placement of QualityConfig

`QualityConfig` is placed in `dev.iadev.domain.model` (same as `DocumentationConfig`, `SecurityConfig`, `DataConfig`). Although the story spec mentions `dev.iadev.config`, the architecture invariant (Rule 04) forbids domain from importing config layer. All YAML sub-config records follow `domain.model` placement.

## Record Hierarchy

```
QualityConfig (domain.model)
├── PerformanceConfig
│   ├── SloConfig
│   │   ├── RestSlo  (p50Ms, p95Ms, p99Ms, throughputRps)
│   │   ├── GrpcSlo  (p50Ms, p95Ms, p99Ms)
│   │   └── CliSlo   (genTimeP95Ms)
│   ├── baselineTolerancePct: int
│   └── toolVersions: Map<String, Object>
├── MutationConfig
│   ├── enabled: boolean
│   ├── threshold: int (80)
│   ├── runtimeCapMin: int
│   └── toolVersions: Map<String, Object>
└── ContractConfig
    ├── enabled: boolean
    ├── pact: boolean
    ├── openapiBreaking: boolean
    └── protoBreaking: boolean
```

## Extension Pattern

Governance record gains `QualityConfig quality` as 6th component (following `documentation` added in EPIC-0071). `ProjectConfig` gains `quality()` delegator and `parseQuality(Map)` static helper.

## YAML Block Shape

```yaml
quality:
  performance:
    enabled: true
    slo:
      rest: { p50_ms: 50, p95_ms: 200, p99_ms: 500, throughput_rps: 1000 }
      grpc: { p50_ms: 20, p95_ms: 100, p99_ms: 200 }
      cli: { gen_time_p95_ms: 3000 }
    baseline_tolerance_pct: 10
    tool_versions: {}
  mutation:
    enabled: true
    threshold: 80
    runtime_cap_min: 10
    tool_versions: {}
  contract:
    enabled: true
    pact: false
    openapi_breaking: true
    proto_breaking: true
```

## Capabilities Strategy

13 YAMLs across 3 families under `capabilities/quality/`:
- `performance/`: rest, grpc, cli, graphql, socket (5)
- `mutation/`: java-pit, stryker, mutmut, go-mutesting (4)
- `contract/`: openapi-diff, pact, buf, scc (4)

Category: `quality` (follows directory-equals-category convention established by existing capabilities).

## ADR Number

ADR-0025 (next free integer ≥ 17 after 0024).
