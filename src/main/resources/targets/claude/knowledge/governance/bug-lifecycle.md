# Knowledge Pack: Bug Lifecycle Management

**Capability:** governance.bug-lifecycle  
**Version:** 1.0  
**Epic:** EPIC-0080

---

## Overview

The Bug Lifecycle Management capability provides structured, deterministic workflows for creating, decomposing, refining, and resolving software bugs. Every bug follows a standardized 9-section RA9 template and decomposes into 2-4 implementation stories based on severity and scope.

---

## Status Lifecycle

```
Pendente (initial, unfiled)
  ↓ [/x-refine-bug approved]
Refinada (ready for investigation)
  ↓ [investigation opens]
Em Investigação (root-cause in progress)
  ↓ [fix assigned]
Em Correção (fix implementation in progress)
  ↓ [regression test GREEN]
Concluída (resolved and deployed)

Optional transitions:
→ Bloqueada (external dependency blocking)
→ Descartada (duplicate / wontfix / superseded)
→ Falha (fix validation failed → back to Em Correção)
```

---

## Decomposition Rules Table

| `severity` | `scope` | Stories produced |
| :--------- | :------ | :--------------- |
| any | single-file | story-01-regression-test, story-02-fix |
| any | single-module | story-01-regression-test, story-02-fix |
| HIGH+ | cross-module | story-01-regression-test, story-02-fix, story-03-doc-update |
| CRITICAL | any | story-01-regression-test, story-02-fix, story-03-doc-update, story-04-rollback-plan |

### Story Blocked-By Chain

```
story-01-regression-test  ← root (no dependency)
         ↓ blocks
story-02-fix              ← blocked by story-01
         ↓ blocks
story-03-doc-update       ← blocked by story-02
story-04-rollback-plan    ← blocked by story-02 (parallel to story-03)
```

This chain enforces TDD ordering: regression test is authored RED before fix turns it GREEN.

---

## Severity Levels

| Level | Criteria | Response Time |
| :---- | :------- | :------------ |
| LOW | Cosmetic, minor inconvenience | Next sprint |
| MEDIUM | Feature partially broken, workaround exists | Within current sprint |
| HIGH | Multiple users affected, no clean workaround | Within 24 hours |
| CRITICAL | System unavailable, data at risk | Immediate response |

---

## Scope Classifications

| Scope | Definition | Typical Effort |
| :---- | :--------- | :------------- |
| single-file | Change confined to one source file | < 1 hour |
| single-module | Change confined to one package/module | 1–4 hours |
| cross-module | Change spans multiple packages/layers | > 4 hours |

---

## Artifact Hierarchy

```
ai/bugs/bug-NNNNNN/
├── bug.md                       # Parent bug (9-section RA9 template)
├── story-01-regression-test.md  # Test story (always present)
├── story-02-fix.md              # Fix story (always present)
├── story-03-doc-update.md       # Doc story (HIGH+/cross-module or CRITICAL)
└── story-04-rollback-plan.md    # Rollback story (CRITICAL only)
```

---

## Template References

| Template | Path | Use |
| :------- | :--- | :-- |
| Bug report | `src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md` | Used by x-create-bug |
| Bug story | `src/main/resources/targets/claude/templates/_TEMPLATE-BUG-STORY.md` | Used by x-internal-decompose-bug |

---

## Skill References

| Skill | Path | Role |
| :---- | :--- | :--- |
| x-create-bug | `.claude/skills/dev/x-create-bug/SKILL.md` | User-facing: create bug |
| x-internal-decompose-bug | `.claude/skills/core/internal/plan/x-internal-decompose-bug/SKILL.md` | Internal: decompose into stories |

---

## Idempotency Contract

All decomposition invocations are idempotent:
- If `story-01-regression-test.md` already exists → exit 0 with `idempotent: true`
- No existing files are modified
- No new commits are created on idempotent runs

---

## Security Constraints

1. **Bug ID validation:** `^bug-[0-9]{6}$` — enforced before any file I/O
2. **Path traversal prevention:** bug-id normalized and anchored to `ai/bugs/`
3. **Description PII:** descriptions must not contain credentials, tokens, or personal data
4. **Reproduction steps:** no SQL concatenation, credentials, or shell injection in examples

---

## Telemetry Events

| Event | Trigger |
| :---- | :------ |
| `tool.call` | x-create-bug invocation |
| `phase.start` | Bug refinement begins |
| `phase.end` | Bug refinement completes |
| `session.start` | Bug lifecycle session opens |
| `session.end` | Bug lifecycle session closes |

**KPI:** P75 total resolution time (Pendente → Concluída) ≤ 8 minutes.

---

## Implementation Map (EPIC-0080)

| Story | Focus | Key Deliverables |
| :---- | :---- | :--------------- |
| story-0080-0001 | Foundation | _TEMPLATE-BUG.md, x-create-bug, capability declaration |
| story-0080-0002 | Decomposition | _TEMPLATE-BUG-STORY.md, x-internal-decompose-bug, wiring |
| story-0080-0003 | Map | Implementation map linking bug→stories→tasks |
| story-0080-0004 | Refinement | x-refine-bug, refinement gate enforcement |
| story-0080-0005 | Enforcement | audit-bug-classification.sh, CI gate |
| story-0080-0006 | PR Lifecycle | Bug PR template, status transitions in PRs |
