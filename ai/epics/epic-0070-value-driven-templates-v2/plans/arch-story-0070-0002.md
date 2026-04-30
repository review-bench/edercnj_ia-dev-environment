# Architecture Plan — story-0070-0002

**Story:** Reescrever `_TEMPLATE-EPIC.md` v2 (foco valor)
**Epic:** EPIC-0070 (Value-Driven Templates v2)
**Scope:** SIMPLE — template markdown rewrite, no Java business logic changes

## 1. Context & Scope

Rewrite `src/main/resources/shared/templates/_TEMPLATE-EPIC.md` from RA9 v1 (9-section technical-heavy) to v2 (9 sections value-driven: Visão & Problema, Persona & Stakeholders, Hipótese & OKRs, Alternativas, Escopo, Riscos, Stories, Quality Gates, Origem & Referências). Remove all technical sections (Packages, Contratos, SOLID, Observabilidade) which migrate to `docs/architecture/system.md` (story-0070-0004).

## 2. Hexagonal Layers Impact

| Layer | Impact |
|-------|--------|
| domain/ | — |
| application/ | — |
| adapter/inbound/ | — |
| adapter/outbound/ | — |
| infrastructure/ | `src/main/resources/shared/templates/_TEMPLATE-EPIC.md` — REWRITE |
| templates (shared) | `src/main/resources/shared/templates/_TEMPLATE-EPIC.md` — primary artifact |

**Dependency direction:** N/A — template artifact only.

## 3. Template Version Contract

### v1 → v2 Section Mapping

| v1 Section | v2 Equivalent | Action |
|-----------|---------------|--------|
| `## 0.5 Cross-Epic Dependencies` | `## 0.5 Cross-Epic Dependencies` | RETAIN (unchanged) |
| `## 1. Contexto & Escopo` | `## 1. Visão & Problema` | REPLACE |
| `## 2. Packages (Hexagonal)` | `## 2. Persona & Stakeholders` | REPLACE |
| `## 3. Contratos & Endpoints` | `## 3. Hipótese & OKRs` | REPLACE |
| `## 4. Materialização SOLID` | `## 4. Alternativas Consideradas` | REPLACE |
| `## 5. Quality Gates` | `## 5. Escopo` | REPLACE |
| `## 6. Segurança` | `## 6. Riscos` | REPLACE |
| `## 7. Observabilidade` | `## 7. Índice de Histórias` | REPLACE |
| `## 8. Decision Rationale` | `## 8. Quality Gates` | REPLACE |
| `## 9. Dependências & File Footprint` | `## 9. Origem & Referências` | REPLACE |
| `## Refinement Verdict` | `## Refinement Verdict` | RETAIN (unchanged) |

### Frontmatter v3.0

```yaml
---
requires-capabilities: [governance.value-driven-templates]
template-version: "2.0"
---
```

Capability `governance.value-driven-templates` declared in story-0070-0001 — already published.

## 4. Design Decisions

**Decisão:** Retain `## 0.5 Cross-Epic Dependencies` section unchanged in v2.
**Motivo:** Cross-epic dependency awareness (EPIC-0076) is orthogonal to the value/technical split. The section is structural metadata, not technical architecture.
**Alternativa descartada:** Move to section 9 (Origem) — disrupts EPIC-0076 integration which reads at a fixed section anchor.
**Consequência:** v2 template has the dependency section as a preamble before section 1, consistent with v1.

**Decisão:** Prefix all v2 section placeholders with explicit guidance (not just `{{PLACEHOLDER}}`).
**Motivo:** Templates without narrative guidance produce poor LLM outputs. EPIC-0070's disfunção 3 is "narrativa perdida" — the fix must be in the template itself.
**Alternativa descartada:** Separate guidance KP only — operators skip KP context.
**Consequência:** Template is longer but produces dramatically better epics.

## 5. File Footprint

```
write:
  - src/main/resources/shared/templates/_TEMPLATE-EPIC.md

read:
  - .claude/rules/30-value-driven-templates.md
  - docs/adr/ADR-0023-value-driven-templates.md
  - capabilities/governance/value-driven-templates.yaml
  - ai/epics/epic-0070-value-driven-templates-v2/story-0070-0002.md

regen:
  - .claude/templates/_TEMPLATE-EPIC.md
  - target/classes/shared/templates/_TEMPLATE-EPIC.md
```
