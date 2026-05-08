---
generated-by: x-task-plan@0792be069d39537b5f4c7c76d7e68372b585697f
generated-at: 2026-04-27T16:50:49Z
story-id: story-0059-0002
---

# Task Breakdown — story-0059-0002: Origin Markers in Artifacts + Anti-Backfill Audit

## Task List

| Task ID | Title | Layer | Size | Depends On | Branch |
| :--- | :--- | :--- | :--- | :--- | :--- |
| TASK-0059-0002-001 | Emit frontmatter in x-arch-plan and x-internal-story-build-plan | Adapter (SKILL.md) | M | — | `feat/task-0059-0002-001-frontmatter-planning-skills` |
| TASK-0059-0002-002 | Emit frontmatter in x-test-plan and skills of security/compliance | Adapter (SKILL.md) | S | TASK-0059-0002-001 | `feat/task-0059-0002-002-frontmatter-remaining-skills` |
| TASK-0059-0002-003 | Add SHA validation and anti-backfill to audit script | Adapter (CI script) | M | TASK-0059-0002-001, TASK-0059-0002-002 | `feat/task-0059-0002-003-audit-anti-backfill` |

## TASK-0059-0002-001: Emit frontmatter in x-arch-plan and x-internal-story-build-plan

**Layer:** Adapter (SKILL.md + source-of-truth copies)
**Test Type:** Unit (grep-based verification)
**Size:** M
**Dependencies:** none

**Files to modify:**
1. `java/src/main/resources/targets/claude/skills/core/plan/x-arch-plan/SKILL.md`
   - Add frontmatter emission instruction in the "Save" step of the subagent prompt
   - Add frontmatter emission instruction in Step 7 (save document)
2. `java/src/main/resources/targets/claude/skills/core/internal/plan/x-internal-story-build-plan/SKILL.md`
   - Add frontmatter emission instruction before each artifact write
3. `.claude/skills/x-arch-plan/SKILL.md` (generated copy — update in sync)
4. `.claude/skills/x-internal-story-build-plan/SKILL.md` (generated copy — update in sync)

**Frontmatter instruction to embed (canonical template):**

```markdown
#### Origin Marker Emission (EPIC-0059 — mandatory)

Before writing the output file, prepend the YAML frontmatter block:

```
GENERATED_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
GENERATED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
```

Output file MUST start with:
```yaml
---
generated-by: <skill-name>@${GENERATED_SHA}
generated-at: ${GENERATED_AT}
story-id: ${STORY_ID}
---
```

This origin marker is required by `audit-execution-integrity.sh` Phase-1 validation (EPIC-0059).
```

**Acceptance Criteria:**
- [ ] x-arch-plan subagent prompt includes the frontmatter emission instruction
- [ ] x-internal-story-build-plan emit instruction present for arch, impl, task breakdown artifacts
- [ ] Template: `generated-by: x-arch-plan@$(git rev-parse HEAD)`
- [ ] Template: `generated-at: $(date -u +%Y-%m-%dT%H:%M:%SZ)`
- [ ] Both source-of-truth and generated copy files updated

**TDD Cycles:**
1. RED: write test that greps x-arch-plan SKILL.md for "generated-by" → fails (not present)
2. GREEN: add frontmatter emission instruction to x-arch-plan SKILL.md
3. REFACTOR: clean up instruction block formatting
4. RED: same test for x-internal-story-build-plan
5. GREEN: add to x-internal-story-build-plan
6. REFACTOR: ensure both instructions use the same canonical template wording

---

## TASK-0059-0002-002: Emit frontmatter in x-test-plan and skills of security/compliance

**Layer:** Adapter (SKILL.md)
**Test Type:** Unit (grep-based)
**Size:** S
**Dependencies:** TASK-0059-0002-001 (establishes canonical template)

**Files to modify:**
1. `java/src/main/resources/targets/claude/skills/core/test/x-test-plan/SKILL.md`
2. `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` (Phase 1E security section)
3. `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` (Phase 1F compliance section)
4. `java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md`
5. Generated copies under `.claude/skills/`

**Acceptance Criteria:**
- [ ] x-test-plan emits frontmatter in tests-story-*.md (with `generated-by: x-test-plan@<sha>`)
- [ ] x-story-implement Phase 1E emits frontmatter in security-story-*.md
- [ ] x-story-implement Phase 1F emits frontmatter in compliance-story-*.md
- [ ] x-task-plan emits frontmatter in tasks-story-*.md

**TDD Cycles:**
1. RED: grep x-test-plan SKILL.md for "generated-by" → fails
2. GREEN: add emission instruction to x-test-plan
3. RED: grep x-story-implement SKILL.md Phase 1E for "generated-by" → fails
4. GREEN: add to Phase 1E
5. RED: grep x-story-implement SKILL.md Phase 1F for "generated-by" → fails
6. GREEN: add to Phase 1F
7. REFACTOR: align wording with task 001 canonical template

---

## TASK-0059-0002-003: Add SHA validation and anti-backfill to audit script

**Layer:** Adapter (CI script)
**Test Type:** Smoke (end-to-end with temp git repo)
**Size:** M
**Dependencies:** TASK-0059-0002-001, TASK-0059-0002-002 (frontmatter format finalized)

**Files to modify:**
1. `scripts/audit-execution-integrity.sh`
   - Add `check_frontmatter_origin()` function
   - Add `check_anti_backfill()` function
   - Integrate into `check_phase1_evidence()`: call both functions per artifact
   - Add `EIE_BACKFILL_DETECTED` to error message catalogue
   - Update `--self-check` to verify new functions are present

**Files to create:**
1. `src/test/bash/audit-anti-backfill-smoke.sh` — standalone smoke test script

**Acceptance Criteria:**
- [ ] `check_frontmatter_origin()` validates presence, format, and SHA existence
- [ ] `check_anti_backfill()` compares artifact first-commit timestamp to story merge timestamp
- [ ] Exit 1 with `EIE_BACKFILL_DETECTED` when artifact is posterior to merge
- [ ] `<!-- audit-exempt: backfill <url> -->` accepted; empty URL → exit 3 `EIE_INVALID_EXEMPTION`
- [ ] Smoke test AT-01 to AT-08 all pass
- [ ] `--self-check` verifies both new functions are defined in the script
- [ ] Grandfathered stories skip frontmatter checks (backward compat)

**TDD Cycles:**
1. RED: AT-01 — artifact without frontmatter → write test, run → fails (function doesn't exist)
2. GREEN: implement `check_frontmatter_origin()` skeleton (just header check)
3. RED: UT-04 — format validation fails
4. GREEN: add regex `^[a-z-]+@[0-9a-f]{40}$`
5. RED: UT-07 — SHA not in git fails
6. GREEN: add `git cat-file -t <sha>` validation
7. RED: AB-05 — backfill detection fails
8. GREEN: implement `check_anti_backfill()` with timestamp comparison
9. RED: AT-05 — exemption passes
10. GREEN: verify existing `has_audit_exempt()` handles "backfill" variant
11. RED: AT-06 — empty exemption link → exit 3
12. GREEN: update exemption regex to require URL after "backfill"
13. REFACTOR: extract `_sha_exists_in_git()` helper, clean error messages

## Implementation Map

```
TASK-0059-0002-001 (x-arch-plan + x-internal-story-build-plan frontmatter)
    ↓ unblocks
TASK-0059-0002-002 (x-test-plan + Phase 1E/1F frontmatter)
    ↓ both unblock
TASK-0059-0002-003 (audit SHA validation + anti-backfill)
```

Serial execution required (002 depends on 001; 003 depends on both).
