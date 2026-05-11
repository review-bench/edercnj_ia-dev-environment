# x-watch-pr-ci — Full Protocol

Detailed reference for `x-watch-pr-ci`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Step 1 — Argument Validation

```bash
# Validate --pr-number
if [[ -z "$PR_NUMBER" || "$PR_NUMBER" -le 0 ]]; then
  echo "ERROR: --pr-number is required and must be > 0" >&2
  exit 70
fi

# Validate --timeout-seconds (60..7200)
if [[ "$TIMEOUT_SECONDS" -lt 60 || "$TIMEOUT_SECONDS" -gt 7200 ]]; then
  echo "ERROR: timeout-seconds must be in range 60..7200" >&2
  exit 1
fi

# Validate --poll-interval-seconds (15..300)
if [[ "$POLL_INTERVAL" -lt 15 || "$POLL_INTERVAL" -gt 300 ]]; then
  echo "ERROR: poll-interval-seconds must be in range 15..300" >&2
  exit 1
fi

# Validate --copilot-review-timeout (60..timeout)
if [[ "$COPILOT_TIMEOUT" -lt 60 || "$COPILOT_TIMEOUT" -gt "$TIMEOUT_SECONDS" ]]; then
  echo "ERROR: copilot-review-timeout must be in range 60..${TIMEOUT_SECONDS}" >&2
  exit 1
fi
```

## Step 2 — Resume from State File

```bash
STATE_FILE="${STATE_FILE:-.claude/state/pr-watch-${PR_NUMBER}.json}"
ELAPSED_OFFSET=0

if [[ -z "$NO_STATE_FILE" && -f "$STATE_FILE" ]]; then
  # Load previous elapsed from state-file (resume after session restart)
  PREV_STARTED=$(jq -r '.startedAt' "$STATE_FILE" 2>/dev/null)
  if [[ "$PREV_STARTED" != "null" && -n "$PREV_STARTED" ]]; then
    ELAPSED_OFFSET=$(( $(date +%s) - $(date -d "$PREV_STARTED" +%s 2>/dev/null || date -j -f "%Y-%m-%dT%H:%M:%SZ" "$PREV_STARTED" +%s) ))
    echo "[resume] Loaded state from $STATE_FILE; offset=${ELAPSED_OFFSET}s" >&2
  fi
fi

mkdir -p "$(dirname "$STATE_FILE")"
START_EPOCH=$(date +%s)
STARTED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
```

## Step 3 — Polling Loop

```bash
COPILOT_LOGIN="copilot-pull-request-reviewer[bot]"
RATE_LIMIT_RETRIES=0
MAX_RATE_LIMIT_RETRIES=3

while true; do
  ELAPSED=$(( $(date +%s) - START_EPOCH + ELAPSED_OFFSET ))

  # ── Fetch PR state ──────────────────────────────────────────────────────
  PR_JSON=$(gh pr view "$PR_NUMBER" \
    --json state,mergedAt,statusCheckRollup 2>&1)
  GH_EXIT=$?

  if [[ $GH_EXIT -ne 0 ]]; then
    if echo "$PR_JSON" | grep -qi "Could not resolve"; then
      emit_json "PR_NOT_FOUND" "$ELAPSED" "[]" '{"present":false}' && exit 70
    fi
    if echo "$PR_JSON" | grep -qi "rate limit\|secondary rate"; then
      RATE_LIMIT_RETRIES=$((RATE_LIMIT_RETRIES + 1))
      if [[ $RATE_LIMIT_RETRIES -gt $MAX_RATE_LIMIT_RETRIES ]]; then
        emit_json "TIMEOUT" "$ELAPSED" "[]" '{"present":false}' && exit 30
      fi
      BACKOFF=$(( 30 * (2 ** (RATE_LIMIT_RETRIES - 1)) ))  # 30s, 60s, 120s
      echo "[rate-limit] retry ${RATE_LIMIT_RETRIES}/${MAX_RATE_LIMIT_RETRIES}, sleeping ${BACKOFF}s" >&2
      sleep "$BACKOFF"
      continue
    fi
    echo "[warn] gh pr view failed (exit $GH_EXIT): $PR_JSON" >&2
  fi

  RATE_LIMIT_RETRIES=0  # reset on success

  PR_STATE=$(echo "$PR_JSON" | jq -r '.state // "UNKNOWN"' 2>/dev/null)
  MERGED_AT=$(echo "$PR_JSON" | jq -r '.mergedAt // "null"' 2>/dev/null)
  CHECKS_JSON=$(echo "$PR_JSON" | jq -c '[.statusCheckRollup[]? | {name: .name, conclusion: (.conclusion // .status // "pending")}]' 2>/dev/null || echo "[]")

  # ── Classify early-exit conditions ─────────────────────────────────────
  if [[ "$PR_STATE" == "MERGED" || ("$MERGED_AT" != "null" && -n "$MERGED_AT") ]]; then
    emit_json "PR_ALREADY_MERGED" "$ELAPSED" "$CHECKS_JSON" '{"present":false}' && exit 40
  fi

  if [[ "$PR_STATE" == "CLOSED" ]]; then
    emit_json "PR_CLOSED" "$ELAPSED" "$CHECKS_JSON" '{"present":false}' && exit 60
  fi

  # ── Fetch Copilot review ────────────────────────────────────────────────
  OWNER_REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner' 2>/dev/null)
  REVIEWS_JSON=$(gh api "repos/${OWNER_REPO}/pulls/${PR_NUMBER}/reviews" 2>/dev/null || echo "[]")
  COPILOT_REVIEW=$(echo "$REVIEWS_JSON" | jq --arg login "$COPILOT_LOGIN" \
    'map(select(.user.login == $login)) | if length > 0 then {present: true, reviewId: .[0].id} else {present: false} end' 2>/dev/null \
    || echo '{"present":false}')
  COPILOT_PRESENT=$(echo "$COPILOT_REVIEW" | jq -r '.present' 2>/dev/null || echo "false")

  # ── Check empty CI (NO_CI_CONFIGURED) ──────────────────────────────────
  CHECK_COUNT=$(echo "$CHECKS_JSON" | jq 'length' 2>/dev/null || echo "0")
  if [[ "$CHECK_COUNT" -eq 0 ]]; then
    emit_json "NO_CI_CONFIGURED" "$ELAPSED" "[]" "$COPILOT_REVIEW" && exit 50
  fi

  # ── Detect failing checks ───────────────────────────────────────────────
  FAILING=$(echo "$CHECKS_JSON" | jq '[.[] | select(.conclusion | IN("failure","timed_out","cancelled","action_required"))] | length' 2>/dev/null || echo "0")
  if [[ "$FAILING" -gt 0 ]]; then
    emit_json "CI_FAILED" "$ELAPSED" "$CHECKS_JSON" "$COPILOT_REVIEW" && exit 20
  fi

  # ── Check all-green ─────────────────────────────────────────────────────
  NON_GREEN=$(echo "$CHECKS_JSON" | jq '[.[] | select(.conclusion | IN("success","neutral","skipped") | not)] | length' 2>/dev/null || echo "1")
  ALL_GREEN=$(( NON_GREEN == 0 ))

  if [[ "$ALL_GREEN" -eq 1 ]]; then
    if [[ "$REQUIRE_COPILOT_REVIEW" == "false" || "$COPILOT_PRESENT" == "true" ]]; then
      emit_json "SUCCESS" "$ELAPSED" "$CHECKS_JSON" "$COPILOT_REVIEW" && exit 0
    fi
    COPILOT_ELAPSED=$(( $(date +%s) - START_EPOCH ))
    if [[ "$COPILOT_ELAPSED" -ge "$COPILOT_REVIEW_TIMEOUT" ]]; then
      emit_json "CI_PENDING_PROCEED" "$ELAPSED" "$CHECKS_JSON" "$COPILOT_REVIEW" && exit 10
    fi
  fi

  # ── Global timeout check ────────────────────────────────────────────────
  if [[ "$ELAPSED" -ge "$TIMEOUT_SECONDS" ]]; then
    emit_json "TIMEOUT" "$ELAPSED" "$CHECKS_JSON" "$COPILOT_REVIEW" && exit 30
  fi

  # ── Write state-file (atomic: .tmp + rename) ────────────────────────────
  if [[ -z "$NO_STATE_FILE" ]]; then
    POLL_COUNT=$(( ${POLL_COUNT:-0} + 1 ))
    LAST_POLL_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    jq -n \
      --argjson prNumber "$PR_NUMBER" \
      --arg startedAt "$STARTED_AT" \
      --arg lastPollAt "$LAST_POLL_AT" \
      --argjson pollCount "$POLL_COUNT" \
      --argjson checksSnapshot "$CHECKS_JSON" \
      --argjson copilotReview "$COPILOT_REVIEW" \
      --arg schemaVersion "1.0" \
      '{
        prNumber: $prNumber,
        startedAt: $startedAt,
        lastPollAt: $lastPollAt,
        pollCount: $pollCount,
        checksSnapshot: $checksSnapshot,
        copilotReview: $copilotReview,
        schemaVersion: $schemaVersion
      }' > "${STATE_FILE}.tmp" 2>/dev/null \
    && mv "${STATE_FILE}.tmp" "$STATE_FILE"
  fi

  echo "[poll ${POLL_COUNT}] elapsed=${ELAPSED}s allGreen=${ALL_GREEN} copilot=${COPILOT_PRESENT}" >&2
  sleep "$POLL_INTERVAL"
done
```

## Helper: emit_json

```bash
emit_json() {
  local status="$1" elapsed="$2" checks="$3" copilot="$4"
  jq -n \
    --arg status "$status" \
    --argjson prNumber "$PR_NUMBER" \
    --argjson checks "$checks" \
    --argjson copilotReview "$copilot" \
    --argjson elapsedSeconds "$elapsed" \
    '{
      status: $status,
      prNumber: $prNumber,
      checks: $checks,
      copilotReview: $copilotReview,
      elapsedSeconds: $elapsedSeconds
    }'
}
```

## State File Schema (RULE-045-03)

Location: `.claude/state/pr-watch-<N>.json`

```json
{
  "prNumber": 42,
  "startedAt": "2026-04-20T10:00:00Z",
  "lastPollAt": "2026-04-20T10:01:05Z",
  "pollCount": 2,
  "checksSnapshot": [
    {"name": "build", "conclusion": "success"},
    {"name": "test", "conclusion": "success"}
  ],
  "copilotReview": {"present": true, "reviewId": 12345678},
  "schemaVersion": "1.0"
}
```

Write is atomic: write to `<path>.tmp`, then rename. On corrupted state: log warning and restart from zero.

## Stdout Contract

Progress logs go to **stderr**. The final JSON summary is the **last line** on **stdout**:

```json
{
  "status": "SUCCESS",
  "prNumber": 42,
  "checks": [
    {"name": "build", "conclusion": "success"},
    {"name": "test",  "conclusion": "success"}
  ],
  "copilotReview": {"present": true, "reviewId": 12345678},
  "elapsedSeconds": 87
}
```

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| Rate limit | Exponential backoff 3× (30s, 60s, 120s); after 3rd failure exit 30 (TIMEOUT) |
| Copilot not configured in repo | Detected after 1st poll (absent from requested_reviewers); downgrade to CI_PENDING_PROCEED |
| State file corrupted | Log warning, reset poll state, continue from elapsed=0 |
| PR closed during polling | Exit 60 immediately |
| PR merged during polling | Exit 40 immediately |

## Rule Compliance

- **Rule 13**: orchestrators invoke via `Skill(skill: "x-watch-pr-ci", args: "...")` — no bare-slash in delegation
- **Rule 14**: skill does NOT create worktrees (sequential, runs in caller's working tree)
- **RULE-045-03**: state-file atomic write + resume
- **RULE-045-04**: Copilot identified by exact login `copilot-pull-request-reviewer[bot]`
- **RULE-045-05**: exit codes 0/10/20/30/40/50/60/70 are stable public contract
- **RULE-045-06**: orchestrators use Rule 13 Pattern 1 INLINE-SKILL
