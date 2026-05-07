---
engineer: Security
story: story-0079-0001
score: 28/30
status: Partial
date: 2026-05-07
---

## Security Review — story-0079-0001: Formalizar frontmatter de agentes e JSON Schema

ENGINEER: Security
STORY: story-0079-0001
SCORE: 28/30
STATUS: Partial

---

### PASSED

- [SEC-01] audit-agent-frontmatter.sh uses `set -euo pipefail` — prevents partial execution on errors (2/2)
- [SEC-02] No user-supplied input processed by the script — all paths are derived from SCRIPT_DIR/PROJECT_ROOT (2/2)
- [SEC-03] No eval or dynamic command construction — grep/awk patterns are hardcoded (2/2)
- [SEC-04] File paths constructed via cd+pwd canonicalization, not string concatenation (2/2)
- [SEC-05] `--self-check` mode validates prerequisites without executing main logic (2/2)
- [SEC-06] Script errors go to stderr (>&2), pass/fail stats to stdout — proper separation (2/2)
- [SEC-07] No secrets, tokens, or credentials in any agent frontmatter (2/2)
- [SEC-08] requires-capabilities field uses [] (empty array) = always active; no capability bypass possible (2/2)
- [SEC-09] JSON schema uses additionalProperties: true — does not reject unknown fields, preventing schema-based injection vectors (2/2)
- [SEC-10] model enum restricted to [Opus, Sonnet, Haiku] — MODEL_ADAPTIVE_FORBIDDEN enforced (2/2)
- [SEC-11] Script uses mapfile (read array) to handle filenames with spaces safely (2/2)
- [SEC-12] No temp files created — no cleanup needed, no race condition on /tmp (2/2)
- [SEC-13] Agent files do not expose credentials, API keys, or internal infrastructure details (2/2)
- [SEC-14] SCHEMA_FILE path validated before use — exit 3 if missing (2/2)

### PARTIAL

- [SEC-15] `extract_frontmatter_field` grep pattern `^${field}:` could theoretically match YAML keys that share a prefix (1/2) — in practice agent frontmatter fields are distinct (name, description, tools, model, requires-capabilities), no ambiguity exists; pattern is non-vulnerable in the current agent corpus

### FAILED

None.

### Verdict: APPROVE (Partial — non-blocking)

Security posture is strong. The single partial finding is a theoretical regex edge case with no practical impact on the current agent corpus.
