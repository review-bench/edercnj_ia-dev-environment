---
epic-id: EPIC-0074
slug: dependency-policy-and-sca-gate
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [security, governance, dependencies, sca, supply-chain]
capabilities-affected: [governance.dependency-policy]
rules-affected: [Rule 05, Rule 24, Rule 27, Rule 32]
adrs-referenced: [ADR-0027]

patterns-introduced:
  - yaml-policy-sub-block-pattern
  - granular-block-on-matrix
  - denied-cves-hard-block
  - scope-policy-per-scope-type
  - d-r9-version-constraint-format
antipatterns-rejected:
  - separate-policy-file
  - global-severity-threshold-only
  - freshness-always-blocking

dependencies-of: [EPIC-0072]
dependencies-for: [EPIC-0075]
---
# Memory: EPIC-0074 — Dependency Policy & SCA Final Gate

## Why this epic existed

Projects could declare any dependency version regardless of known CVEs, banned licenses, or staleness. No gate blocked high-severity vulnerabilities from merging. License compliance was checked manually. Freshness drift accumulated silently until audit-forced remediation. The supply chain attack surface was unaudited per-merge.

## Hypothesis tested

A **YAML-configurable `dependencies.policy` sub-block** with 5 enforcement dimensions (CVE severity, license whitelist, version constraints, freshness, scope) inserted as mandatory conditional gate in `x-story-implement` Phase 3 (after contract tests, D-R11 sequence) would block supply chain risk without breaking existing projects (safe default `enabled: false`). **Confirmed**: Rule 32 delivered; `x-dep-policy-validate` skill; `DependencyPolicyConfig` Java record; Rule 27 Surface 13 added; ADR-0027.

## Decisions taken (with why)

1. **Policy in `dependencies.policy` YAML sub-block** (not separate file) — keeps configuration co-located with project definition; avoids file sync overhead; consistent with how `quality.*` gates are configured.
2. **Granular `block-on` matrix** (D-R10 defaults) — `severity-cve: BLOCK`, `license: BLOCK`, `min-version: BLOCK`, `max-version: WARN_ONLY`, `freshness: WARN_ONLY`; each dimension independently configurable to BLOCK/WARN_ONLY/IGNORE.
3. **`denied-cves` hard-block** (RULE-074-01) — CVEs in this list are blocked regardless of scope-policy, block-on.severity-cve, or patch availability; represents organizational policy decisions overriding automated scoring.
4. **Freshness `WARN_ONLY` default** (not blocking) — blocking on stale deps would make onboarding impossible for legacy projects; warn first, teams can tighten over time.
5. **Scope-policy defaults** (D-R11): `compile`/`runtime` → BLOCK; `test`/`dev`/`provided`/`build` → WARN_ONLY; test deps with CVEs warn but don't block prod build.
6. **Three disjoint version constraint formats** (D-R9): JVM (`groupId+artifactId`), NPM/PyPI (`name`), Go (`module`); combining fields = `ConfigValidationException`.

## Alternatives rejected (with why)

- **Separate policy file** (`dependencies-policy.yaml`) — extra file sync step; breaks if YAML and policy file diverge; co-location preferred.
- **Global severity threshold only** — no way to express "CVE-2024-12345 is hard-blocked regardless of CVSS score"; `denied-cves` list needed for org-level policy.
- **Freshness always blocking** — legacy projects would need immediate remediation of hundreds of stale transitive deps on opt-in; WARN_ONLY default removes adoption barrier.

## Reusable patterns produced

- **`yaml-policy-sub-block-pattern`**: gate configuration co-located with project YAML as `dependencies.policy` sub-block; consistent with `quality.*` gates.
- **`granular-block-on-matrix`**: each enforcement dimension independently set to BLOCK/WARN_ONLY/IGNORE; avoids all-or-nothing gates.
- **`denied-cves-hard-block`**: explicit CVE list overrides all other settings; organizational policy signal above automated scoring.
- **`scope-policy-per-scope-type`**: compile/runtime stricter than test/dev; mirrors Maven scope semantics.

## Anti-patterns observed

- **Single global severity threshold** — can't express "this specific CVE is hard-blocked even if CVSS < threshold"; always add `denied-cves` list.
- **Freshness as hard block by default** — breaks adoption for legacy projects; always use WARN_ONLY as freshness default.

## Links

- Epic: `ai/epics/epic-0074-dependency-policy-and-sca-gate/epic-0074.md`
- ADRs: `docs/adr/ADR-0027-dependency-policy-gate.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0074-dependency-policy-and-sca-gate/reports/`
