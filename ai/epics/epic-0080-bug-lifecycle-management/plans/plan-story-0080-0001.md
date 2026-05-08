---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Implementation Plan — story-0080-0001

`/x-create-bug` skill scaffolding + `_TEMPLATE-BUG.md` (markdown-only delivery).

## Section 1: Impact Analysis

**Components affected (CREATE-only — no production Java touched):**

| Path | Action | Notes |
| :--- | :--- | :--- |
| `src/main/resources/shared/templates/_TEMPLATE-BUG.md` | CREATE | RA9 v2 (9 sections) + bug subsections |
| `src/main/resources/targets/claude/capabilities/governance/bug-lifecycle.yaml` | CREATE | Frontmatter v3.0 capability declaration |
| `src/main/resources/targets/claude/skills/core/plan/x-create-bug/SKILL.md` | CREATE | Skill body, INLINE-SKILL delegation only |
| `src/test/java/dev/iadev/skills/bug/SlugGeneratorTest.java` | CREATE | Unit test for AC-4 path-traversal safety |
| `src/test/java/dev/iadev/skills/bug/BugCreationSmokeIT.java` | CREATE | Acceptance smoke (AC-1, AC-2, AC-3, AC-4) |
| `src/main/resources/targets/claude/skills/core/plan/x-commit-planning/SKILL.md` | MODIFY | Whitelist gap: accept `ai/bugs/**` paths |

**Risk:** MEDIUM — mostly authoring; primary risk is the `x-commit-planning` whitelist gap (must extend before AC-1 passes) and CWE-22 hardening of slug pipeline.

**Dependencies:** none external; downstream stories 0080-0002..0006 import this template/capability.

## Section 2: Class Design

No production classes. Test-only:

- `dev.iadev.skills.bug.SlugGeneratorTest` — exercises a tiny `SlugGenerator` helper that mirrors the shell pipeline used in the skill (so unit tests pin the algorithm; the skill itself shells the same transformation). Helper lives at `src/test/java/dev/iadev/skills/bug/SlugGenerator.java` (test-scope only).
- `dev.iadev.skills.bug.BugCreationSmokeIT` — JUnit 5 IT, `@Tag("smoke")`, runs the skill end-to-end against a temp git repo.

## Section 3: Contracts (Interfaces & DTOs)

**Skill CLI contract** (frozen by AC):

```
/x-create-bug "<short-description>" [--severity=low|med|high|critical] [--scope=single-file|single-module|cross-module] [--non-interactive]
```

**Capability frontmatter v3.0 schema** (`governance/bug-lifecycle.yaml`):

```yaml
id: governance.bug-lifecycle
version: 1.0.0
schema-version: 3.0
title: Bug Lifecycle
category: governance
provides:
  - bug-template
  - bug-folder-layout
  - bug-classification-rule
requires-capabilities: []
status: active
since: epic-0080
```

**Template frontmatter contract** (`_TEMPLATE-BUG.md`):

```yaml
---
template-version: 1.0.0
template-id: bug
schema-version: 3.0
requires-capabilities: [governance.bug-lifecycle]
---
```

## Section 4: Data Flow

```
user
 → /x-create-bug "<desc>" [flags]
   → INLINE-SKILL x-internal-precheck-worktree    (exit 15 on dirty)
   → arg parse + slug pipeline (CWE-22 hardened)  (exit 2 on empty slug)
   → filesystem scan ai/bugs/bug-NNNNNN → next id (exit 1 on collision after 3 retries)
   → mkdir ai/bugs/bug-NNNNNN
   → render _TEMPLATE-BUG.md → ai/bugs/bug-NNNNNN/bug.md
   → INLINE-SKILL x-create-git-branch  bug/NNNNNN-<slug>
   → INLINE-SKILL x-commit-planning    (paths under ai/bugs/**)
   → INLINE-SKILL x-create-pr          --label bug-scaffold --base develop
   → stdout: PR URL
```

## Section 5: Database Migration

N/A — markdown-only story, no persistence layer touched.

## Section 6: Configuration

N/A — no new properties. Skill respects existing `CLAUDE_RECOVERY_MODE`, `CLAUDE_TELEMETRY_DISABLED`, and the `--non-interactive` convention from Rule 20 (interactive gates).

## Section 7: Observability

- Telemetry events emitted by host (Rule 13 INLINE-SKILL): `skill.start`, `skill.end`, `tool.call` per delegated skill — no extra instrumentation needed in this story.
- Smoke test asserts presence of `tool.call:x-create-bug` event in `events.ndjson`.
- No log statements in skill body (markdown skill); shell helpers MUST NOT echo the raw description (slug only).
- Sensitive-data exclusion: description is user-supplied free text; the skill stores it verbatim in `bug.md` body (acceptable — same as feature/story templates) but NEVER logs or echoes it to stderr beyond the sanitised slug.

## Section 8: Test Strategy

**Unit (`SlugGeneratorTest`)** — JUnit 5 + AssertJ, ≥ 95% line coverage on helper:

| Case | Input | Expected slug | Notes |
| :--- | :--- | :--- | :--- |
| happy | `"checkout total wrong when promo applied"` | `checkout-total-wrong-when-promo-applied` | NFKD identity branch |
| traversal | `"../../../etc/passwd injection"` | `etc-passwd-injection` | AC-4: strips `../`, `/` |
| unicode | `"Açaí — café"` | `acai-cafe` | NFKD strips combining marks |
| collapse | `"  multi   space   "` | `multi-space` | trim + collapse |
| truncate | 80-char input | length ≤ 40 | bounded |
| empty | `"///"` | throws → exit 2 `ARGS_INVALID` | AC-4 negative |
| only-marks | `"---"` | throws → exit 2 | AC-4 negative |

**Integration (`BugCreationSmokeIT`)** — `@Tag("smoke")`, `@TempDir` git repo seeded as `develop`:

| AC | Assertion |
| :--- | :--- |
| AC-1 | folder exists, branch exists, commit message matches, PR labelled `bug-scaffold`, frontmatter contains `governance.bug-lifecycle`, refinement-verdict `status: pending` |
| AC-2 | dirty tree → exit 15, no folder, no branch, stderr contains guard message |
| AC-3 | wall-clock < 90 s (uses `Stopwatch`; soft bound 90 s, hard fail at 120 s to avoid flakes) |
| AC-4 | `"../../../etc/passwd injection"` → branch matches `^bug/[0-9]{6}-[a-z0-9-]{1,40}$`; `"///"` → exit 2 |

Edge cases: counter collision when `ai/bugs/bug-000001` exists (auto-increments to `000002`); 3 retries then exit 1 `COUNTER_COLLISION`.

## Section 9: Native / Framework Compatibility

N/A — no Java production code, no reflection registration, no GraalVM or framework annotations introduced. Test classes use standard JUnit 5; no native-image build implications.

## Section 10: Layers Affected

| Layer | Package / Path | Action |
| :--- | :--- | :--- |
| docs/template | `src/main/resources/shared/templates/` | CREATE `_TEMPLATE-BUG.md` |
| docs/capability | `targets/claude/capabilities/governance/` | CREATE `bug-lifecycle.yaml` |
| skill | `targets/claude/skills/core/plan/x-create-bug/` | CREATE `SKILL.md` |
| skill | `targets/claude/skills/core/plan/x-commit-planning/` | MODIFY whitelist |
| test | `src/test/java/dev/iadev/skills/bug/` | CREATE `SlugGeneratorTest`, `BugCreationSmokeIT`, helper |

Dependency direction: docs → skill (composition only); no inward arrows into `domain` / `application` / `adapter`. Architecture golden rule preserved.

## Section 11: Event Design

N/A — this project is not event-driven (`architecture.event_driven = false`).

## Section 12: Compliance Impact

N/A — no personal data, no cardholder data, no consent collection introduced. Description text is engineering-only.

## Section 13: API Gateway Impact

N/A — `infrastructure.api_gateway = none`.

## Section 14: Cloud Provider Considerations

N/A — `cloud.provider = none` (cloud-agnostic constraint, §1).

## Section 15: Transactional Outbox

N/A — `architecture.outbox_pattern = false`.

---

## Implementation Approach & Design Decisions

1. **Markdown-only delivery.** Skill is composition: it shells out to existing skills via INLINE-SKILL (Rule 13 Pattern 1). No new Java production code, so coverage gates are satisfied by `SlugGeneratorTest` on the test-scope helper.
2. **Slug helper duplicated in test.** The slug pipeline lives in shell inside the skill; we mirror it in a tiny test-scope Java class so AC-4 has executable, deterministic coverage. If divergence occurs, the smoke test catches it (AC-4 runs the real skill).
3. **6-digit zero-padded id by filesystem scan.** No counter file (avoids merge conflicts on parallel bug filings). Algorithm: `ls ai/bugs/ | grep '^bug-[0-9]{6}$' | sort | tail -1` → increment. TOCTOU mitigation in §Risks.
4. **CWE-22 slug pipeline (AC-4):** `description → NFKD normalize → drop combining marks → replace [^a-z0-9] with '-' (after lowercase) → collapse runs of '-' → trim leading/trailing '-' → truncate(40)`. Empty result → exit 2.
5. **Status transitions** in `bug.md` use the same vocabulary as stories (Rule 29) — initial value `Pendente`.

## File Creation Order

1. `_TEMPLATE-BUG.md` — template is the contract; everything downstream renders from it.
2. `capabilities/governance/bug-lifecycle.yaml` — required for `requires-capabilities` resolution at template-render time.
3. `x-commit-planning/SKILL.md` — extend whitelist to `ai/bugs/**` BEFORE skill that depends on it.
4. `x-create-bug/SKILL.md` — composes the above.
5. `SlugGeneratorTest` (TDD red → green) — fail first, then pin the algorithm.
6. `BugCreationSmokeIT` — final acceptance gate; runs after generator regenerates `.claude/`.

## Per-File Implementation Notes

### `_TEMPLATE-BUG.md`

Sections (RA9 v2 order):

1. Visão (`As a … I need … so that …` — bug variant: "As an engineer I need to capture <regression> …")
2. Persona & Cenário de Uso (reporter persona + reproduction context)
3. Entrega de Valor (impact on KPI-3 MTTR; severity-weighted)
4. Critérios de Aceite (regression test must fail before fix, pass after)
5. Contratos (files to touch — left blank, filled at refinement)
6. Plano de Ação (TBD until refinement)
7. Definition of Done
8. Riscos & Premissas
9. Refinement Verdict (`status: pending`)

Bug-specific subsections injected under §1:
- **§1.1 Reproduction Recipe** (numbered steps, environment, seed data)
- **§1.2 Observed vs Expected** (two-column markdown table)
- **§1.3 Root-Cause Hypothesis** (free text + suspected file/module)
- **§1.4 Regression Test Slot** (path + name placeholder for the failing test)

Frontmatter: see Section 3.

### `capabilities/governance/bug-lifecycle.yaml`

Schema v3.0; `provides` enumerates `bug-template`, `bug-folder-layout`, `bug-classification-rule` so downstream stories (0080-0002..0006) attach via `requires-capabilities: [governance.bug-lifecycle]`.

### `x-create-bug/SKILL.md`

Frontmatter:

```yaml
---
name: x-create-bug
model: sonnet
user-invocable: true
requires-capabilities: [governance.bug-lifecycle]
---
```

Body (delegation chain):

1. `Skill(skill: "x-internal-precheck-worktree")` — exit 15 propagates.
2. Shell block: parse args, run slug pipeline, scan for next id, render template.
3. `Skill(skill: "x-create-git-branch", args: "bug/NNNNNN-<slug>")`.
4. `Skill(skill: "x-commit-planning", args: "ai/bugs/bug-NNNNNN/")`.
5. `Skill(skill: "x-create-pr", args: "--base develop --label bug-scaffold --title 'bug: scaffold bug-NNNNNN — <desc>'")`.

Exit codes documented in `## Error Handling`. Recovery section explicitly excludes `--skip-verification` (Rule 22 SKIP_IN_HAPPY_PATH guard).

### `SlugGeneratorTest`

7 cases listed in Section 8. AssertJ. JUnit 5 `@ParameterizedTest` for the table.

### `BugCreationSmokeIT`

`@TempDir` repo, `git init`, seed `develop` branch with one commit, run skill via `ProcessBuilder`, assert filesystem + git state. Use `gh pr create --dry-run` mock or stub `gh` on `PATH` for hermetic CI.

## Integration Points with Existing Skills

| Composed skill | Why | Pass-through args |
| :--- | :--- | :--- |
| `x-internal-precheck-worktree` | AC-2 dirty-tree guard | none |
| `x-create-git-branch` | uniform branch-creation contract | `bug/NNNNNN-<slug>` |
| `x-commit-planning` | enforces planning-doc commit policy | `ai/bugs/bug-NNNNNN/` |
| `x-create-pr` | uniform PR creation + telemetry | `--base develop --label bug-scaffold` |

## Risk Mitigations

| Risk | Mitigation |
| :--- | :--- |
| **TOCTOU on id allocation** (two `/x-create-bug` runs in parallel pick the same NNNNNN) | After `mkdir`, re-scan; if our id is no longer the max+1 of pre-existing dirs, `rmdir` and retry up to 3 times → exit 1 `COUNTER_COLLISION`. Acceptable since concurrent bug filings are rare; collision exit lets caller retry. |
| **CWE-22 path traversal** (AC-4) | Slug pipeline strips `..`, `/`, `\`, NUL, control chars before any filesystem operation. Branch name validated against `^bug/[0-9]{6}-[a-z0-9-]{1,40}$` regex post-generation; mismatch → exit 2. |
| **`x-commit-planning` whitelist gap** | MUST extend `x-commit-planning` to accept `ai/bugs/**` BEFORE `x-create-bug` ships. Tracked as item #3 in File Creation Order. Smoke test fails fast if missing. |
| **Slug truncation collision** (two long descriptions truncate to same 40 chars) | id prefix `NNNNNN` makes branch + folder globally unique even when slug collides; no extra logic needed. |
| **Recovery-mode bypass abuse** | Skill respects `CLAUDE_RECOVERY_MODE=1` only for the precheck step (Rule 27), never for slug validation — AC-4 exit 2 is non-bypassable. |

## Exit Code Catalogue

| Code | Symbol | Meaning | Source |
| :--- | :--- | :--- | :--- |
| 0 | OK | Scaffold + branch + commit + PR succeeded | normal |
| 1 | `COUNTER_COLLISION` | Could not allocate unique `bug-NNNNNN` after 3 retries | this skill |
| 2 | `ARGS_INVALID` | Empty/invalid description, slug normalised to empty (AC-4) | this skill |
| 15 | `WORKTREE_AMBIGUOUS` | Dirty working tree (AC-2) | inherited from `x-internal-precheck-worktree`, Rule 20 §Working-Tree Guard |

Other propagated codes (from composed skills) pass through unchanged; documented in `## Error Handling` of the SKILL.

---

**Artifact:** `/Users/edercnj/workspaces/ia-dev-environment/ai/epics/epic-0080-bug-lifecycle-management/plans/plan-story-0080-0001.md`
