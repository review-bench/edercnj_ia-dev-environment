#!/usr/bin/env bash
# audit-bug-classification.sh
# CI gate: validates bug files in ai/bugs/ conform to governance.bug-lifecycle rules.
#
# Exit codes:
#   0  — all bugs pass validation
#   1  — one or more bugs failed validation (details on stderr)
#   2  — usage error (bad arguments)
#
# Usage:
#   audit-bug-classification.sh [--bugs-dir <path>] [--json] [--strict]
#
# Flags:
#   --bugs-dir <path>  Directory to scan (default: ai/bugs)
#   --json             Emit JSON report on stdout instead of human-readable
#   --strict           Exit 1 on any WARNING (not just ERROR)

set -euo pipefail

BUGS_DIR="ai/bugs"
JSON_MODE=false
STRICT_MODE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bugs-dir)
      BUGS_DIR="$2"; shift 2 ;;
    --json)
      JSON_MODE=true; shift ;;
    --strict)
      STRICT_MODE=true; shift ;;
    *)
      echo "Usage: audit-bug-classification.sh [--bugs-dir <path>] [--json] [--strict]" >&2
      exit 2 ;;
  esac
done

if [[ ! -d "$BUGS_DIR" ]]; then
  echo "INFO: No bugs directory found at ${BUGS_DIR} — nothing to audit" >&2
  if $JSON_MODE; then
    echo '{"status":"ok","bugsScanned":0,"violations":[]}'
  fi
  exit 0
fi

VIOLATIONS=()
BUGS_SCANNED=0

scan_bug() {
  local bug_file="$1"
  local bug_id
  bug_id=$(basename "$(dirname "$bug_file")")
  BUGS_SCANNED=$((BUGS_SCANNED + 1))

  # Validate bug-id format
  if [[ ! "$bug_id" =~ ^bug-[0-9]{6}$ ]]; then
    VIOLATIONS+=("ERROR|${bug_id}|BUG_ID_FORMAT|Bug directory name must match bug-NNNNNN")
    return
  fi

  if [[ ! -f "$bug_file" ]]; then
    VIOLATIONS+=("ERROR|${bug_id}|BUG_FILE_MISSING|bug.md not found in ${bug_id}/")
    return
  fi

  local content
  content=$(cat "$bug_file")

  # Check requires-capabilities frontmatter
  if ! echo "$content" | grep -q "requires-capabilities:"; then
    VIOLATIONS+=("ERROR|${bug_id}|MISSING_CAPABILITY_DECL|bug.md must declare requires-capabilities")
  elif ! echo "$content" | grep -q "governance.bug-lifecycle"; then
    VIOLATIONS+=("ERROR|${bug_id}|WRONG_CAPABILITY|bug.md must require governance.bug-lifecycle")
  fi

  # Check all 9 RA9 sections present
  local missing_sections=()
  echo "$content" | grep -q "## 1. Visão"           || missing_sections+=("1-Visao")
  echo "$content" | grep -q "## 2. Persona"          || missing_sections+=("2-Persona")
  echo "$content" | grep -q "## 3. Entrega"          || missing_sections+=("3-Entrega")
  echo "$content" | grep -q "## 4. Critérios"        || missing_sections+=("4-Criterios")
  echo "$content" | grep -q "## 5. Reproduction"     || missing_sections+=("5-Recipe")
  echo "$content" | grep -q "## 6. Root-Cause"       || missing_sections+=("6-RootCause")
  echo "$content" | grep -q "## 7. Regression"       || missing_sections+=("7-RegressionTest")
  echo "$content" | grep -q "## 8. Dependências"     || missing_sections+=("8-Dependencies")
  echo "$content" | grep -q "## 9. Histórico"        || missing_sections+=("9-History")

  for section in "${missing_sections[@]}"; do
    VIOLATIONS+=("ERROR|${bug_id}|MISSING_SECTION_${section}|Required section not found")
  done

  # Check severity is valid
  if echo "$content" | grep -q "^\*\*Severity:\*\*"; then
    local severity
    severity=$(echo "$content" | grep "^\*\*Severity:\*\*" | sed 's/.*\*\*Severity:\*\* *//' | head -1)
    if [[ ! "$severity" =~ ^(LOW|MEDIUM|HIGH|CRITICAL)$ ]]; then
      VIOLATIONS+=("ERROR|${bug_id}|INVALID_SEVERITY|Severity '${severity}' must be LOW/MEDIUM/HIGH/CRITICAL")
    fi
  fi

  # Check status is valid
  if echo "$content" | grep -q "^\*\*Status:\*\*"; then
    local status
    status=$(echo "$content" | grep "^\*\*Status:\*\*" | sed 's/.*\*\*Status:\*\* *//' | head -1)
    local valid_statuses="Pendente Refinada Em\ Investigação Em\ Correção Concluída Bloqueada Descartada Falha"
    local status_valid=false
    for vs in "Pendente" "Refinada" "Em Investigação" "Em Correção" "Concluída" "Bloqueada" "Descartada" "Falha"; do
      [[ "$status" == "$vs" ]] && status_valid=true && break
    done
    if ! $status_valid; then
      VIOLATIONS+=("ERROR|${bug_id}|INVALID_STATUS|Status '${status}' is not a valid lifecycle status")
    fi
  fi

  # Check Refinement Verdict block
  if ! echo "$content" | grep -q "## Refinement Verdict"; then
    VIOLATIONS+=("WARNING|${bug_id}|MISSING_REFINEMENT_VERDICT|bug.md should have Refinement Verdict block")
  fi
}

# Scan all bug.md files
while IFS= read -r -d '' bug_file; do
  scan_bug "$bug_file"
done < <(find "$BUGS_DIR" -name "bug.md" -print0 2>/dev/null)

# Determine exit code
EXIT_CODE=0
ERROR_COUNT=0
WARN_COUNT=0

for v in "${VIOLATIONS[@]:-}"; do
  level="${v%%|*}"
  if [[ "$level" == "ERROR" ]]; then
    ERROR_COUNT=$((ERROR_COUNT + 1))
  elif [[ "$level" == "WARNING" ]]; then
    WARN_COUNT=$((WARN_COUNT + 1))
  fi
done

[[ $ERROR_COUNT -gt 0 ]] && EXIT_CODE=1
$STRICT_MODE && [[ $WARN_COUNT -gt 0 ]] && EXIT_CODE=1

# Output
if $JSON_MODE; then
  violations_json="["
  first=true
  for v in "${VIOLATIONS[@]:-}"; do
    IFS='|' read -r level bug_id code message <<< "$v"
    $first || violations_json+=","
    violations_json+="{\"level\":\"${level}\",\"bugId\":\"${bug_id}\",\"code\":\"${code}\",\"message\":\"${message}\"}"
    first=false
  done
  violations_json+="]"

  overall="ok"
  [[ $EXIT_CODE -ne 0 ]] && overall="fail"

  echo "{\"status\":\"${overall}\",\"bugsScanned\":${BUGS_SCANNED},\"errorCount\":${ERROR_COUNT},\"warnCount\":${WARN_COUNT},\"violations\":${violations_json}}"
else
  if [[ ${#VIOLATIONS[@]:-} -eq 0 ]]; then
    echo "audit-bug-classification: OK — ${BUGS_SCANNED} bug(s) scanned, 0 violations"
  else
    echo "audit-bug-classification: FAIL — ${BUGS_SCANNED} bug(s) scanned, ${ERROR_COUNT} error(s), ${WARN_COUNT} warning(s)" >&2
    for v in "${VIOLATIONS[@]:-}"; do
      IFS='|' read -r level bug_id code message <<< "$v"
      echo "  [${level}] ${bug_id}: ${code} — ${message}" >&2
    done
  fi
fi

exit $EXIT_CODE
