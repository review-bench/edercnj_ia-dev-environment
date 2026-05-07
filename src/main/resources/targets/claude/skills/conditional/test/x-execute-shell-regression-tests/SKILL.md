---
name: x-execute-shell-regression-tests
description: "Regression-shell gate: runs scenario scripts vs baseline; blocks merge on diffs."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
requires-capabilities: []
requires-any:
  - quality.regression.self
  - quality.regression.service
argument-hint: "<STORY-ID> [--mode self|service] [--update-baseline] [--scenarios-file <path>]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-execute-shell-regression-tests

Regression-shell quality gate. Reads `QualityConfig.regression` from the project YAML,
dispatches scenario scripts, captures output, diffs against
`governance/baselines/regression-baseline.json`, and exits non-zero when unexpected
divergence is detected.

Two modes:

- **self** — generator validates its own output: runs `ia-dev-env generate` on a
  reference YAML config and compares the produced `.claude/` directory against the
  stored golden snapshot.
- **service** — client project validates its services: executes the scenario scripts in
  `tests/regression/scenarios.yaml` (HTTP/gRPC/CLI) and compares responses against
  recorded baselines.

## Triggers

- `/x-execute-shell-regression-tests story-0073-0001` — auto-detects mode from `quality.regression.mode`
- `/x-execute-shell-regression-tests story-0073-0001 --mode self` — force self mode
- `/x-execute-shell-regression-tests story-0073-0001 --mode service` — force service mode
- `/x-execute-shell-regression-tests story-0073-0001 --update-baseline` — record new baseline (opt-in)
- `/x-execute-shell-regression-tests story-0073-0001 --scenarios-file tests/regression/custom.yaml` — override scenarios file

## Parameters

| Flag | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `STORY-ID` | String | — | Required. Story being gated. |
| `--mode` | Enum | from config | Override regression mode (`self` or `service`). |
| `--update-baseline` | Boolean | false | Write new baseline; do not fail on divergence. |
| `--scenarios-file` | String | from config | Override path to `scenarios.yaml`. |
| `--report` | String | auto | Output report path. |

## Phase 1 — Parse & Configure

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-execute-shell-regression-tests Phase-1-Parse`

Read `quality.regression` from project YAML:

```bash
quality:
  regression:
    enabled: true
    mode: service          # self | service
    scenarios-file: tests/regression/scenarios.yaml
```

Resolve effective mode: `--mode` flag overrides YAML; YAML overrides default `service`.

When `quality.regression.enabled=false` → emit `WARN: regression gate disabled` and exit 0.
When `--mode self` and no generator binary found → exit 2 `OPERATIONAL_ERROR`.
When `--mode service` and scenarios file absent → exit 1 `REGRESSION_SCENARIOS_MISSING`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-execute-shell-regression-tests Phase-1-Parse ok`

## Phase 2 — Execute Scenarios

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-execute-shell-regression-tests Phase-2-Execute`

### Self mode

```bash
# Generate to a temp dir and diff against golden
ia-dev-env generate --config <reference-config.yaml> --output /tmp/regression-output/
diff -r --exclude="*.log" governance/baselines/regression-self-golden/ /tmp/regression-output/
```

Capture exit code and diff output. Any diff line counts as a divergence unless the path
is listed in `governance/baselines/regression-self-baseline.txt` (exempt list).

### Service mode

Execute each scenario in `scenarios.yaml` using the declared tool:

| Scenario `type` | Tool | Min version |
| :--- | :--- | :--- |
| `rest` | `curl` | 7.68 |
| `grpc` | `grpcurl` | 1.8 |
| `websocket` | `wscat` | 5.0 |
| `cli` | native shell | — |

For each scenario:
1. Run the command.
2. Capture stdout/stderr + exit code.
3. Compare against `governance/baselines/regression-baseline.json` (scenario ID → expected response hash).
4. Record PASS/FAIL per scenario.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-execute-shell-regression-tests Phase-2-Execute ok`

## Phase 3 — Report & Gate

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-execute-shell-regression-tests Phase-3-Report`

Produce report at `ai/epics/epic-XXXX/reports/regression-report-STORY-ID.md` using
`_TEMPLATE-REGRESSION-SHELL.md`.

When `--update-baseline`:
- Write new `governance/baselines/regression-baseline.json` from current run results.
- Append entry to `governance/baselines/regression-baseline-updates.log` (append-only).
- Exit 0.

When NOT `--update-baseline`:
- Count total scenarios, passed, failed.
- If `failed > 0` → exit 1 `REGRESSION_DETECTED`.
- If `failed == 0` → exit 0.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-execute-shell-regression-tests Phase-3-Report ok`

## Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | `OK` | All scenarios passed (or gate disabled). |
| 1 | `REGRESSION_DETECTED` | ≥1 scenario diverged from baseline. |
| 2 | `OPERATIONAL_ERROR` | Tool missing, scenarios file absent, or binary not found. |
| 3 | `BASELINE_CORRUPT` | `governance/baselines/regression-baseline.json` malformed. |

## Integration Notes

- Invoked by `x-implement-story` Phase 3.Q when `quality.regression.enabled=true` (conditional gate — EPIC-0073, Rule 24).
- Evidence artifact: `ai/epics/epic-XXXX/reports/regression-report-STORY-ID.md` (Rule 24 §Mandatory Evidence Artifacts).
- Baseline updates require an entry in `governance/baselines/regression-baseline-updates.log` — silent overwrites fail `audit-regression-shell.sh`.
- Internal skill `x-internal-update-status` is NOT invoked here; status updates are the caller's responsibility.
