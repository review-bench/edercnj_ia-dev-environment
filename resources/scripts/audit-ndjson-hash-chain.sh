#!/usr/bin/env bash
# Layer:      2 (detectivo — CI audit)
# Rule:       Rule 26 (Audit Gate Lifecycle), Rule 24 (Execution Integrity)
# Introduced: EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0019
#
# audit-ndjson-hash-chain.sh — NDJSON Integrity Hash Chain
#
# Computes a rolling SHA-256 hash chain over events.ndjson to detect
# tampered or injected telemetry events.
#
# Hash chain algorithm:
#   hash[0] = SHA-256("" + line[0])           # genesis: empty previous hash
#   hash[N] = SHA-256(hash[N-1] + line[N])    # each subsequent event chains on prior
#
# Chain anchor stored in: <state-dir>/ndjson-chain-<epic-id>.json
#
# Exit codes (Rule 26 §Standardized Exit Codes):
#   0 — OK (chain valid / chain initialized)
#   1 — CHAIN_INTEGRITY_VIOLATED (hash mismatch detected)
#   2 — OPERATIONAL_ERROR (missing args, file not found, no SHA tool)
#
# Usage:
#   audit-ndjson-hash-chain.sh --ndjson-file <path> --state-dir <dir> --epic-id <id> --init
#   audit-ndjson-hash-chain.sh --ndjson-file <path> --state-dir <dir> --epic-id <id> --verify
#   audit-ndjson-hash-chain.sh --self-check

set -uo pipefail

SCRIPT_NAME="$(basename "${BASH_SOURCE[0]}")"

# ── SHA-256 helper (portability: Linux sha256sum vs macOS shasum) ─────────────
sha256_of() {
    local input="$1"
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s' "$input" | sha256sum | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        printf '%s' "$input" | shasum -a 256 | awk '{print $1}'
    else
        printf 'OPERATIONAL_ERROR: no sha256 tool (sha256sum or shasum) found\n' >&2
        exit 2
    fi
}

# ── Arg parse ─────────────────────────────────────────────────────────────────
NDJSON_FILE=""
STATE_DIR=""
EPIC_ID=""
MODE=""          # init | verify
SELF_CHECK=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ndjson-file) NDJSON_FILE="$2"; shift 2 ;;
        --state-dir)   STATE_DIR="$2";   shift 2 ;;
        --epic-id)     EPIC_ID="$2";     shift 2 ;;
        --init)        MODE="init";       shift ;;
        --verify)      MODE="verify";     shift ;;
        --self-check)  SELF_CHECK=true;   shift ;;
        *)
            printf 'OPERATIONAL_ERROR: unknown flag: %s\n' "$1" >&2
            exit 2
            ;;
    esac
done

# ── Self-check ─────────────────────────────────────────────────────────────────
if "$SELF_CHECK"; then
    if ! command -v sha256sum >/dev/null 2>&1 && ! command -v shasum >/dev/null 2>&1; then
        printf 'OPERATIONAL_ERROR: neither sha256sum nor shasum found on PATH\n' >&2
        exit 2
    fi
    printf 'SELF_CHECK_OK: %s prerequisites satisfied\n' "$SCRIPT_NAME" >&2
    exit 0
fi

# ── Validate required args ────────────────────────────────────────────────────
if [[ -z "$NDJSON_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: --ndjson-file is required\n' >&2
    exit 2
fi

if [[ -z "$STATE_DIR" ]]; then
    printf 'OPERATIONAL_ERROR: --state-dir is required\n' >&2
    exit 2
fi

if [[ -z "$EPIC_ID" ]]; then
    printf 'OPERATIONAL_ERROR: --epic-id is required\n' >&2
    exit 2
fi

if [[ -z "$MODE" ]]; then
    printf 'OPERATIONAL_ERROR: one of --init or --verify is required\n' >&2
    exit 2
fi

# ── Validate NDJSON file existence ───────────────────────────────────────────
if [[ ! -f "$NDJSON_FILE" ]]; then
    printf 'OPERATIONAL_ERROR: NDJSON file not found: %s\n' "$NDJSON_FILE" >&2
    exit 2
fi

# ── Chain anchor path ─────────────────────────────────────────────────────────
ANCHOR_FILE="${STATE_DIR}/ndjson-chain-${EPIC_ID}.json"

# ── Compute hash chain over NDJSON lines ─────────────────────────────────────
compute_chain() {
    local ndjson_file="$1"
    local prev_hash=""
    local line_count=0
    local last_hash=""

    while IFS= read -r line || [[ -n "$line" ]]; do
        # Skip empty lines
        [[ -z "$line" ]] && continue
        local combined="${prev_hash}${line}"
        local current_hash
        current_hash="$(sha256_of "$combined")"
        prev_hash="$current_hash"
        last_hash="$current_hash"
        line_count=$((line_count + 1))
    done < "$ndjson_file"

    printf '%s %d\n' "${last_hash}" "${line_count}"
}

# ── Init mode: build chain and store anchor ───────────────────────────────────
if [[ "$MODE" == "init" ]]; then
    mkdir -p "$STATE_DIR"

    result="$(compute_chain "$NDJSON_FILE")"
    final_hash="${result% *}"
    line_count="${result##* }"

    # Normalize: empty file has genesis hash of empty string
    if [[ -z "$final_hash" ]]; then
        final_hash="$(sha256_of "")"
    fi

    timestamp="$(date -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u +"%Y-%m-%dT%H:%M:%SZ")"

    cat > "$ANCHOR_FILE" <<EOF
{
  "epicId": "${EPIC_ID}",
  "ndjsonFile": "${NDJSON_FILE}",
  "chainHead": "${final_hash}",
  "lineCount": ${line_count},
  "timestamp": "${timestamp}",
  "algorithm": "sha256-chain"
}
EOF

    printf 'CHAIN_INIT_OK: %s lines hashed, head=%s\n' "$line_count" "$final_hash" >&2
    exit 0
fi

# ── Verify mode: recompute chain and compare anchor ───────────────────────────
if [[ "$MODE" == "verify" ]]; then
    if [[ ! -f "$ANCHOR_FILE" ]]; then
        printf 'OPERATIONAL_ERROR: anchor file not found (run --init first): %s\n' "$ANCHOR_FILE" >&2
        exit 2
    fi

    # Read stored head hash and line count from anchor
    stored_head=""
    stored_count=0
    if command -v jq >/dev/null 2>&1; then
        stored_head="$(jq -r '.chainHead' "$ANCHOR_FILE" 2>/dev/null || true)"
        stored_count="$(jq -r '.lineCount' "$ANCHOR_FILE" 2>/dev/null || echo 0)"
    else
        # Fallback: grep-based extraction
        stored_head="$(grep -o '"chainHead": *"[^"]*"' "$ANCHOR_FILE" | sed 's/.*"chainHead": *"\([^"]*\)".*/\1/' || true)"
        stored_count="$(grep -o '"lineCount": *[0-9]*' "$ANCHOR_FILE" | grep -o '[0-9]*$' || echo 0)"
    fi

    if [[ -z "$stored_head" ]]; then
        printf 'OPERATIONAL_ERROR: could not read chainHead from anchor: %s\n' "$ANCHOR_FILE" >&2
        exit 2
    fi

    # Recompute
    result="$(compute_chain "$NDJSON_FILE")"
    current_hash="${result% *}"
    current_count="${result##* }"

    # Empty file: both should resolve to genesis hash
    if [[ -z "$current_hash" ]]; then
        current_hash="$(sha256_of "")"
        current_count=0
    fi

    if [[ "$current_hash" != "$stored_head" ]] || [[ "$current_count" != "$stored_count" ]]; then
        printf 'CHAIN_INTEGRITY_VIOLATED: stored head=%s (lines=%s), recomputed head=%s (lines=%s)\n' \
            "$stored_head" "$stored_count" "$current_hash" "$current_count" >&2
        exit 1
    fi

    printf 'CHAIN_VERIFY_OK: %s lines verified, head=%s\n' "$current_count" "$current_hash" >&2
    exit 0
fi

# Should not reach here (MODE already validated above)
printf 'OPERATIONAL_ERROR: unexpected mode: %s\n' "$MODE" >&2
exit 2
