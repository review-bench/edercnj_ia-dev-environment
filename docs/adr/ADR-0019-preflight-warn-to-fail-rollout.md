# ADR-0016 — Preflight Gates WARN→FAIL Rollout Strategy

**Status:** Accepted
**Date:** 2026-04-28
**Supersedes:** —
**Superseded by:** —
**Related:** Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle), EPIC-0063, story-0063-0016

---

## Context

EPIC-0063 introduces `enforce-preflight-gates.sh` (Camada 0 hook) that intercepts `git push`,
`gh pr create`, and `Skill(x-create-pr)` tool calls and blocks them when one or more preflight
gates fail. Switching from WARN mode (log only) to FAIL mode (block) is a breaking change for
teams that have not yet adapted their workflows.

A direct FAIL-mode launch would:
1. Break existing CI and developer workflows immediately with no adjustment period.
2. Produce false-positive blocks for edge cases not covered by the initial gate calibration.
3. Generate blame without data — no metrics exist yet to quantify the false-positive rate.

A 2-phase rollout is required to reduce risk, collect metrics, and give operators time to calibrate.

## Decision

Adopt a **2-phase explicit rollout** for `enforce-preflight-gates.sh`, governed by a feature
toggle (`PREFLIGHT_ENFORCE_MODE` env var or `.claude/state/rollout-mode.json` file).

### Phase 1 — WARN mode (default during rollout window)

- Hooks log violations to stderr but exit 0 (do not block).
- Telemetry events (`preflight.warn_mode`) are emitted to `events.ndjson` for every violation.
- Duration: minimum 1 release cycle (approximately 30 days) or until readiness criteria are met.

### Phase 2 — FAIL mode (blocking)

- Hooks exit non-zero on violations, blocking the intercepted tool call.
- `CLAUDE_RECOVERY_MODE=1` remains the only escape hatch (Rule 27 §RULE-059-07).
- Activated only after `audit-rollout-readiness.sh` validates all exit criteria.

### Toggle Precedence

```
PREFLIGHT_ENFORCE_MODE env var
  → if set, value used (validated: warn|fail)
  → else: read .claude/state/rollout-mode.json
    → if exists, value used
    → else: default = "warn" (safe default during rollout)
```

Accepted values: `warn`, `fail`. Any other value → `OPERATIONAL_ERROR` (exit 2).

### Rollout Exit Criteria (before activating FAIL mode)

1. `recovery_mode_used` event rate < 5% across stories in the last 30 days.
2. Zero open issues labelled `false-positive` against `enforce-preflight-gates.sh`.
3. Minimum 10 stories merged while operating in WARN mode (baseline data).

The script `scripts/audit-rollout-readiness.sh` validates these criteria and MUST exit 0
before the release that activates FAIL mode is cut. It runs on the CI release branch pipeline.

### State Management

The current rollout mode is stored in `.claude/state/rollout-mode.json`:

```json
{
  "mode": "warn",
  "set_at": "2026-04-28T00:00:00Z",
  "previous_mode": "warn",
  "script": "audit-rollout-status.sh"
}
```

Operators use `scripts/audit-rollout-status.sh --set-mode <warn|fail>` to transition modes.
The script also reports current mode, transition date, and bypass count.

## Alternatives Considered

### Alternative 1: Big-bang launch in FAIL mode from day one

**Rejected.** Zero adjustment period. The false-positive rate of new gates is unknown.
Blocking developer workflows without data risks loss of adoption and manual bypasses.

### Alternative 2: Permanent WARN mode (never block)

**Rejected.** WARN-only mode is not governance — it is noise. Without blocking, the gates
have no enforcement value and bypass behaviour accumulates. The goal is to block violations;
WARN is only a calibration period.

### Alternative 3: Per-team opt-in to FAIL mode

**Rejected.** Fragmented enforcement across teams undermines the governance model. A single
cohesive rollout with clear criteria ensures uniform protection.

### Alternative 4: Time-based cutover without metrics gate

**Rejected.** A calendar date with no readiness check ignores actual adoption data. The
`audit-rollout-readiness.sh` gate makes the transition evidence-based, not arbitrary.

## Consequences

### Positive

- Operators have a minimum 1-release adjustment period before blocking begins.
- False positives are identified and corrected during WARN mode without workflow disruption.
- The readiness gate ensures FAIL mode is only activated when adoption metrics support it.
- `audit-rollout-status.sh --set-mode` provides a clear, auditable mode transition mechanism.
- Toggle precedence (env > file > default) allows both per-session and per-repository overrides.

### Negative

- WARN mode carries a risk that operators ignore violations, delaying real adoption.
- Two-phase rollout adds coordination overhead for the release that activates FAIL mode.
- The default `warn` during rollout means new projects inheriting the hook are not blocked.

### Neutral

- CI release pipelines must invoke `audit-rollout-readiness.sh` before the FAIL-mode release.
- The `.claude/state/rollout-mode.json` file must be committed to git for CI to see it.

## Related ADRs

- ADR-0015 (Audit Gate Lifecycle Convention) — defines the 5-layer taxonomy this hook lives in.
- ADR-0010 (Interactive Gates Convention) — establishes the `--interactive` / non-interactive default.
- ADR-0016 (Capability-Driven Composition) — separate ADR sharing this numbering slot (EPIC-0064).

## Story Reference

- story-0063-0016 — implements this decision
- EPIC-0063 — Local-First Pre-Flight Gates (parent epic)
