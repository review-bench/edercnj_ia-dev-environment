---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Task Breakdown -- story-0080-0001: /x-create-bug Skill Scaffolding

## Overview

Story-0080-0001 delivers the /x-create-bug skill entry point for EPIC-0080. All work is confined to
the generator source tree (java/src/main/resources/targets/claude/) and test code. No production Java
runtime paths change. The seven tasks follow a strict Red-Green-Refactor (TDD) ordering:
test-definition tasks (004, 005, 007) precede or pair with the implementation tasks they exercise
(001, 002, 003), and doc/smoke tasks close the cycle.

**Total estimated complexity:** MEDIUM-HIGH (7 tasks, 3 new artefacts, 3 test files, 1 whitelist
extension, 1 golden-file regeneration).

---

## task-0080-0001-001 -- Create _TEMPLATE-BUG.md

**Type:** Dev
**Complexity:** MEDIUM
**Depends on:** (none -- first task, unblocked)
**Blocks:** task-0080-0001-003, task-0080-0001-004

### TDD Cycle

**Red:** Write CreateBugFrontmatterAssemblyTest (task-0080-0001-004) first with assertions against
the expected template structure: 9 RA9 sections present, bug-specific subsections present, frontmatter
keys bug-id, severity, status: open, requires-capabilities: [governance.bug-lifecycle]. Tests fail
because the template file does not yet exist.

**Green:** Create java/src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md with all required
sections. The golden-file test in task-0080-0001-004 now passes.

**Refactor:** Verify that all 9 RA9 v2 dimension headings use the correct heading levels. Ensure
reproduction recipe, observed-vs-expected, root-cause hypothesis, and regression-test slot subsections
are under a clearly named Bug Details parent section. Confirm line width <= 120 throughout.

### Files to Create

| Path | Action |
|---|---|
| java/src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md | CREATE |
| src/test/resources/golden/templates/_TEMPLATE-BUG.md | CREATE (golden copy for assertion) |

### Template Required Sections

All of the following headings must be present in the template:

- ## 1. Vision
- ## 2. Persona and Usage Scenario
- ## 3. Value Delivery
- ## 4. Acceptance Criteria
- ## 5. Contracts
- ## 6. Tasks
- ## 7. Dependencies
- ## 8. Decision Rationale
- ## 9. Refinement Verdict
- ## Bug Details
  - ### Reproduction Recipe (preconditions, steps, env)
  - ### Observed vs. Expected
  - ### Root-Cause Hypothesis
  - ### Regression Test Slot (test name + path placeholder)

### Frontmatter Keys Required

All of these keys must be present in the YAML frontmatter block:

- bug-id: placeholder (substituted at scaffold time)
- created-at: placeholder (substituted at scaffold time)
- severity: placeholder (enum: low, medium, high, critical)
- scope: placeholder (enum: single-file, single-module, cross-module)
- status: open
- requires-capabilities: [governance.bug-lifecycle]

### Exit Criteria

- [ ] _TEMPLATE-BUG.md exists at java/src/main/resources/targets/claude/templates/
- [ ] All 9 RA9 v2 section headings present (exact heading text matches RA9 standard)
- [ ] Bug Details section contains all 4 bug-specific subsections
- [ ] Frontmatter includes requires-capabilities: [governance.bug-lifecycle] (Rule 28)
- [ ] Refinement Verdict section contains status: pending placeholder
- [ ] Golden file written to src/test/resources/golden/templates/_TEMPLATE-BUG.md
- [ ] No line exceeds 120 characters

---

## task-0080-0001-002 -- Create bug-lifecycle.yaml capability declaration

**Type:** Dev
**Complexity:** LOW
**Depends on:** (none -- unblocked, can run in parallel with task-0080-0001-001)
**Blocks:** task-0080-0001-003, task-0080-0001-004

### TDD Cycle

**Red:** Extend CreateBugFrontmatterAssemblyTest (task-0080-0001-004) with an assertion that
bug-lifecycle.yaml exists at the expected path and is valid YAML with required v3.0 capability schema
keys (id, version, description, provides, requires). Test fails -- file does not exist.

**Green:** Create java/src/main/resources/targets/claude/capabilities/governance/bug-lifecycle.yaml
with all required v3.0 capability schema keys. Test passes.

**Refactor:** Validate that id value matches the requires-capabilities reference used in
_TEMPLATE-BUG.md (governance.bug-lifecycle). Confirm provides list covers the skill name x-create-bug.

### Files to Create

| Path | Action |
|---|---|
| java/src/main/resources/targets/claude/capabilities/governance/bug-lifecycle.yaml | CREATE |
| src/test/resources/golden/capabilities/governance/bug-lifecycle.yaml | CREATE (golden) |

### Capability Schema (v3.0) Required Keys

- id: governance.bug-lifecycle
- version: 1.0.0
- description: Provides structured bug lifecycle management: scaffold, classification, SLA
  enforcement, RCA capture, and regression-test linkage.
- provides: [x-create-bug, x-audit-bug-classification]
- requires: []
- status: active

### Exit Criteria

- [ ] bug-lifecycle.yaml exists at java/src/main/resources/targets/claude/capabilities/governance/
- [ ] YAML parses without error (SnakeYAML validation in test)
- [ ] id: governance.bug-lifecycle matches requires-capabilities reference in _TEMPLATE-BUG.md
- [ ] provides list includes x-create-bug
- [ ] version follows semver: 1.0.0
- [ ] Golden file written to src/test/resources/golden/capabilities/governance/
- [ ] Rule 28 capability frontmatter contract satisfied (v3.0 schema keys all present)

---

## task-0080-0001-003 -- Create x-create-bug/SKILL.md

**Type:** Dev
**Complexity:** HIGH
**Depends on:** task-0080-0001-001 (template shape locked), task-0080-0001-002 (capability ID locked)
**Blocks:** task-0080-0001-005, task-0080-0001-007

### TDD Cycle

**Red:** Write BugCreationSmokeIT (task-0080-0001-007) skeleton with assertions against the JSON
output envelope shape (bugId, branch, prUrl, scaffoldPath, elapsedMs, status). Write acceptance test
scaffold (task-0080-0001-005) asserting AC-1 happy-path folder creation, AC-2 dirty-tree exit code
15, and AC-3 wall-clock <= 90 s. All tests fail -- skill does not exist.

**Green:** Create java/src/main/resources/targets/claude/skills/dev/x-create-bug/SKILL.md
implementing all required phases:

  Phase 0 -- Arg Parsing and Slug Generation
    POSIX pipeline: NFKD-normalize, strip non-[A-Za-z0-9] chars, lowercase, collapse hyphens,
    trim leading/trailing hyphens, truncate to 40 chars, fallback to untitled if empty.

  Phase 1 -- Worktree Precheck
    Delegate: Skill(skill: "x-internal-precheck-worktree")
    On DIRTY: exit 15 WORKTREE_AMBIGUOUS -- no side effects, no folder, no branch.

  Phase 2 -- ID Allocation
    Scan ai/bugs/bug-*/ for max existing ID, pick max+1, zero-pad to 6 digits.
    Flock on ai/bugs/.allocator-lock (timeout 5 s); retry up to 5 times on collision;
    exit 19 BUSY after 5 retries.

  Phase 3 -- Scaffold
    mkdir ai/bugs/bug-XXXXXX/
    Render bug.md from _TEMPLATE-BUG.md substituting: BUG_ID, CREATED_AT, SEVERITY, SCOPE.

  Phase 4 -- Branch Creation
    Delegate: Skill(skill: "x-create-git-branch", args: "bug/ID-SLUG")
    On branch-exists collision: increment ID once, retry.
    On second collision: exit 16 BRANCH_COLLISION.

  Phase 5 -- Commit
    Delegate: Skill(skill: "x-commit-planning", args: "--path ai/bugs/bug-ID/ ...")
    Commit message format: "bug: scaffold bug-ID -- DESCRIPTION"

  Phase 6 -- PR Creation
    Delegate: Skill(skill: "x-create-pr", args: "--label bug-scaffold --base develop")
    On non-zero gh exit: print recovery hint (manual gh pr create command); exit 17.

  Phase 7 -- Output and Telemetry
    Emit JSON envelope to stdout (6 keys: bugId, branch, prUrl, scaffoldPath, elapsedMs, status).
    Emit skill_end telemetry event to events.ndjson.

  Triggers and Examples sections.
  Error Codes table (exit codes 2, 15, 16, 17, 18, 19).

Also modify: java/src/main/resources/targets/claude/skills/git/x-commit-planning/SKILL.md
  Extend path whitelist to include ai/bugs/** (mirrors existing ai/epics/** entry).

**Refactor:** Verify each phase is <= 25 logical steps. Ensure all delegations use Pattern 1
(INLINE-SKILL): Skill(skill: "x-foo", ...). Confirm zero bare-slash /x-foo invocations in delegation
context (Rule 13). Validate arg schema includes --non-interactive flag.

### Files to Create / Modify

| Path | Action |
|---|---|
| java/src/main/resources/targets/claude/skills/dev/x-create-bug/SKILL.md | CREATE |
| java/src/main/resources/targets/claude/skills/git/x-commit-planning/SKILL.md | MODIFY -- extend whitelist |
| src/test/resources/golden/skills/x-create-bug/SKILL.md | CREATE (golden) |

### Skill Frontmatter Required Keys

- name: x-create-bug
- description: Scaffold a bug-XXXXXX folder with refinement-ready bug.md and bug/* branch.
- model: sonnet
- visibility: public
- user-invocable: true
- requires-capabilities: [governance.bug-lifecycle]
- allowed-tools: [Bash, Skill, Write, Edit]

### Args Schema

- description: required, string, minLength 8, maxLength 120
- severity: optional, enum [low, medium, high, critical], default medium
- scope: optional, enum [single-file, single-module, cross-module], default single-module
- --non-interactive: flag, default false

### Error Codes Table

| Code | Constant | Condition |
|---|---|---|
| 2 | ARGS_INVALID | Description normalizes to empty slug |
| 15 | WORKTREE_AMBIGUOUS | Working tree dirty (Rule 20 Working-Tree Guard) |
| 16 | BRANCH_COLLISION | Branch already exists after one retry |
| 17 | PR_NETWORK_FAILURE | gh pr create exits non-zero |
| 18 | TEMPLATE_MISSING | _TEMPLATE-BUG.md not found at load time |
| 19 | BUSY | Allocator lock timeout (5 s, 5 retries) |

### JSON Output Envelope (stdout)

The skill must emit to stdout a JSON object with exactly these 6 keys:

- bugId: string matching ^bug-[0-9]{6}$
- branch: string matching ^bug/[0-9]{6}-[a-z0-9-]{1,40}$
- prUrl: GitHub PR URL string
- scaffoldPath: string starting with ai/bugs/bug-
- elapsedMs: integer (milliseconds wall-clock)
- status: string "scaffold-ready"

### Exit Criteria

- [ ] SKILL.md exists at java/src/main/resources/targets/claude/skills/dev/x-create-bug/
- [ ] Frontmatter contains all required keys (name, model, visibility, user-invocable,
      requires-capabilities, allowed-tools, args)
- [ ] Slug generation pipeline strips ../, /, backslash, control chars, unicode combining marks
- [ ] Branch naming documented regex: ^bug/[0-9]{6}-[a-z0-9-]{1,40}$
- [ ] Folder naming documented regex: ^bug-[0-9]{6}$
- [ ] All delegations use Skill(skill: "x-foo", ...) Pattern 1 -- no bare-slash invocations
- [ ] Exit codes 2, 15, 16, 17, 18, 19 documented in Error Codes section
- [ ] JSON output envelope shape matches story-0080-0001 section 5.3 contract (6 keys)
- [ ] x-commit-planning whitelist extended to ai/bugs/**
- [ ] --non-interactive flag accepted and suppresses prompts
- [ ] Golden file written

---

## task-0080-0001-004 -- Unit tests: slug generation and frontmatter assembly

**Type:** Test
**Complexity:** MEDIUM
**Depends on:** task-0080-0001-001 (template exists), task-0080-0001-002 (capability exists)
**Blocks:** (none -- test task, CI gate consumer)

### TDD Cycle

**Red:** Create both test class files. All assertions fail initially because templates and capability
files do not yet exist (written before tasks 001 and 002 complete, per TDD discipline).

**Green:** Templates and capabilities created in tasks 001 and 002 make golden-file assertions pass.
Slug-generation logic documented in task 003 provides the canonical pipeline for assertion targets.

**Refactor:** Consolidate shared fixture setup into @BeforeAll. Ensure each test class is <= 250
lines (split if necessary). Verify all test method names follow method_scenario_expected convention.

### Test Files to Create

| Path | Test Class | Coverage Target |
|---|---|---|
| java/src/test/java/.../skills/CreateBugSlugGenerationTest.java | CreateBugSlugGenerationTest | AC-4 slug sanitization |
| java/src/test/java/.../skills/CreateBugFrontmatterAssemblyTest.java | CreateBugFrontmatterAssemblyTest | Template and capability structure |

### CreateBugSlugGenerationTest -- 12 Parametrized Vectors (minimum)

| # | Input Description | Expected Slug Output | Exit Code |
|---|---|---|---|
| 1 | checkout total wrong when promo applied | checkout-total-wrong-when-promo-applied | 0 |
| 2 | ../../../etc/passwd injection | etc-passwd-injection | 0 |
| 3 | //// (only path separators) | (empty -- maps to exit 2) | 2 |
| 4 | Login fails on Safari | login-fails-on-safari | 0 |
| 5 | leading and trailing whitespace | trimmed-slug | 0 |
| 6 | camelCaseTitle | camelcasetitle | 0 |
| 7 | UPPERCASE INPUT | uppercase-input | 0 |
| 8 | unicode chars: cafe, resume | unicode-cafe-resume | 0 |
| 9 | .hidden/./traversal path | hidden-traversal | 0 |
| 10 | multi---hyphens in input | multi-hyphens (collapsed) | 0 |
| 11 | 200-character description string | 40-character truncated slug | 0 |
| 12 | control characters embedded | stripped to clean slug | 0 |

### CreateBugFrontmatterAssemblyTest -- Assertions (8 minimum)

1. Template file exists at generator source path (not just golden path)
2. All 9 RA9 heading strings present in template (exact heading match)
3. Bug Details section present with all 4 named subsections
4. Frontmatter key requires-capabilities contains governance.bug-lifecycle
5. Frontmatter key status equals open
6. bug-lifecycle.yaml parses as valid YAML without exception (SnakeYAML)
7. id field in capability YAML equals governance.bug-lifecycle
8. provides list in capability YAML contains x-create-bug

### Exit Criteria

- [ ] CreateBugSlugGenerationTest exists with >= 12 @ParameterizedTest cases
- [ ] CreateBugFrontmatterAssemblyTest exists with >= 8 named assertions
- [ ] Test naming follows method_scenario_expected convention throughout
- [ ] No isNotNull() used alone as the sole assertion on any test (weak assertion prohibition)
- [ ] Line coverage >= 95%, branch coverage >= 90% on exercised code
- [ ] Both test classes are <= 250 lines each
- [ ] All tests pass on mvn test

---

## task-0080-0001-005 -- Acceptance test: sandboxed git repo full-flow

**Type:** Test
**Complexity:** HIGH
**Depends on:** task-0080-0001-003 (skill must exist for invocation)
**Blocks:** task-0080-0001-007 (SmokeIT reuses this fixture)

### TDD Cycle

**Red:** Write BugScaffoldAcceptanceTest with test methods for AC-1 (folder created, branch created,
PR opened, frontmatter valid), AC-2 (dirty tree produces exit 15 with zero side effects), and AC-3
(wall-clock elapsed <= 90 s). Tests fail because skill does not exist until task 003 completes.

**Green:** Skill delivered by task 003 makes AC-1 and AC-2 tests pass. AC-3 timer assertion passes
when execution completes within the SLA.

**Refactor:** Extract SandboxedGitRepo test fixture (temp dir, git init, git config user.email and
user.name) to a reusable JUnit 5 @Extension or @TempDir abstraction. Remove any Thread.sleep --
use process exit-code wait with explicit timeout. Verify cleanup runs in @AfterEach.

### Test File to Create

| Path | Test Class |
|---|---|
| java/src/test/java/.../skills/BugScaffoldAcceptanceTest.java | BugScaffoldAcceptanceTest |

### AC-1 Scenario (method: scaffold_happyPath_completeFolderAndBranch)

Given: sandboxed git repo with clean worktree
When: skill invoked, description="checkout total wrong", severity=high, scope=single-module,
      --non-interactive
Then:
  - ai/bugs/bug-000001/bug.md exists
  - bug.md frontmatter contains requires-capabilities: [governance.bug-lifecycle]
  - bug.md Refinement Verdict section contains status: pending
  - git branch bug/000001-checkout-total-wrong exists
  - stdout parses as JSON with bugId="bug-000001", status="scaffold-ready"

### AC-2 Scenario (method: scaffold_dirtyWorktree_exits15WithZeroSideEffects)

Given: sandboxed git repo with one uncommitted modified file
When: skill invoked with any description
Then:
  - process exit code == 15
  - ai/bugs/ directory did not change (no new subdirectory)
  - git branch count unchanged (no new branch)
  - stderr contains the string: working tree not clean

### AC-3 Scenario (method: scaffold_nonInteractiveMode_completesWithin90Seconds)

Given: sandboxed git repo with clean worktree, stdin piped with all prompts answered
When: skill invoked with --non-interactive and answers via stdin
Then: System.currentTimeMillis() delta from start to end < 90000 ms

### Exit Criteria

- [ ] BugScaffoldAcceptanceTest exists with all three AC test methods
- [ ] AC-2 method asserts zero side effects: no folder created, no branch created, no commit
- [ ] AC-3 method uses wall-clock ms delta assertion -- no Thread.sleep
- [ ] Sandboxed git repo cleaned up via @TempDir or equivalent in @AfterEach
- [ ] Test method names follow method_scenario_expected convention
- [ ] Tests pass on mvn test or mvn verify depending on IT profile placement

---

## task-0080-0001-006 -- Update .claude/README.md skill index

**Type:** Doc
**Complexity:** LOW
**Depends on:** task-0080-0001-003 (skill defined and frontmatter locked)
**Blocks:** (none)

### TDD Cycle

**Red:** Not applicable for pure documentation. Baseline: confirm .claude/README.md skill index table
has no row for x-create-bug.

**Green:** Insert x-create-bug row into the skill index table. Row must include skill name,
description (copied verbatim from SKILL.md frontmatter), visibility (public), user-invocable (true),
and model (sonnet).

**Refactor:** Confirm table remains sorted alphabetically by skill name after insertion. Verify
description text is not paraphrased -- must match SKILL.md description field character-for-character.

### File to Modify

| Path | Action |
|---|---|
| .claude/README.md | MODIFY -- insert x-create-bug row into skill index table |

### Required Row Values

| Column | Value |
|---|---|
| Skill | x-create-bug |
| Description | Scaffold a bug-XXXXXX folder with refinement-ready bug.md and bug/* branch. |
| Visibility | public |
| User-invocable | true |
| Model | sonnet |

### Exit Criteria

- [ ] .claude/README.md skill index table contains exactly one row for x-create-bug
- [ ] Row description matches SKILL.md frontmatter description field exactly (no paraphrase)
- [ ] Table remains sorted alphabetically by skill name
- [ ] visibility: public and user-invocable: true values are present in the row
- [ ] No other existing rows in the table are modified

---

## task-0080-0001-007 -- BugCreationSmokeIT end-to-end integration test

**Type:** Smoke
**Complexity:** HIGH
**Depends on:** task-0080-0001-003 (skill), task-0080-0001-005 (sandboxed git fixture)
**Blocks:** (none -- final gate)

### TDD Cycle

**Red:** Create BugCreationSmokeIT class skeleton with assertions on the JSON envelope shape (all 6
keys present with correct types and patterns), PR URL regex, and status value. Tests fail until
skill and GitHub integration are both available.

**Green:** Full end-to-end invocation with stdin-piped answers (description=test-bug, severity=high,
scope=single-module) in sandboxed git repo with gh stub or test org. JSON envelope parsed; all 6
key assertions pass including elapsedMs < 90000.

**Refactor:** Extract JSON envelope parsing to a shared BugEnvelopeAssert utility class. Annotate
with @Tag("smoke") for selective CI execution. Replace any Thread.sleep with Process.waitFor(120,
TimeUnit.SECONDS). Add @DisabledIfEnvironmentVariable to skip gracefully when GH_TOKEN absent.

### Test File to Create

| Path | Test Class |
|---|---|
| java/src/test/java/.../it/BugCreationSmokeIT.java | BugCreationSmokeIT |

### JSON Envelope Key Assertions (all 6 required)

| Key | Assertion Type | Constraint |
|---|---|---|
| bugId | regex match | ^bug-[0-9]{6}$ |
| branch | regex match | ^bug/[0-9]{6}-[a-z0-9-]{1,40}$ |
| prUrl | regex match | ^https://github\.com/.+/pull/[0-9]+$ |
| scaffoldPath | startsWith | ai/bugs/bug- |
| status | equals | scaffold-ready |
| elapsedMs | lessThan | 90000 (Long) |

### Exit Criteria

- [ ] BugCreationSmokeIT class annotated with @Tag("smoke")
- [ ] All 6 JSON envelope keys asserted with the constraint types shown above
- [ ] PR URL assertion uses regex pattern match, not substring contains
- [ ] elapsedMs < 90000 assertion present (enforces AC-3)
- [ ] No Thread.sleep -- process wait uses Process.waitFor(120, TimeUnit.SECONDS)
- [ ] IT runs clean on mvn verify -Psmoke
- [ ] Test skips gracefully (not fails) when GH_TOKEN absent via @DisabledIfEnvironmentVariable

---

## Dependency Summary

```
task-0080-0001-001 (Template)    ---+---> task-0080-0001-003 (Skill)
task-0080-0001-002 (Capability)  ---+         |
                                              +--> task-0080-0001-005 (Acceptance)
task-0080-0001-001 (Template)    ---------    |
task-0080-0001-002 (Capability)  --> task-0080-0001-004 (Unit Tests)
                                              |
task-0080-0001-003 (Skill)       --> task-0080-0001-006 (Doc)
task-0080-0001-003 (Skill)  ------+--> task-0080-0001-007 (SmokeIT)
task-0080-0001-005 (Acceptance)---+
```

## Execution Order (Parallelism Opportunities)

| Wave | Tasks | Can Run in Parallel |
|---|---|---|
| Wave 1 | task-0080-0001-001, task-0080-0001-002 | YES -- no inter-dependency between them |
| Wave 2 | task-0080-0001-003, task-0080-0001-004 | YES -- both depend only on Wave 1 outputs |
| Wave 3 | task-0080-0001-005, task-0080-0001-006 | YES -- both depend only on task 003 |
| Wave 4 | task-0080-0001-007 | NO -- requires task 003 and task 005 both complete |

## Coverage Targets

| Test Scope | Line Coverage | Branch Coverage |
|---|---|---|
| CreateBugSlugGenerationTest exercises | >= 95% | >= 90% |
| CreateBugFrontmatterAssemblyTest exercises | >= 95% | >= 90% |
| BugScaffoldAcceptanceTest exercises | >= 95% | >= 90% |
| BugCreationSmokeIT exercises | >= 95% | >= 90% |
| Mutation score (when enabled) | >= 80% | n/a |
