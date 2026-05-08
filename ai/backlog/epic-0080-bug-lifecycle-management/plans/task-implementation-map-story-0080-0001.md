---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Task Implementation Map -- story-0080-0001: /x-create-bug Skill Scaffolding

## Dependency Matrix

Each cell contains Y (task in row BLOCKS task in column) or -- (no blocking relationship).
Read as: "Row task must complete before column task can start."

| Blocks -->          | 001 | 002 | 003 | 004 | 005 | 006 | 007 |
|---------------------|-----|-----|-----|-----|-----|-----|-----|
| task-0080-0001-001  | --  | --  | Y   | Y   | --  | --  | --  |
| task-0080-0001-002  | --  | --  | Y   | Y   | --  | --  | --  |
| task-0080-0001-003  | --  | --  | --  | --  | Y   | Y   | Y   |
| task-0080-0001-004  | --  | --  | --  | --  | --  | --  | --  |
| task-0080-0001-005  | --  | --  | --  | --  | --  | --  | Y   |
| task-0080-0001-006  | --  | --  | --  | --  | --  | --  | --  |
| task-0080-0001-007  | --  | --  | --  | --  | --  | --  | --  |

Legend: Y = blocks (row must complete before column). -- = no blocking relationship.

---

## Critical Path

The longest dependency chain determines the minimum implementation duration.

```
task-0080-0001-001  (MEDIUM)
         |
         v
task-0080-0001-003  (HIGH)   <-- task-0080-0001-002 also feeds here
         |
         v
task-0080-0001-005  (HIGH)
         |
         v
task-0080-0001-007  (HIGH)   <-- final gate
```

Critical path length: 4 tasks (001 -> 003 -> 005 -> 007)

Off-critical-path tasks that can be parallelized with the critical path:
- task-0080-0001-002 runs in parallel with task-0080-0001-001 (Wave 1)
- task-0080-0001-004 runs in parallel with task-0080-0001-003 (Wave 2)
- task-0080-0001-006 runs in parallel with task-0080-0001-005 (Wave 3)

---

## Topological Execution Order

Wave-based schedule respecting all dependencies:

### Wave 1 (parallel)

| Task | Type | Complexity | Blocked By | Blocks |
|---|---|---|---|---|
| task-0080-0001-001 | Dev | MEDIUM | none | 003, 004 |
| task-0080-0001-002 | Dev | LOW | none | 003, 004 |

Both tasks have no dependencies. Execute in parallel. Wave 1 unblocks Wave 2.

### Wave 2 (parallel, after Wave 1 complete)

| Task | Type | Complexity | Blocked By | Blocks |
|---|---|---|---|---|
| task-0080-0001-003 | Dev | HIGH | 001, 002 | 005, 006, 007 |
| task-0080-0001-004 | Test | MEDIUM | 001, 002 | none |

task-0080-0001-004 unit tests should be authored RED (failing) before Wave 1 artefacts exist,
consistent with TDD discipline: test files are created empty/failing, then pass once Wave 1 delivers
the template and capability files. task-0080-0001-003 is the heaviest task in the story; task 004
runs in parallel to reduce wall-clock time.

### Wave 3 (parallel, after task-0080-0001-003 complete)

| Task | Type | Complexity | Blocked By | Blocks |
|---|---|---|---|---|
| task-0080-0001-005 | Test | HIGH | 003 | 007 |
| task-0080-0001-006 | Doc | LOW | 003 | none |

task-0080-0001-005 requires the skill to be invocable; task-0080-0001-006 requires the skill
frontmatter to be finalized (description field must be stable for copy into README).

### Wave 4 (sequential, after both task-0080-0001-003 and task-0080-0001-005 complete)

| Task | Type | Complexity | Blocked By | Blocks |
|---|---|---|---|---|
| task-0080-0001-007 | Smoke | HIGH | 003, 005 | none |

BugCreationSmokeIT is the final integration gate. It reuses the sandboxed git fixture from task 005
and requires the full skill to be functional (task 003) and the fixture infrastructure to be
verified (task 005).

---

## Blocking Relationships (directed)

```
task-0080-0001-001 --> task-0080-0001-003 (template shape required by skill renderer)
task-0080-0001-001 --> task-0080-0001-004 (template file required for golden assertion)
task-0080-0001-002 --> task-0080-0001-003 (capability ID required in skill frontmatter)
task-0080-0001-002 --> task-0080-0001-004 (capability file required for YAML parse assertion)
task-0080-0001-003 --> task-0080-0001-005 (skill must be invocable for acceptance test)
task-0080-0001-003 --> task-0080-0001-006 (skill frontmatter description must be stable for doc)
task-0080-0001-003 --> task-0080-0001-007 (skill must be functional for smoke IT)
task-0080-0001-005 --> task-0080-0001-007 (sandboxed git fixture and AC validations reused)
```

No circular dependencies. DAG is acyclic. Topological sort confirmed.

---

## Artefact Ownership Map

| Artefact | Created By | Consumed By |
|---|---|---|
| java/.../templates/_TEMPLATE-BUG.md | task-0080-0001-001 | task-0080-0001-003, task-0080-0001-004 |
| src/test/resources/golden/templates/_TEMPLATE-BUG.md | task-0080-0001-001 | task-0080-0001-004 |
| java/.../capabilities/governance/bug-lifecycle.yaml | task-0080-0001-002 | task-0080-0001-003, task-0080-0001-004 |
| src/test/resources/golden/capabilities/governance/bug-lifecycle.yaml | task-0080-0001-002 | task-0080-0001-004 |
| java/.../skills/dev/x-create-bug/SKILL.md | task-0080-0001-003 | task-0080-0001-005, task-0080-0001-006, task-0080-0001-007 |
| java/.../skills/git/x-commit-planning/SKILL.md (whitelist extend) | task-0080-0001-003 | task-0080-0001-005, task-0080-0001-007 |
| src/test/resources/golden/skills/x-create-bug/SKILL.md | task-0080-0001-003 | CI audit (LifecycleIntegrityAuditTest) |
| java/.../skills/CreateBugSlugGenerationTest.java | task-0080-0001-004 | CI gate (mvn test) |
| java/.../skills/CreateBugFrontmatterAssemblyTest.java | task-0080-0001-004 | CI gate (mvn test) |
| java/.../skills/BugScaffoldAcceptanceTest.java | task-0080-0001-005 | task-0080-0001-007 (fixture reuse) |
| .claude/README.md (skill index row) | task-0080-0001-006 | Developer documentation |
| java/.../it/BugCreationSmokeIT.java | task-0080-0001-007 | CI gate (mvn verify -Psmoke) |

---

## Risk Register

| Risk | Probability | Impact | Mitigating Task | Fallback |
|---|---|---|---|---|
| Slug pipeline differs between OSes (NFKD normalization) | MEDIUM | HIGH | task-0080-0001-004 (NFR-7) | Pin to Java-side normalization, not POSIX tr |
| x-commit-planning whitelist extension breaks existing paths | LOW | HIGH | task-0080-0001-005 AC-1 | Revert whitelist; use separate commit skill instance |
| GitHub gh CLI unavailable in smoke IT environment | MEDIUM | MEDIUM | task-0080-0001-007 @DisabledIfEnvironmentVariable | Mock gh stub via PATH injection |
| Template section count drifts from RA9 v2 (9 sections) | LOW | HIGH | task-0080-0001-004 golden assertion | Pin heading list in test constant |
| BugCreationSmokeIT wall-clock > 90 s in slow CI | LOW | MEDIUM | task-0080-0001-005 AC-3 baseline | Increase timeout to 120 s for IT; keep 90 s for acceptance |

---

## TDD Commit Sequence (per Red-Green-Refactor)

The following commit order demonstrates correct TDD discipline. Each Red commit must precede its
paired Green commit. Refactor commits must not introduce new behavior (tests remain unchanged).

| # | Commit Type | Description | Task |
|---|---|---|---|
| 1 | Red | CreateBugFrontmatterAssemblyTest skeleton (all assertions failing) | 004 |
| 2 | Red | CreateBugSlugGenerationTest skeleton with 12 parametrized vectors (all failing) | 004 |
| 3 | Green | Create _TEMPLATE-BUG.md; golden file written; assembly test passes | 001 |
| 4 | Green | Create bug-lifecycle.yaml; capability YAML assertion passes | 002 |
| 5 | Red | BugScaffoldAcceptanceTest skeleton (AC-1, AC-2, AC-3 all failing) | 005 |
| 6 | Red | BugCreationSmokeIT skeleton (JSON envelope assertions all failing) | 007 |
| 7 | Green | Create x-create-bug/SKILL.md; extend x-commit-planning whitelist | 003 |
| 8 | Green | BugScaffoldAcceptanceTest AC-1, AC-2, AC-3 pass with live skill | 005 |
| 9 | Green | Slug generation tests pass (pipeline extracted from SKILL.md and validated) | 004 |
| 10 | Refactor | Extract SandboxedGitRepo fixture to @Extension; no new behavior | 005 |
| 11 | Green | BugCreationSmokeIT JSON envelope assertions pass | 007 |
| 12 | Green | .claude/README.md skill index row added | 006 |
| 13 | Refactor | Extract BugEnvelopeAssert utility; cleanup IT boilerplate; no new behavior | 007 |

Atomic commit invariant: each Red commit is a complete failing test (compilable, executable).
Each Green commit makes exactly the tests from the paired Red commit pass. No new tests are added
in Green commits. Refactor commits leave all test outcomes unchanged.
