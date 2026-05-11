# x-merge-pr — Full Protocol

Detailed phase-by-phase reference for `x-merge-pr`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Mutual Exclusivity and Validation

- `--pr` MUST be a positive integer. Zero, negative, or non-numeric input aborts with `PR_NOT_FOUND` before any `gh` call.
- `--strategy` MUST be one of `merge`, `squash`, `rebase`. Any other value aborts with `INVALID_STRATEGY` (exit 1 class).
- `--wait-timeout-min` MUST satisfy `1 <= N <= 600`. Out-of-range values abort with `INVALID_TIMEOUT`.
- `--auto` and `--wait-timeout-min` are independent; when `--auto` is set, `--wait-timeout-min` is accepted but ignored (logged as informational warning).

## Phase 0 — Argument Parsing and Validation

### Step 0.1 — Parse Flags

Extract the five flags from the command line in the order they appear. Assign defaults from the parameters table. Stop and emit `PR_NOT_FOUND` (exit 1) if `--pr` is absent, empty, or not a positive integer.

Log: `"[Phase 0] pr={N} strategy={STRATEGY} deleteBranch={BOOL} auto={BOOL} waitTimeoutMin={N}"`

### Step 0.2 — Validate Enumerations

- If `--strategy` is present but not in `{merge, squash, rebase}`, abort with `INVALID_STRATEGY` (exit code 1 class).
- If `--wait-timeout-min` is present and outside `[1, 600]`, abort with `INVALID_TIMEOUT` (exit code 1 class).
- If both validations pass, proceed to Phase 1.

Log: `"[Phase 0] arguments validated"`

## Phase 1 — PR State Inspection and Idempotency

### Step 1.1 — Single `gh pr view` Call

Execute exactly ONE state-fetching call (performance contract: < 5s in the `--auto` path depends on this being the only gh invocation before merge):

```bash
gh pr view {PR_NUMBER} --json number,state,mergeable,reviewDecision,statusCheckRollup,headRefName,baseRefName
```

Capture stdout into a local variable `prState`. If the call fails with a non-zero exit:

- gh exit 1 with message containing "Could not resolve" or "not found" → abort `PR_NOT_FOUND` (exit 1).
- Any other gh failure → abort with a generic error carrying the gh stderr as message.

Log: `"[Phase 1] fetched state for PR #{N}: state={STATE} mergeable={MERGEABLE} reviewDecision={DECISION}"`

### Step 1.2 — Idempotency Short-Circuit

Apply the following decision table BEFORE any further processing:

| `state` | Action | Response |
| :--- | :--- | :--- |
| `MERGED` | Emit success envelope with `merged=true, prState=MERGED, waitedSec=0, autoEnabled=false`. Exit 0. | Idempotent no-op. |
| `CLOSED` | Abort with `PR_CLOSED` (exit 2). | — |
| `OPEN` | Continue to Phase 2. | — |

Log (when short-circuiting): `"[Phase 1] PR #{N} already in terminal state {STATE}; idempotent exit"`

## Phase 2 — Mode Dispatch

Branch on the `--auto` flag value recorded in Phase 0:

| `--auto` | Next Phase |
| :--- | :--- |
| `true` | Phase 3a (Auto-merge path) |
| `false` | Phase 3b (Synchronous path) |

## Phase 3a — Auto-Merge Path (`--auto`)

### Step 3a.1 — Invoke `gh pr merge --auto`

Build the command by appending flags in this order:

```bash
gh pr merge {PR_NUMBER} --{STRATEGY} --auto [--delete-branch]
```

- `--{STRATEGY}` is one of `--merge`, `--squash`, `--rebase`.
- `--delete-branch` is appended ONLY when `--delete-branch true` (the default).

Execute the command. The performance contract is strict: this step MUST return in < 5s. Do NOT poll, do NOT wait.

### Step 3a.2 — Interpret Result

| `gh` exit | Interpretation |
| :--- | :--- |
| 0 | Auto-merge enabled. Emit `{merged:false, prState:OPEN, waitedSec:0, autoEnabled:true}`. Exit 0. |
| Non-zero, message contains "Pull request is in clean state" | PR was already mergeable and was merged synchronously by GitHub. Refetch state (Phase 1), emit `{merged:true, mergeSha:..., prState:MERGED, waitedSec:0, autoEnabled:false}`. Exit 0. |
| Non-zero, message contains "auto-merge is not allowed" | Repo does not permit auto-merge. Emit error with clear message and exit with `CI_FAILING`-adjacent code 4 (repo misconfiguration; caller should enable auto-merge in repo settings). |
| Non-zero, other | Emit generic error carrying gh stderr. Exit 1. |

Log: `"[Phase 3a] auto-merge enabled for PR #{N}; skill returning"`

## Phase 3b — Synchronous Path (Default)

### Step 3b.1 — Pre-Check: `mergeable`

Inspect the `mergeable` field captured in Phase 1:

| Value | Action |
| :--- | :--- |
| `MERGEABLE` | Proceed to Step 3b.2. |
| `CONFLICTING` | Abort with `MERGE_CONFLICT` (exit 5). |
| `UNKNOWN` | Enter polling loop (Step 3b.1a). |

#### Step 3b.1a — Polling Loop for `UNKNOWN`

Loop with a fixed 10-second sleep between iterations. Break when `mergeable != UNKNOWN` or total elapsed time exceeds `--wait-timeout-min` minutes:

```bash
elapsed=0
timeout_sec=$(( WAIT_TIMEOUT_MIN * 60 ))
while [ "$mergeable" = "UNKNOWN" ] && [ "$elapsed" -lt "$timeout_sec" ]; do
  sleep 10
  elapsed=$(( elapsed + 10 ))
  mergeable=$(gh pr view {N} --json mergeable --jq '.mergeable')
done
```

If the loop exits with `mergeable == UNKNOWN`, abort with `TIMEOUT` (exit 6). Record `waitedSec = elapsed`.

### Step 3b.2 — Pre-Check: `reviewDecision`

| Value | Action |
| :--- | :--- |
| `APPROVED` | Proceed to Step 3b.3. |
| `REVIEW_REQUIRED` \| `CHANGES_REQUESTED` \| `null` | Abort with `NOT_APPROVED` (exit 3). |

### Step 3b.3 — Pre-Check: `statusCheckRollup`

Parse the `statusCheckRollup` array. A "failing" check has `conclusion` in `{FAILURE, ERROR, TIMED_OUT, CANCELLED}` OR `status == IN_PROGRESS` past the timeout.

| Summary | Action |
| :--- | :--- |
| All checks `conclusion == SUCCESS` OR `status == COMPLETED && conclusion == NEUTRAL` | Proceed to Step 3b.4. |
| Any failing check | Abort with `CI_FAILING` (exit 4). |
| Any check still `IN_PROGRESS` | Enter polling loop identical to Step 3b.1a, re-fetching `statusCheckRollup` until all checks complete or timeout. On timeout, abort `TIMEOUT` (exit 6). |

Log: `"[Phase 3b] pre-checks passed: mergeable=MERGEABLE reviewDecision=APPROVED checks=all-green"`

### Step 3b.4 — Invoke `gh pr merge` (Synchronous)

Build and execute the command (NO `--auto` flag):

```bash
gh pr merge {PR_NUMBER} --{STRATEGY} [--delete-branch]
```

Capture stdout and stderr. On non-zero exit:

- stderr contains "not mergeable" → abort `MERGE_CONFLICT` (exit 5).
- stderr contains "approval" → abort `NOT_APPROVED` (exit 3).
- other → abort with generic error carrying stderr.

### Step 3b.5 — Fetch Merge SHA

After a successful `gh pr merge`, refetch PR state to capture the merge commit SHA:

```bash
gh pr view {PR_NUMBER} --json mergeCommit --jq '.mergeCommit.oid'
```

Record as `mergeSha`. If the field is null (rare race), retry after 2 seconds once; if still null, emit success with `mergeSha=null` and a warning log.

### Step 3b.6 — Emit Result

Emit to stdout:

```json
{"merged":true,"mergeSha":"{SHA}","prState":"MERGED","waitedSec":{N},"autoEnabled":false}
```

Exit 0.

Log: `"[Phase 3b] PR #{N} merged synchronously; mergeSha={SHA} waitedSec={N}"`

## Phase 4 — Result Emission

All exit paths (success and failure) emit EXACTLY one line of JSON to stdout and exit with the code from the table in "Output Contract > Exit Codes". Error envelopes carry:

```json
{"merged":false,"prState":"{STATE}","waitedSec":{N},"autoEnabled":false,"errorCode":"{CODE}","message":"{HUMAN_MESSAGE}"}
```

Stderr SHOULD carry the `[Phase N] ...` log lines; stdout MUST carry only the single JSON envelope. This separation allows callers to pipe `stdout | jq` without contamination.

## Worked Examples

### Example 1 — Happy Path (Synchronous, merge strategy)

```bash
/x-merge-pr --pr 123
```

Result (stdout):

```json
{"merged":true,"mergeSha":"a1b2...","prState":"MERGED","waitedSec":3,"autoEnabled":false}
```

### Example 2 — Squash Strategy with Auto-Merge

```bash
/x-merge-pr --pr 456 --strategy squash --auto
```

Result (returns in <5s):

```json
{"merged":false,"prState":"OPEN","waitedSec":0,"autoEnabled":true}
```

GitHub will merge asynchronously when CI + approvals pass.

### Example 3 — Rebase Strategy, Preserve Branch

```bash
/x-merge-pr --pr 789 --strategy rebase --delete-branch false
```

Result (stdout):

```json
{"merged":true,"mergeSha":"c3d4...","prState":"MERGED","waitedSec":5,"autoEnabled":false}
```

### Example 4 — Idempotent Re-Invocation

```bash
/x-merge-pr --pr 123     # already merged by previous run
```

Result (exits 0 immediately, no gh pr merge call):

```json
{"merged":true,"prState":"MERGED","waitedSec":0,"autoEnabled":false}
```

### Example 5 — Error: Not Approved

```bash
/x-merge-pr --pr 123
```

Result (exit 3):

```json
{"merged":false,"prState":"OPEN","waitedSec":0,"autoEnabled":false,"errorCode":"NOT_APPROVED","message":"PR #123 not approved"}
```

## Called Tools

- `gh pr view` (one call minimum, two in the auto-merge "clean state" edge case)
- `gh pr merge` (one call in synchronous path; one call in `--auto` path)
- `sleep` (only in the `UNKNOWN` / `IN_PROGRESS` polling loop)

No git operations are performed by this skill — branch deletion is delegated to `gh pr merge --delete-branch`.

## Testing Contract

### Unit Tests (mocked `gh` CLI)

1. `idempotent_alreadyMerged_returnsMergedTrue` — `gh pr view` returns `state=MERGED`; skill exits 0 with `{merged:true, waitedSec:0}`.
2. `closed_pr_returnsPrClosed` — `gh pr view` returns `state=CLOSED`; skill exits 2 with `PR_CLOSED`.
3. `sync_happy_path_mergeStrategy` — all pre-checks pass; `gh pr merge --merge` succeeds; skill emits `{merged:true, mergeSha:...}`.
4. `sync_happy_path_squashStrategy` — same as above with `--strategy squash`; verifies `--squash` flag propagation.
5. `sync_happy_path_rebaseStrategy` — same with `--strategy rebase`; verifies `--rebase` flag propagation.
6. `sync_notApproved_exitsNotApproved` — `reviewDecision=REVIEW_REQUIRED`; skill exits 3 with `NOT_APPROVED`.
7. `sync_ciFailing_exitsCiFailing` — `statusCheckRollup` contains a `FAILURE` conclusion; skill exits 4.
8. `sync_mergeConflict_exitsMergeConflict` — `mergeable=CONFLICTING`; skill exits 5.
9. `sync_timeout_exitsTimeout` — `mergeable=UNKNOWN` persists past `--wait-timeout-min=1`; skill exits 6 after ~60s.
10. `auto_mode_enablesAutoMerge` — `--auto` → `gh pr merge --auto` returns 0; skill emits `{autoEnabled:true, merged:false}` in <5s.
11. `auto_mode_cleanStateEdgeCase` — `gh pr merge --auto` returns "clean state"; skill refetches and reports as synchronous merge.
12. `deleteBranchFalse_doesNotAppendFlag` — `--delete-branch false`; verify `gh pr merge` invocation does NOT include `--delete-branch`.
13. `invalidStrategy_exitsInvalidStrategy` — `--strategy invalid`; skill exits 1 with `INVALID_STRATEGY` before any `gh` call.
14. `invalidTimeout_exitsInvalidTimeout` — `--wait-timeout-min 0` or `--wait-timeout-min 601`; skill exits 1 with `INVALID_TIMEOUT`.
15. `missingPr_exitsPrNotFound` — no `--pr`; skill exits 1 with `PR_NOT_FOUND`.

### Smoke Test (gated by `GITHUB_TOKEN`)

Single end-to-end test (`PrMergeSmokeTest`) that:

1. Creates a disposable PR on a fixture repository branch.
2. Invokes `/x-merge-pr --pr {N} --auto`.
3. Verifies `autoEnabled=true` and exit 0.
4. Waits up to 60s for GitHub to merge asynchronously.
5. Cleans up the test branch.

Skipped when `GITHUB_TOKEN` is absent; reported as `SKIPPED` to avoid failing CI in contributor forks.

### Coverage Targets

- Line coverage: ≥ 95% (skill-level logic exercised by the 15 unit scenarios above).
- Branch coverage: ≥ 90% (each decision table branch in Phases 1, 3a, 3b covered).
