# ADR-0026 — Regression Shell + Dynamic Application Security Testing (DAST)

**Status:** Accepted
**Date:** 2026-05-01
**Epic:** EPIC-0073

## Context

EPIC-0072 introduced three conditional quality gates (performance, mutation, contract). EPIC-0073 extends the quality gate portfolio with two additional conditional gates:

1. **Regression Shell** — runs a curated set of scenario scripts against the target project to detect regressions introduced by code generation changes. Two modes: `self` (generator validates its own output) and `service` (client project validates its services).
2. **DAST** — Dynamic Application Security Testing via OWASP ZAP and Nuclei. Two tiers: `smoke` (ZAP passive + Nuclei lightweight templates, PR gate) and `full` (ZAP active scan + complete Nuclei template set, nightly).

## Decisions

### D-R1: Extend `QualityConfig` (not a new top-level config record)

Regression and DAST configuration lives under `quality.regression` and `quality.dast` respectively, extending the existing `QualityConfig` record introduced by EPIC-0072. This preserves the single `quality:` YAML block convention and the established `fromMap` pattern.

### D-R2: Atomic capabilities, not bundles

Five atomic capability files are introduced (`quality.regression.self`, `quality.regression.service`, `quality.dast.zap-passive`, `quality.dast.zap-active`, `quality.dast.nuclei`). Atomic granularity allows frontmatter to use glob patterns (`quality.dast.*`) or reference individual capabilities as needed.

`quality.regression.self` and `quality.regression.service` are mutually exclusive (declared via `excludes` field) since the same project cannot run in both modes simultaneously.

### D-R3: No new Rule required

All invariants are covered by existing rules:
- **Rule 06** (Security Baseline) — DAST gate enforces ZAP/Nuclei scanning
- **Rule 24** (Execution Integrity) — DAST evidence artifacts are mandatory
- **Rule 26** (Audit Gate Lifecycle) — `audit-regression-shell.sh` and `audit-dast-gate.sh` follow the CI script taxonomy
- **Rule 28** (Capability Frontmatter Contract) — new capability files follow the v3.0 schema

### D-R7: Atomic capabilities vs. bundle capability

Rejected `quality.dast` (single bundle capability) in favor of atomic IDs (`quality.dast.zap-passive`, `quality.dast.zap-active`, `quality.dast.nuclei`). Rationale: a project may enable ZAP passive without Nuclei, or run ZAP active only in nightly. Atomic capabilities allow fine-grained composition without a capability resolver DSL.

### D-R8: Nuclei templates-version pinning

Nuclei template versions must match `^v\d+(\.x|\.\d+(\.\d+)?)$`. The values `latest`, `master`, and `HEAD` are rejected with `ConfigValidationException(NUCLEI_VERSION_UNPINNED)`. Major-version upgrades (e.g., `v9.x` → `v10.x`) require an explicit PR and smoke test rerun to catch template changes that alter scan results. The `v9.x` series format (major pinned, minor wildcard) is the recommended default — it receives patch-level template updates without breaking changes.

### D-R9: DAST target=production forbidden

`DastConfig` rejects `target=production` at construction time with `ConfigValidationException(DAST_TARGET_PRODUCTION_FORBIDDEN)`. The allowed targets are `local-container`, `preview-env`, and `staging`. This prevents accidental active ZAP scans against production endpoints.

### D-R10: Tier mapping

| Tier | ZAP mode | Nuclei templates | Timeout | Pipeline |
|------|----------|-----------------|---------|----------|
| `smoke` | passive (read-only spider) | top-50 lightweight | ~10 min | PR gate |
| `full` | active (injection payloads) | complete set | ~35 min | nightly |
| `none` | disabled | disabled | — | — |

The `smoke` tier is the default for `tier-pr`; `full` is the default for `tier-nightly`. Setting `tier-pr=none` disables the PR gate without disabling nightly.

## Consequences

- `QualityConfig` record now has 5 parameters (was 3 in EPIC-0072). All existing 3-param usages in tests updated to 5-param form.
- `capabilities/_index.yaml` gains 5 new IDs in the `quality` category.
- Projects that enable `quality.dast.enabled=true` must configure a non-production target; the production guard is enforced at YAML parse time.
- Nuclei version pinning prevents supply-chain drift where template updates silently change scan results; explicit upgrades are traceable in git.
