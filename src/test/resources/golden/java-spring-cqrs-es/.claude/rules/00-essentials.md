---
requires-capabilities: []
---
# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

## §1. Project Identity

- **Name:** my-spring-cqrs
- **Purpose:** Describe your CQRS/ES service here
- **Architecture:** cqrs
- **Language:** java 21
- **Framework:** spring-boot 3.x
- **Interfaces:** rest, event-consumer, event-producer

### Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | cqrs |
| Language | java 21 |
| Framework | spring-boot 3.x |
| Build Tool | gradle |
| Database | none |
| Migration | none |
| Cache | none |
| Container | docker |
| Orchestrator | none |
| Resilience | Mandatory (always enabled) |

### Constraints

- Cloud-Agnostic: ZERO dependencies on cloud-specific services
- Horizontal scalability: Application must be stateless
- Externalized configuration: All configuration via environment variables or ConfigMaps


---

## §2. Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 |
| Line width | ≤ 120 characters |
| Train wreck depth | ≤ 2 levels |

**Coverage (RULE-005-01 — Absolute Gate):** ≥ 95% line / ≥ 90% branch. No pre-existing exemption. Mutation score ≥ 80% when enabled.

**TDD:** Red-Green-Refactor mandatory. Test-first commits required. Weak assertions forbidden (`isNotNull()` alone is never sufficient).

**Error handling:** NEVER return null. NEVER pass null as argument. Exceptions MUST carry context.

> Full reference: `Read .claude/knowledge/governance/rules/coding-standards-rule.md` and `.claude/knowledge/governance/rules/quality-gates.md`

---

## §3. Architecture Golden Rule

**Architecture:** {{ARCHITECTURE}} ({{ARCH_STYLE}})

**Dependency direction:** Inward only.

```
adapter.inbound → application → domain ← adapter.outbound
```

**Domain MUST have zero external library imports.** If domain needs I/O or serialization → define a port interface, implement in adapter.

| Layer | Can depend on | Cannot depend on |
|-------|--------------|-----------------|
| domain | Standard library only | adapter, application, framework |
| application | domain.* | adapter.*, framework |
| adapter.inbound | application, domain.port | adapter.outbound |
| adapter.outbound | domain.port, domain.model | adapter.inbound |

Implementation order: domain → ports → adapters → application → inbound → tests.

> Full reference: `Read .claude/knowledge/governance/rules/architecture-summary.md`

---

## §4. Forbidden — Top Level

**Code quality:**
- Boolean flags as function parameters
- Mutable global state / God classes
- `sleep()` for synchronization
- `System.out` / `print()` in production code
- Wildcard imports, dead code, duplicated utility methods

**Security:**
- Hardcoded secrets, tokens, or credentials anywhere in source
- `Math.random()` for security-sensitive values (use `SecureRandom`)
- SQL concatenation with user input (use parameterized queries)
- Trust-all TLS (`X509TrustManager` with empty implementations)
- Path operations without normalization and prefix verification
- Deserializing untrusted input without explicit safe mode

**Operations:**
- Hardcoded timeouts, ports, or hostnames
- Logging sensitive data (PII, credentials)

> Full reference: `Read .claude/knowledge/governance/rules/security-baseline.md` and `.claude/knowledge/security/anti-patterns-java.md`

---

## §5. Lifecycle Integrity Contract

Every story MUST be implemented via `x-implement-story`. Every task via `x-implement-task`. No PR to `epic/*` or `develop` may be merged without all 13 surface evidence artifacts.

### `flowVersion` Table

| Value | Semantics |
| :--- | :--- |
| `"1"` | Legacy — story PRs → develop; no epic branch |
| `"2"` | Story PRs → epic/XXXX; task tracking required |
| `"3"` | Local-First — non-interactive default |
| `"4"` | v4 layout: `ai/epics/<epic-slug>/` via PathResolver |
| `"5"` | Product-First — `productFirstLifecycle: true` |

Field absent → defaults to `"1"` (legacy) with WARNING.

### 5 Non-Negotiable Invariants

1. `flowVersion` resolved in `execution-state.json`
2. All mandatory evidence artifacts present before PR merge
3. `refinementVerdict.status = "approved"` before any implement call (Camada 0: exit 33 `REFINEMENT_REQUIRED`)
4. CI-watch state file exists for every merged PR
5. Every story/task traceable to orchestrator skill

### 3 Bypass Exceptions Only

1. `--legacy-flow` for `flowVersion=1` epics
2. `hotfix/*` branches with `## Hotfix Bypass Justification` in PR body
3. `CLAUDE_RECOVERY_MODE=1` — `--skip-review` and `--no-ci-watch` only; NEVER bypasses refinement gate

> Full reference: `Read .claude/knowledge/governance/rules/lifecycle-contract.md`

---

## §6. Skill Invocation Protocol

**Forbidden** in delegation contexts: bare-slash `/x-foo`. Permitted ONLY in `## Triggers` / `## Examples` sections.

### 3 Permitted Delegation Patterns

| Pattern | When | Call shape |
| :--- | :--- | :--- |
| **1 — INLINE-SKILL** | Synchronous call, act on return | `Skill(skill: "x-foo", args: "...")` |
| **2 — SUBAGENT-GENERAL** | Parallel/isolated work | `Agent(subagent_type: "general-purpose", ...)` |
| **3 — SUBAGENT-RESEARCH** | Read-only exploration | `Agent(subagent_type: "Explore", ...)` |

**Pattern 1:** `Skill(skill: "x-foo", args: "--flag value")`

**Pattern 2:** `Agent(subagent_type: "general-purpose", description: "...", prompt: "FIRST ACTION: TaskCreate(...). Invoke x-foo: Skill(skill: \"x-foo\", ...). LAST ACTION: TaskUpdate(...).")`

Declare `model:` explicitly on every `Skill(...)` and `Agent(...)` inside orchestrators.

> Full reference: `Read .claude/knowledge/governance/rules/skill-invocation.md`

---

## §7. Knowledge Pack Index

| Topic | KP Path |
| :--- | :--- |
| Coding standards | `.claude/knowledge/governance/rules/coding-standards-rule.md` |
| Architecture summary | `.claude/knowledge/governance/rules/architecture-summary.md` |
| Quality gates | `.claude/knowledge/governance/rules/quality-gates.md` |
| Security baseline | `.claude/knowledge/governance/rules/security-baseline.md` |
| Security anti-patterns (Java) | `.claude/knowledge/security/anti-patterns-java.md` |
| Operations baseline | `.claude/knowledge/governance/rules/operations-baseline.md` |
| Release process | `.claude/knowledge/governance/rules/release-process.md` |
| Branching model | `.claude/knowledge/governance/rules/branching.md` |
| Skill invocation protocol | `.claude/knowledge/governance/rules/skill-invocation.md` |
| Skill visibility | `.claude/knowledge/governance/rules/skill-visibility.md` |
| Model selection | `.claude/knowledge/governance/rules/model-selection.md` |
| Project scope | `.claude/knowledge/governance/rules/project-scope.md` |
| Epic branch model | `.claude/knowledge/governance/rules/epic-branch-model.md` |
| Interactive gates | `.claude/knowledge/governance/rules/interactive-gates.md` |
| Task hierarchy | `.claude/knowledge/governance/rules/task-hierarchy.md` |
| Audit gate lifecycle | `.claude/knowledge/governance/rules/audit-gate-lifecycle.md` |
| Lifecycle integrity contract | `.claude/knowledge/governance/rules/lifecycle-contract.md` |
| Capability frontmatter | `.claude/knowledge/governance/rules/capability-frontmatter.md` |
| Tool-call grammar | `.claude/knowledge/governance/rules/tool-call-grammar.md` |
| Value-driven templates | `.claude/knowledge/governance/rules/value-driven-templates.md` |
| Documentation freshness gate | `.claude/knowledge/governance/rules/doc-freshness-gate.md` |
| Dependency policy gate | `.claude/knowledge/governance/rules/dependency-policy.md` |
| AI memory production | `.claude/knowledge/governance/rules/ai-memory-production.md` |
| Domain template | `.claude/knowledge/governance/rules/domain-template.md` |
| Lifecycle KPs (full) | `.claude/knowledge/lifecycle/` |
| Architecture patterns | `.claude/knowledge/architecture.md` |
| Testing conventions | `.claude/knowledge/testing.md` |
| Language standards | `.claude/knowledge/coding-standards.md` |
