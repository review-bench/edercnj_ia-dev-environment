---
name: kp-lifecycle-execution-integrity
description: "Full reference for Rule 24 Execution Integrity: mandatory evidence artifacts table, 5-layer enforcement details, Camada 0 pre-flight scripts, audit script exit codes, and baseline format."
requires-capabilities: []
---

# Knowledge Pack: Execution Integrity (Rule 24 — Full Reference)

## Mandatory Evidence Artifacts

| Sub-skill | Artifact path | Enforced by |
| :--- | :--- | :--- |
| `x-internal-verify-story` | `ai/epics/epic-XXXX/reports/verify-envelope-STORY-ID.json` | Camada 3 |
| `x-review-codebase` | `ai/epics/epic-XXXX/plans/review-story-STORY-ID.md` | Camada 3 |
| `x-review-pr` | `ai/epics/epic-XXXX/plans/techlead-review-story-STORY-ID.md` | Camada 3 |
| `x-internal-write-story-report` | `ai/epics/epic-XXXX/reports/story-completion-report-STORY-ID.md` | Camada 3 |
| `x-plan-architecture` | `ai/epics/epic-XXXX/plans/arch-story-STORY-ID.md` | Camada 3 (soft) |
| `x-watch-pr-ci` | `.claude/state/pr-watch-{PR_NUMBER}.json` | Camada 2 (Stop hook) |
| `x-create-pr` | telemetry NDJSON (`gh pr create` event in `events.ndjson`) | Camada 4 |
| `x-drive-tdd` / `x-execute-tests` | `ai/epics/epic-XXXX/reports/test-run-STORY-ID.txt` | Camada 3 (soft) |
| `x-commit-changes` (TDD cycle) | `git log --oneline` on PR branch | Camada 4 |
| `x-audit-dependencies` | `ai/epics/epic-XXXX/reports/dependency-audit-STORY-ID.md` | Camada 3 |
| `x-model-threats` | `ai/epics/epic-XXXX/plans/threat-model-story-STORY-ID.md` | Camada 3 (soft) |
| `x-validate-docs` | `ai/epics/epic-XXXX/reports/doc-validate-report-STORY-ID.md` | Camada 3 (EPIC-0071) |
| `x-execute-performance-tests` | `ai/epics/epic-XXXX/reports/perf-report-STORY-ID.md` | Camada 3 (soft — conditional: `quality.performance.enabled=true`) |
| `x-execute-mutation-tests` | `ai/epics/epic-XXXX/reports/mutation-report-STORY-ID.md` | Camada 3 (soft — conditional: `quality.mutation.enabled=true`) |
| `x-execute-contract-tests` | `ai/epics/epic-XXXX/reports/contract-report-STORY-ID.md` | Camada 3 (soft — conditional: `quality.contract.enabled=true`) |
| `x-validate-dependency-policy` | `ai/epics/epic-XXXX/reports/dep-policy-validation-report-STORY-ID.md` | Camada 3 (soft — conditional: `dependencies.policy.enabled=true`) |

## Enforcement Layers (5 Total)

### Camada 0 — Local Pre-Flight (EPIC-0063)

Fires BEFORE any remote operation (`git push`, `gh pr create`, `Skill x-create-pr`). Physically blocking — the only layer that can **prevent** a bad action.

| Aspect | Detail |
| :--- | :--- |
| Trigger | PreToolUse hook (`enforce-preflight-gates.sh`) |
| Único bypass | `CLAUDE_RECOVERY_MODE=1` (with visible WARNING) |
| Scripts | `scripts/preflight.sh` orchestrates: `audit-review-content.sh`, `audit-verify-envelope.sh`, `audit-coverage-local.sh`, `audit-execution-integrity.sh --scope=telemetry` |

### Camada 1 — Normative

Rule 24 loaded every conversation. CLAUDE.md carries "EXECUTION INTEGRITY — NÃO NEGOCIÁVEL" block. Orchestrator SKILL.md phrases every sub-skill invocation as **MANDATORY TOOL CALL**.

### Camada 2 — Runtime Stop Hook

`.claude/hooks/verify-story-completion.sh` fires on every `Stop` event. Checks evidence artifacts exist for recent story PR activity. Emits WARNING (exit 2) when evidence is absent.

### Camada 3 — CI Audit

`scripts/audit-execution-integrity.sh` runs on every PR to `develop` or `epic/*`. Verifies mandatory artifact set for each merged story. Fails with `EIE_EVIDENCE_MISSING` on absent evidence.

### Camada 4 — Observability

Telemetry NDJSON (`events.ndjson`) provides continuous audit trail. Artifact IS the proof — if it does not exist, the sub-skill was not invoked.

## Audit Script Exit Codes

| Exit | Code | Meaning |
| :--- | :--- | :--- |
| 0 | `OK` | All merged stories have required evidence (or grandfathered). |
| 1 | `EIE_EVIDENCE_MISSING` | At least one merged story lacks mandatory artifacts. |
| 2 | `EIE_BASELINE_CORRUPT` | `governance/baselines/execution-integrity-baseline.txt` malformed. |
| 3 | `EIE_INVALID_EXEMPTION` | `audit-exempt` marker missing a reason. |

## Baseline (Grandfather List)

`governance/baselines/execution-integrity-baseline.txt` lists stories merged before Rule 24 was introduced. Format: one `STORY-ID` per line with `# reason` comment. **Immutable** — no new entries after Rule 24 merges. CI refuses additions via a separate immutability check.

```
story-0051-0001  # pre-Rule-24, merged 2026-04-23, inline execution was legal at time
```

Per-story escape: `<!-- audit-exempt: <reason> -->` line in the story markdown (rare, reviewed exceptions only).

## Non-Inlining Contract

The LLM executing any skill MUST NOT:
1. Summarize what a declared sub-skill would do in place of invoking it.
2. Simulate the sub-skill's output in its own context window.
3. Skip a declared sub-skill on grounds of "trivial case", "obvious result", or similar heuristic.
4. Claim a phase passed without the sub-skill being invoked.

The only legitimate way to not invoke a declared sub-skill is via explicit `--skip-*` flag passed by the caller.
