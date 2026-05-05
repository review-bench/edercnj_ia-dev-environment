---
name: x-internal-rnf-validate
description: Validate RNF no-relax markers and justification gate for override inheritance
visibility: internal
user-invocable: false
model: haiku
allowed-tools: [Bash]
requires-capabilities: []
---

> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.

# Skill: x-internal-rnf-validate

## Purpose

Validates a list of RNF override specs against the no-relax marker and justification gate rules:

- `SECURITY` and `COMPLIANCE` are **hard-blocked** — cannot be relaxed even with justification.
- All other categories that are relaxed **require a non-blank justification**.
- Categories marked `norelax` pass unconditionally.

Delegates to `XInternalRnfValidateCommand` (CLI adapter) and `ValidateRNFNoRelaxUseCase` (application layer) which calls `RNFNoRelaxValidator` (domain).

## Parameters

| Flag | Required | Format | Description |
| :--- | :--- | :--- | :--- |
| `--override` | No (repeatable) | See formats below | One RNF override spec per flag |
| `--dry-run` | No | flag | Parse and report only; always returns exit 0 |

### Override Spec Formats

| Format | Example | Meaning |
| :--- | :--- | :--- |
| `CATEGORY:norelax` | `PERFORMANCE:norelax` | RNF carried forward unchanged |
| `CATEGORY:norelax:originalValue` | `SECURITY:norelax:TLS 1.3` | Same, with original value context |
| `CATEGORY:relaxed:originalValue:newValue:justification` | `PERFORMANCE:relaxed:P99<200ms:P99<500ms:Batch path` | Relaxed RNF with mandatory justification |

`CATEGORY` must be a valid `RNFCategory` enum value (case-insensitive): `PERFORMANCE`, `SCALABILITY`, `RELIABILITY`, `SECURITY`, `COMPLIANCE`, `OBSERVABILITY`, `DATA_INTEGRITY`, `MAINTAINABILITY`, `PORTABILITY`, `USABILITY`.

## Exit Codes

| Exit | Meaning |
| :--- | :--- |
| 0 | All overrides valid (or `--dry-run` mode) |
| 1 | Validation failure — one or more hard-block or missing-justification violations |
| 2 | Execution error — malformed spec or unexpected exception |

## Examples

### All no-relax (passes)
```bash
x-internal-rnf-validate \
  --override PERFORMANCE:norelax:P99<200ms \
  --override SECURITY:norelax:TLS1.3
# exit 0
```

### Relaxed with justification (passes)
```bash
x-internal-rnf-validate \
  --override PERFORMANCE:relaxed:P99<200ms:P99<500ms:Batch processing path
# exit 0
```

### Hard-blocked category (fails)
```bash
x-internal-rnf-validate \
  --override SECURITY:relaxed:TLS1.3:TLS1.2:Legacy client
# exit 1 — Violation: RNF category SECURITY is mandatory and cannot be relaxed
```

### Relaxed without justification (fails)
```bash
x-internal-rnf-validate \
  --override PERFORMANCE:relaxed:P99<200ms:P99<1s:
# exit 1 — Violation: RNF category PERFORMANCE override requires justification
```

### Dry run with violation (reports but exits 0)
```bash
x-internal-rnf-validate --dry-run \
  --override COMPLIANCE:relaxed:PCI-DSS-L1:PCI-DSS-L2:Cost reduction
# exit 0 (violations printed but not blocking)
```

## Integration Notes

| Caller | Usage |
| :--- | :--- |
| `x-refine-story` | Validates RNF inheritance constraints before story refinement is approved |
| `x-internal-create-story` | Validates override specs declared in epic-level story specs |

Implementation classes:
- Application: `dev.iadev.application.capability.ValidateRNFNoRelaxUseCase`
- CLI adapter: `dev.iadev.adapter.inbound.cli.XInternalRnfValidateCommand`
- Domain: `dev.iadev.domain.capability.RNFNoRelaxValidator`

**Haiku eligibility (Rule 23 §criterion a):** utility command execution — no architectural choices or design reasoning required.
