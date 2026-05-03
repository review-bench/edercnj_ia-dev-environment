---
epic-id: EPIC-0063
slug: local-first-preflight-gates
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, preflight, camada-0, enforcement, local-first, rule28]
capabilities-affected: []
rules-affected: [Rule 24, Rule 26, Rule 28]
adrs-referenced: [ADR-0016]

patterns-introduced:
  - preflight-script-as-local-gate
  - pretooluse-hook-blocking-gate
  - inline-grammar-markers-required-optional-conditional
  - wave-dispatch-audit-n-siblings
  - content-audit-stub-rejection
antipatterns-rejected:
  - stub-review-artifacts-passing-presence-check
  - ci-only-enforcement-without-local-first-gate

dependencies-of: [EPIC-0059, EPIC-0062]
dependencies-for: [EPIC-0064, EPIC-0065]
---
# Memory: EPIC-0063 — Local-First Pre-Flight Gates (No-Bypass Hardening)

## Why this epic existed

During EPIC-0062, 8 stories were merged with stub reviews (≤2 lines, `Verdict: GO`), placeholder `verify-envelope.json`, and empty `dependency-audit.md`. All passed the Camada 3 audit (`audit-execution-integrity.sh`) because the auditor validated **presence** of files, not **content**. The problem was only detected in the final PR (#751), which received NO-GO with 3 critical findings, requiring rework. Root cause: all enforcement was detective (after the fact); no preventive layer existed.

## Hypothesis tested

Inverting the flow — making the PR a "publication of locally-validated code" rather than "let's see if CI passes" — via (1) `scripts/preflight.sh` as local gate, (2) `enforce-preflight-gates.sh` PreToolUse hook intercepting `git push`/`gh pr create`/`Skill x-pr-create`, (3) content audits rejecting stubs heuristically (≥50 lines, ≥3 H2 sections, ≥2 file references from diff, GO/NO-GO present), and (4) Rule 28 tool-call grammar markers `[required]`/`[optional]`/`[conditional]` on every `Skill(...)` declaration in orchestrators would prevent the EPIC-0062 pattern from recurring. **Confirmed**: all 21 stories delivered; Rule 28 published; `audit-tool-call-grammar.sh` delivered; `Epic0063LocalFirstSmokeTest` with 15 scenarios passing.

## Decisions taken (with why)

1. **`enforce-preflight-gates.sh` as PreToolUse hook** (Camada 0) — physically blocks tool execution before remote ops; only `CLAUDE_RECOVERY_MODE=1` bypasses (documented in Rule 27).
2. **Content audits via heuristics** — minimum thresholds (≥50 lines, ≥3 H2 sections) reject stubs without AI-based quality scoring; fast and deterministic.
3. **Rule 28 grammar markers** — `[required]`/`[optional]`/`[conditional: <expr>]` on each `Skill(...)`/`Agent(...)` in Anexo B orchestrators; static lint detects missing markers; dynamic audit cross-checks against telemetry NDJSON.
4. **Gate ordering lightweight-first** — format → audits lightweight → mvn test → coverage (D2 decision); stubs detected before 3-minute `mvn test` run.

## Alternatives rejected (with why)

- **CI-only enforcement** — CI is detective (after-the-fact); EPIC-0062 showed detective-only is insufficient when stub artifacts pass presence checks.
- **AI-based review quality scoring** — too slow for a local pre-flight gate; deferred as tier-2 future work.
- **`--changed-only` optimization for `mvn test`** — removed from v1 (D3 decision); P95 SLO adjusted to 8min for full `mvn test`; backlogged for future epic.

## Reusable patterns produced

- **`preflight-script-as-local-gate`**: `scripts/preflight.sh` orchestrates format + lightweight audits + `mvn test` + coverage; runs before every push.
- **`pretooluse-hook-blocking-gate`**: `enforce-preflight-gates.sh` intercepts `git push`, `gh pr create`, `Skill x-pr-create`; physically blocking (exit non-zero stops the tool call).
- **`inline-grammar-markers-required-optional-conditional`**: Rule 28 markers on every `Skill()`/`Agent()` in orchestrator SKILL.md; `[required]` → dynamic cross-check against NDJSON; `[conditional: expr]` → evaluated against story context.
- **`wave-dispatch-audit-n-siblings`**: N parallel sibling `Agent(...)` calls must produce N telemetry events; Rule 13 Pattern 2 fan-out is auditable.
- **`content-audit-stub-rejection`**: heuristic thresholds (lines/sections/diff-references/verdict) reject stub artifacts at Camada 0 before they reach CI.

## Anti-patterns observed

- **Stub review artifacts passing presence check** — CI auditor checks file existence, not content; stubs (≤2 lines) pass and produce technical debt that only surfaces at final PR review.

## Links

- Epic: `ai/epics/epic-0063-local-first-preflight-gates/epic-0063.md`
- ADRs: `docs/adr/ADR-0016-preflight-rollout.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0063-local-first-preflight-gates/reports/`
