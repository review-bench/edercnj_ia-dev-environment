# Proposta — EPICs 0069 a 0075 (Planning Branch)

**Branch:** `chore/epics-0069-0075-planning`
**Worktree:** `.claude/worktrees/epics-planning-v2/` (recriado após o original ser removido por cleanup paralelo — veja PR description)
**Criado em:** 2026-04-28
**Autor:** sessão de planejamento entre operador (Eder) e Claude Opus 4.7
**Status global:** `Backlog` — todos os 7 épicos são propostas pendentes de refinamento e priorização.

---

## Sumário Executivo

Este branch entrega **7 épicos de planejamento** (0069 → 0075) que tratam, em conjunto, lacunas observáveis no fluxo atual de Refinamento → Planejamento → Implementação → Documentação → Memória do `ia-dev-environment`. Cada épico foi modelado em formato v4 (`flowVersion: "4"`, layout `ai/epics/`), capability-aware desde o início (Rule 28, EPIC-0064), e respeita as decisões já tomadas pelo operador na conversa de planejamento:

1. EPIC-0070 **substitui** EPIC-0056 (RA9) — declarado no header do 0070; marker formal na story-0070-0008 quando o épico for implementado.
2. CHANGELOG passa a formato **híbrido** (Highlights + Keep-a-Changelog).
3. DAST é **tiered** (smoke em PR + full nightly).
4. Arquitetura técnica sai dos épicos individuais e vai para `docs/architecture/system.md` vivo.
5. Refinamento de história/épico vira **gate bloqueante** (Rule 29).
6. Documentação é DoD (Rule 31).
7. Performance + Mutation + Contract testing ganham gates conditional via YAML.
8. Política de dependências (min/max, licenses) ganha gate executável (Rule 32).
9. Memória de IA (`ai/memory/epic-XXXX-summary.md`) é gerada por épico, indexada para retrieval futuro (Rule 33).

## Catálogo de Épicos

| # | Slug | Stories | Tema | Rule nova | ADR |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 0069 | `refinement-and-dor-gate` | 7 | Skill `/x-story-refine` + `/x-epic-refine`; status `Refinada`; gate bloqueante | 29 | 0018 |
| 0070 | `value-driven-templates-v2` | 8 | Reescreve TEMPLATE-EPIC + TEMPLATE-STORY focando valor; cria TEMPLATE-ARCH-SYSTEM (alimentado pelo YAML); supersedes EPIC-0056 | 30 | 0019 |
| 0071 | `documentation-as-dod` | 8 | `/x-doc-validate` bloqueante; CHANGELOG híbrido; Phase 3 MANDATORY doc-generate+validate | 31 | 0020 |
| 0072 | `comprehensive-test-strategy` | 9 | `/x-test-performance` (Newman/ghz/k6/hyperfine), `/x-test-mutation` (PIT/Stryker/mutmut/go-mutesting), `/x-test-contract` (openapi-diff/Pact/buf/SCC) | (Rule 05 estendida) | 0021 |
| 0073 | `regression-shell-and-dast` | 7 | `/x-test-regression-shell` (modo self+service); `/x-pentest-dynamic` (tier smoke+full) | (sem rule nova) | 0022 |
| 0074 | `dependency-policy-and-sca-gate` | 6 | YAML `dependencies.policy`; `/x-dep-policy-validate` gate final; surface 13 da Rule 27 | 32 | 0023 |
| 0075 | `ai-memory-layer` | 7 | `ai/memory/epic-XXXX-summary.md` por épico; `/x-internal-epic-summary` (haiku); `/x-memory-search` (5 modos); surface 14 da Rule 27 | 33 | 0024 |

**Total:** 7 épicos, 52 stories, 7 ADRs novos, 5 rules normativas novas, ~20 capabilities novas, 7 IMPLEMENTATION-MAPs.

## Ordem de Execução Recomendada

```
EPIC-0069 (Refinement & DoR Gate)        [prereq: EPIC-0064 done]
        │
        ▼
EPIC-0070 (Templates v2)                 [prereq: 0069 ideal; supersedes EPIC-0056]
        │
        ├──► EPIC-0071 (Doc as DoD)              [prereq: 0070]
        │
        ├──► EPIC-0072 (Perf + Mutation + Contract)
        │
        ├──► EPIC-0073 (Regression Shell + DAST)
        │
        ├──► EPIC-0074 (Dependency Policy)
        │
        └──► EPIC-0075 (AI Memory Layer)         [prereq: 0070, 0071]
```

EPICs 0071–0075 são paralelizáveis após 0069 + 0070 concluídos. Sugestão prática:
- Sprint 1: 0069 (fundação)
- Sprint 2: 0070 (templates)
- Sprint 3-5: 0071, 0072, 0074 paralelos
- Sprint 6: 0073 (testes regressão+DAST)
- Sprint 7: 0075 (memória — consome saídas dos demais)

## Contratos Compartilhados

- **flowVersion:** `"4"` em todos.
- **Layout:** `ai/epics/epic-XXXX-<slug>/`.
- **Capabilities-aware:** todos artefatos novos com frontmatter v3.0.
- **Phase 3 de `x-story-implement`** ganha invocações MANDATORY (Rule 24) condicionais ao YAML em 4 dos 7 épicos (0071, 0072, 0073, 0074).
- **Rule 27 (Zero-Bypass)** ganha **2 surfaces novas**: surface 13 (dep-policy-validation-report — EPIC-0074) e surface 14 (epic-memory-summary — EPIC-0075).
- **Rule 05 (Quality Gates)** estendida em EPIC-0072 com mutation threshold + perf budget.
- **CHANGELOG híbrido** introduzido em 0071, dogfood logo no release que entrega o épico.

## Política de Refinement (dogfood)

Todas as 52 stories deste branch estão em status `Pendente` com marker `🟡 REFINEMENT REQUIRED` e seção `## Refinement Notes` explicitando o que precisa ser refinado. **A primeira coisa que vai acontecer quando EPIC-0069 for implementada é estas próprias stories serem refinadas via `/x-story-refine`** — dogfood intencional. Os épicos pais também serão refinados via `/x-epic-refine`.

## Próximos Passos Operacionais

1. Operador revisa este branch.
2. Eventuais ajustes (escopo, ordem, divisão) são aplicados.
3. PR opened a partir deste branch para `develop`.
4. Após merge, sprint-zero do EPIC-0069 inicia.
5. EPIC-0069 implementado entrega o gate de refinement, então o ciclo de refinamento dos próprios épicos 0070-0075 começa.

## Arquivos Entregues por Este Branch

```
ai/epics/
├── epic-0069-refinement-and-dor-gate/
│   ├── epic-0069.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0069-{0001..0007}.md (7 stubs)
├── epic-0070-value-driven-templates-v2/
│   ├── epic-0070.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0070-{0001..0008}.md (8 stubs)
├── epic-0071-documentation-as-dod/
│   ├── epic-0071.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0071-{0001..0008}.md (8 stubs)
├── epic-0072-comprehensive-test-strategy/
│   ├── epic-0072.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0072-{0001..0009}.md (9 stubs)
├── epic-0073-regression-shell-and-dast/
│   ├── epic-0073.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0073-{0001..0007}.md (7 stubs)
├── epic-0074-dependency-policy-and-sca-gate/
│   ├── epic-0074.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0074-{0001..0006}.md (6 stubs)
├── epic-0075-ai-memory-layer/
│   ├── epic-0075.md
│   ├── IMPLEMENTATION-MAP.md
│   └── story-0075-{0001..0007}.md (7 stubs)
└── EPICS-0069-0075-PROPOSAL.md       (este arquivo)
```

**Total: 7 epic.md + 7 IMPLEMENTATION-MAP.md + 52 story stubs + 1 proposal index = 67 arquivos.**

---

## Referências

- Conversa de planejamento: 2026-04-28 entre Eder e Claude Opus 4.7.
- Pre-requisito principal: EPIC-0064 (Capability-Driven Composition) concluído.
- Substitui: EPIC-0056 (RA9 Standardized Templates) — declarado em EPIC-0070 header.
