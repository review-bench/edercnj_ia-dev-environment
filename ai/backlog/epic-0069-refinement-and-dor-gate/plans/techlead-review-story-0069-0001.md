# Tech-Lead Review — story-0069-0001

**Epic:** EPIC-0069 (Story Refinement & DoR Gate)
**Story:** story-0069-0001 — Governance Foundation Artifacts
**Reviewer role:** Tech Lead holistic review
**Date:** 2026-04-30
**Scope:** Rule 29, ADR-0022, `capabilities/governance/refinement-gate.yaml`, KP `refinement/dimensions.md`, cross-references, backward compatibility, enforcement layer completeness, blocker analysis for stories 0002–0007

---

## VERDICT: GO (with required follow-up items)

---

## Executive Summary

The four delivered artifacts (Rule 29, ADR-0022, capability YAML, KP dimensions.md) are internally coherent, correct, and sufficient to unblock stories 0002–0007. The normative contract, the backward-compatibility story, and the capability universality claim all hold up under scrutiny. Three issues are identified — none block story 0001 completion, but two of them (F1, F2) MUST be resolved within the story 0001 PR before merge.

---

## 1. Architecture Coherence

### 1.1 State Machine Extension Ownership

**Claim under review:** Rule 29 (not Rule 22) is the correct owner of the `Refinada` status and its transitions.

**Finding: CORRECT.**

Rule 22 is "Skill Visibility" — its scope is the `x-internal-*` naming convention, frontmatter visibility fields, and the audit script that enforces them. It has no state machine content and never did. The original scaffold reference to "Rule 22 — lifecycle-integrity" was a naming error (confirmed by D-R3(b) in the cross-cutting refinement notes and by the `story-0069-0004.md` correction note). Rule 29 declares the `Refinada` state and its transitions in `## State Machine Extension`, which is the appropriate locality: the rule that introduces a gate is also the rule that names the state that gate produces.

**The state machine table is well-formed.** Seven states, six transitions, one forbidden transition (`Pendente → Em Andamento` post-EPIC-0069), and two explicit exceptions (legacy flow, hotfix). This matches the enforcement contract in `## Enforcement`.

### 1.2 Dependency Direction

Rule 29 → references Rule 19 (backward compat), Rule 24 (execution integrity), Rule 26 (audit gate taxonomy), Rule 27 (zero-bypass). All four back-references are used legitimately:

- Rule 19: `flowVersion=1` hook no-op; `refinementVerdict` absence resolves to `tbd`.
- Rule 24: mandatory evidence artifact contract (parallel to how Rule 24 audits review/verify artifacts).
- Rule 26: placement of `enforce-refinement-gate.sh` at Camada 0 (PreToolUse); `audit-refinement-gate.sh` at Camada 2 (CI Script).
- Rule 27: hotfix bypass exception re-used from Rule 27 Exception 2 — correct.

The capability YAML (`requires-capabilities: []`) creates no dependency on any other capability. Universal inclusion is architecturally sound for a process gate.

**No architecture violations found.**

---

## 2. Backward Compatibility

### 2.1 `refinementVerdict` Absence Fallback

Rule 29 §`refinementVerdict` states:

> Absence of `refinementVerdict` in a pre-EPIC-0069 state file resolves to `{ status: "tbd" }`. Hook behavior: `flowVersion=1` → hook no-op (legacy flow); `flowVersion≥2` + `status="tbd"` → hook blocks with `REFINEMENT_REQUIRED` (exit 33).

This is correct: `tbd` is a safe default (no accidental approvals), and the legacy-flow exemption prevents breaking in-flight epics. The fallback matrix has two distinct cases:

| Condition | Resolved behavior |
| :--- | :--- |
| `refinementVerdict` absent + `flowVersion=1` | hook no-op (safe legacy pass-through) |
| `refinementVerdict` absent + `flowVersion≥2` | blocks with `REFINEMENT_REQUIRED` (enforced) |
| `refinementVerdict.status = "approved"` | gate passes |
| `refinementVerdict.status = "rejected"` | gate blocks |
| `refinementVerdict.status = "tbd"` | equivalent to absent for flowVersion≥2 — blocks |

The matrix is complete and there is no ambiguous case.

### 2.2 Rule 19 Fallback Matrix Entry — MISSING (F1)

**Finding: DEFECT — non-blocking for delivery but mandatory before PR merge.**

Rule 19 §Field Additions to `execution-state.json` mandates:

> New fields added to `execution-state.json` MUST: (2) Be documented in the companion ADR and **in this rule's fallback matrix**. (3) Have a fallback entry defined **here** before any orchestrator reads them in production.

`refinementVerdict` is documented in Rule 29 and in ADR-0022. It is NOT documented as a named subsection in Rule 19's Fallback Matrix. The pattern established by `taskTracking` (EPIC-0055) and `interactiveMode` (EPIC-0068) requires a dedicated `### \`refinementVerdict\` Field (EPIC-0069)` subsection in Rule 19.

The field's backward-compatibility semantics are correctly defined in Rule 29 — but Rule 19 is the canonical aggregator that hooks and CI audit scripts consult as a unified reference. Absence from Rule 19 creates a gap between the normative standard and the specific rule's claim.

**Required action:** Add a `### \`refinementVerdict\` Field (EPIC-0069)` subsection to Rule 19's Fallback Matrix section before merging this story's PR. The content can be verbatim from Rule 29 §Backward compatibility (Rule 19) plus the two-row fallback table.

### 2.3 `--legacy-refinement` Semantics

Rule 29 defines `--legacy-refinement` as the only non-hotfix bypass. The flag is scoped to:
1. `hotfix/*` branches — correct (Rule 27 Exception 2).
2. `flowVersion=1` epics — hook no-op automatically (no flag needed).

The rule explicitly states that `CLAUDE_RECOVERY_MODE=1` does NOT bypass the refinement gate. This is intentional and correctly separated from the `--skip-review`/`--no-ci-watch` recovery paths — security posture is tighter here because refinement is a planning gate, not a post-implementation gate.

**Assessment: backward compatibility design is sound. One documentation gap (F1) requires correction before PR merge.**

---

## 3. Enforcement Layer Completeness

### 3.1 Layer Review

| Camada | Mechanism | Specified in Rule 29 | Artifact exists |
| :--- | :--- | :--- | :--- |
| 0 — PreToolUse | `enforce-refinement-gate.sh` | YES | NO — not yet created (story-0005) |
| 1 — Normative | Rule 29 + CLAUDE.md block | YES | PARTIAL — see F2 below |
| 2 — CI Script | `audit-refinement-gate.sh` | YES | NO — not yet created (story-0006) |
| 3 — Java Test | `Epic0069RefinementGateSmokeIT` | YES | NO — not yet created (story-0007) |

The hook, CI script, and Java test are deferred to stories 0005, 0006, and 0007 respectively. This is by design — story 0001 delivers the normative foundation; implementation layers follow in subsequent stories. The deferred artifacts are correctly scoped.

### 3.2 CLAUDE.md Block — MISSING (F2)

**Finding: DEFECT — non-blocking for delivery but mandatory before PR merge.**

Rule 29 §Enforcement states: "Camada 1 — Normative: This rule + CLAUDE.md block `REFINEMENT GATE — INEGOCIÁVEL`." The CLAUDE.md at the project root does NOT contain this block as of this review. The pattern for such blocks is established by Rule 27's "ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL" and Rule 24's "EXECUTION INTEGRITY — NÃO NEGOCIÁVEL."

The story-0007 scope includes the CLAUDE.md update. However, the CLAUDE.md block is a Camada 1 artifact — its absence means the normative layer is not complete at the time of merging story 0001. Story-0001 claims to deliver "the normative foundation"; the CLAUDE.md block is part of that foundation per Rule 29's own enforcement table.

Additionally, the Rules table in CLAUDE.md does not list Rule 29. The pattern requires adding `| 29 | \`29-refinement-gate.md\` | refinement gate contract (Camada 0 hook + multi-persona dispatcher + state machine extension — EPIC-0069) |` to the table.

**Required action:** Either (a) add the CLAUDE.md block and Rules table entry in this story's PR, or (b) explicitly defer both to story-0007 and mark them as OUT OF SCOPE in the task breakdown for story-0001 — the current task breakdown does not mention CLAUDE.md at all, leaving an ambiguity about ownership.

Recommendation: move the CLAUDE.md changes to story-0007 explicitly (story-0007.md already names "CLAUSE.md ganha bloco REFINEMENT GATE — INEGOCIÁVEL") and update the story-0001 task breakdown to reflect this explicitly, removing the implicit claim in Rule 29's enforcement table that Camada 1 is complete at story-0001 merge.

### 3.3 Self-Check Contract

Rule 29 §Audit self-check requires:
1. `29-refinement-gate.md` exists — YES (delivered by this story).
2. `capabilities/governance/refinement-gate.yaml` exists — YES (delivered by this story).
3. `enforce-refinement-gate.sh` is registered in `settings.json` — NO (story-0005 scope).
4. Baseline file `governance/baselines/refinement-gate-baseline.txt` exists — NO (not delivered).

Items 3 and 4 from the self-check cannot pass at the story-0001 stage. This is expected and consistent with deferred delivery. However, the baseline file (`governance/baselines/refinement-gate-baseline.txt`) is a story-0001 scope item that appears in the story's own task breakdown via the plan, yet it was not created as a deliverable. The plan lists it as a prerequisite for the self-check but does not explicitly assign it to a story. **The baseline file should be created (even if empty) as part of this story's PR**, since `audit-refinement-gate.sh --self-check` (story-0006) will check for it.

---

## 4. Cross-Reference Verification

### 4.1 Rule 29 ↔ ADR-0022

| Reference | Rule 29 | ADR-0022 | Match |
| :--- | :--- | :--- | :--- |
| ADR link | Line 5: `[ADR-0022 — Refinement Gate](../../docs/adr/ADR-0022-refinement-gate.md)` | Implementation Notes: `docs/adr/ADR-0022-refinement-gate.md` | YES |
| Capability | `governance.refinement-gate` | `governance.refinement-gate` | YES |
| Exit code | 33 / `REFINEMENT_REQUIRED` (Camada 0) | "exit `33`" | YES |
| CI code | `REFINEMENT_GATE_VIOLATION` (Camada 2) | "exit `REFINEMENT_GATE_VIOLATION`" | YES |
| Status values | `approved`, `rejected`, `tbd` | Implicit in Decision §3 | YES |
| `verdictHash` | Defined in `refinementVerdict` spec | Referenced in Consequences §Positive | YES |

**All bidirectional cross-references between Rule 29 and ADR-0022 are correct and consistent.**

### 4.2 Rule 29 ↔ KP `dimensions.md`

The KP declares `requires-capabilities: [governance.refinement-gate]`. This means the KP is only included in generated outputs for projects with the capability active — correct behavior. The KP is consumed by `x-story-refine` (story-0002) and `x-epic-refine` (story-0003) via frontmatter `context:` reference, not directly by Rule 29 itself.

The coverage matrix at the bottom of `dimensions.md` matches the persona tables in Rule 29 §Personas (Story) and §Dimensions (Story). Minor observation: Performance Engineer and SRE/DevOps rows have `—` in most cells — this is correct since they are conditional personas with narrow dimension ownership.

### 4.3 ADR-0018 Stale Reference Drift (F3)

**Finding: DOCUMENTATION DRIFT — non-blocking, informational.**

The planning artifacts produced before the D-R6 collision check (story-0069-0001.md, epic-0069.md, IMPLEMENTATION-MAP.md, story-0069-0007.md, reports/epic-execution-plan-0069.md) still reference "ADR-0018" as the working-title. This is expected — the story explicitly documents D-R6 as a working-title policy and records the resolution (ADR-0022). The actual deliverables (Rule 29, ADR-0022 file, plan-story-0069-0001.md, arch-story-0069-0001.md) all use the correct ADR-0022 number.

The stale references are in planning/tracking artifacts, not in the normative content. They are confusing to read but do not affect enforcement or auditability. **The IMPLEMENTATION-MAP.md story-0069-0004 row also has a stale "Rule 22" reference** (it says "status `Refinada` na Rule 22" — should be Rule 29), which is also a documentation artifact of the pre-D-R3 scaffold. The story-0069-0004.md file itself has already been corrected to say "Rule 29."

**Recommendation:** clean up stale ADR-0018 references in planning artifacts as a housekeeping commit within story-0001's PR. Not blocking, but reduces future reader confusion.

### 4.4 ADR-0022 Not in `docs/adr/README.md` Index (F4)

**Finding: MISSING INDEX ENTRY — non-blocking for story 0001 delivery, but the README index claim to cover "all ADRs" becomes stale.**

`docs/adr/README.md` ends at ADR-0021 and does not list ADR-0022. The file was last rebuilt post-EPIC-0062 with 22 ADRs (0001–0021 + 0048). ADR-0022 is the first new ADR added since that rebuild.

**Required action before PR merge:** add row `| ADR-0022 | [Refinement Gate Convention (EPIC-0069)](ADR-0022-refinement-gate.md) | Accepted | 2026-04-30 |` to the table in `docs/adr/README.md`.

---

## 5. Capability Universality

### 5.1 Stack Dependency Leak Check

The capability YAML declares:
- `requires-capabilities: []` — no prerequisite.
- `requires: []` — no inter-capability dependency.
- `excludes: []` — no mutex.
- `universal: true` — explicit.

The KP `dimensions.md` declares `requires-capabilities: [governance.refinement-gate]` — correct conditional inclusion (only when the gate is active).

Rule 29 references Performance Engineer as conditional on `capability build.maven.standard + perf story scope`. This is a runtime conditional on persona activation, not a capability prerequisite of the gate itself. The gate is available to all stacks; the Performance Engineer persona within the gate is activated for Maven+perf stacks. This is the correct layering.

**No stack dependency leak detected. The capability is genuinely universal.**

---

## 6. Unblocking Analysis for Stories 0002–0007

| Story | Dependency on story-0001 | Can proceed after story-0001 merge? |
| :--- | :--- | :--- |
| 0002 (`x-story-refine`) | Needs `governance.refinement-gate` declared (Rule 28 §Invariant 3); needs KP `dimensions.md` via `context:` reference | YES — both delivered |
| 0003 (`x-epic-refine`) | Same as 0002; needs epic dimensions from KP | YES — both delivered |
| 0004 (`refinementVerdict` field) | Needs Rule 29 §`refinementVerdict` spec as domain model source | YES — delivered |
| 0005 (PreToolUse hook) | Needs Rule 29 exit code `33` and capability path to read from state | YES — delivered |
| 0006 (CI audit script) | Needs capability ID, `verdictHash` spec, baseline file path | YES — capability and spec delivered. Baseline file path known. |
| 0007 (Smoke IT) | Needs story 0001 artifacts for self-check validation | YES — artifacts delivered. CLAUDE.md block deferred here. |

**All six subsequent stories can proceed. Story 0001 delivers all required foundation contracts.**

---

## 7. Issue Summary

| ID | Severity | Category | Description | Must fix before merge? |
| :--- | :--- | :--- | :--- | :--- |
| F1 | MEDIUM | Backward Compatibility | `refinementVerdict` field not documented in Rule 19 Fallback Matrix as required by Rule 19 §Field Additions | YES |
| F2 | MEDIUM | Enforcement | CLAUDE.md block `REFINEMENT GATE — INEGOCIÁVEL` and Rule 29 row missing from CLAUDE.md Rules table. Ownership ambiguity (story-0001 vs story-0007) | YES — resolve ownership explicitly in task breakdown |
| F3 | LOW | Documentation Drift | Planning artifacts (story-0069-0001.md, epic-0069.md, IMPLEMENTATION-MAP.md, story-0069-0007.md) still reference ADR-0018 and one entry still says "Rule 22" | RECOMMENDED (housekeeping) |
| F4 | LOW | Documentation | ADR-0022 missing from `docs/adr/README.md` index | YES — add before merge |
| F5 | INFO | Completeness | `governance/baselines/refinement-gate-baseline.txt` not created. Self-check item 4 in Rule 29 §Audit requires it. | RECOMMENDED — create empty file in story-0001 PR |

---

## 8. Positive Findings

- **Rule 29 §Forbidden** explicitly prohibits manual `verdictHash` divergence bypass, introduction of new bypass env vars, and adding baseline entries post-EPIC-0069 merge. All three are non-obvious bypass surfaces that less rigorous rules omit.
- **ADR-0022 alternatives section** (A through E) is unusually thorough — five rejected alternatives with specific rejection rationale. This level of documentation serves future maintainers well.
- **Persona tables** in Rule 29 include conditional columns, which is the correct design for a universal gate that adapts to project capability profiles.
- **`verdictHash`** as a tamper-detection mechanism for state↔markdown divergence is an elegant design that enables `audit-refinement-gate.sh` (story-0006) to detect manual bypass without parsing all markdown.
- **KP `dimensions.md`** correctly distinguishes between NO-GO (silent reject) and Advisory (question to operator) — the NO-GO Escalation Protocol section is precise and directly actionable by persona-agents.

---

## 9. Required Actions Before PR Merge

1. **F1 — Rule 19 update:** Add `### \`refinementVerdict\` Field (EPIC-0069)` subsection to `.claude/rules/19-backward-compatibility.md` (and the source-of-truth copy in `java/src/main/resources/targets/claude/rules/19-backward-compatibility.md`) with the fallback matrix from Rule 29 §Backward compatibility.

2. **F2 — Scope clarification:** Update `ai/epics/epic-0069-refinement-and-dor-gate/plans/tasks-story-0069-0001.md` to explicitly call out CLAUDE.md changes as OUT OF SCOPE for story-0001 (moved to story-0007). Remove the implicit gap between Rule 29's enforcement table claim ("Camada 1 normative") and story-0001's deliverables.

3. **F4 — ADR README:** Add ADR-0022 row to `docs/adr/README.md`.

4. **F5 (recommended) — Baseline file:** Create empty `governance/baselines/refinement-gate-baseline.txt` with a header comment documenting the baseline is intentionally empty (no pre-EPIC-0069 epics exempted).

5. **F3 (recommended) — Stale references:** Update planning artifacts to replace ADR-0018 with ADR-0022 and the IMPLEMENTATION-MAP story-0004 summary to say "Rule 29" instead of "Rule 22."

---

## Orchestrator Evidence

- **Specialist review:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/review-story-0069-0001.md` (GO)
- **Architecture plan:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/arch-story-0069-0001.md`
- **Implementation plan:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/plan-story-0069-0001.md`
- **Test plan:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/tests-story-0069-0001.md`
- **Task breakdown:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/tasks-story-0069-0001.md`
- **Security assessment:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/security-story-0069-0001.md` (LOW risk)
- **Compliance assessment:** `ai/epics/epic-0069-refinement-and-dor-gate/plans/compliance-story-0069-0001.md` (LOW risk)
