# Architecture Plan — story-0070-0003

**Story:** Reescrever `_TEMPLATE-STORY.md` v2 (foco valor + Gherkin tipado)
**Epic:** EPIC-0070

## Scope: SIMPLE — template markdown rewrite

## Section Mapping: v1 → v2

| v1 Section | v2 Equivalent | Action |
|-----------|---------------|--------|
| `## 1. Contexto & Escopo` | `## 1. Visão` | REPLACE |
| `## 2. Packages (Hexagonal)` | `## 2. Persona & Cenário` | REPLACE |
| `## 3. Contratos & Endpoints` | `## 3. Entrega de Valor` | REPLACE |
| `## 4. Materialização SOLID` | `## 4. AC (Gherkin — 4 categorias)` | REPLACE |
| `## 5. Quality Gates` | `## 5. Contratos` | REPLACE |
| `## 6. Segurança` | `## 6. Tasks` | REPLACE |
| `## 7. Observabilidade` | `## 7. Dependências` | REPLACE |
| `## 8. Decision Rationale` | `## 8. Decision Rationale` | RETAIN (same name) |
| `## 9. Dependências & File Footprint` | `## 9. Refinement Verdict` | REPLACE |

## v2 Section Definitions

1. **Visão** — "Como <Persona>, quero <capacidade>, para que <benefício>"
2. **Persona & Cenário** — specific persona with context; mapped from role to user story
3. **Entrega de Valor** — measurable value delivery + success metric
4. **AC (Gherkin — 4 categorias)** — 4 mandatory categories: degenerate, happy, error/boundary, performance/SLA, security (template shows 4 populated skeletons)
5. **Contratos** — typed request/response/event contracts (input for EPIC-0071 x-spec-drift)
6. **Tasks** — task breakdown per story
7. **Dependências** — story dependencies + file footprint
8. **Decision Rationale** — 4-line micro-template (unchanged from v1)
9. **Refinement Verdict** — slot for `/x-story-refine` (EPIC-0069 contract)

## Frontmatter v3.0

```yaml
---
requires-capabilities: [governance.value-driven-templates]
template-version: "2.0"
---
```

## File Footprint

```
write:
  - src/main/resources/shared/templates/_TEMPLATE-STORY.md

read:
  - .claude/rules/30-value-driven-templates.md
  - docs/adr/ADR-0023-value-driven-templates.md
  - ai/epics/epic-0070-value-driven-templates-v2/story-0070-0003.md

regen:
  - .claude/templates/_TEMPLATE-STORY.md
```
