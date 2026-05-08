---
generated-by: x-story-implement-security@unknown
generated-at: 2026-04-27T18:00:00Z
story-id: story-0059-0008
---

# Security Assessment — story-0059-0008: Telemetria como Prova-de-Vida do Orquestrador

## Threat Model Summary

**STRIDE analysis scope:** Bash audit script extension (`check_telemetry`) + Stop hook (`stage-telemetry.sh`) + `events.ndjson` as committed evidence artifact.

**Overall risk level:** LOW — changes are confined to Bash scripts operating on local file system and git CLI. No network calls, no credentials, no user-supplied input beyond git log output.

## Identified Threats

### T1: Telemetry Backfill (NDJSON injection)

**Category:** Tampering
**Severity:** MEDIUM
**Description:** An actor could manually inject fake `phase.start x-story-implement` events into `events.ndjson` before staging it, creating the appearance of a valid orchestrator run when the orchestrator was never invoked.

**Mitigation:**
- story-0059-0002 introduces anti-backfill detection for planning artifacts. A similar mechanism applies here: the commit timestamp of `events.ndjson` changes is compared to the story merge timestamp.
- The `stage-telemetry.sh` Stop hook stages the file at the end of each LLM turn — manual injection would require a separate commit with a timestamp after the story run began, which is visible in `git log`.
- Full tamper-evidence is addressed by the cross-story audit chain (story-0059-0002's `check_anti_backfill` applies to all artifacts including `events.ndjson`).

**Status:** Partially mitigated (relies on story-0059-0002 anti-backfill for full coverage)

### T2: Story ID Injection via Commit Messages

**Category:** Injection
**Severity:** LOW
**Description:** A malicious actor could craft a commit message containing `story-9999-9999` to cause the audit to check for telemetry for a non-existent story, potentially causing a false negative or audit confusion.

**Mitigation:**
- The pattern `grep -oP "story-\d{4}-\d{4}"` matches only exactly 4 digits on each side — no shell metacharacters can pass through.
- The story ID is passed to `check_telemetry()` as a positional argument and used only in grep patterns (not in eval or command substitution).
- A non-existent story with no `events.ndjson` file returns `EIE_TELEMETRY_MISSING` (exit 1) — the worst case is a false positive CI failure, not a bypass.

**Status:** Mitigated

### T3: Grep Pattern False Positives / False Negatives

**Category:** Tampering
**Severity:** LOW
**Description:** A `phase.start` event for a different skill that happens to include `x-story-implement` as a substring could match the grep pattern.

**Mitigation:**
- The grep pattern requires `"skill":"x-story-implement"` (exact JSON field match with quotes). Skills named `x-story-implement-extended` would require the closing quote, preventing substring matches.
- The `storyId` field is also matched, ensuring the event is for the correct story.

**Status:** Mitigated

### T4: Stop Hook Interference with Git State

**Category:** Denial of Service
**Severity:** LOW
**Description:** The `stage-telemetry.sh` Stop hook calls `git add` on every LLM turn when a story is active. If the git index is locked (another process holds the lock), the hook could fail.

**Mitigation:**
- The hook exits 0 on `git add` failure (fail-open contract) — it logs a WARN to stderr but does not interrupt the LLM turn.
- Git index locks are transient (typically resolve in milliseconds). The hook will succeed on the next turn.

**Status:** Accepted (fail-open design is intentional)

### T5: NDJSON File Size / Memory Exhaustion

**Category:** Denial of Service
**Severity:** NEGLIGIBLE
**Description:** If `events.ndjson` grows very large (thousands of events), `grep` operations could be slow.

**Mitigation:**
- Telemetry events are bounded per story run (~20-50 events per story under normal conditions). The EPIC-0057 scenario had 171 events — still well within `grep` performance limits.
- No file size limit enforcement is required at this scale.

**Status:** Accepted (negligible at expected file sizes)

## Security Controls

| Control | Type | Implementation |
| :--- | :--- | :--- |
| Story ID input sanitization | Preventive | `grep -oP "story-\d{4}-\d{4}"` — digits only, no metacharacters |
| NDJSON event matching with quoted field names | Preventive | Exact JSON field match prevents substring skill name collisions |
| Fail-open on git add failure | Resilience | Hook exits 0 on any `git add` error |
| Grandfathered baseline exemption | Resilience | Pre-EPIC-0059 stories skip telemetry check |
| No eval / no command substitution with user data | Preventive | All story IDs passed as positional arguments only |

## OWASP Mapping

| OWASP Category | Relevance | Control |
| :--- | :--- | :--- |
| A03 — Injection | Low (Bash script, git log parsing) | Positional arg passing; numeric-only story IDs |
| A04 — Insecure Design | Low | Fail-open design for Stop hook (intentional) |
| A09 — Security Logging | Partial | Audit emits named exit codes to stderr; WARN for hook failures |

## Security Sign-off

**Risk Level:** LOW — The changes introduce two Bash scripts with a minimal attack surface. The most significant risk (T1 — telemetry backfill) is partially covered by the story-0059-0002 anti-backfill mechanism and is not new — any committed file could be retroactively modified. The security benefit (deterministic detection of orchestrator bypass) substantially outweighs the residual risk introduced.
