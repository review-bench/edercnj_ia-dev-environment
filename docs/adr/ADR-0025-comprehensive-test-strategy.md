# ADR-0025 — Comprehensive Test Strategy (Performance, Mutation, Contract)

**Status:** Accepted  
**Date:** 2026-05-01  
**Epic:** EPIC-0072 (Comprehensive Test Strategy)  
**Authors:** Architect agent, story-0072-0001

---

## Context

The `ia-dev-env` generator ships skills for unit testing (`x-test-tdd`), E2E (`x-test-e2e`), integration, and smoke tests. Three critical quality dimensions have no first-class skill support:

1. **Performance testing** — no standard way to gate on latency SLOs (P50/P95/P99) or throughput. Projects define ad-hoc k6/Gatling/JMeter scripts without integration into the story lifecycle.
2. **Mutation testing** — coverage metrics (≥ 95% line, Rule 05) can be satisfied by weak assertions. Mutation score exposes assertion quality gaps not visible in coverage reports.
3. **Contract testing** — API-first contracts (OpenAPI, Protobuf, Pact) drift silently between producer and consumer when no breaking-change gate enforces compatibility.

Without these three skills:
- Performance regressions reach production undetected.
- High coverage hides low assertion quality (mutation score ≈ 40% on some epics).
- Contract breaking changes are caught only at runtime integration test time (slow feedback).

---

## Decision

Introduce three stack-aware skills and accompanying governance:

### 1. `/x-test-performance` (story-0072-0002)

Stack-aware performance gate per `interfaces[].type`:
- REST → Newman/k6/Artillery
- gRPC → ghz
- CLI → hyperfine
- GraphQL → k6 + graphql plugin

Invokes configured tool, compares results against `QualityConfig.performance.slo.*` thresholds, persists baseline JSON for drift detection.

### 2. `/x-test-mutation` (story-0072-0003)

Stack-aware mutation gate per `language`:
- Java → PIT (Pitest)
- JavaScript/TypeScript → Stryker
- Python → mutmut
- Go → go-mutesting

Enforces `QualityConfig.mutation.threshold` (default 80%). Runtime capped by `runtimeCapMin` to prevent CI stall.

### 3. `/x-test-contract` (story-0072-0004)

Stack-aware contract gate per `interfaces[].spec` or broker type:
- OpenAPI → openapi-diff
- Protobuf → buf breaking
- Pact → Pact broker integration
- Event schema → schema-compatibility-checker (SCC)

Fails on `BREAKING` classification; warns on `NON_BREAKING`.

### 4. `quality:` YAML block (this story — 0072-0001)

Declarative per-project configuration under `quality:` top-level key:

```yaml
quality:
  performance:
    enabled: true
    slo:
      rest: { p50_ms: 50, p95_ms: 200, p99_ms: 500, throughput_rps: 1000 }
      grpc: { p50_ms: 20, p95_ms: 100, p99_ms: 200 }
      cli: { gen_time_p95_ms: 3000 }
    baseline_tolerance_pct: 10
  mutation:
    enabled: true
    threshold: 80
    runtime_cap_min: 10
  contract:
    enabled: true
    pact: false
    openapi_breaking: true
    proto_breaking: true
```

Defaults when `quality:` absent: all `enabled: false` (Rule 19 backward-compat — existing projects do not break).

### 5. Capabilities families

13 capability YAMLs under `capabilities/quality/`:
- `performance/`: rest, grpc, cli, graphql, socket
- `mutation/`: java-pit, stryker, mutmut, go-mutesting
- `contract/`: openapi-diff, pact, buf, scc

### 6. `QualityConfig.java` placement

`QualityConfig` and sub-records are placed in `dev.iadev.domain.model` — following the pattern of `DocumentationConfig`, `SecurityConfig`, `DataConfig`. This preserves domain purity: the config layer (`ConfigLoader`) calls `QualityConfig.fromMap()` indirectly through `ProjectConfig.fromMap()` → `Governance.fromMap()`. Domain does not import from config layer.

### 7. Governance integration

`x-story-implement` Phase 3 gains three **MANDATORY conditional invocations** (story-0072-0008):
```
Skill(x-test-performance) [conditional: flag.performance_enabled]
Skill(x-test-mutation)    [conditional: flag.mutation_enabled]  
Skill(x-test-contract)    [conditional: flag.contract_enabled]
```

Three CI audit scripts (stories 0072-0005/0006/0007) enforce baselines via Rule 26 Camada 2.

---

## Alternatives Considered

### Alternative A — Single `/x-test-quality` orchestrator skill

One skill that runs perf + mutation + contract sequentially.

**Rejected:** violates skill taxonomy SRP (each skill = one concern). Parallel execution is faster. Stack-awareness per domain is cleaner in separate skills.

### Alternative B — Configuration via `testing:` block (existing)

Extend the existing `testing:` YAML block with SLO/mutation/contract sub-keys.

**Rejected:** `testing:` already carries `smoke_tests`, `performance_tests` (boolean flags). Adding numeric SLOs and tool configuration to it would violate its current role as a simple feature-flag block. A dedicated `quality:` block provides clean separation of concerns.

### Alternative C — Hardcoded defaults (no YAML config)

Skills use hardcoded defaults (REST P95=200ms, mutation=80%, etc.) without per-project override.

**Rejected:** different projects have different SLO budgets (fintech vs. internal tools). Declarative per-project thresholds are essential for adoption.

---

## Consequences

**Positive:**
- Three new skill types discoverable via `/help`.
- Performance SLO gates become blocking in Phase 3 (conditional on capability).
- Mutation score becomes a first-class quality dimension alongside coverage.
- Contract breaking changes detected before PR merge.
- Existing projects unaffected (all `enabled: false` by default).

**Negative:**
- Governance record gains a 6th parameter (`QualityConfig`), slightly increasing complexity.
- CI runtime increases for projects that enable all three gates.
- Mutation testing runtime is capped (`runtimeCapMin`) to mitigate CI stall risk.

---

## Decision Record (§11 per `_TEMPLATE-ARCHITECTURE-SYSTEM.md`)

| Epic | ADR | Decision |
| :--- | :--- | :--- |
| EPIC-0072 | ADR-0025 | Introduce quality: YAML block + QualityConfig + 3 capability families + 3 skills |

---

> See also: [`EPIC-0072`](../../ai/epics/epic-0072-comprehensive-test-strategy/epic-0072.md) — story-0072-0001 through story-0072-0009.
