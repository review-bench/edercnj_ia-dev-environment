# Audit Gates Catalog

> **Maintained by:** Rule 26 §Catalog-before-Add (RULE-004).
> **Last updated:** story-0068-0004 (EPIC-0068 — Continuous-Flow Heartbeat Hook).
> **Purpose:** Single source of truth for every governance gate across all 5 layers (Camada 0–4).
>
> No gate of any layer may be introduced in any Rule, ADR, SKILL.md, or code comment
> without a simultaneous entry here. See Rule 26 §Catalog-before-Add for the enforcement contract.

---

## Layer Legend

| Camada | Layer | Trigger | Mode |
| :--- | :--- | :--- | :--- |
| **0** | Local Hook (preventive) | PreToolUse / Stop / SessionStart | **Preventive** |
| **1** | Normative | Each conversation (rules always active) | Normative |
| **2** | CI Script (`audit-*.sh`) | PR open / push | **Detectivo** |
| **3** | Java Test (`*AuditTest.java`) | `mvn verify` | **Detectivo** |
| **4** | CI Workflow (`.github/workflows/*.yml`) | GitHub Actions | **Detectivo** |

---

## Hook Runtime (Camada 0)

### enforce-continuous-flow.sh (EPIC-0068)

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 19 §interactiveMode Field, Rule 24 §Camada 0, Rule 26 §Camada 0, Rule 27 §Exception 2 |
| **Layer** | 0 — Hook runtime (preventive, Stop event) |
| **Trigger** | `Stop` event (every LLM turn end) |
| **Reads** | `execution-state.json` (`interactiveMode`, `taskTracking.openTasks`), `telemetry/events.ndjson`, current branch via `git rev-parse` |
| **Writes** | None (read-only, idempotent) |
| **Self-check** | `--self-check` validates `jq` on PATH + `ai/epics/` or `plans/` exists |
| **Exit codes** | `0` = no nudge needed · `2` = `CONTINUOUS_FLOW_INTERRUPT` nudge emitted to LLM |
| **Bypass** | None — Rule 27 hotfix exception honored automatically via branch detection (`hotfix/*` → exit 0) |
| **Related rules** | Rule 19, Rule 24, Rule 25, Rule 26, Rule 27, Rule 28 (Tool-Call Grammar) |
| **Introduced** | story-0068-0002 + story-0068-0003 (EPIC-0068) |
| **Source** | `src/main/resources/targets/claude/hooks/enforce-continuous-flow.sh` |
| **Java smoke test** | `Epic0068ContinuousFlowSmokeTest` (7 tests — decision matrix (a)–(h)) |

---

## CI Scripts (Camada 2)

### audit-bypass-flags.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 45, Rule 27 §Camada 3 |
| **Layer** | 2 — CI Script |
| **Validates** | `--no-ci-watch` and `--skip-*` flags appear only inside `## Recovery` blocks in SKILL.md |
| **Introduced** | story-0057-0005 (EPIC-0057) |
| **Exit Codes** | `0` = OK · `1` = `BYPASS_FLAG_VIOLATION` · `2` = `OPERATIONAL_ERROR` · `3` = `INVALID_EXEMPTION` |

---

### audit-capability-coverage.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Invariant 1 |
| **Layer** | 2 — CI Script |
| **Validates** | Every artefact in conditional directory declares `requires-capabilities` in frontmatter v3.0 |
| **Introduced** | story-0064-0201 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `CAPABILITY_COVERAGE_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-capability-determinism.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Audit table (row 6) |
| **Layer** | 2 — CI Script |
| **Validates** | Two consecutive compositions produce bytewise-identical output SHA per file |
| **Introduced** | story-0064-0206 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `DETERMINISM_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-capability-graph.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Invariants 3 & 4 |
| **Layer** | 2 — CI Script |
| **Validates** | Capability refs point to real IDs; mutex symmetry; no cycles; no orphans |
| **Introduced** | story-0064-0203 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `GRAPH_VIOLATION` · `2` = `AsymmetricMutex` · `3` = `UnknownCapability` |

---

### audit-coverage-local.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 05 §Coverage Thresholds, Rule 24 §Camada 0, Rule 26 |
| **Layer** | 2 — CI Script (also invoked by Camada 0 preflight) |
| **Validates** | JaCoCo report: line coverage ≥ 95%, branch coverage ≥ 90% |
| **Introduced** | story-0063-0007 (EPIC-0063) |
| **Exit Codes** | `0` = OK · `1` = `COVERAGE_BELOW_THRESHOLD` · `2` = `OPERATIONAL_ERROR` |

---

### audit-epic-branches.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 21 §Audit |
| **Layer** | 2 — CI Script |
| **Validates** | `epic/*` PRs carry `flowVersion: "2"` in `execution-state.json`; no force-push after first merge; `x-git-cleanup-branches` excludes `epic/*` |
| **Introduced** | story-0058-0004 (EPIC-0058) |
| **Exit Codes** | `0` = OK · `1` = `EPIC_BRANCH_VIOLATION` · `2` = `OPERATIONAL_ERROR` · `3` = `BASELINE_CORRUPT` |

---

### audit-execution-integrity.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 24 §Camada 3, Rule 27 §Camada 3 |
| **Layer** | 2 — CI Script |
| **Validates** | Every merged story PR has required evidence artifacts (verify envelope, reviews, completion report, CI-watch state file) |
| **Introduced** | EPIC-0052; extended story-0059-0003 (EPIC-0059) |
| **Exit Codes** | `0` = OK · `1` = `EIE_EVIDENCE_MISSING` · `2` = `EIE_BASELINE_CORRUPT` · `3` = `EIE_INVALID_EXEMPTION` |

---

### audit-flow-version.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 19 §Audit |
| **Layer** | 2 — CI Script |
| **Validates** | `flowVersion` present and in `{"1","2","3","4"}` in every `execution-state.json`; `flowVersion=2` requires `taskTracking.enabled=true` |
| **Introduced** | story-0058-0003 (EPIC-0058) |
| **Exit Codes** | `0` = OK · `1` = `FLOW_VERSION_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-fragment-coherence.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Invariant 7 |
| **Layer** | 2 — CI Script |
| **Validates** | Fragment-slots declared in parent artefact ↔ refs in body ↔ fragments on disk |
| **Introduced** | story-0064-0204 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `FRAGMENT_COHERENCE_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-frontmatter-schema.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Invariant 2 |
| **Layer** | 2 — CI Script |
| **Validates** | 100% of `.md` artefacts valid against frontmatter schema v3.0 |
| **Introduced** | story-0064-0202 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `FRONTMATTER_SCHEMA_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-model-selection.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 23 §Audit Contract |
| **Layer** | 2 — CI Script |
| **Validates** | Orchestrator SKILL.md files declare explicit `model:` in frontmatter; `Agent(...)` blocks include `model:`; agent files declare `Recommended Model:` (not `Adaptive`) |
| **Introduced** | story-0050-0009 (EPIC-0050) |
| **Exit Codes** | `0` = OK · `1` = `MODEL_SELECTION_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-output-pruning.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 §Audit table (row 5) |
| **Layer** | 2 — CI Script |
| **Validates** | For each canonical profile, `.claude/` output does not contain artefact whose required capability is absent |
| **Introduced** | story-0064-0205 (EPIC-0064) |
| **Exit Codes** | `0` = OK · `1` = `OUTPUT_PRUNING_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-phase-gates.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 25 §Enforcement Layers (Layer 4) |
| **Layer** | 2 — CI Script |
| **Validates** | Every numbered `## Phase N` in Anexo B orchestrators has a PRE gate and a POST-family gate invocation of `x-internal-phase-gate` |
| **Introduced** | story-0055-0003 (EPIC-0055) |
| **Exit Codes** | `0` = OK · `26` = `PHASE_GATE_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-pr-template.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 27 §Surface 11 (PR body `## Orchestrator Evidence`) |
| **Layer** | 2 — CI Script |
| **Validates** | PR bodies targeting `develop` or `epic/*` contain `## Orchestrator Evidence` section |
| **Introduced** | EPIC-0059 |
| **Exit Codes** | `0` = OK · `1` = `PR_TEMPLATE_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-refinement-gate.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 29 §Audit (Camada 2), Rule 27 (zero-bypass for refinement) |
| **Layer** | 2 — CI Script |
| **Validates** | Every merged story/epic on PRs to `develop` or `epic/*` has `refinementVerdict.status="approved"` in `execution-state.json`; detects state↔markdown divergence via `verdictHash`; honors hotfix exception (Rule 27 Exception 2), `audit-exempt` markers, and `governance/baselines/refinement-gate-baseline.txt` |
| **Introduced** | story-0069-0006 (EPIC-0069) |
| **Exit Codes** | `0` = OK · `1` = `REFINEMENT_GATE_VIOLATION` (sub-codes: `missing-verdict`, `rejected-verdict`, `verdict-mismatch`) · `2` = `OPERATIONAL_ERROR` · `3` = `BASELINE_CORRUPT`/`INVALID_EXEMPTION` · `4` = `RULE_29_ENFORCEMENT_BROKEN` |

---

### enforce-refinement-gate.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 29 §Camada 0, Rule 26 §Camada 0 |
| **Layer** | 0 — Local PreToolUse Hook (preventive) |
| **Validates** | Blocks invocations of `x-story-implement`, `x-epic-implement`, `x-task-implement`, `x-epic-orchestrate` when target's `refinementVerdict.status != "approved"`. Bypasses: `CLAUDE_RECOVERY_MODE=1` (Rule 27), hotfix branches (Rule 27 Exception 2), `flowVersion=1` (Rule 19) |
| **Introduced** | story-0069-0005 (EPIC-0069) |
| **Exit Codes** | `0` = OK (allow) · `33` = `REFINEMENT_REQUIRED` (block) · `2` = `OPERATIONAL_ERROR` (self-check) |

---

### audit-review-content.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 24 §Mandatory Evidence Artifacts, Rule 27 §Surfaces 4 & 5 |
| **Layer** | 2 — CI Script |
| **Validates** | `review-story-*.md` and `techlead-review-story-*.md` meet minimum content heuristics (≥50 lines, ≥3 sections, ≥2 file references, decision marker) |
| **Introduced** | story-0063-0002 (EPIC-0063) |
| **Exit Codes** | `0` = OK · `1` = `REVIEW_CONTENT_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-review-frontmatter.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 26 §CI Script, EPIC-0067 |
| **Layer** | 2 — CI Script |
| **Validates** | `review-story-*.md` and `techlead-review-story-*.md` contain well-formed YAML frontmatter v1.0 conforming to `governance/schemas/review-frontmatter-1.0.json` (10 required fields, `decision` ∈ `{GO,NO-GO,GO-WITH-RESERVATIONS}`) |
| **Introduced** | story-0067-0004 (EPIC-0067) |
| **Baseline** | `governance/baselines/review-frontmatter-baseline.txt` (empty — no pre-EPIC-0067 grandfather entries) |
| **Exit Codes** | `0` = OK · `1` = `REVIEW_FRONTMATTER_VIOLATION` · `2` = `OPERATIONAL_ERROR` · `3` = `INVALID_EXEMPTION` |
| **Flags** | `--all` · `--epic <ID>` · `--story <ID>` · `--self-check` · `--repo <path>` (context only) |

---

### audit-skill-visibility.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 22 §Audit Script, Rule 26 §RULE-004 |
| **Layer** | 2 — CI Script |
| **Validates** | `x-internal-*` skills carry `visibility: internal` + `user-invocable: false`; body marker present; no user-facing cross-references; every `audit-*.sh` reference in rules has a catalog entry here |
| **Introduced** | story-0058-0005 (EPIC-0058) |
| **Exit Codes** | `0` = OK · `1` = `SKILL_VISIBILITY_VIOLATION` · `2` = `OPERATIONAL_ERROR` · `3` = `INVALID_EXEMPTION` |

---

### audit-task-hierarchy.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 25 §Enforcement Layers (Layer 4) |
| **Layer** | 2 — CI Script |
| **Validates** | Anexo B orchestrators emit `TaskCreate`/`TaskUpdate` per phase; `subject:` matches hierarchy regex; `x-internal-phase-gate` PRE invocation present |
| **Introduced** | story-0055-0002 (EPIC-0055) |
| **Exit Codes** | `0` = OK · `25` = `TASK_HIERARCHY_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

### audit-tool-call-grammar.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 28 (tool-call-grammar) §Audit |
| **Layer** | 2 — CI Script |
| **Validates** | Every `Skill(...)` and `Agent(subagent_type: "general-purpose", ...)` in Anexo B orchestrators carries an obligation marker `[required]`, `[optional]`, or `[conditional: <expr>]` |
| **Introduced** | story-0063-0012 (EPIC-0063) |
| **Exit Codes** | `0` = OK · `1` = `GRAMMAR_MARKER_MISSING` · `2` = `OPERATIONAL_ERROR` · `3` = `BASELINE_CORRUPT` |

---

### audit-verify-envelope.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 24 §Mandatory Evidence Artifacts, Rule 27 §Surface 3 |
| **Layer** | 2 — CI Script |
| **Validates** | `verify-envelope-*.json` has `acCheckResults.length >= acCheckCount` when `passed=true` (prevents schema stubs) |
| **Introduced** | story-0063-0002 (EPIC-0063) |
| **Exit Codes** | `0` = OK · `1` = `VERIFY_ENVELOPE_VIOLATION` · `2` = `OPERATIONAL_ERROR` |

---

---

### audit-template-version.sh

| Field | Value |
| :--- | :--- |
| **Rule Anchor** | Rule 30 §Audit (EPIC-0070) |
| **Layer** | 2 — CI Script |
| **Validates** | New epic/story markdown files under `ai/epics/` use v2 template structure OR carry `--legacy-template-v1` annotation |
| **Introduced** | story-0070-0008 (EPIC-0070) — **RESERVED; script not yet delivered** |
| **Exit Codes** | `0` = OK · `1` = `TEMPLATE_VERSION_VIOLATION` · `2` = `OPERATIONAL_ERROR` · `3` = `BASELINE_CORRUPT` |

---

## Notes

- Scripts listed above are source-of-truth copies shipped to consumer projects via `ScriptsAssembler`.
- Every script MUST implement `--self-check` (Rule 26 §self-check contract).
- Exit codes 0–3 follow the Rule 26 §Standardized Exit Codes matrix (exceptions: `audit-task-hierarchy.sh` exit 25, `audit-phase-gates.sh` exit 26 — Rule 25 domain-specific codes).
- Entries are append-only. Removal requires a Rule 26 amendment and a SemVer MINOR bump.
