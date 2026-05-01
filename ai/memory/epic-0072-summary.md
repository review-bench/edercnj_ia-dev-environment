# EPIC-0072 — Comprehensive Test Strategy: Memory Summary

**Concluded:** 2026-05-01
**Epic branch:** `epic/0072`
**Stories:** 9 (all Concluída)
**ADR:** [ADR-0025](../../docs/adr/ADR-0025-comprehensive-test-strategy.md)

## What was delivered

Three conditional quality gates were added to `x-story-implement` Phase 3 (§3.Q). They fire
in the D-R11 fast-fail sequence: **performance → mutation → contract**. The first failure
cancels subsequent gates. All gates are disabled by default (`quality.*.enabled=false` in
project YAML) — backward compatible per Rule 19.

| Gate | Skill | Disabled exit | Blocking exit |
| :--- | :--- | :--- | :--- |
| Performance regression | `x-test-performance` | 50 PERF_DISABLED | 1 PERF_REGRESSION_DETECTED → x-story-implement exit 14 |
| Mutation score | `x-test-mutation` | 50 MUTATION_DISABLED | 1 MUTATION_SCORE_BELOW_THRESHOLD → exit 17 |
| Contract breaking | `x-test-contract` | n/a (WARN) | 1 CONTRACT_BREAKING_CHANGE → exit 18 |

## Key files modified / created

| File | Change |
| :--- | :--- |
| `src/main/resources/targets/claude/skills/conditional/test/x-test-performance/SKILL.md` | NEW (story-0072-0002) |
| `src/main/resources/targets/claude/skills/conditional/test/x-test-mutation/SKILL.md` | NEW (story-0072-0003) |
| `src/main/resources/targets/claude/skills/conditional/test/x-test-contract/SKILL.md` | NEW (story-0072-0004) |
| `src/main/resources/targets/claude/scripts/audit-perf-baseline.sh` | NEW (story-0072-0005) |
| `src/main/resources/targets/claude/scripts/audit-mutation-score.sh` | NEW (story-0072-0006) |
| `src/main/resources/targets/claude/scripts/audit-contract-breaking.sh` | NEW (story-0072-0007) |
| `src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` | MODIFIED — added §3.Q (story-0072-0008) |
| `src/main/resources/targets/claude/rules/24-execution-integrity.md` | MODIFIED — 3 conditional artifact rows (story-0072-0008) |
| `src/main/resources/targets/claude/scripts/audit-execution-integrity.sh` | MODIFIED — QUALITY_*_ENABLED env var checks (story-0072-0008) |
| `src/test/java/dev/iadev/smoke/Epic0072TestStrategySmokeIT.java` | NEW — 8 E2E scenarios (story-0072-0009) |
| `CHANGELOG.md` | ADDED — EPIC-0072 [Unreleased] entry with [Breaking] (story-0072-0009) |
| `CLAUDE.md` | ADDED — "Concluded — EPIC-0072" block (story-0072-0009) |
| `docs/adr/ADR-0025-comprehensive-test-strategy.md` | NEW (story-0072-0001) |
| `src/main/java/dev/iadev/domain/model/QualityConfig.java` | NEW (story-0072-0001) |

## Design decisions

- **D-R11 fast-fail**: perf first because it's cheapest (ms vs seconds for mutation). Contract
  last because it needs the build artifacts from the perf/mutation runs.
- **Conditional defaults to false**: projects without quality config stay green. Gate activation
  requires explicit opt-in in project YAML (`quality.performance.enabled: true` etc).
- **Telemetry sub-phases**: Phase-3-Quality-Perf, Phase-3-Quality-Mutation,
  Phase-3-Quality-Contract — 3 separate pairs for per-gate timing visibility in telemetry analysis.
- **Exit codes non-colliding**: 14/17/18 chosen to avoid existing codes (12=PHASE_GATE_FAILED,
  15=WORKTREE_AMBIGUOUS, 33=REFINEMENT_REQUIRED, 40-70=Rule 45 range).
- **SKILL.md line limit raised**: `x-story-implement` limit raised 360→410 in
  `Epic0047CompressionSmokeTest` to accommodate §3.Q addition (403 lines total).
- **audit-execution-integrity.sh backward compat**: conditional checks via env vars
  (`QUALITY_*_ENABLED:-false`) so CI environments without quality config pass cleanly.

## Stack-awareness matrix

| Gate | REST | gRPC | CLI | Java | JS/TS | OpenAPI | proto3 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Performance | Newman | ghz | hyperfine | — | Artillery | — | — |
| Mutation | — | — | — | PIT/pitest | Stryker | — | — |
| Contract | openapi-diff | — | — | SCC | — | openapi-diff | buf |

## Integration with existing gates

- Rule 24 §Mandatory Evidence Artifacts: 3 new conditional rows (perf-report, mutation-report,
  contract-report) — conditional on `quality.*.enabled=true` in Camada 3 audit.
- Rule 05 §Quality Gates: extended with 3 conditional gates + D-R11 fast-fail doc.
- `--skip-quality` flag: Recovery-block-only bypass (same pattern as `--skip-doc`, `--skip-review`).

## Smoke test coverage

`Epic0072TestStrategySmokeIT` (8 scenarios) validates:
1. Perf regression blocking + REST stack (Newman dispatch)
2. Perf success + tolerance config
3. Mutation blocking + Java stack (PIT dispatch)
4. Mutation success + threshold config
5. Contract blocking without CHANGELOG (OpenAPI stack)
6. Contract pass with CHANGELOG (proto3/buf stack)
7. Opt-out (all disabled) — PERF_DISABLED/MUTATION_DISABLED + config defaults
8. Stack-awareness across all 3 skills (6 tool dispatches)

Also covered by story-specific ITs: `Epic0072Phase3IntegratedSmokeIT` (11 scenarios in 5
nested classes verifying SKILL.md, Rule 24, audit script structural invariants).
