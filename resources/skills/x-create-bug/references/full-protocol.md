# x-create-bug — Full Protocol

Detailed reference for `x-create-bug`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 1 — Parse and Validate Arguments

```bash
# Parse positional description
DESCRIPTION="${1}"
if [[ -z "$DESCRIPTION" || ${#DESCRIPTION} -lt 8 || ${#DESCRIPTION} -gt 200 ]]; then
  echo "ABORT: Description must be 8–200 characters"
  exit 1
fi

# Parse flags with defaults
SEVERITY="${SEVERITY:-MEDIUM}"
SCOPE="${SCOPE:-STANDARD}"
OUTPUT="${OUTPUT:-docs/bugs/}"
BUG_ID="${BUG_ID:-}"

# Validate severity and scope
case "$SEVERITY" in
  LOW|MEDIUM|HIGH|CRITICAL) ;;
  *) echo "ABORT: Severity must be LOW|MEDIUM|HIGH|CRITICAL"; exit 1 ;;
esac

case "$SCOPE" in
  SIMPLE|STANDARD|COMPLEX) ;;
  *) echo "ABORT: Scope must be SIMPLE|STANDARD|COMPLEX"; exit 1 ;;
esac

# Verify output directory exists
[[ -d "$OUTPUT" ]] || { echo "ABORT: Output directory not found: $OUTPUT"; exit 1; }
```

## Step 2 — Generate Slug

Slug generation uses NFKD normalization, non-alphanumeric stripping, lowercase conversion, multi-hyphen collapse, and truncation:

```bash
# NFKD normalize, extract alphanumeric + hyphens, lowercase, collapse hyphens
SLUG=$(echo "$DESCRIPTION" \
  | iconv -f UTF-8 -t ASCII//TRANSLIT \
  | sed 's/[^a-zA-Z0-9 -]//g' \
  | tr ' ' '-' \
  | tr '[:upper:]' '[:lower:]' \
  | sed 's/-\+/-/g' \
  | sed 's/^-\|-$//g' \
  | cut -c 1-40)

if [[ -z "$SLUG" ]]; then
  echo "ABORT: Slug generation failed (no alphanumeric content)"
  exit 1
fi
```

## Step 3 — Assign Bug ID

Auto-generate next ID by scanning existing bug files:

```bash
if [[ -z "$BUG_ID" ]]; then
  # Find highest existing ID
  MAX_ID=$(ls "$OUTPUT"/bug-*.md 2>/dev/null \
    | sed 's/.*bug-\([0-9]*\)-.*/\1/' \
    | sort -n | tail -1)
  NEXT_ID=$((${MAX_ID:-0} + 1))
  BUG_ID=$(printf "bug-%04d" "$NEXT_ID")
else
  # Validate user-provided ID format
  if [[ ! "$BUG_ID" =~ ^bug-[0-9]+$ ]]; then
    echo "ABORT: Bug ID must match pattern bug-NNNN"
    exit 1
  fi
fi
```

## Step 4 — Load Template

Load the bug template from the standard location:

```bash
TEMPLATE_PATH=".claude/templates/_TEMPLATE-BUG.md"
if [[ ! -f "$TEMPLATE_PATH" ]]; then
  echo "ABORT: Template not found: $TEMPLATE_PATH"
  exit 1
fi

TEMPLATE=$(cat "$TEMPLATE_PATH")
```

## Step 5 — Instantiate Template

Substitute template variables with bug metadata:

```bash
# Substitution map
BUG_FILE="${OUTPUT}/${BUG_ID}-${SLUG}.md"
BUG_BRANCH="bug/${BUG_ID}-${SLUG}"
CREATED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Apply substitutions
INSTANTIATED=$(echo "$TEMPLATE" \
  | sed "s|{{BUG_DESCRIPTION}}|${DESCRIPTION}|g" \
  | sed "s|{{BUG_ID}}|${BUG_ID#bug-}|g" \
  | sed "s|{{SLUG}}|${SLUG}|g" \
  | sed "s|{{SEVERITY}}|${SEVERITY}|g" \
  | sed "s|{{SCOPE}}|${SCOPE}|g" \
  | sed "s|{{PERSONA}}|Developer|g" \
  | sed "s|{{VERSION_OR_SHA}}|HEAD|g" \
  | sed "s|{{OS_RUNTIME}}|Linux / JVM 21|g" \
  | sed "s|{{JAVA_VERSION}}|21|g" \
  | sed "s|{{DATE}}|${CREATED_AT}|g")
```

## Step 6 — Write Bug File

Create the bug file with instantiated content:

```bash
echo "$INSTANTIATED" > "$BUG_FILE"
if [[ $? -ne 0 ]]; then
  echo "ABORT: Failed to write bug file: $BUG_FILE"
  exit 1
fi
```

## Step 7 — Initialize Status

Ensure the Status field is set to Pendente:

```bash
# Verify Status field is present
if ! grep -q "^\*\*Status:\*\* Pendente$" "$BUG_FILE"; then
  # If not present, add after the header
  sed -i.bak '/^# Bug:/a\
**Status:** Pendente' "$BUG_FILE"
  rm -f "$BUG_FILE.bak"
fi
```

## Step 8 — Emit Response

Return the created bug metadata:

```bash
jq -n \
  --arg bugId "$BUG_ID" \
  --arg slug "$SLUG" \
  --arg bugFile "$BUG_FILE" \
  --arg severity "$SEVERITY" \
  --arg scope "$SCOPE" \
  --arg status "Pendente" \
  --arg created "$CREATED_AT" \
  '{bugId: $bugId, slug: $slug, bugFile: $bugFile,
    severity: $severity, scope: $scope, status: $status,
    created: $created}'
```

## Examples

### Create Simple Bug

```bash
/x-create-bug "Login endpoint returns 500 on invalid credentials"
```

Output:
```json
{
  "bugId": "bug-0001",
  "slug": "login-endpoint-returns-500-on-invalid-credentials",
  "bugFile": "docs/bugs/bug-0001-login-endpoint-returns-500-on-invalid-credentials.md",
  "severity": "MEDIUM",
  "scope": "STANDARD",
  "status": "Pendente",
  "created": "2026-05-07T14:23:15Z"
}
```

### Create High-Severity Security Bug

```bash
/x-create-bug "Session tokens visible in browser console when logging in" --severity CRITICAL --scope COMPLEX
```

### Create in Custom Directory

```bash
/x-create-bug "Cache invalidation broken after deploy" --output ./archive --id bug-2026001
```

## Performance Contract

Target: < 500 ms for slug generation + file creation. No network I/O; all operations are local file and string processing.

## Testing — Acceptance Scenarios

1. **Happy path** — description with spaces creates correct slug.
2. **Boundary** — 8-char and 200-char descriptions accepted.
3. **Edge case** — description with unicode/special chars generates valid slug.
4. **Error** — missing template aborts with clear message.
5. **Idempotency** — re-running with `--id` and different description updates the file correctly.

## Telemetry

When invoked, emits:

- `tool.call` event with `tool=x-create-bug`, `args={description, severity, scope}`, `duration_ms`.
- Session timestamps (start/end) for bug creation workflow.
