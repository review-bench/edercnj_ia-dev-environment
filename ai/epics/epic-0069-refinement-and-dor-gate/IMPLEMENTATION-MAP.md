# Mapa de Implementação — EPIC-0069 (Story Refinement & DoR Gate)

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0069.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0069-0001 | Capability `governance.refinement-gate` + Rule 29 + ADR-0018 | — | — | 0002, 0003, 0004 | Pendente |
| story-0069-0002 | Skill `/x-story-refine` (interativa, 6 dimensões) | — | 0001 | 0005, 0007 | Pendente |
| story-0069-0003 | Skill `/x-epic-refine` (interativa, 7 dimensões) | — | 0001 | 0005, 0007 | Pendente |
| story-0069-0004 | Field `refinementVerdict` em `execution-state.json` + status `Refinada` na Rule 22 | — | 0001 | 0005, 0006 | Pendente |
| story-0069-0005 | PreToolUse hook `enforce-refinement-gate.sh` + exit code `33` | — | 0002, 0003, 0004 | 0007 | Pendente |
| story-0069-0006 | CI audit `audit-refinement-gate.sh` + entry no audit-gates-catalog | — | 0004 | 0007 | Pendente |
| story-0069-0007 | Smoke test E2E `Epic0069RefinementGateSmokeIT` (6 cenários) + CHANGELOG | — | 0002, 0003, 0005, 0006 | — | Pendente |

> **Valores de Status:** `Pendente` (padrão) · `Refinada` · `Em Andamento` · `Concluída` · `Falha` · `Bloqueada` · `Parcial`

> **Nota:** Dependência implícita — story-0007 (smoke) precisa que rule normativa (story-0001) esteja documentada para validação cross-reference, mas via 0005/0006 a dependência é satisfeita transitivamente.

---

## 2. Fases de Implementação

```
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 0 — Governance Foundation (sequencial)            ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────┐               ║
║   │  story-0069-0001  Capability + Rule 29 + ADR-0018    │               ║
║   └──────────────────────┬───────────────────────────────┘               ║
╚══════════════════════════╪═══════════════════════════════════════════════╝
                           │
                           ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 1 — Skills + Domain Model (paralelo, 3 stories)   ║
║                                                                          ║
║   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐        ║
║   │ story-0069-0002 │   │ story-0069-0003 │   │ story-0069-0004 │        ║
║   │ x-story-refine  │   │ x-epic-refine   │   │ state field +   │        ║
║   │ (Adapter In)    │   │ (Adapter In)    │   │ Rule 22 update  │        ║
║   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘        ║
╚════════════╪═════════════════════╪═════════════════════╪═════════════════╝
             │                     │                     │
             └─────────────┬───────┴─────────────────────┘
                           ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 2 — Infrastructure (paralelo, 2 stories)          ║
║                                                                          ║
║   ┌─────────────────────────┐   ┌────────────────────────────┐           ║
║   │ story-0069-0005         │   │ story-0069-0006            │           ║
║   │ enforce-refinement-     │   │ audit-refinement-gate.sh   │           ║
║   │ gate.sh (PreToolUse)    │   │ (CI script + catalog)      │           ║
║   └─────────────┬───────────┘   └────────────┬───────────────┘           ║
╚═════════════════╪════════════════════════════╪══════════════════════════╝
                  │                            │
                  └────────────┬───────────────┘
                               ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 3 — Verification & Release                        ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────┐       ║
║   │  story-0069-0007  Smoke test E2E (6 cenários) + CHANGELOG    │       ║
║   └──────────────────────────────────────────────────────────────┘       ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Caminho Crítico

```
story-0069-0001 → story-0069-0004 → story-0069-0005 → story-0069-0007
   Phase 0          Phase 1            Phase 2            Phase 3
```

**4 fases no caminho crítico, 4 stories na cadeia mais longa (0001 → 0004 → 0005 → 0007).**

Atrasos em qualquer ponto da cadeia atrasam o épico inteiro. Stories 0002, 0003 e 0006 são paralelizáveis dentro de suas fases e absorvem buffer.

---

## 4. Grafo de Dependências (Mermaid)

```mermaid
graph TD
    S0001["story-0069-0001<br/>Governance Foundation<br/>(Capability + Rule 29 + ADR)"]
    S0002["story-0069-0002<br/>x-story-refine"]
    S0003["story-0069-0003<br/>x-epic-refine"]
    S0004["story-0069-0004<br/>state field + Rule 22"]
    S0005["story-0069-0005<br/>PreToolUse hook"]
    S0006["story-0069-0006<br/>CI audit script"]
    S0007["story-0069-0007<br/>Smoke E2E + CHANGELOG"]

    S0001 --> S0002
    S0001 --> S0003
    S0001 --> S0004
    S0002 --> S0005
    S0003 --> S0005
    S0004 --> S0005
    S0004 --> S0006
    S0002 --> S0007
    S0003 --> S0007
    S0005 --> S0007
    S0006 --> S0007

    classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
    classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff
    classDef fase2 fill:#533483,stroke:#e94560,color:#fff
    classDef fase3 fill:#e94560,stroke:#fff,color:#fff

    class S0001 fase0
    class S0002,S0003,S0004 fase1
    class S0005,S0006 fase2
    class S0007 fase3
```

---

## 5. Resumo por Fase

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Governance | 1 (sequencial — fundação) | — |
| 1 | 0002, 0003, 0004 | Adapter Inbound + Domain Model | 3 paralelas | Fase 0 concluída |
| 2 | 0005, 0006 | Infrastructure (hook + CI) | 2 paralelas | Fase 1 concluída |
| 3 | 0007 | Test + Documentation | 1 (sequencial) | Fase 2 concluída |

**Total: 7 histórias em 4 fases.**

---

## 6. Detalhamento por Fase

### Fase 0 — Governance Foundation

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0069-0001 | Capability declarativa, Rule 29 normativa, ADR-0018 decision record, KP playbook de refinement | `capabilities/governance/refinement-gate.yaml`, `.claude/rules/29-refinement-gate.md`, `docs/adr/ADR-0018-refinement-gate.md`, `knowledge/refinement/dimensions.md` |

**Entregas da Fase 0:**
- Fundação normativa pronta para skills, hooks e auditoria consumirem.
- Capabilities families declaradas em `capabilities/_index.yaml`.

### Fase 1 — Skills + Domain Model

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0069-0002 | Skill interativa de refinamento de história (6 dimensões: persona/valor, AC Gherkin, contratos, métricas, alternativas, riscos) | `targets/claude/skills/refine/x-story-refine/SKILL.md`, KP shared `knowledge/refinement/dimensions.md` |
| 0069-0003 | Skill interativa de refinamento de épico (7 dimensões: problema, persona ampla, hipótese, OKRs, alternativas estratégicas, riscos de produto, escopo) | `targets/claude/skills/refine/x-epic-refine/SKILL.md` |
| 0069-0004 | Field `refinementVerdict` em ExecutionState; status `Refinada` na máquina de estados (Rule 22 estendida); `_TEMPLATE-REFINEMENT-VERDICT.md` | `domain/model/ExecutionState.java`, `targets/claude/rules/core/22-lifecycle-integrity.md` (modified), `shared/templates/_TEMPLATE-REFINEMENT-VERDICT.md` |

**Entregas da Fase 1:**
- Skills user-invocáveis prontas para uso (independentes do gate runtime — gate vem na Fase 2).
- Modelo de domínio + máquina de estados extendidos.

### Fase 2 — Infrastructure (Hook + CI)

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0069-0005 | PreToolUse hook que bloqueia `x-story-implement` / `x-epic-implement` / `x-task-implement` / `x-epic-orchestrate` quando `refinementVerdict.status != "approved"` | `targets/claude/hooks/PreToolUse/enforce-refinement-gate.sh`, registro em `settings.json` via `HooksAssembler` |
| 0069-0006 | CI script consolida verificação de PRs merged sem refinement aprovado | `targets/claude/scripts/_default/audit-refinement-gate.sh`, entry em `docs/audit-gates-catalog.md` |

**Entregas da Fase 2:**
- Gate runtime (Camada 0) ativo.
- Gate CI (Camada 2) ativo.

### Fase 3 — Verification & Release

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0069-0007 | Smoke test E2E cobrindo 6 cenários (aprovação, bloqueio, refinement parcial, retomada, opt-out legacy, hotfix bypass); CHANGELOG entry MINOR | `java/src/test/java/dev/iadev/skills/Epic0069RefinementGateSmokeIT.java`, `CHANGELOG.md` (entry com Highlights) |

**Entregas da Fase 3:**
- Smoke verde em CI.
- CHANGELOG documenta mudança.
- CLAUDE.md ganha bloco "REFINEMENT GATE — INEGOCIÁVEL".

---

## 7. Observações Estratégicas

### Gargalo Principal

**story-0069-0001** é o gargalo absoluto — bloqueia todo o resto. Investir tempo extra em design da capability + rule + ADR compensa porque define o contrato que as 6 stories seguintes implementam.

### Histórias Folha (sem dependentes)

**story-0069-0007** é a única folha — natural, é o smoke test final.

### Otimização de Tempo

- Fase 1: 3 paralelas — alocar 3 sub-agents/devs simultaneamente.
- Fase 2: 2 paralelas — alocar 2 simultaneamente.
- Fase 0 e 3: sequenciais por natureza.
- Tempo mínimo (paralelismo perfeito): 4 fases × tempo médio de story.

### Dependências Cruzadas

- story-0005 depende de 0002 + 0003 + 0004 (3 entradas) — é o ponto de convergência do épico.
- story-0007 (smoke) depende de 0002, 0003, 0005, 0006 — natural para teste E2E.

### Marco de Validação Arquitetural

**story-0069-0004** (state field + Rule 22) é o checkpoint arquitetural. Se Rule 22 estendida estiver mal-modelada (transição `Refinada` ambígua), todo o resto sofre. Gate de revisão extra recomendado nesta story antes de Fase 2 começar.

---

## 8. Dependências entre Tasks (Cross-Story)

> Section gerada quando histórias contiverem tasks formais. Como as stories deste épico estão em `Pendente` (a serem refinadas via EPIC-0069 dogfood), tasks ainda não foram detalhadas e esta seção é placeholder.

A ser populada quando `/x-story-refine` rodar sobre cada story do EPIC-0069 — meta: o próprio épico de refinement será o primeiro a usar a skill que entrega.

---

## 8.5 Restrições de Paralelismo

> Análise gerada por `/x-parallel-eval` (a executar quando stories estiverem refinadas com File Footprint blocks).

**Conflitos detectados:** desconhecido (footprint a ser registrado pós-refinement).

**Hotspot esperado por análise prévia:**
- `settings.json` (regen) — story-0005 toca para registrar hook.
- `capabilities/_index.yaml` (regen) — story-0001 toca.
- `CHANGELOG.md` (regen) — story-0007 toca.
- `docs/audit-gates-catalog.md` (regen) — story-0006 (entry para o novo CI script `audit-refinement-gate.sh`). Story-0001 publica Rule 29 + ADR-0018, mas a Rule por si não tem entry no catálogo de audit gates (catálogo lista gates executáveis: hooks, CI scripts, Java tests, workflows — não rules).

Recomendação preliminar: zero colisão hard prevista; story-0001 e story-0006 podem rodar em paralelo dentro de suas respectivas fases sem conflito sobre o catálogo.
