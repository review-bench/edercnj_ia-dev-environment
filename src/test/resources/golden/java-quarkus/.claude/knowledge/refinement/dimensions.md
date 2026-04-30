---
name: refinement-dimensions
description: Shared heuristics for story and epic refinement dimensions — acceptance and rejection rules used by x-story-refine and x-epic-refine persona-agents
requires-capabilities: [governance.refinement-gate]
---

# Knowledge Pack: Refinement Dimensions

Shared heuristics used by `/x-story-refine` and `/x-epic-refine` persona-agents.
Each persona-agent loads this KP alongside its own `agents/core/<persona>.md` to apply consistent accept/reject rules.

---

## Story Dimensions (6 Required + 1 Advisory)

### 1. Persona

**Accept when:**
- Role is specific and represents a real user type (e.g., "Como desenvolvedor backend que consome a API de pagamentos")
- Role implies a real interaction with the system
- Persona differs from what would apply to every other story in the epic

**Reject (NO-GO — silent) when:**
- Persona is `"sistema"`, `"usuário do sistema"`, `"alguém"`, or equivalent generic
- Persona is copied verbatim from the epic header without differentiation
- Persona is a team role internal to development ("como desenvolvedor", "como QA") for a production feature story

---

### 2. Value

**Accept when:**
- Value proposition is falsifiable: there is a concrete outcome that could be measured or observed
- Connects the feature to a business or user outcome (not just describes what the feature does)
- Completes the sentence: "so that [measurable outcome]"

**Reject (NO-GO — silent) when:**
- Value is pure description: "para que o sistema funcione corretamente"
- Value is circular: "para que eu possa usar a funcionalidade"
- Value is vague improvement: "para melhorar a experiência" without any anchor

---

### 3. AC — Acceptance Criteria

**Accept when:**
- Written in Gherkin (`Given/When/Then`) or equivalent structured form
- Covers **all 4 mandatory categories**:
  1. Happy-path (main success scenario)
  2. Error/boundary (at least one negative or edge case)
  3. Performance/SLA (latency, throughput, or availability target with unit)
  4. Security/auth (authentication, authorization, or data protection scenario)

**Reject (NO-GO — silent) when:**
- AC has no error/boundary scenario
- AC has no performance or SLA scenario
- AC has no security/auth scenario
- AC uses language without a testable assertion ("o sistema deve responder corretamente", "a API deve ser rápida")
- AC has fewer than 3 distinct scenarios total

**Advisory (question, not NO-GO):**
- AC has ≥8 scenarios — suggest grouping or splitting the story

---

### 4. Contracts

**Accept when:**
- All request and response types are explicitly declared (Java types, JSON schema, or equivalent)
- Event schemas are declared for event-driven interactions
- No `Object`, `Map<String, Any>`, `dynamic`, or untyped equivalents in the public contract surface

**Reject (NO-GO — silent) when:**
- Contract uses `Object` or `Map<String, Any>` as payload type
- Response type is undeclared ("retorna JSON")
- Event schema is absent for a story that produces or consumes events

**Advisory (question):**
- Contract references types from other bounded contexts — confirm ownership/import strategy

---

### 5. Metrics

**Accept when:**
- At least one metric has a **unit** and a **target** (e.g., "p95 latency < 200ms", "error rate < 0.1%", "throughput ≥ 500 rps")
- Metric is observable (can be measured by a tool or test)

**Reject (NO-GO — silent) when:**
- Metric section is absent or empty
- Metric is qualitative only: "deve ser rápido", "deve ser confiável"
- Metric has no unit: "latência aceitável"
- Metric has no target: "latência em ms"

---

### 6. Alternatives

**Accept when:**
- At least one alternative design or approach was considered
- Each alternative has a rationale for rejection (even brief: "rejected because adds complexity without proportional benefit")

**Reject (NO-GO — silent) when:**
- No alternatives section or section is empty
- Single approach documented with no consideration of alternatives

**Advisory (question):**
- Alternative was a close call — ask operator to document the trade-off more explicitly

---

### 7. Risks (Advisory — becomes required for COMPLEX scope)

**Accept when:**
- At least one risk is identified (dependency, external API, data migration, performance degradation, security vector)
- Mitigation or acceptance noted for each risk

**Advisory (question when absent, NO-GO for COMPLEX):**
- Stories touching external APIs, database migrations, or sensitive data with no risks declared → ask operator to enumerate risks

---

## Epic Dimensions (7 Required)

### 1. Problem

**Accept when:**
- Problem stated as an observable operational pain with evidence
- Evidence may be: user feedback, metric, incident report, or operational observation
- Problem is falsifiable — a solution would be detectable

**Reject when:**
- Problem is vague: "melhorar a experiência", "aumentar a qualidade"
- Problem lacks evidence anchor

---

### 2. Persona (Epic-level)

**Accept when:**
- Affected persona is broader than a single user role — may be a user segment, a team, or the system itself when relevant
- Persona connects to the stated problem

**Reject when:**
- Persona is absent or is a copy-paste of the system description

---

### 3. Value Hypothesis

**Accept when:**
- Hypothesis in `If [action] then [outcome] because [mechanism]` form or equivalent
- Outcome is measurable or observable

**Reject when:**
- Hypothesis absent or is pure description of features delivered

---

### 4. OKRs/KPIs

**Accept when:**
- At least one OKR or KPI with unit and measurement method defined
- Measurement method is concrete (telemetry, CI report, user survey with sample size, etc.)

**Reject when:**
- No KPIs defined
- KPIs are qualitative only or lack measurement method

---

### 5. Alternatives

**Accept when:**
- At least two strategic alternatives considered (e.g., "build vs buy", "in this epic vs next", "scope A vs scope B")
- Each alternative has a rejection rationale

**Reject when:**
- No alternatives section
- Fewer than 2 alternatives documented

---

### 6. Risks

**Accept when:**
- At least one product risk (scope creep, market assumption, persona mismatch) identified
- At least one technical risk (architectural, performance, security, dependency) identified

**Reject when:**
- No risks section
- Only product risks (no technical) or only technical risks (no product)

---

### 7. Scope

**Accept when:**
- In-scope list is explicit
- Out-of-scope list has **≥3 items** (heuristic: if you can't name 3 things this epic intentionally excludes, the scope is probably under-defined)

**Reject when:**
- No out-of-scope list
- Out-of-scope has fewer than 3 items (advisory → question to operator, not hard NO-GO)

---

## NO-GO Escalation Protocol

When a persona-agent applies a NO-GO silently, it MUST:

1. Return `verdict.dimension.<dimensionName> = { checked: false, blocker: "<specific reason citing rule/dimension>" }` in its gap-report
2. NOT convert the NO-GO into a question — questions imply the outcome is negotiable
3. Include the blocker text in `refinementVerdict.blockers[]` of the consolidated verdict

When all personas have submitted their gap-reports and one or more have `blocker ≠ null`, the consolidated `refinementVerdict.status = "rejected"` regardless of operator responses. Operator must edit the story/epic to resolve the blocker and re-run the skill.

---

## Dimension Coverage Matrix

| Persona | persona | value | ac | contracts | metrics | alternatives | risks |
|---------|---------|-------|----|-----------|---------|--------------|-------|
| Product Owner | ✓ primary | ✓ primary | ✓ completeness | — | — | — | — |
| Tech Lead | — | — | ✓ typed | ✓ primary | — | ✓ | — |
| Architect | consolidation | — | — | ✓ arch | — | ✓ arch | ✓ arch |
| Security Engineer | — | — | ✓ auth scenario | — | — | — | ✓ security |
| QA Engineer | — | — | ✓ coverage | — | — | — | ✓ quality |
| Performance Engineer | — | — | ✓ perf scenario | — | ✓ primary | — | ✓ perf |
| SRE/DevOps | — | — | — | — | ✓ SLOs | — | ✓ operational |
