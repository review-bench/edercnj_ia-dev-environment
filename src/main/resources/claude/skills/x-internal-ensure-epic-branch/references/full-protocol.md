# x-internal-ensure-epic-branch — Full Protocol

Detailed phase-by-phase reference for `x-internal-ensure-epic-branch`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Workflow — Step-by-Step

### Step 1 — Argument parsing and `--epic-id` validation

```bash
EPIC_ID=""
BASE_BRANCH="develop"
PUSH="true"

while [ $# -gt 0 ]; do
  case "$1" in
    --epic-id) EPIC_ID="$2"; shift 2 ;;
    --base)    BASE_BRANCH="$2"; shift 2 ;;
    --push)    PUSH="$2"; shift 2 ;;
    *) echo "ERROR: unknown flag: $1" >&2; exit 64 ;;
  esac
done

if ! printf '%s' "$EPIC_ID" | grep -Eq '^[0-9]{4}$'; then
  echo "INVALID_EPIC_ID — Epic ID must be 4 digits" >&2
  exit 1
fi
```

The 4-digit regex matches the story's Section 5.1 validation rule. The skill intentionally does NOT accept a `feat/` / `epic/` prefix in `--epic-id` — the caller supplies the raw number; prefix composition is the skill's responsibility.

### Step 2 — Base branch existence check

```bash
if ! git rev-parse --verify --quiet "$BASE_BRANCH" >/dev/null; then
  echo "BASE_NOT_FOUND — Base branch '$BASE_BRANCH' not found" >&2
  exit 2
fi
BASE_SHA=$(git rev-parse --verify "$BASE_BRANCH")
```

Exits early before touching any network operation. Matches `x-create-git-branch` Step 3 semantics.

### Step 3 — Compute target branch name

```bash
BRANCH_NAME="epic/${EPIC_ID}"
```

Hard-coded prefix — the RULE-001 convention has exactly one form. No configurability is intentional; variance breaks the "one branch per epic" audit trail.

### Step 4 — Local existence detection

```bash
ALREADY_EXISTED="false"
if git rev-parse --verify --quiet "$BRANCH_NAME" >/dev/null; then
  ALREADY_EXISTED="true"
fi
```

A local branch is authoritative over remote — the epic lifecycle runs N times on a developer workstation between pushes. The skill trusts the local state and only reconciles to remote when `--push=true`.

### Step 5 — Remote existence detection (skipped when `--push=false`)

```bash
REMOTE_EXISTS="false"
if [ "$PUSH" = "true" ]; then
  if git ls-remote --exit-code --heads origin "$BRANCH_NAME" >/dev/null 2>&1; then
    REMOTE_EXISTS="true"
  fi
fi
```

When `--push=false`, the remote check is skipped and the complementary-push branch becomes unreachable. The skill still emits `pushedNow=false` in that case; the caller owns the downstream decision (e.g., a dry-run mode may want local-only).

### Step 6 — Three-state decision and execution

```bash
CREATED="false"
PUSHED_NOW="false"

if [ "$ALREADY_EXISTED" = "true" ] && [ "$REMOTE_EXISTS" = "true" ]; then
  # State A — idempotent no-op
  :
elif [ "$ALREADY_EXISTED" = "true" ] && [ "$REMOTE_EXISTS" = "false" ] && [ "$PUSH" = "true" ]; then
  # State B — complementary push
  if ! git push -u origin "$BRANCH_NAME" 2>/tmp/epic-branch-ensure.push.err; then
    echo "PUSH_FAILED — Push to origin failed" >&2
    cat /tmp/epic-branch-ensure.push.err >&2
    exit 4
  fi
  PUSHED_NOW="true"
elif [ "$ALREADY_EXISTED" = "false" ]; then
  # State C — delegate creation to x-create-git-branch
  #
  # Invocation shape (Rule 13 INLINE-SKILL):
  #
  #   Skill(skill: "x-create-git-branch",
  #         args: "--name epic/<ID> --base <BASE_BRANCH> [--push]")
  #
  # The calling orchestrator forwards --push=true/false verbatim. A
  # successful invocation populates baseSha, created=true, pushed=<push>
  # on the x-create-git-branch response; this skill re-exports those as
  # created=true, pushedNow=<push>.
  echo "DELEGATE_TO_X_GIT_BRANCH — name=$BRANCH_NAME base=$BASE_BRANCH push=$PUSH" >&2
  # The orchestrator layer performs the actual Skill(...) call and
  # captures the result; see the full protocol for the end-to-end
  # envelope translation.
  CREATED="true"
  if [ "$PUSH" = "true" ]; then
    PUSHED_NOW="true"
  fi
fi
```

State A, B, and C are mutually exclusive. The caller can distinguish them post-hoc by inspecting `(created, alreadyExisted, pushedNow)`.

### Step 7 — Emit structured response

```bash
printf '{"branchName":"%s","baseSha":"%s","created":%s,"alreadyExisted":%s,"pushedNow":%s}\n' \
  "$BRANCH_NAME" "$BASE_SHA" "$CREATED" "$ALREADY_EXISTED" "$PUSHED_NOW"
```

Single-line JSON keeps parsing cost at O(1) for the caller, which is typically the very first step of an epic entry-point — any overhead here is paid N times per day on a developer workstation.

## Worked Examples

### Example 1 — New epic, branch absent locally and remotely

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0050")
```

Expected stdout:
```json
{"branchName":"epic/0050","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":true}
```
Exit: 0. Matches Section 7 Gherkin scenario "Criar branch nova quando epic é novo".

### Example 2 — Idempotent no-op, branch present locally + remotely

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0049")
```

Expected stdout:
```json
{"branchName":"epic/0049","baseSha":"<sha>","created":false,"alreadyExisted":true,"pushedNow":false}
```
Exit: 0. Matches "Idempotência — branch já existe local + remoto".

### Example 3 — Complementary push, local-only branch

```bash
# Branch created manually earlier; never pushed.
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0049 --push true")
```

Expected stdout:
```json
{"branchName":"epic/0049","baseSha":"<sha>","created":false,"alreadyExisted":true,"pushedNow":true}
```
Exit: 0. Matches "Push complementar — branch local mas não remota".

### Example 4 — Custom base

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0051 --base main")
```

Expected stdout:
```json
{"branchName":"epic/0051","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":true}
```
Exit: 0.

### Example 5 — Missing base branch

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0052 --base mybase")
```

Expected stderr:
```
BASE_NOT_FOUND — Base branch 'mybase' not found
```
Exit: 2. Matches "Erro — base inexistente".

### Example 6 — Malformed epic-id

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 49")
```

Expected stderr:
```
INVALID_EPIC_ID — Epic ID must be 4 digits
```
Exit: 1. Matches "Boundary — epic-id mal formado".

### Example 7 — Local-only flow (`--push false`)

```bash
Skill(skill: "x-internal-ensure-epic-branch",
      args: "--epic-id 0053 --push false")
```

Expected stdout:
```json
{"branchName":"epic/0053","baseSha":"<sha>","created":true,"alreadyExisted":false,"pushedNow":false}
```
Exit: 0. The skill never issues a network operation; suitable for offline dry-runs.

## Performance Contract (story Section 3.5)

| Operation | Budget |
| :--- | :--- |
| State A (idempotent no-op; no network) | < 1s (two `git rev-parse` calls + `ls-remote`) |
| State B (complementary push) | < 3s (`ls-remote` + single push round-trip) |
| State C (delegate to `x-create-git-branch`) | < 3s (branch create + push; matches `x-create-git-branch` budget) |

The "< 1s" ceiling for idempotency is the dominant case in a multi-entry-point workflow: `x-implement-epic` followed by `x-orchestrate-epic` followed by `x-epic-map` on the same epic will trigger State A three times; the aggregate overhead must stay sub-3s.

## Testing — Acceptance Scenarios

The story ships the following acceptance test scenarios, which are the reference contract every downstream caller (0049-0018, 0049-0021, 0049-0022) can rely on:

1. **Happy path (State C)** — branch absent both sides; assert `created=true, alreadyExisted=false, pushedNow=true`; assert the ref exists locally (`git rev-parse`) and on origin (`git ls-remote`).
2. **Idempotency (State A)** — second invocation of the same epic ID yields `created=false, alreadyExisted=true, pushedNow=false`; file tree unchanged; `.git/packed-refs` unchanged.
3. **Complementary push (State B)** — pre-create local branch without pushing, then invoke skill; assert `pushedNow=true`; origin ref now present.
4. **`--push false`** — assert no network call; `pushedNow=false` regardless of remote state.
5. **BASE_NOT_FOUND** — pass `--base unknown`; exit 2; message contains `BASE_NOT_FOUND`.
6. **INVALID_EPIC_ID** — pass `--epic-id 49`; exit 1; message contains `INVALID_EPIC_ID`.

No Java production code is introduced by this story; the SKILL.md is the deliverable. Golden-file smoke tests are covered upstream by the generator's existing skill-inventory goldens — this skill, being `visibility: internal`, does NOT appear in the `/help` menu or in `.claude/README.md`, so no golden-file updates are required.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills ARE still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-ensure-epic-branch")` invocations from other skills resolve correctly. The invariant: **user cannot see them; orchestrators can invoke them.**

## Exit Semantics

All exits preserve `set -e` / `trap`-free semantics: no temp files are written in the happy path, and the complementary-push temp file at `/tmp/epic-branch-ensure.push.err` is overwritten on the next invocation (no accumulation).

## Idempotency Contract — Full Matrix

The skill satisfies RULE-002 idempotency over the full 3-state matrix:

| State before | First call | Second call |
| :--- | :--- | :--- |
| Branch absent (local + remote) | `created=true, alreadyExisted=false, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local, not remote | `created=false, alreadyExisted=true, pushedNow=true` | `created=false, alreadyExisted=true, pushedNow=false` |
| Branch local + remote | `created=false, alreadyExisted=true, pushedNow=false` | `created=false, alreadyExisted=true, pushedNow=false` |

Two concurrent invocations of the same `--epic-id` serialize on the `git` process lock; the later caller always observes the earlier caller's result as "already exists". No `flock` is required — git's own `.git/index.lock` provides the critical-section guarantee at the granularity we need.
