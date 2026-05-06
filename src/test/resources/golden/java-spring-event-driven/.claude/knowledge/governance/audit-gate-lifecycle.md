---
name: kp-governance-audit-gate-lifecycle
description: "Full reference for Rule 26 Audit Gate Lifecycle: 5-layer decision tree, Camada 0 hook contract, naming conventions, exit codes 0-3, and --self-check template."
requires-capabilities: []
---

# Knowledge Pack: Audit Gate Lifecycle (Rule 26 — Full Reference)

## Decision Tree (5 Camadas)

```
New gate proposed
│
├─ Needs to run during LLM turn?  ── Yes ──► Camada 0 — Hook runtime (.claude/hooks/)
│
└─ No
   │
   ├─ Requires structured code parsing (AST, class loading)?  ── Yes ──► Camada 3 — Java test (src/test/java/)
   │
   └─ No
      │
      ├─ Checks artifacts on PR / push?  ── Yes ──► Camada 2 — CI script (scripts/audit-*.sh)
      │
      └─ Orchestrates other gates?  ──────────── Yes ──► Camada 4 — CI workflow (.github/workflows/)
```

| Camada | Layer | Location | Mode |
| :--- | :--- | :--- | :--- |
| **0** | Local Hooks Preventivos | `.claude/hooks/verify-*.sh`, `enforce-*.sh` | **Preventive** — blocks before artifact produced |
| **1** | Normative | `.claude/rules/*.md`, `CLAUDE.md` | Normative — LLM context |
| **2** | CI Script | `scripts/audit-*.sh` | Detectivo — stateless repo check |
| **3** | Java Test | `*AuditTest.java` or `*Lint.java` | Detectivo — code parsing |
| **4** | CI Workflow | `.github/workflows/*.yml` | Detectivo — orchestrates 2/3 |

## Camada 0 — Local Hooks Preventivos

| Property | Value |
| :--- | :--- |
| Trigger | Claude Code events: `SessionStart`, `PreToolUse`, `PostToolUse`, `Stop` |
| Location | `.claude/hooks/verify-*.sh` and `enforce-*.sh` |
| Exit codes | `0` = OK (silent) · `2` = WARNING (surfaced to LLM as blocking notification) |
| Latency | < 500ms |
| Telemetry | `CLAUDE_TELEMETRY_DISABLED=1` suppresses telemetry but NOT the hook itself |
| Defense | Preventive — blocks BEFORE artifact produced; complemented by Camadas 2-3 |

**Preventivo ≠ Substituto de Detectivo.** Camada 0 runs only on developer machines. CI runners do NOT run Camada 0. Both layers are required — neither can be omitted.

### Hook Header Contract

Every `verify-*.sh` and `enforce-*.sh` in `.claude/hooks/` MUST include:

```bash
#!/usr/bin/env bash
# Layer:      0 (preventive — fires during LLM turn)
# Trigger:    Stop | PreToolUse | PostToolUse | SessionStart
# Event:      <claude-code-event-name>
# Exit codes: 0=OK, 2=WARNING (surfaced to LLM), non-zero=block tool call
# Latency:    < 500ms
# Telemetry:  emits to NDJSON when CLAUDE_TELEMETRY_DISABLED != 1
```

## Naming Conventions

### Hook Runtime (Camada 0)

| Prefix | Purpose | Examples |
| :--- | :--- | :--- |
| `verify-*.sh` | Read-only checks; exit 2 on violation | `verify-phase-gates.sh`, `verify-story-completion.sh` |
| `enforce-*.sh` | Active enforcement; blocks tool call | `enforce-no-bypass-flags.sh`, `enforce-phase-sequence.sh` |
| `session-*.sh` | Session lifecycle hooks | `session-start.sh` |

### Other Layers

| Layer | Pattern | Examples |
| :--- | :--- | :--- |
| CI script | `audit-{subject}.sh` (prefix mandatory) | `audit-flow-version.sh`, `audit-skill-visibility.sh` |
| Java test | `{Subject}AuditTest.java` or `{Subject}Lint.java` | `LifecycleIntegrityAuditTest.java` |
| CI workflow | `{action}.yml` (kebab-case) | `ci.yml`, `release.yml` |

## Exit Codes (CI Scripts)

| Code | Meaning | Named constant | Example |
| :--- | :--- | :--- | :--- |
| 0 | Success | — | — |
| 1 | Violation detected | `<RULE>_VIOLATION` | `FLOW_VERSION_VIOLATION: ...` |
| 2 | Operational error | `OPERATIONAL_ERROR` | `OPERATIONAL_ERROR: jq not found on PATH` |
| 3 | Baseline/exemption corrupt | `BASELINE_CORRUPT` | `BASELINE_CORRUPT: file malformed at line 12` |

## `--self-check` Template

Every `audit-*.sh` MUST implement `--self-check`:

```bash
case "${1:-}" in
  --self-check)
    command -v jq >/dev/null 2>&1 || { echo "OPERATIONAL_ERROR: jq required" >&2; exit 2; }
    [[ -f "governance/baselines/my-baseline.txt" ]] || { echo "OPERATIONAL_ERROR: baseline missing" >&2; exit 2; }
    exit 0
    ;;
esac
```

Exit 0 = structurally valid; exit 2 (`OPERATIONAL_ERROR`) = any structural prerequisite missing.
