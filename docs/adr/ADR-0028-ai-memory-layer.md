# ADR-0028 — AI Memory Layer

**Status:** Accepted
**Date:** 2026-05-03
**Epic:** EPIC-0075 (AI Memory Layer)
**Rule:** Rule 33 (AI Memory Production)

---

## Context

Completed epics leave strategic decisions, alternatives, and patterns scattered across ADRs, stories, PR descriptions, and conversations. Three operational pain points surfaced:

1. **Recurrent questions.** "Why did we keep Spring 3.x and not migrate?" → ~15-30 min per reconstruction.
2. **LLM context amnesia.** A new Claude session suggests approaches that were already evaluated and rejected in a previous epic, triggering re-litigation.
3. **Onboarding cost.** New contributors read code + rules + system.md, but the "why" behind key choices requires cross-reading dozens of epic documents.

The pattern we needed: a **compact, indexable, structured summary** per completed epic — optimized for retrieval (human and LLM via grep+frontmatter index), not for narrative reading.

---

## Decision

Introduce an AI Memory Layer with the following components:

1. **`ai/memory/` directory** — flat set of `epic-XXXX-summary.md` files + `_index.yaml` manifest.
2. **`_TEMPLATE-EPIC-MEMORY-SUMMARY.md`** — frontmatter v3.0 schema (tags, capabilities-affected, rules-affected, patterns-introduced, antipatterns-rejected) + structured body sections (why, hypothesis, decisions, alternatives, patterns, anti-patterns, links). Cap: ≤ 200 lines.
3. **`x-internal-epic-summary`** (model: `haiku`, deterministic) — reads epic v2 documents + story files + ADRs + completion reports; extracts structured sections; writes summary + updates `_index.yaml`. Invoked **MANDATORY** at Phase 5 of `x-epic-implement` when `governance.ai-memory` is active.
4. **`x-memory-search`** (user-invocable, model: `haiku`) — retrieval via 5 modes: `--by-tag`, `--by-capability`, `--by-rule`, `--by-pattern`, `--by-epic`. Implementation: grep + frontmatter parse; no RAG/vector DB needed at current volume.
5. **Rule 33** — mandates production, defines contracts, enforcement matrix.
6. **`audit-memory-coverage.sh`** — Camada 2 CI gate that fails when a completed epic lacks its summary.

---

## Alternatives Considered

### Alternative A: RAG Vectorial (embeddings + FAISS/Pinecone)

**Rejected.** Current volume: ~30 epics; projected 2-year volume: ~200. Grep + frontmatter index responds in < 100ms for this scale. RAG requires embedding infrastructure, vector DB, and latency on each call. Cost exceeds benefit. Reevaluate when entries exceed 200.

### Alternative B: Extend system.md (EPIC-0070) to include historical decisions

**Rejected.** `system.md` describes "current state." Mixing historical context would cause unbounded growth and blur its focus. Orthogonal concerns.

### Alternative C: Harness memory only (`~/.claude/projects/.../memory/`)

**Rejected.** Harness memory is session/operator-scoped, invisible to other contributors, and not version-controlled. Our memory must be collective, versionable, and available from `git clone`.

### Alternative D: Incremental retro-seed (1 epic per release)

**Rejected** for initial rollout. Batch approach (story-0075-0006) generates all 28 historical summaries in one sprint, providing useful retrieval volume from day 1. After initial seed, one-at-a-time via Phase 5 is the steady-state.

---

## Consequences

### Positive
- Strategic decisions recoverable in seconds via `/x-memory-search`.
- New LLM sessions gain historical context without re-reading all epics.
- Onboarding: reading `ai/memory/_index.yaml` + last 5 summaries ≈ 1h for strategic panorama.
- Memory grows automatically with each completed epic (Phase 5 gate).

### Negative
- `ai/memory/_index.yaml` is a hotspot: multiple skills read/write it — must be maintained atomically.
- Summary quality depends on the quality of epic v2 source documents; older epics may require spot-check after retro-seed.
- Volume: if entries exceed 200, grep+frontmatter index performance degrades — reevaluation point.

### Neutral
- Manual archiving via `indexable: false` in `_index.yaml` — requires operator discipline for periodic pruning.
- No cross-reference enforcement between `ai/memory/` entries (knowledge graph is future work).

---

## Related

- Rule 33: `.claude/rules/33-ai-memory-production.md`
- Rule 24 (Execution Integrity): Phase 5 MANDATORY TOOL CALL
- Rule 27 (Zero-Bypass): Surface 14
- Rule 28 (Capability Frontmatter): `governance.ai-memory`
- EPIC-0070 (system.md anchor): complementary, not overlapping
- EPIC-0075: `ai/epics/epic-0075-ai-memory-layer/`
