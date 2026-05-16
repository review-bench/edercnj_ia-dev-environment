# x-review-pr — Full Protocol Reference

Supplementary details carved out of SKILL.md to satisfy the orchestrator size contract (ADR-0007).

## State File Schema (Step 8.4)

**Path:** `plans/review/<pr-number>/state.json`

**Schema (Rule 20 §State File Schema — version 1.0):**

```json
{
  "phase": "GATE_FIX_PR",
  "lastPhaseCompletedAt": "<ISO-8601 UTC>",
  "lastGateDecision": "<PROCEED|FIX_PR|ABORT|null>",
  "fixAttempts": [
    {
      "at": "<ISO-8601 UTC>",
      "delegateSkill": "x-fix-pr",
      "prNumber": 123,
      "outcome": "applied"
    }
  ],
  "schemaVersion": "1.0"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `phase` | String | Yes | Always `"GATE_FIX_PR"` for this skill |
| `lastPhaseCompletedAt` | String (ISO-8601 UTC) | Yes | Updated on each write |
| `lastGateDecision` | String \| null | Yes | One of `PROCEED`, `FIX_PR`, `ABORT`, or `null` before first interaction |
| `fixAttempts` | Array | Yes | Always present; `[]` before first fix; max 3 items |
| `schemaVersion` | String | Yes | Literal `"1.0"` |

**`fixAttempts` entry fields:** `at` (ISO-8601 UTC), `delegateSkill` (always `"x-fix-pr"`), `prNumber` (PR number), `outcome` (`applied` \| `no_comments` \| `compile_regression` \| `aborted`).

**Lifecycle:**
- Written atomically (write to `<path>.tmp`, rename) when slot 2 (FIX-PR) is selected
- Not written for PROCEED or ABORT selections
- Not written on non-interactive path (default)

**`--resume-review <pr>` flag:**

When present, reads the state file at `plans/review/<pr>/state.json` and restores `gateAttempts` from `fixAttempts.size()`. If the state file satisfies the schema (Rule 20), the gate loop resumes from the last decision point. If the state file is absent or invalid, the gate starts fresh (gateAttempts = 0) with a warning:
```
WARNING: State file not found at plans/review/<pr>/state.json. Starting gate from scratch.
```
If the state file fails schema validation, emit `GATE_SCHEMA_INVALID` with the path and the missing/malformed field name.

## Phase 5 — Frontmatter YAML Template

Full frontmatter block conforming to `governance/schemas/review-frontmatter-1.0.json`:

```
<!-- template-version: 1.0 -->
---
schema-version: "1.0"
generated-by: x-review-pr@<git rev-parse HEAD>
story-id: <STORY_ID>
epic-id: <EPIC_ID>
date: <date -u +%Y-%m-%dT%H:%M:%SZ>
decision: <GO|NO-GO|GO-WITH-RESERVATIONS>
score: <integer 0-55>
score-max: 55
severity-counts:
  critical: <count>
  high: <count>
  medium: <count>
  low: <count>
  info: <count>
blocking-findings:
<YAML list of critical/high findings, empty list [] if none>
checklist:
  passed: <integer 0-45>
  total: 45
  failed-sections:
<YAML list of failed section IDs, empty list [] if none>
---
# Tech Lead Review — <STORY_ID>
...existing prose body...
```

**Note:** `x-review-pr` does NOT emit the `reviewers` field — the Tech Lead is the sole
reviewer; `checklist` replaces `reviewers` as the optional field per schema spec.

After writing the artifact, validate:

    Bash command: `$CLAUDE_PROJECT_DIR/.claude/scripts/audit-review-frontmatter.sh --story <STORY_ID>`

If the script returns exit ≠ 0, abort with `REVIEW_FRONTMATTER_INVALID`. No fallback.

---

## Phase 0 — Context & Idempotency (full detail)

Open a phase tracker:

```text
TaskCreate(subject: "{STORY_ID} › Review-PR › Phase 0 - Context", activeForm: "Loading PR context")
```

### Step 0 — Idempotency Pre-Check (RULE-002 — Artifact reuse)

Before executing the Tech Lead review, check if a report already exists and is still valid.

1. Extract story ID from argument or branch name (e.g., `story-XXXX-YYYY`)
2. Derive epic directory: `ai/epics/epic-XXXX/reviews/`
3. Check if Tech Lead report exists:

   ```bash
   ls ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   ```

4. If report exists AND the branch has no new commits since last report:

   ```bash
   # Portable mtime: BSD (macOS) first, then GNU coreutils
   stat -f %m ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null \
     || stat -c %Y ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md 2>/dev/null
   git log -1 --format=%ct HEAD
   ```

   - If `mtime(report) >= commit_date`: log `Reusing existing tech lead review from {date}` and skip to Step 5 (dashboard update)
   - If code changed after report: proceed with full review

5. If no report exists, proceed normally.

| Aspect | Behavior |
|--------|----------|
| **Check** | Compare `mtime(report)` vs `mtime(latest commit)` |
| **Skip** | Reuse existing report when `mtime(report) >= commit_date` |
| **Override** | Proceed with full review when code changed after report |

### Step 1 — Detect Context

Determine what to review and set `[BASE_BRANCH]`:

- **PR number:** `gh pr view NNN --json title,body,baseRefName,headRefName,files`
- **STORY reference:** Find and checkout the branch
- **No argument:** Use current branch, `BASE_BRANCH=main`

Validate diff exists:

```bash
git diff [BASE_BRANCH] --stat
git diff [BASE_BRANCH] --name-only
```

### Step 2 — Gather Context

Read knowledge packs to calibrate the review:

- `.claude/knowledge/coding-standards.md` — {{LANGUAGE}} naming, injection, mapper conventions
- `.claude/knowledge/architecture.md` — layer boundaries, dependency direction
- `.claude/rules/05-quality-gates.md` — coverage thresholds, merge checklist
- `.claude/knowledge/testing.md` — TDD workflow, Double-Loop TDD, TPP ordering

Check for existing artifacts (extract epic ID XXXX and story sequence YYYY from story ID): specialist review reports, implementation plan, test plan, common mistakes document.

### Step 3 — Template Detection

```bash
test -f .claude/templates/_TEMPLATE-TECH-LEAD-REVIEW.md && echo "TL_TEMPLATE_AVAILABLE" || echo "TL_TEMPLATE_MISSING"
```

- If `TL_TEMPLATE_AVAILABLE`: read template; follow ALL sections. Report MUST include standardized header (Story ID, Date, Author, Template Version per RULE-011). Score in format `XX/{review_max_score}` with status `GO`/`NO-GO` (RULE-005).
- If `TL_TEMPLATE_MISSING`: log warning `Template not found, using inline format` and use inline format as fallback (RULE-012). Skip dashboard and remediation updates.

Persist `interactiveMode` to `execution-state.json` (EPIC-0068 — consumed by Stop hook `enforce-continuous-flow.sh`):

```text
Skill(skill: "x-internal-update-status", args: "--file ai/epics/epic-XXXX/execution-state.json --type story --id <STORY-ID> --field interactiveMode --value <interactive|non-interactive>")
```

Value: `"interactive"` when `--interactive` passed; otherwise `"non-interactive"` (Rule 20 default).

---

## Phase 1 — Execute 45-point review (Step 4 full detail)

PRE gate (Rule 25):

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode pre --skill x-review-pr --phase Phase-1-Review")
TaskCreate(subject: "{STORY_ID} › Review-PR › Phase 1 - Review", activeForm: "Running 45-point tech-lead review")
```

POST gate:

```text
Skill(skill: "x-internal-verify-phase-gates", model: "haiku", args: "--mode post --skill x-review-pr --phase Phase-1-Review --expected-artifacts ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md")
```

### Step 4 — Execute Tech Lead Review

1. List ALL modified files: `git diff [BASE_BRANCH] --name-only`
2. View FULL diff: `git diff [BASE_BRANCH]`
3. For EACH source file, read FULL content and apply {review_max_score}-point checklist
4. Focus on CROSS-FILE issues (inconsistencies, cross imports, repeated patterns)
5. Compile and verify: `{{COMPILE_COMMAND}}` + `{{BUILD_COMMAND}}`
6. **Execute full test suite** (MANDATORY — EPIC-0042):

   ```bash
   {{TEST_COMMAND}}
   ```

   - If ANY test fails: record test failures AND set decision to **automatic NO-GO** (overrides rubric score)
   - Log each failing test name and failure reason under a dedicated **Test Execution Results** section

7. **Execute coverage analysis** (MANDATORY — EPIC-0042):

   ```bash
   {{COVERAGE_COMMAND}}
   ```

   - If line coverage < 95% or branch coverage < 90%: record as CRITICAL finding
   - **Absolute-gate note (Rule 05 RULE-005-01):** gate fires regardless of whether the deficit was caused by this PR or was pre-existing on the base branch. Tech Lead MUST NOT override NO-GO with a "pre-existing" justification. Only permitted escape paths: (a) close gap in this PR, (b) close it in a predecessor PR, (c) merge approved ADR temporarily lowering the gate for a specific package (with sunset date).

8. **Execute smoke tests** (CONDITIONAL — EPIC-0042, only when `testing.smoke_tests == true`):

   ```bash
   {{SMOKE_COMMAND}}
   ```

   - If ANY smoke test fails: record as CRITICAL AND set decision to **automatic NO-GO**
   - If `testing.smoke_tests == false`: log `"Smoke tests skipped (testing.smoke_tests=false)"` and proceed

9. If specialist reports exist, verify CRITICAL issues were fixed.

---

## Phase 2 — Step 5: Update Consolidated Dashboard (RULE-006 cumulative)

1. **Check if dashboard exists:**

   ```bash
   test -f ai/epics/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md && echo "DASHBOARD_EXISTS" || echo "DASHBOARD_MISSING"
   ```

2. **If dashboard exists (created by x-review-codebase):**
   - Read existing dashboard
   - Update the **Tech Lead Score** section: replace placeholder `--/{review_max_score} | Status: Pending` with actual `XX/{review_max_score} | Status: GO/NO-GO`
   - Update the **Overall Score** to include Tech Lead score in the total
   - Update the **Overall Status** considering both specialist scores and Tech Lead decision
   - Append a new **Round** to the **Review History** section with date, Tech Lead score, and status
   - Preserve all existing specialist scores and previous rounds

3. **If dashboard does not exist:**
   - Log: `Dashboard not found, creating fresh dashboard`
   - Check dashboard template; if available, create with only Tech Lead Score populated (specialists marked `--` / `Pending`)
   - If template missing: skip dashboard creation with warning

### Step 7 — Console Summary

```text
============================================================
 TECH LEAD REVIEW — [STORY_ID]
============================================================
 Decision:  GO | NO-GO
 Score:     XX/{review_max_score} (GO >= {review_go_threshold})
 Critical:  N issues
 Medium:    N issues
 Low:       N issues

 Test Execution Results (EPIC-0042):
 Test Suite:    PASS (XXX tests, X failures)
 Coverage:      XX% line, XX% branch
 Smoke Tests:   PASS/FAIL/SKIP (N tests)
------------------------------------------------------------
 Report:      ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md
 Dashboard:   ai/epics/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md (updated)
 Remediation: ai/epics/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md (updated)
============================================================
```

Replace placeholders with actual values from Step 4 execution. If smoke tests were skipped, show `SKIP (0 tests)`.

---

## Phase 3 — Step 6: Update Remediation Tracking

1. **Check if remediation exists:**

   ```bash
   test -f ai/epics/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md && echo "REMEDIATION_EXISTS" || echo "REMEDIATION_MISSING"
   ```

2. **If exists:** Read; for each finding, update `Open` → `Fixed` if Tech Lead confirms; keep `Open` if unfixed. Add new Tech Lead findings as `Open`. Update Remediation Summary counts.

3. **If missing:** Log `Remediation not found, creating fresh remediation with Tech Lead findings`; check template; if available, create with Tech Lead findings only (all `Open`); else skip with warning.

### Step 8 — Handle NO-GO (Auto-Remediation, EPIC-0042)

1. **Classify NO-GO findings:**
   - `TEST_FAILURE`: unit/integration/smoke test failures detected in Step 4.6-4.8
   - `COVERAGE_GAP`: coverage below 95% line or 90% branch detected in Step 4.7
   - `CODE_QUALITY`: rubric score below threshold (non-test issues)

2. **Auto-remediate by classification:**

   **For TEST_FAILURE:**

   ```text
   Agent(
     subagent_type: "general-purpose",
     description: "Fix failing tests for NO-GO remediation",
     prompt: "Read the failing test output from the review report at ai/epics/epic-XXXX/reviews/review-tech-lead-story-XXXX-YYYY.md. Identify the root cause of each failing test. Fix the IMPLEMENTATION (NOT the test) to make tests pass. Run {{TEST_COMMAND}} to verify the fix. Commit via Skill(skill: 'x-commit-changes', args: '--type fix --subject \"fix failing tests from tech lead review\"')."
   )
   ```

   **For COVERAGE_GAP:**

   ```text
   Agent(
     subagent_type: "general-purpose",
     description: "Add test coverage for NO-GO remediation",
     prompt: "Read the coverage report. Identify uncovered lines/branches. Write tests for the uncovered code paths following TDD discipline (test first). Run {{TEST_COMMAND}} + {{COVERAGE_COMMAND}} to verify coverage meets 95% line / 90% branch. Commit via Skill(skill: 'x-commit-changes', args: '--type test --subject \"add test coverage for uncovered branches\"')."
   )
   ```

   **For CODE_QUALITY:** Apply fixes inline following the remediation tracking file guidance.

3. **Re-run review automatically** (max 2 cycles total): after remediation agent completes, re-execute Step 4. If still NO-GO after 2 cycles: proceed to Step 8.4.

4. **Opt-out:** Pass `--no-auto-remediation` to force manual mode.

#### Step 8.4 — Exhausted-Retry Gate (Rule 20 Interactive Gate)

Reached when auto-remediation cycles are exhausted (2 retries without convergence) or when `--no-auto-remediation` is set and the review returns NO-GO.

**Non-interactive path (default — `--interactive` absent):**
Skip `AskUserQuestion`. Print HALT text and return NO-GO:

```text
REVIEW NO-GO: Auto-remediation exhausted without convergence. Remaining issues recorded in report.
Run with --resume-review <pr> --interactive to re-enter the gate interactively.
```

Exit with NO-GO. No state file written.

**Interactive path (`--interactive` present):**

Initialize `gateAttempts = 0`.

**Gate loop pseudocode:**

```text
WHILE gateAttempts < 3:
  Present AskUserQuestion:
    question: "Auto-remediation exhausted after 2 retry cycles. The Tech Lead review returned NO-GO. How would you like to proceed?"
    options:
      - { header: "Proceed", label: "Continue (Recommended)", description: "Re-dispatch auto-remediation (+2 loops). If the review converges to GO, the gate closes and the skill exits normally." }
      - { header: "Fix PR", label: "Run x-fix-pr and retry", description: "Invokes x-fix-pr on the current PR; re-presents this menu on return." }
      - { header: "Abort", label: "Cancel the operation", description: "Terminates the skill with REVIEW_REMEDIATION_EXHAUSTED. No further remediation is attempted." }

  On PROCEED (slot 1):
    gateAttempts++
    Re-dispatch auto-remediation agents (same classification logic as Step 8 sub-steps 1-3, with 2 new retry cycles)
    Re-execute Step 4 (full review)
    IF review returns GO:
      Exit gate with GO — skill completes normally
    ELSE:
      IF gateAttempts >= 3:
        Emit REVIEW_FIX_LOOP_EXCEEDED and terminate (see guard-rail below)
      ELSE:
        Continue loop (re-present menu)

  On FIX-PR (slot 2):
    gateAttempts++
    Write/update state file at plans/review/<pr>/state.json (opt-in persistence):
      phase: "GATE_FIX_PR"
      lastPhaseCompletedAt: <ISO-8601 UTC now>
      lastGateDecision: "FIX_PR"
      fixAttempts: [... previous ..., { at: <now>, delegateSkill: "x-fix-pr", prNumber: <PR>, outcome: "pending" }]
      schemaVersion: "1.0"
    Invoke x-fix-pr via Rule 13 Pattern 1 INLINE-SKILL:
        Skill(skill: "x-fix-pr", args: "<PR>")
    Update last fixAttempt.outcome to "applied" (or appropriate outcome)
    Update state file: lastGateDecision = "FIX_PR", lastPhaseCompletedAt = <now>
    IF gateAttempts >= 3:
      Emit REVIEW_FIX_LOOP_EXCEEDED and terminate
    ELSE:
      Continue loop (re-present menu)

  On ABORT (slot 3):
    Emit: "Review NO-GO final: operator aborted after ${gateAttempts} remediation attempt(s) on PR ${PR}"
    Exit with code REVIEW_REMEDIATION_EXHAUSTED
```

**Guard-rail — `REVIEW_FIX_LOOP_EXCEEDED` (3 consecutive PROCEED or FIX-PR without convergence):**

```text
REVIEW_FIX_LOOP_EXCEEDED: Fix loop exceeded 3 attempts on PR ${PR} review;
gate terminated with automatic ABORT.
Resume via --resume-review ${PR} or manual intervention.
```

No 4th option is offered. The gate terminates immediately. The menu was presented exactly 3 times (RULE-002 invariant: total option count remains 3 at all previous presentations).
