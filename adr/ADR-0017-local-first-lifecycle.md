# ADR-0017 — Local-First Lifecycle Convention

**Status:** Accepted (2026-04-28, EPIC-0061 story-0061-0006)
**Deciders:** Operator + Claude Code (EPIC-0061 planning session 2026-04-27)
**Supersedes:** —
**Related:** [Rule 26 §Camada 0](../../.claude/rules/26-audit-gate-lifecycle.md), EPIC-0061 RULE-006

---

## Context

Before EPIC-0061, governance lived in two overlapping places:

1. **Local hooks** (`.claude/hooks/verify-*.sh`, `enforce-*.sh`) — fired during LLM session,
   preventive, only on developer machines with Claude Code.
2. **Scripts** (`scripts/audit-*.sh`) and **workflow** (`.github/workflows/audit.yml`) —
   fired in CI, detectivo, running after every PR push.

This created redundancy: the same logic existed in bash (CI) and was also approximated
by hooks (local). The overlap caused:

- ~5% flakiness in `audit.yml` from shell dep resolution (`jq`, `gh` missing on runners)
- Parallel `audit.yml` job adding ~3min compute per PR
- Contributor confusion about "which layer catches what"

EPIC-0061 decision tree (operator Q2): **remove `audit.yml`; keep hooks; Java auditors
cover CI via `mvn verify`**.

---

## Decision

Adopt a **5-layer taxonomy** for governance gates with an explicit semantic distinction
between **Camada 0 (preventivo)** and **Camadas 1-4 (detectivos)**:

| Camada | When | Mode | Cannot be skipped by |
| :--- | :--- | :--- | :--- |
| 0 — Local Hooks | During LLM turn | Preventivo | Operator must have Claude Code installed |
| 1 — Normative | Each conversation | Declarativo | LLM context loading |
| 2 — CI Script | PR push | Detectivo | CI audit scripts |
| 3 — Java Test | `mvn verify` | Detectivo | Java AuditTest classes |
| 4 — CI Workflow | GitHub Actions | Detectivo — orchestrates 2/3 | GitHub Actions job |

**Camada 0 is additive**, not a replacement. Defense-in-depth requires both:
- Camada 0 catches violations early (low friction, fast feedback during development)
- Camadas 2-3 catch the same violations in CI (no dependency on local tooling)

---

## Consequences

### Positive

- **Onboarding clarity**: contributors ask "where does this gate go?" → answer is now a 2-question flowchart (preventivo? detectivo?)
- **CI cost reduction**: no `audit.yml` parallel job (~38% compute reduction)
- **Zero flakiness from shell deps**: Camada 2 (`scripts/audit-*.sh`) is now delivered to generated projects as templates, not run in the generator's CI

### Negative

- **Hooks only on dev machines**: Camada 0 doesn't run in CI runners. If a developer works without Claude Code installed, Camada 0 is skipped. Mitigated by Camada 3 (Java AuditTest in `mvn verify`).
- **More layers to document**: 5 layers require contributors to understand the distinction. Mitigated by this ADR + Rule 26 §Camada 0.

### Neutral

- Historical documentation (CHANGELOG, ADRs pre-EPIC-0061) uses "Camada 1" to mean what is now still "Camada 1" (Normative). No renumbering was done; Camada 0 was inserted before the existing sequence.

---

## Validation

`Rule26CamadaZeroSmokeIT` (EPIC-0061 story-0061-0006 TASK-0061-0006-004) validates:
1. Rule 26 §Camada 0 section exists
2. ADR-0017 is referenced from Rule 26
3. All `verify-*.sh` and `enforce-*.sh` hooks have Camada 0 contract header comments

---

## Related

- [Rule 26 — Audit Gate Lifecycle](../../.claude/rules/26-audit-gate-lifecycle.md) §Camada 0
- EPIC-0061 RULE-006 (Camada 0 — Local Hooks Preventivos)
- [ADR-0015 — Audit Gate Lifecycle Convention](ADR-0015-audit-gate-lifecycle.md) (predecessor)
- story-0061-0005 (removes `audit.yml`, enabling this ADR)
