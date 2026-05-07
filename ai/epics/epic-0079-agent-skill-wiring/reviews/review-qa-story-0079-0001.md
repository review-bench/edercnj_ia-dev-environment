---
engineer: QA
story: story-0079-0001
score: 32/36
status: Partial
date: 2026-05-07
---

## QA Review — story-0079-0001: Formalizar frontmatter de agentes e JSON Schema

ENGINEER: QA
STORY: story-0079-0001
SCORE: 32/36
STATUS: Partial

---

### PASSED

- [QA-01] Frontmatter blocks are well-formed YAML between `---` delimiters on all 18 agent files (2/2)
- [QA-02] All 5 required fields present: name, description, tools, model, requires-capabilities (2/2)
- [QA-03] `model` enum values are correct: Opus for architect, Sonnet for all others; "Adaptive" not present (2/2)
- [QA-04] `requires-capabilities: []` used consistently for universal agents (2/2)
- [QA-05] JSON schema `additionalProperties: true` — extra fields tolerated per Rule 28 invariant (2/2)
- [QA-06] audit-agent-frontmatter.sh self-check passes (exit 0), `--self-check` flag implemented (2/2)
- [QA-07] Script exit codes follow Rule 26 taxonomy: 0=OK, 1=FRONTMATTER_VIOLATION, 2=OPERATIONAL_ERROR, 3=SCHEMA_CORRUPT (2/2)
- [QA-08] MODEL_ADAPTIVE_FORBIDDEN check implemented in check_agent() (2/2)
- [QA-09] Script uses `set -euo pipefail` for strict bash error handling (2/2)
- [QA-10] Script handles missing agents directories gracefully (find -name "*.md" 2>/dev/null) (2/2)
- [QA-11] `18/18 agentes validados` — all agents pass validation (2/2)
- [QA-12] docs/audit-gates-catalog.md entry added for audit-agent-frontmatter.sh (2/2)
- [QA-13] java-developer.md correctly gains full frontmatter (was missing entirely) (2/2)
- [QA-14] architect.md uses `tools: [Read, Write, Edit, Bash, Grep, WebSearch, WebFetch, Agent]` — full toolset for deep planner (2/2)
- [QA-15] governance/schemas/agent-frontmatter-1.0.json is valid JSON (2/2)
- [QA-16] `description` field has `minLength: 10, maxLength: 255` constraints (2/2)

### PARTIAL

- [QA-17] No automated test validates the JSON schema against agent files in Maven CI (1/2) — `scripts/audit-agent-frontmatter.sh` exists as Camada 2 but no Java smoke test (`Epic0079AgentFrontmatterSmokeIT`) was added; story scope is Camada 2 only, Camada 3 deferred to story-0079-0005 — acceptable per DoD
- [QA-18] `extract_frontmatter_field` uses awk+grep+sed chain that may not handle multi-line YAML values (1/2) — single-line field values are the only format used in practice, but robustness could be improved

### FAILED

None.

### Verdict: APPROVE (Partial — within story DoD scope)

Story-0079-0001 scope was Camada 2 (audit-agent-frontmatter.sh). Java smoke test (Camada 3) is deferred to story-0079-0005. The partial findings do not block merge.
