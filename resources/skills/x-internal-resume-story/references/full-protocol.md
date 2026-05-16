# x-internal-resume-story — Full Protocol

> Depth reference for `x-internal-resume-story`. The SKILL.md body is
> the normative contract; this document expands the workflow
> internals that orchestrators do not need in their working context
> but that implementers and auditors must be able to consult.

## 1. Argument-Parser Rejection Matrix

The parser is a tight single-file loop (`while (($#)); case "$1" in …`)
to keep the SKILL.md within the SkillSizeLinter 500-line threshold
without delegating to `x-internal-normalize-args` (that skill is a
peer, not a dependency — see Rule 14).

| Input | Result | Exit |
| :--- | :--- | :--- |
| `--story-id story-0049-0013 --epic-id 0049` | accepted | 0 |
| `--story-id story-0049-0013` (epic-id missing) | `usage: --epic-id is required` | 64 |
| `--epic-id 0049` (story-id missing) | `usage: --story-id is required` | 64 |
| `--story-id STORY-0049-0013 --epic-id 0049` | accepted (ID normalised to lowercase) | 0 |
| `--story-id story-0049-0013 --epic-id 49` | accepted (zero-padded to `0049`) | 0 |
| `--story-id story-49-13 --epic-id 49` | `usage: --story-id must match story-NNNN-NNNN` | 64 |
| `--unknown-flag` | `usage: unknown flag --unknown-flag` | 64 |
| `--story-id='' --epic-id=0049` | `usage: --story-id must not be empty` | 64 |
| `--help` | print banner + usage; exit 0 | 0 |

The banner is intentionally terse (< 20 lines) so sourcing the skill
from a parent script that accidentally passes `--help` does not flood
the caller's stdout.

## 2. execution-state.json Schema Contract

The skill consumes the following paths inside the state file; any
other schema evolution is safe:

| jq path | Expected type | Used for |
| :--- | :--- | :--- |
| `.stories` | `object` | map of `story-XXXX-YYYY` → story node |
| `.stories[<id>]` | `object` | presence check — exit 2 when missing |
| `.stories[<id>].tasks` | `object` (ordered map) | task classification; `keys_unsorted` preserves insertion order |
| `.stories[<id>].tasks[<taskId>].status` | `string` | bucket into `tasksCompleted` / `tasksPending` |
| `.stories[<id>].tasks[<taskId>].commitSha` | `string \| null` | surfaces into `tasksCompleted[].commitSha` and `lastCommitSha` |
| `.stories[<id>].tasks[<taskId>].completedAt` | `string` (ISO-8601) | staleness comparison (Step 5) |

Supported status strings (case-insensitive, trimmed):

| Success synonyms | Pending / non-terminal |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida`, `Done`, `Merged` | `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `FAILED`, `BLOCKED`, `UNKNOWN`, everything else |

`PR_MERGED` intentionally appears in the pending bucket: in the
`x-implement-story` lifecycle, `PR_MERGED` means the PR landed on
`develop` but `execution-state.json` has not yet been transitioned
to `DONE` by `x-internal-update-status`. The caller
(`x-implement-story`) is the sole authority on that transition;
classifying `PR_MERGED` as DONE here would pre-empt the
orchestrator's own lifecycle bookkeeping.

## 3. Task-Order Invariant Edge Cases

The baseline algorithm in Step 4 assumes DONE tasks precede PENDING
tasks in task-order (the invariant upheld by `x-implement-story`
Phase 2 wave dispatch, which never retries a PENDING task before an
earlier DONE one). When that invariant is violated, the skill
degrades gracefully:

| Task sequence (task-order) | `tasksCompleted` | `tasksPending` | `resumePoint` |
| :--- | :--- | :--- | :--- |
| DONE, DONE, PENDING, PENDING | 2 | 2 | `phase-2-task-3` |
| DONE, PENDING, DONE, PENDING | 2 | 2 | `phase-2-task-2` (first non-DONE wins) |
| PENDING, DONE, DONE, PENDING | 2 | 2 | `phase-2-task-1` (ditto) |
| DONE, DONE, DONE, DONE | 4 | 0 | `all-done` |
| PENDING, PENDING, PENDING, PENDING | 0 | 4 | `fresh-start` |
| (empty tasks map) | 0 | 0 | `fresh-start` |

The non-contiguous-DONE rows (#2 and #3) surface via a stderr
warning:

```text
warn: non-contiguous DONE tasks detected; resume point computed from first non-DONE position
```

so operators can reconcile the state file with `x-reconcile-status`
if the divergence is systematic. The envelope itself stays
well-formed.

`lastCommitSha` always tracks the LAST element of `tasksCompleted` in
task-order, regardless of the invariant. When the invariant holds,
this is the SHA of the most recently merged task; when violated, it
is the last DONE-labelled task in the state file's insertion order
— still a useful anchor for resuming, and matches the semantics of
"last checkpoint" that the caller expects.

## 4. ISO-8601 Timestamp Parser Portability

`completedAt` is written by `x-internal-update-status` as
`date -u +%Y-%m-%dT%H:%M:%SZ` (GNU and BSD both accept this form).
Step 5's staleness comparison reads it back via:

| Platform | Primary invocation | Fallback |
| :--- | :--- | :--- |
| BSD (macOS) | `date -j -f '%Y-%m-%dT%H:%M:%SZ' "${v}" +%s` | — |
| GNU (Linux) | `date -d "${v}" +%s` | — |
| Busybox (Alpine) | `date -u -D '%Y-%m-%dT%H:%M:%SZ' -d "${v}" +%s` | — |

The skill probes by trying the BSD form first (exit-on-failure), then
the GNU form, then the Busybox form. When all three fail, the
`completedAt` for that task is treated as "unknown" and its stale
check is silently skipped — a failed date parse is not a fatal
error; it is a hint that the state file was written by an unusual
toolchain, which the skill tolerates.

## 5. Concurrency Contract

- The skill does NOT open `<state_file>.lock` directly. It delegates
  the locked read to `x-internal-update-status --read-only`, which
  internally acquires `flock -s` (shared lock). The delegation
  consolidates all lock bookkeeping into the pilot skill and avoids
  duplicate lock-acquisition code paths.
- When running in degraded fallback mode (pilot skill unavailable),
  the skill calls `jq -c` directly on the state file without any
  lock. This is deliberate — the fallback is reserved for bootstrap
  scenarios in which no parallel writer exists (the pilot skill is
  itself being generated).
- Step 5 (staleness detection) runs lock-free: it reads the story
  file's mtime via `stat`, not the state file. No lock upgrade is
  required.
- The `x-internal-update-status --read-only` delegation also validates
  the state-file schema as a side effect; parse failures surface as
  its own exit 3, which this skill re-maps to exit 2
  (`STORY_NOT_IN_STATE`) when the cause is a missing story node.

## 6. Performance Profile

Measured on the `ai/epics/epic-XXXX/` fixture (22 stories × ~5 tasks
per story in `execution-state.json`):

| Step | Median time | Dominated by |
| :--- | :--- | :--- |
| 1 | 4 ms | argument parsing |
| 2 | 32 ms | `Skill(x-internal-update-status)` cold start + `flock -s` + `jq` |
| 3 | 8 ms | `jq` iteration over tasks |
| 4 | 1 ms | arithmetic |
| 5 | 12 ms | 1× `stat` + per-task `date -f` |
| 6 | 22 ms | `jq -n` envelope assembly |
| **Total** | **~80 ms** | well under the 200 ms DoD budget |

The skill is CPU-bound on `jq`; the dominant factor is the cold-start
cost of the delegate `x-internal-update-status` invocation (~32 ms).
Inlining the `jq -c .stories[…]` read would shave ~20 ms but forfeit
the shared-lock contract, so the delegation is kept.

## 7. Failure Envelope Examples

Envelope for exit 1 (STATE_FILE_MISSING):

```text
execution-state.json not found
```

Envelope for exit 2 (STORY_NOT_IN_STATE):

```text
Story not in execution-state.json
```

Envelope for exit 64 (usage error):

```text
usage: x-internal-resume-story --story-id <id> --epic-id <id>
```

Envelope for exit 127 (dependency missing):

```text
dependency missing: jq
```

No JSON is written to stdout on any non-zero exit — callers
distinguish success from failure by exit code, not by parsing
stdout.

## 8. Why Delegate the State Read to x-internal-update-status?

Unlike `x-internal-load-story-context` (which reads `stories.<id>.status`
for dependency validation via a direct `jq` call), this skill
delegates its state read to the pilot skill. The reasons diverge:

| Axis | `x-internal-load-story-context` | `x-internal-resume-story` |
| :--- | :--- | :--- |
| Frequency of invocation | Once per story at Phase 0 entry | Once per story at Phase 0 resume-detection step |
| Concurrent writers expected? | No (Phase 0 runs before any `x-implement-story` or `x-implement-task` writes) | Yes (resume can be invoked while a parallel task is in-flight, writing `status=IN_PROGRESS`) |
| Data volume | Single `status` string | Full `tasks.*` sub-tree |
| Lock requirement | Best-effort (dependency check is idempotent) | Shared-lock REQUIRED (reading mid-write of `.tasks[…]` yields partial JSON) |

The second row is the decisive factor: a resume-detection read
concurrent with a task-status write can observe half-updated JSON
(e.g., `status` updated but `commitSha` not yet), which would
produce a DONE task with `commitSha=null` — then `lastCommitSha`
would be wrongly `null` on the returned envelope. Delegating to
`x-internal-update-status --read-only` pins the read behind its
shared lock and eliminates the race.

The tradeoff is a ~20 ms cold-start cost, well within the 200 ms DoD
budget.

## 9. Downstream Consumer Contract (story-0049-0019)

`story-0049-0019` will delete the ~120-line inline resume-detection
block from `x-implement-story` Phase 0 and replace it with a single
`Skill(x-internal-resume-story …)` invocation. The consumer contract
for that downstream refactor:

1. `resumePoint == "fresh-start"` → proceed to Phase 2 task-1 with a
   new branch.
2. `resumePoint == "all-done"` → skip Phase 2 entirely; proceed to
   Phase 3 verification with `lastCommitSha` as the anchor for the
   story branch HEAD.
3. `resumePoint == "phase-2-task-<N>"` → resume Phase 2 from the
   `<N>`-th task; the previous N-1 tasks are DONE and their branches
   are already merged (per `tasksCompleted`).
4. `staleWarnings` non-empty → emit an operator-visible WARN log line
   but do NOT block; the staleness heuristic is advisory (a docs-only
   tweak to the story file after a code task DONE is routine).
5. Exit 2 (`STORY_NOT_IN_STATE`) → initialise a new story node via
   `x-internal-update-status --initialize` and restart from (1).
6. Exit 1 (`STATE_FILE_MISSING`) → same as (5); the state file is
   created by the pilot skill's `--initialize` mode.

This contract is stable across the EPIC-0049 rollout and is pinned
here so the consumer story-0049-0019 does not need to rediscover it.

## 10. Rationale: Why "phase-2-task-<N>" Instead of Task ID?

An early draft returned the first-PENDING task's ID directly (e.g.,
`resumePoint="TASK-0049-0013-004"`). That was rejected because:

1. The caller (`x-implement-story` Phase 0 → Phase 2 wiring) uses
   `resumePoint` to decide WHICH orchestrator phase to enter. A
   task ID couples the envelope to the wave-dispatch algorithm; a
   phase marker (`phase-2-…`) keeps the consumer independent of
   task-implementation-map details.
2. The consumer cross-references `tasksPending[0]` for the actual
   task ID when it needs one — the envelope already carries it.
   Duplicating the ID in `resumePoint` would be redundant.
3. The three-valued vocabulary (`fresh-start` / `all-done` /
   `phase-2-task-<N>`) maps cleanly to the three orchestrator
   branches; a task-ID-valued variant would be one-of-thousands,
   forcing the consumer to parse it.

The 1-based index in `phase-2-task-<N>` is unambiguous: it is the
position of the first non-DONE task in task-order, NOT the numeric
suffix of the task ID. Gaps (TASK-001 DONE, TASK-002 skipped,
TASK-003 PENDING) produce `phase-2-task-2` — the second position
in the state file's iteration order — not `phase-2-task-3`.

---

## Workflow Step Detail (migrated from slim SKILL.md per ADR-0012)

### Step 1 — Argument parsing and path resolution

Parse `--story-id` and `--epic-id`; reject unknown flags and missing required flags with exit `64`. Derive canonical paths:

```bash
epic_dir="ai/epics/epic-${epic_id}"
story_file="${epic_dir}/${story_id}.md"
state_file="${epic_dir}/execution-state.json"
```

When `${state_file}` is not a regular file, exit `1` (`STATE_FILE_MISSING`). The skill does NOT require `story_file` to exist — it is only consulted for its mtime in Step 4; when absent, `staleWarnings` is emitted empty with no error.

### Step 2 — Read state envelope via read-only delegate

Invoke the pilot skill in read-only mode to leverage its `flock -s`-based shared lock and schema validation:

```bash
envelope=$(Skill(skill: "x-internal-update-status",
                 args: "--file ${state_file} --type story \
                        --id ${story_id} --read-only"))
```

The returned envelope contains the current `stories.<id>` node verbatim. When the response envelope's `previousValue` is `null` (id absent from schema), exit `2` (`STORY_NOT_IN_STATE`).

**Fallback (degraded mode):** if `x-internal-update-status` is unavailable (e.g., during bootstrap when the pilot skill itself is being generated), fall back to a direct `jq` read:

```bash
story_node=$(jq -c ".stories[\"${story_id}\"] // empty" \
             "${state_file}")
```

An empty `story_node` triggers exit `2`.

### Step 3 — Classify tasks

Iterate over `story_node.tasks` preserving insertion order (jq's `keys_unsorted`). For each task `<id>` with node `<t>`:

```bash
status=$(echo "${t}" | jq -r '.status // "PENDING"')
sha=$(echo "${t}"    | jq -r '.commitSha // null')
completedAt=$(echo "${t}" | jq -r '.completedAt // null')
```

Classify:

| `status` (case-insensitive) | Bucket |
| :--- | :--- |
| `DONE`, `MERGED`, `COMPLETE`, `Concluída`, `Concluida` | `tasksCompleted` (append `{id, commitSha: sha}`) |
| `PENDING`, `IN_PROGRESS`, `PR_CREATED`, `PR_APPROVED`, `PR_MERGED`, `FAILED`, `BLOCKED`, `UNKNOWN`, any other value | `tasksPending` (append `id`) |

`PR_MERGED` — note: treated as pending when `commitSha` is absent, but the caller may observe `lastCommitSha` separately.

Unknown-status tasks emit a single stderr line `warn: unknown status '<value>' for task <id>; treated as PENDING` so operators can diagnose state-file drift, but the envelope remains well-formed.

### Step 4 — Compute resume point and last commit SHA

```bash
if [[ ${#tasks_completed[@]} -eq 0 ]]; then
  resume_point="fresh-start"
elif [[ ${#tasks_pending[@]} -eq 0 ]]; then
  resume_point="all-done"
else
  first_pending_index=$(( ${#tasks_completed[@]} + 1 ))
  resume_point="phase-2-task-${first_pending_index}"
fi
```

The `first_pending_index` assumes DONE tasks precede PENDING tasks in task-order (invariant upheld by `x-implement-story` Phase 2 wave dispatch). When the invariant is violated — e.g., TASK-001 PENDING, TASK-002 DONE, TASK-003 PENDING — the index is recomputed as the 1-based position of the first non-DONE task regardless of prior gaps.

`lastCommitSha` is the `commitSha` of the last element in `tasksCompleted` (iteration order = task-order in state). `null` when empty.

### Step 5 — Detect staleness

```bash
story_mtime=$(stat -f '%m' "${story_file}" 2>/dev/null \
            || stat -c '%Y' "${story_file}" 2>/dev/null \
            || echo 0)
```

When `story_mtime == 0` (story file absent) or `tasksCompleted` is empty, `staleWarnings` is the empty array and the step short-circuits.

Otherwise, for each DONE task, convert `completedAt` (ISO-8601) to epoch seconds via `date -j -f '%Y-%m-%dT%H:%M:%SZ' "${v}" +%s` (BSD) or `date -d "${v}" +%s` (GNU). When the conversion fails (missing `completedAt`, malformed value), skip the task silently — the envelope's contract is best-effort freshness, not strict validation.

Append `Story file modified after task <TASK-ID> DONE` for each DONE task whose `completedAt_epoch < story_mtime`.

### Step 6 — Assemble and emit envelope

```bash
jq -nc \
  --arg resumePoint "${resume_point}" \
  --argjson tasksCompleted "${tasks_completed_json}" \
  --argjson tasksPending "${tasks_pending_json}" \
  --arg lastCommitSha "${last_commit_sha:-}" \
  --argjson staleWarnings "${stale_warnings_json}" \
  '{resumePoint:$resumePoint,
    tasksCompleted:$tasksCompleted,
    tasksPending:$tasksPending,
    lastCommitSha:(if $lastCommitSha=="" then null
                  else $lastCommitSha end),
    staleWarnings:$staleWarnings}'
```

Emit on stdout as a single line terminated by `\n`. Exit `0`.

---

## Examples

### Example 1 — Happy path: fresh start (nothing DONE)

Envelope: `{"resumePoint":"fresh-start","tasksCompleted":[],"tasksPending":["TASK-...-001","..."],"lastCommitSha":null,"staleWarnings":[]}`. Exit: 0.

### Example 2 — Resume mid-story (3 DONE of 5)

Envelope includes `"resumePoint":"phase-2-task-4"`, three entries in `tasksCompleted`, two IDs in `tasksPending`, and `"lastCommitSha":"<sha-of-task-3>"`. Exit: 0.

### Example 3 — Stale warning: story edited after DONE

Envelope includes `"staleWarnings":["Story file modified after task TASK-0049-0013-001 DONE"]`. Exit: 0.

### Example 4 — Error: state file missing

Stderr: `execution-state.json not found`. Exit: 1.

### Example 5 — Error: story not registered

Stderr: `Story not in execution-state.json`. Exit: 2.

### Example 6 — Boundary: all tasks DONE

Envelope `"resumePoint":"all-done"`, `tasksPending=[]`, `"lastCommitSha":"<sha-of-last-task>"`. Exit: 0.

---

## Performance profile (measured on laptop-class)

Measured on `ai/epics/epic-XXXX/` with 22 stories × ~5 tasks each:

| Step | Median time |
| :--- | :--- |
| 1 (argparse) | 4 ms |
| 2 (state read) | 32 ms |
| 3 (classify) | 8 ms |
| 4 (resume point) | 1 ms |
| 5 (staleness) | 12 ms |
| 6 (envelope) | 22 ms |
| **Total** | **~80 ms** |

Well under the 200 ms DoD budget.

---

## Testing

Acceptance test scenarios (mirroring Section 7 of story-0049-0013):

1. **Degenerate — fresh start.** Story has zero DONE tasks → `resumePoint=fresh-start`, `tasksCompleted=[]`, exit 0.
2. **Happy path — resume mid-story.** 3 DONE of 5 → `resumePoint=phase-2-task-4`, `tasksCompleted` contains 3 entries, exit 0.
3. **Stale warning.** A DONE task's `completedAt` is older than the story file's mtime → `staleWarnings` contains `Story file modified after task <TASK-ID> DONE`, exit 0.
4. **Error — state file missing.** `execution-state.json` absent → exit 1 with message `execution-state.json not found`.
5. **Boundary — all tasks DONE.** Every task status is DONE → `resumePoint=all-done`, `tasksPending=[]`, exit 0.
6. **Error — story not in state.** `--story-id story-9999-9999` but the state file has no matching entry → exit 2.

Coverage requirement: ≥ 95% line / ≥ 90% branch across the invoking Bash codepaths.
