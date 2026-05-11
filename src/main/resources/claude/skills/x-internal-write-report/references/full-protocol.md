# x-internal-write-report — Full Protocol

Carve-out companion for [`../SKILL.md`](../SKILL.md) per ADR-0007. The
main SKILL.md is the normative contract; this file is reserved for
future expansion (extended workflow pipelines, jq recipes,
troubleshooting matrix). At the time story-0049-0007 landed the main
SKILL.md was already comprehensive, so this page exists primarily to
satisfy the `SkillSizeLinter` references-sibling requirement while
keeping a clear seam for follow-up stories to migrate detail out of
the 500-line window.

## §1 — Renderer pipeline (reference)

The canonical rendering order is:

1. Read template body from disk (`.claude/templates/<name>` or
   absolute path).
2. Parse `--data` as inline JSON or `@path` file.
3. Expand all `{{#each <collection>}} … {{/each}}` blocks inside-out.
4. Substitute remaining `{{<dot-path>}}` placeholders via `jq -r`.
5. Write rendered body to the target path via atomic `mv` (overwrite
   mode) OR merge by `## ID:` marker (`--append=true`).

The canonical Bash skeleton lives in SKILL.md §Workflow — this section
is reserved for future expansion into full jq recipes and
troubleshooting matrix.

## §2 — Relationship with sibling internal skills

| Skill | Shared behaviour | Distinct behaviour |
| :--- | :--- | :--- |
| `x-internal-update-status` (pilot, 0049-0005) | Convention anchors, `visibility: internal`, Bash-only toolset | Mutates markdown Status fields; no template rendering |
| `x-internal-normalize-args` (0049-0007) | Convention anchors, stdout JSON envelope, no file writes (this one DOES write files) | Parses argv against schema; no template rendering |

## §3 — Follow-up carve-out opportunities

Candidate sections to migrate out of the main SKILL.md in a future
story if/when it grows past 500 lines again:

- The 6 worked examples (§Examples 1–7).
- The full Outputs / Error Handling tables.
- The Testing scenario catalogue (10 scenarios).

Until then, the main SKILL.md remains the single source of truth for
the skill's contract; orchestrators MUST read only SKILL.md for
invocation semantics.

---

## §4 — Workflow Step Detail (migrated from slim SKILL.md per ADR-0012)

### Step 1 — Argument parsing and validation

Parse flags; reject unknown flags. Reject duplicate flags. Enforce:

- `--template`, `--output`, `--data` all mandatory.
- `--append` is boolean; absence defaults to `false`.

When `--template` is a bare name (no `/`), resolve against `<CLAUDE_PROJECT_DIR>/.claude/templates/<name>`; an absolute or relative path containing `/` is used verbatim (subject to traversal guard against `..` above the project root per Rule 06).

### Step 2 — Template read

```bash
if [[ ! -r "${template_path}" ]]; then
  echo "Template '${template_name}' not found in .claude/templates/" >&2
  exit 1
fi
template_body=$(cat "${template_path}")
```

### Step 3 — Data payload parse

```bash
if [[ "${data_arg}" == @* ]]; then
  data_path="${data_arg#@}"
  [[ -r "${data_path}" ]] || { echo "Invalid JSON in --data" >&2; exit 2; }
  data_json=$(cat "${data_path}")
else
  data_json="${data_arg}"
fi
if ! echo "${data_json}" | jq -e '.' >/dev/null 2>&1; then
  echo "Invalid JSON in --data" >&2
  exit 2
fi
```

### Step 4 — `{{#each}}` expansion (inside-out)

Expand the deepest `{{#each}}` block first, substituting per-element placeholders against the current array element (with fallback to root data). Repeat until no `{{#each}}` tokens remain. A simple regex-based state machine is sufficient because blocks are well-bracketed and never straddle line continuations.

Implementation contract:

```bash
while echo "${body}" | grep -q '{{#each '; do
  body=$(expand_innermost_each "${body}" "${data_json}")
done
```

The helper `expand_innermost_each`:

1. Locates the innermost `{{#each <expr>}} … {{/each}}` match.
2. Resolves `<expr>` via `jq` against `${data_json}`; if the result is not an array, exit with a clear error (non-zero, message includes the expression).
3. For each element, substitutes block-local placeholders (`{{field}}` or `{{field.nested}}`), falling back to the root data.
4. Concatenates rendered elements in array order, replacing the whole `{{#each}}…{{/each}}` span in the body.

### Step 5 — Simple placeholder substitution (strict)

After all `{{#each}}` blocks are resolved, walk remaining `{{<dot-path>}}` tokens and substitute each via `jq -r`. When the lookup yields `null` or the path is absent, exit 3 with `UNRESOLVED_PLACEHOLDER` and the exact token in the message. Count every successful substitution into `placeholdersReplaced`.

### Step 6 — Atomic write (overwrite or --append merge)

**Overwrite mode (default):**

```bash
mkdir -p "$(dirname "${output_path}")"
tmp="${output_path}.tmp.$$"
printf '%s' "${rendered}" > "${tmp}"
if ! mv "${tmp}" "${output_path}"; then
  rm -f "${tmp}"
  echo "Failed to write ${output_path}" >&2
  exit 4
fi
```

**Append mode (`--append=true`):** the rendered body is interpreted as a sequence of one or more sections, each beginning with a line of the form `## ID: <value>` (leading `##` required; value is the trimmed remainder). For every such section in the rendered body:

1. Scan the existing output file for an identical `## ID: <value>` marker.
2. If found, replace the existing section (up to the next `## ID:` marker or EOF) with the new section body. Do not increment `entriesAppended`.
3. If not found, append the new section at the end. Increment `entriesAppended`.
4. When the existing output file does not exist, behave as overwrite mode and count every rendered section as appended.

Both branches end with the same atomic `mv` tmp → final contract as overwrite mode. The append merge is performed in-memory (a single awk/jq pipeline), never line-by-line via interleaved writes.

### Step 7 — Emit response

```bash
bytes=$(wc -c < "${output_path}" | tr -d ' ')
if [[ "${append_mode}" == true ]]; then
  printf '{"outputPath":"%s","bytesWritten":%s,"placeholdersReplaced":%s,"entriesAppended":%s}\n' \
    "${output_path}" "${bytes}" "${replaced}" "${appended}"
else
  printf '{"outputPath":"%s","bytesWritten":%s,"placeholdersReplaced":%s,"entriesAppended":null}\n' \
    "${output_path}" "${bytes}" "${replaced}"
fi
```

Exit 0.

---

## §5 — Examples

### Example 1 — Simple render without loops

Template `.claude/templates/_TEMPLATE-EPIC-HEADER.md`:

```markdown
# Epic {{epicId}} — {{title}}

Status: {{status}}
```

Invocation:

```text
Skill(skill: "x-internal-write-report",
      args: "--template _TEMPLATE-EPIC-HEADER.md \
             --output ai/epics/epic-XXXX/reports/header.md \
             --data '{\"epicId\":\"XXXX\",\"title\":\"Skill hygiene\",\"status\":\"IN_PROGRESS\"}'")
```

Output file contains `Epic XXXX — Skill hygiene` and `Status: IN_PROGRESS`. Stdout: `{"outputPath":"...","bytesWritten":52,"placeholdersReplaced":3,"entriesAppended":null}`. Exit: 0.

### Example 2 — `{{#each}}` loop with per-item fields

Template fragment:

```markdown
{{#each stories}}
- {{id}} ({{status}})
{{/each}}
```

Data:

```json
{"stories":[{"id":"story-0049-0001","status":"DONE"},
            {"id":"story-0049-0002","status":"DONE"},
            {"id":"story-0049-0006","status":"IN_PROGRESS"}]}
```

Rendered body:

```markdown
- story-0049-0001 (DONE)
- story-0049-0002 (DONE)
- story-0049-0006 (IN_PROGRESS)
```

`placeholdersReplaced` = 6 (two per iteration).

### Example 3 — `--append` with update in place (no duplication)

Existing file has two `## ID: story-XXXX-NNNN` sections; invocation with `{"id":"story-XXXX-0001","status":"DONE"}` updates the existing first section without duplicating. Stdout: `"entriesAppended":0`.

### Example 4 — `--append` with new entries

Same output file as Example 3; data now introduces an unseen ID. Final file gains a third section at the end. Stdout: `"entriesAppended":1`.

### Example 5 — Error: template not found

Stderr: `Template '_TEMPLATE-NONEXISTENT.md' not found in .claude/templates/`. Exit: 1.

### Example 6 — Error: unresolved placeholder (strict mode)

Template contains `{{undefined_key}}`; data is `{"epicId":"0049"}`. Stderr: `Placeholder '{{undefined_key}}' has no value (strict mode)`. Exit: 3.

### Example 7 — Error: invalid JSON

`--data '{epicId:0049}'` (unquoted keys). Stderr: `Invalid JSON in --data`. Exit: 2.

---

## §6 — Testing Scenarios (10 acceptance scenarios)

Story-0049-0006 ships acceptance test scenarios that every future `x-internal-write-report` consumer MUST be able to rely on:

1. **Simple render** — `{{epicId}}` substituted; `placeholdersReplaced=1`.
2. **Nested placeholder** — `{{stories.story-0049-0001.status}}` resolves.
3. **`{{#each}}` happy path** — array of three stories renders three lines.
4. **`{{#each}}` empty collection** — zero iterations, no error.
5. **`--append` update in place** — existing `## ID:` section replaced; `entriesAppended=0`; file sha256 changes; no duplicate section.
6. **`--append` new entry** — unseen ID appended at EOF; `entriesAppended=1`.
7. **TEMPLATE_NOT_FOUND** — absent template; exit 1; stderr matches.
8. **INVALID_JSON** — malformed `--data`; exit 2.
9. **UNRESOLVED_PLACEHOLDER** — strict miss; exit 3 with exact token.
10. **WRITE_FAILED** — output directory read-only; exit 4; tmp cleaned up.

Goldens under `src/test/resources/golden/internal/ops/x-internal-write-report/` lock the SKILL.md rendering. Coverage requirement: ≥ 95% line / ≥ 90% branch across the invoking Bash codepaths.
