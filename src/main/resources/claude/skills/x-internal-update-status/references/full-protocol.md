# x-internal-update-status — Full Protocol

Detailed reference for `x-internal-update-status`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Workflow — Step-by-Step

### Step 1 — Argument parsing and validation

Parse flags; reject unknown flags; enforce mutual exclusivity of `--read-only` with any write-implying flag combination. When `--type=epic`, the resolved path is `<field>` at document root; when `--type=story`, the path is `stories.<id>.<field>`; when `--type=task`, the path is `stories.<parentStoryId>.tasks.<id>.<field>`. Parent story ID is inferred from the task ID prefix (`TASK-0049-0005-001` ⇒ `story-0049-0005`).

### Step 2 — Lock acquisition

```bash
lock_file="${file}.lock"
exec {fd}>"${lock_file}"
if ! flock -w 30 "${fd}"; then
  echo "Lock timeout on ${lock_file}" >&2
  exit 2
fi
```

### Step 3 — File existence and initialization

- When the file is absent and `--initialize=false`: exit 1.
- When absent and `--initialize=true`: write an empty skeleton `{"version":1,"stories":{}}` via the same atomic tmp+rename contract before proceeding.

### Step 4 — Read and validate path

Parse JSON via `jq`; resolve the path per Step 1. When the path does not exist, exit 3 with the resolved path in the message.

### Step 5 — Idempotency check

Compute `previousValue` at the resolved path. When `previousValue == newValue`, emit the response JSON with `noOp=true` and exit 0 without touching the file. The lock is still released via `flock` descriptor closure.

### Step 6 — Atomic write

```bash
tmp="${file}.tmp.$$"
jq --arg v "${newValue}" "<path-expression>" "${file}" > "${tmp}"
if ! mv "${tmp}" "${file}"; then
  rm -f "${tmp}"
  echo "Atomic write failed: mv returned non-zero" >&2
  exit 4
fi
```

### Step 7 — Emit response and release lock

```bash
file_sha=$(shasum -a 256 "${file}" | cut -d' ' -f1)
printf '{"previousValue":%s,"newValue":"%s","fileSha":"%s","noOp":false}\n' \
  "${prev_json}" "${newValue}" "${file_sha}"
```

The `flock` descriptor is closed on process exit; no explicit unlock is required.

### Step 8 — `--read-only` short-circuit

When `--read-only=true`, Steps 2, 6, and 7 are skipped. The skill opens the file with a shared (`flock -s`) lock, reads the value, emits the response with `noOp=true` and `newValue==previousValue`, and exits 0.

## Worked Examples

### Example 1 — Happy path: mark a story as MERGED

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

Output:
```json
{"previousValue":"IN_PROGRESS","newValue":"MERGED","fileSha":"a1b2...","noOp":false}
```
Exit: 0.

### Example 2 — No-op: value already matches

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value MERGED")
```

Output:
```json
{"previousValue":"MERGED","newValue":"MERGED","fileSha":"a1b2...","noOp":true}
```
Exit: 0.

### Example 3 — Initialize a fresh state file

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type epic --id 0049 \
             --initialize")
```

Output:
```json
{"previousValue":null,"newValue":"2","fileSha":"c3d4...","noOp":false}
```
Exit: 0.

### Example 4 — Task-level update

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type task --id TASK-0049-0005-003 \
             --field prNumber --value 612")
```

Output:
```json
{"previousValue":null,"newValue":"612","fileSha":"e5f6...","noOp":false}
```
Exit: 0.

### Example 5 — Read-only query

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type story --id story-0049-0005 \
             --field status --value UNUSED \
             --read-only")
```

Output:
```json
{"previousValue":"MERGED","newValue":"MERGED","fileSha":"a1b2...","noOp":true}
```
Exit: 0. Note: `--value` is required by the argument schema but is ignored under `--read-only`.

### Example 6 — Invalid path (schema rejection)

```bash
Skill(skill: "x-internal-update-status",
      args: "--file ai/epics/epic-XXXX/execution-state.json \
             --type story --id unknown-story \
             --field status --value DONE")
```

Stderr:
```
Path 'stories.unknown-story.status' not found in schema
```
Exit: 3.

## Concurrency Contract

- All readers and writers must acquire the same `<file>.lock` file descriptor. Exclusive (`flock -x`) for writes; shared (`flock -s`) for `--read-only`.
- Two concurrent invocations targeting **different fields** of the same file serialize through the lock; both updates are preserved.
- Two concurrent invocations targeting **the same field** serialize; the later caller observes the earlier caller's value as `previousValue` — this is the correct "last writer wins" semantic.
- Lock is released automatically on process exit (file descriptor closure). No `trap`-based cleanup required.

## Trailer Injection Contract (story-0059-0004)

Every write operation that stages and commits `execution-state.json` **MUST** inject the canonical trailer into the commit message so that the `.githooks/commit-msg` hook (story-0059-0004 surface F guard) allows the commit to proceed.

### Canonical Trailer Format

```
Co-Authored-By: x-internal-update-status@<40-char-git-sha>
```

- The `<sha>` is the HEAD commit of the repository at the moment the skill executes (`$(git rev-parse HEAD)`).
- Uses the standard Git trailer key `Co-Authored-By:` (parseable via `git interpret-trailers`).

### Commit Invocation Pattern

When this skill issues a `git commit` that includes `execution-state.json` in the staged files, it MUST pass the trailer:

```bash
SKILL_SHA=$(git rev-parse HEAD)
git commit -m "<subject>" \
  --trailer "Co-Authored-By: x-internal-update-status@${SKILL_SHA}"
```

This trailer is validated by `.githooks/commit-msg` which checks:

```bash
git interpret-trailers --parse < "$COMMIT_MSG_FILE" \
  | grep -qE '^Co-Authored-By:\s+x-internal-update-status@[0-9a-f]{40}$'
```

### Recovery escape

In documented recovery operations where the operator manually edits `execution-state.json` (e.g., state corruption), the trailer MUST still be present. The operator adds the trailer with an approved SHA. The hook validates format only, not SHA authenticity (RULE-059).

## Testing — Acceptance Scenarios

The PILOT story (story-0049-0005) ships the following acceptance test scenarios, which are the reference contract every future `x-internal-*` skill MUST replicate in its own directory:

1. **Write happy path** — status transition PENDING → MERGED; assert `noOp=false` and fileSha changes.
2. **No-op detection** — same value twice; assert `noOp=true`, file mtime unchanged.
3. **Read-only** — assert no write occurs; shared lock only.
4. **Lock contention** — spawn 2 concurrent processes mutating distinct fields; both updates present in final file; JSON valid.
5. **FILE_NOT_FOUND** — absent file without `--initialize`; exit 1.
6. **INVALID_PATH** — unknown story ID; exit 3 with path in message.

Goldens under `src/test/resources/golden/internal/ops/x-internal-update-status/` lock the SKILL.md rendering. Coverage requirement: ≥ 95% line / ≥ 90% branch across the invoking Bash codepaths.

## Generator Filter Contract

The `ia-dev-env` generator MUST exclude skills with `visibility: internal` from:

1. The `.claude/README.md` skill-inventory table.
2. The `/help` menu listing surfaced by Claude Code.
3. User-facing autocomplete in the chat input.

Internal skills are still copied into `.claude/skills/` (flat layout) so `Skill(skill: "x-internal-...")` invocations from other skills resolve correctly. The invariant: **user cannot see them; orchestrators can invoke them.**

## Outputs

| Artifact | Path | Description |
| :--- | :--- | :--- |
| Updated state file | `<--file>` | JSON document mutated in place via atomic rename |
| Response envelope | stdout | Single-line JSON (`previousValue` / `newValue` / `fileSha` / `noOp`) |
| Lock file | `<--file>.lock` | Created empty on first invocation; retained for reuse |
