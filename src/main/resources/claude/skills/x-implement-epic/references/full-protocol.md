# x-implement-epic — Full Protocol

> Companion reference to `SKILL.md` (thin orchestrator).
> This file collects everything that would bloat the main skill body:
> retry/backoff schedules, circuit-breaker rules, recovery algorithms,
> resume workflow, and the `SubagentResult` error shape emitted by delegates.
> Ordered by section reference used from `SKILL.md`.

## §1 — `args-schema.json`

The schema consumed by `x-internal-normalize-args` in Phase 0. Stored as a
sibling file at [`references/args-schema.json`](args-schema.json) so the
normalizer can load it via `@` syntax. The schema declares every flag
listed in `SKILL.md` §Parameters. Mutual-exclusion groups are declared there
(e.g., `--phase` vs `--story`).

Do NOT duplicate the schema in this file — read `args-schema.json`
directly. This reference is concerned with the runtime semantics the
orchestrator layers on top of the normalizer output.

## §2 — Phase 3: retry / backoff / circuit-breaker

### 3.1 Per-story retry

Each story dispatch may return one of the following `SubagentResult` envelopes on non-success:

```json
{
  "status": "FAILED",
  "errorClass": "TRANSIENT" | "CONTEXT" | "PERMANENT",
  "summary": "<short cause>",
  "retryable": true | false
}
```

Retry schedule (per story, max 3 attempts including the first):

| Attempt | Delay before retry | Condition |
| :---: | :---: | :--- |
| 1 (initial) | — | — |
| 2 | 2s | `errorClass=TRANSIENT` or (`errorClass=CONTEXT` and `retryable=true`) |
| 3 | 4s | Same as above |
| — | — | `errorClass=PERMANENT` → never retried |

The total backoff cap is 8s (2 + 4 + 2 jitter). `CONTEXT`-class errors
(context-window pressure from a dispatched subagent) retry with a
reduced-context variant of the prompt on attempt 2 onwards — the
orchestrator truncates the story's prior artifacts to their headers.

### 3.2 Block propagation

When a story transitions to `FAILED` and `--revert-on-failure=false`:

1. Every story whose `blockedBy` transitively includes the failed story's
   ID is marked `BLOCKED` via `x-internal-update-status` (atomic).
2. The orchestrator emits a human-readable block:
   ```
   [block-propagation] story-XXXX-YYYY FAILED — propagating BLOCKED to: story-XXXX-ZZZZ, story-XXXX-WWWW
   ```
3. Phase 3 aborts with exit code `STORY_FAILED` after the propagation
   is persisted.

### 3.3 Circuit-breaker

Trip conditions (evaluated after every story result):

| Trigger | Action |
| :--- | :--- |
| 3 consecutive `FAILED` in the same phase | OPEN state: pause execution |
| 5 total `FAILED` in the same phase (sliding window) | OPEN state: abort phase |

In OPEN state with `--non-interactive=false`, the orchestrator emits the
EPIC-0043 standard 3-option menu via `AskUserQuestion`:

- PROCEED — mark remaining stories `BLOCKED`, exit with `STORY_FAILED`
- FIX-AND-RESUME — exit with instructions to fix locally and resume
- ABORT — exit immediately without propagation

With `--non-interactive=true`, circuit-breaker trip = automatic
`STORY_FAILED` exit. The breaker state is NOT persisted to
`execution-state.json` (it resets on resume).

## §3 — Phase 4: integrity-gate failure recovery

When `x-internal-verify-epic-integrity` returns `passed=false`:

### 4.1 `--revert-on-failure=true` path

1. Identify the last story whose merge commit to `epic/<EPIC-ID>` precedes
   the failure (read from `execution-state.json` — most recent
   `prMergeStatus=MERGED` story). Use `x-internal-update-status
   --read-only` to fetch without locking.
2. Revert via:
   ```
   Skill(skill: "x-merge-branches", args: "--source <prior-HEAD-sha> --target epic/<EPIC-ID> --strategy merge --message \"revert: integrity-gate failure on story-XXXX-YYYY\"")
   ```
3. Mark the story `REVERTED` in execution-state (status update delegate).
4. Re-run the gate exactly once. If `passed=true` → continue to Phase 5
   with the reverted story logged in `storiesExecuted[].status=REVERTED`.
5. If `passed=false` on the second run → exit `INTEGRITY_GATE_FAILED`.

### 4.2 Agent-assisted remediation path (default)

1. Dispatch a Rule 13 Pattern 2 (SUBAGENT-GENERAL) regression-fix agent
   with a prompt that pins:
   - The integrity-gate envelope (`failures` array, `coverageDelta`)
   - The story PRs merged since the last successful gate run
   - A single instruction: "produce one commit on `epic/<EPIC-ID>` that
     restores the gate — do not create a PR; the orchestrator will re-run
     the gate."
2. Wait for the subagent to return its `SubagentResult`. Expected shape:
   ```json
   {"status": "SUCCESS", "commitSha": "<sha>", "summary": "fix(epic-XXXX): restore coverage in XModule"}
   ```
3. Re-run the gate once. If `passed=true` → continue. If `passed=false`
   or the subagent returned `FAILED` → exit `INTEGRITY_GATE_FAILED`.

Only one recovery attempt per gate invocation. Repeated failures require
operator intervention.

## §4 — Phase 5: TTY detection and final-PR gate

### 5.1 TTY detection

The orchestrator does NOT probe `/dev/tty` directly (that would be an
inline `test -t` — forbidden by the no-inline-shell rule). Instead it
relies on `--non-interactive`:

| `--non-interactive` | Behavior |
| :--- | :--- |
| `true` | Skip Phase 5.3 menu; leave the PR open and exit 0 |
| `false` (default) | Emit `AskUserQuestion` menu; caller's TTY handling dictates whether the menu blocks |

### 5.2 Final-PR gate menu

Three fixed options (EPIC-0043 / RULE-020):

- **PROCEED** — leave the PR open; log `[final-pr-gate] PR #<N> left open for human review.`
- **FIX-PR** — invoke `Skill(skill: "x-fix-pr", args: "<prNumber>")`; on return, loop back to the menu. Guard-rail: max 3 consecutive FIX-PR before auto-exit with `GATE_FIX_LOOP_EXCEEDED` (EPIC-0043).
- **ABORT** — exit 0 without further action; PR remains open.

Do NOT offer a MERGE option. The final PR is the last human review point
(RULE-004). Auto-merging it would defeat the entire EPIC-0049 design
principle.

## §5 — Resume workflow

1. Read `execution-state.json` via `x-internal-update-status --read-only`.
2. For each story whose `status` is IN_PROGRESS / PR_CREATED /
   PR_PENDING_REVIEW, the resume projection inside
   `x-internal-build-epic-plan`'s envelope already includes the
   reclassified status (the sub-skill handles `gh pr view`
   cross-validation — orchestrator never calls `gh` directly).
3. Stories already in `SUCCESS` skip the Phase 3 loop.
4. Stories in `FAILED` with `retries < MAX_RETRIES` are reset to
   `PENDING` by the build-plan sub-skill; stories at the retry budget
   remain `FAILED`.
5. Phase 4 (integrity gate) runs regardless of resume — the gate is
   idempotent.

## §6 — `SubagentResult` error shape

Every delegate skill that can produce an error envelope adheres to the
following shape:

```json
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "storyId": "story-XXXX-YYYY",
  "summary": "<short cause>",
  "errorClass": "TRANSIENT" | "CONTEXT" | "PERMANENT",
  "retryable": true | false,
  "commitSha": "<sha>" | null,
  "prNumber": 123 | null,
  "prUrl": "..." | null,
  "coverageLine": 95.8 | null,
  "coverageBranch": 91.2 | null,
  "contextPressureDetected": false
}
```

The orchestrator records the full envelope in its in-memory per-story
map but only persists `status`, `prNumber`, `prUrl`, and `commitSha` via
`x-internal-update-status`. The other fields stay in the subagent's
context and are surfaced only when needed for the retry/circuit-breaker
logic.

## §7 — `--auto-approve-pr` propagation

Controls only the task-PR-into-parent-branch flow inside each story
(RULE-004 task-level parent-branch mode). Propagation:

- Phase 3 dispatches `x-implement-story <STORY-ID> --auto-approve-pr [...]` unchanged.
- Each story creates its own `feat/story-XXXX-YYYY-<desc>` parent branch
  **off the epic branch**. Task PRs inside the story target that parent branch.
- When the story finishes, the parent branch is merged into `epic/<EPIC-ID>`
  via `x-implement-story`'s own Phase 3.7 (story-level PR). The orchestrator
  sees this as a single story PR per usual.

## §8 — Known limitations

- The orchestrator does not snapshot `mainShaBeforePhase` inline — the
  integrity-gate sub-skill computes its coverage delta from the commit
  range declared by the build-plan's `storiesExecuted` array, which is
  deterministic after Phase 3.
- `--story` mode skips Phase 4 entirely (single-story runs).
- `--dry-run` exits after Phase 1; `execution-state.json` is NOT created (dry-run is
  read-only on state).
