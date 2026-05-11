# x-plan-tests — Full Protocol

Detailed reference for `x-plan-tests`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 0 — Pre-Check: Idempotency (RULE-002 — Staleness Check)

Before generating a test plan, verify whether a valid plan already exists:

1. **Resolve paths:** Extract epic ID (XXXX) and story sequence (YYYY) from the story ID. Compute:
   - Story path: the story file provided as input
   - Plan path: `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

2. **Check existence:** If the plan file does NOT exist, proceed to generation (Step 1).

3. **Compare modification times:** If the plan file exists:
   - If `mtime(story file) <= mtime(plan file)` — the plan is **fresh**. Log: `"Reusing existing test plan from {date}"` (where `{date}` is the plan file's last modified date). Return the existing plan. **Do NOT invoke any subagent.**
   - If `mtime(story file) > mtime(plan file)` — the plan is **stale**. Log: `"Regenerating stale test plan for {story-id}"`. Proceed to generation (Step 1).

4. **First generation:** If the plan file does not exist at all, log: `"Generating test plan for {story-id}"`. Proceed to generation (Step 1).

## Step 1 — Gather Context (Subagent via Task, model: opus)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-tests Phase-1-KP-Read`

Launch a **single** `general-purpose` subagent with explicit `model: "opus"` (Rule 23 RULE-002 — deep test planning quality for Double-Loop TDD + TPP):

    Agent(
      subagent_type: "general-purpose",
      model: "opus",
      description: "Test Planning Assistant gathers context for test plan generation",
      prompt: "<see the blockquote below>"
    )

> You are a **Test Planning Assistant** gathering context for test plan generation.
>
> **Read the template for required output format (RULE-007):**
> - Read template at `.claude/templates/_TEMPLATE-TEST-PLAN.md` for required output format.
> - If the template file does NOT exist, log: `"Template not found, using inline format"` and continue without it (RULE-012 — graceful fallback for projects without templates).
>
> **Read these knowledge packs:**
> - `.claude/knowledge/testing/testing-philosophy.md` — 8 test categories, fixture patterns, data uniqueness, async handling, real vs in-memory DB decisions
> - `.claude/knowledge/testing/testing-conventions.md` — {{LANGUAGE}}-specific test frameworks, naming conventions, directory structure, assertion libraries
> - `.claude/knowledge/architecture.md` — exception hierarchy, layer boundaries (unit vs integration), dependency direction
>
> **Read testing knowledge pack for {{LANGUAGE}}-specific patterns:**
> - Read `.claude/knowledge/testing.md` for {{LANGUAGE}}-specific test frameworks, conventions, and patterns
>
> **Read the story:** `{STORY_PATH}`
> Extract: acceptance criteria, sub-tasks, business rules, dependencies.
>
> **Scan existing code:**
> - List existing classes in target packages
> - List existing test classes and patterns established
>
> **Return a structured context summary:**
> 1. **Test categories applicable** to this story (from the 8 categories in testing-philosophy)
> 2. **Naming convention:** `[method]_[scenario]_[expected]` pattern with {{LANGUAGE}}-specific format
> 3. **Test frameworks and assertion libraries** to use (from testing-conventions)
> 4. **Fixture pattern:** `final class` + `private constructor` + `static methods`, naming `a{Entity}()`
> 5. **Acceptance criteria** extracted from story (each becomes ≥1 test)
> 6. **Business rules** extracted (each maps to parametrized tests)
> 7. **Exception types** in scope (each needs error path test)
> 8. **Layer boundaries** — which classes need unit vs integration tests
> 9. **Existing patterns** found in current test codebase
> 10. **Contract tests applicable** (if testing.contract_tests == true)
> 11. **Chaos tests applicable** (if testing.chaos_tests == true)
> 12. **Template sections** — if template was found, list the 8 mandatory sections to populate

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-tests Phase-1-KP-Read ok`

## Step 2 — Generate Test Scenarios (Inline)

Using the context returned by the subagent, generate a Double-Loop TDD test plan. Organize scenarios by implementation order (TPP), NOT by test category.

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-tests Phase-2-Acceptance-Tests`

### 2.1 — Acceptance Tests (Outer Loop)

For each Gherkin scenario in the story, generate an acceptance test entry:

| Field | Description |
|-------|-------------|
| ID | `AT-N` (sequential) |
| Gherkin | Reference to the original scenario |
| Status | RED until all unit tests for this acceptance criteria complete |
| Components | List of classes/modules under test |
| Test Type | Integration, API, or E2E (depending on story scope) |
| Depends on | UT/IT IDs that must pass for this AT to go GREEN, or `--` |
| Parallel | `yes` if independent of other ATs |

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-tests Phase-2-Acceptance-Tests ok`

### 2.2 — Unit Tests (Inner Loop, TPP Order)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-tests Phase-3-Unit-Tests-TPP`

Generate unit test scenarios in strict TPP order. Each scenario represents one Red-Green-Refactor cycle.

#### TPP Level 1 — Degenerate Cases
- Null, empty, zero inputs — return default/error
- Transform: `{}→nil` or `nil→constant`

#### TPP Level 2 — Unconditional Paths
- Single valid input — direct output (no branching)
- Transform: `constant→variable`

#### TPP Level 3 — Simple Conditions
- Single if/else branching
- Transform: `unconditional→conditional`

#### TPP Level 4 — Complex Conditions
- Multiple branches, switch/match, compound boolean
- Transform: deeper conditional logic

#### TPP Level 5 — Iterations
- Collection processing, loops, map/filter/reduce
- Transform: `scalar→collection`, `statement→recursion/iteration`

#### TPP Level 6 — Edge Cases
- Boundary values (at-min, at-max, past-max)
- Transform: `value→mutated value`

For each unit test entry, include:

| Field | Required | Description |
|-------|----------|-------------|
| ID | M | `UT-N` (sequential within TPP order) |
| Test | M | What to test (method + scenario + expected) |
| Implementation | O | Minimum code to pass this test |
| Transform | M | TPP transformation applied |
| TPP Level | M | 1-6 |
| Components | M | Classes/modules needed |
| Depends on | M | Previous test IDs that are prerequisites, or `--` |
| Parallel | M | `yes` if independent of other tests at same level |

### 2.3 — Integration Tests (Cross-Component)

Position AFTER the unit tests of the components involved. Include only when multiple components interact (DB, HTTP, messaging).

| Field | Required | Description |
|-------|----------|-------------|
| ID | M | `IT-N` (sequential) |
| Test | M | What to test |
| Components | M | Interacting components |
| Depends on | M | UT IDs that must pass first |
| Parallel | M | `yes`/`no` |

### 2.4 — CRUD-Only Story Optimization

When a story describes a purely CRUD operation without branching logic:
- UTs should cover: degenerate (Level 1) — constant (Level 1) — variable (Level 2)
- Do NOT generate conditional or iteration UTs unless the business rules demand them
- Acceptance tests should focus on the full CRUD flow

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-tests Phase-3-Unit-Tests-TPP ok`

## Step 3 — Estimate and Validate (Inline)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-tests Phase-4-Report`

### 3.1 — Coverage Estimation Table

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| [Name] | [count] | [count] | [count] | [%] | [%] |

Flag any class where estimated coverage < 95% line / 90% branch.

### 3.2 — Quality Checks

1. Every Gherkin scenario maps to ≥1 acceptance test (AT)
2. Every acceptance criterion maps to ≥1 unit test chain (UT)
3. UT-1 is ALWAYS a degenerate case (TPP Level 1)
4. UTs follow non-decreasing TPP level order
5. Every exception has ≥1 error path test
6. Boundary values use triplet pattern (at-min, at-max, past-max)
7. Dependency markers are complete (no orphan UTs)
8. Estimated coverage meets thresholds (≥ 95% line, ≥ 90% branch)
9. Test naming follows convention: `[method]_[scenario]_[expected]`
10. No unnecessary UTs for CRUD-only stories (max Level 3 unless justified)

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-tests Phase-4-Report ok`

## Output

Save to: `ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p ai/epics/epic-XXXX/plans`.

### Origin Marker (EPIC-0059 — mandatory)

Before writing the test plan file, prepend the YAML frontmatter block at the very top:

```yaml
---
generated-by: x-plan-tests@$(git rev-parse HEAD 2>/dev/null || echo "unknown")
generated-at: $(date -u +%Y-%m-%dT%H:%M:%SZ)
story-id: ${STORY_ID}
---
```

This frontmatter is required by `audit-execution-integrity.sh` Phase-1 validation (EPIC-0059, Rule 24). Artifacts without this block fail the CI audit with `EIE_EVIDENCE_MISSING`.

```markdown
# Test Plan — STORY-ID: [Title]

## Summary
- Acceptance tests: X (from Y Gherkin scenarios)
- Unit tests: ~Z (in TPP order)
- Integration tests: ~W
- Estimated line coverage: ~P%
- Estimated branch coverage: ~Q%

## Acceptance Tests (Outer Loop)

### AT-1: [Gherkin scenario name]
- **Gherkin**: [reference to story scenario]
- **Status**: RED until all unit tests complete
- **Components**: [list of components under test]
- **Acceptance Criteria**: [what must be true for this AT to pass]
- **Depends on**: UT-1, UT-2, ... (UTs that must pass)
- **Parallel**: yes/no

## Unit Tests (Inner Loop — TPP Order)

### UT-1: [degenerate case description] — TPP Level 1
- **Test**: [methodUnderTest]_[scenario]_[expectedBehavior]
- **Implementation**: [minimum code to pass]
- **Transform**: {}→nil
- **Components**: [class/module]
- **Depends on**: --
- **Parallel**: yes

### UT-2: [next case description] — TPP Level 2
- **Test**: [methodUnderTest]_[scenario]_[expectedBehavior]
- **Implementation**: [minimum code to pass]
- **Transform**: constant→variable
- **Components**: [class/module]
- **Depends on**: UT-1
- **Parallel**: no

...

## Integration Tests (Cross-Component)

### IT-1: [integration scenario]
- **Test**: [description]
- **Components**: [interacting components]
- **Depends on**: UT-3, UT-7
- **Parallel**: no

## Coverage Estimation
[table]

## Risks and Gaps
- [Hard-to-test scenarios]
- [Coverage gaps needing attention]
```

## Anti-Patterns

- Do NOT write test code — only plan scenarios
- Do NOT organize test plan by category (Happy Path, Error Path, etc.) — use TPP ordering
- Do NOT skip error paths
- Do NOT forget boundary values (0, -1, max, empty, null)
- Do NOT plan tests for trivial getters/setters
- Do NOT ignore existing test patterns
- Do NOT create redundant tests covering the same branch

## Template Fallback (RULE-012)

When `.claude/templates/_TEMPLATE-TEST-PLAN.md` is **not available** (projects predating EPIC-0024):

1. Log warning: `"Template not found, using inline format"`
2. Generate the test plan using the inline output format defined in the **Output** section above
3. Execution continues normally — no interruption, no error
4. The inline format produces the same 8 conceptual sections but without the template's strict structure

This ensures backward compatibility with projects that have not yet adopted template-based generation.

## Planning Status Propagation (Rule 22 / EPIC-0046)

> V2-gated: only runs when `SchemaVersionResolver.resolve(ai/epics/epic-XXXX/execution-state.json) == V2`. v1 epics: skip silently (Rule 19).

After writing `tests-story-XXXX-YYYY.md`, check the associated story's lifecycle status. The test plan, like the architecture plan, is a secondary writer — the primary transition `Pendente → Planejada` is owned by `x-plan-story`. When `x-plan-tests` runs standalone (without `x-plan-story`), it promotes the story from `Pendente` to `Planejada` so the source artifact reflects that the test plan is in place.

**Steps (end of test-plan generation, BEFORE the final commit):**

1. Detect v2 via SchemaVersionResolver. If v1: skip.
2. Read current story status:
   ```bash
   CURRENT=$(java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       read ai/epics/epic-XXXX/story-XXXX-YYYY.md)
   ```
3. If `CURRENT == "Pendente"` → `write Planejada`. Idempotent when already `Planejada`.
4. Stage and commit:
   ```bash
   git add ai/epics/epic-XXXX/story-XXXX-YYYY.md ai/epics/epic-XXXX/plans/tests-story-XXXX-YYYY.md
   ```

       Skill(skill: "x-commit-changes", args: "docs(story-XXXX-YYYY): add test plan + update status to Planejada")

**Fail-loud:** non-zero CLI exit aborts the skill (RULE-046-08).
