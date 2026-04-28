# Mapa de Implementação — EPIC-0070 (Value-Driven Templates v2)

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0070.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0070-0001 | Capability + Rule 30 + ADR-0019 + decisão template v2 | — | — | 0002, 0003, 0004 | Pendente |
| story-0070-0002 | Reescrever `_TEMPLATE-EPIC.md` v2 | — | 0001 | 0005, 0007, 0008 | Pendente |
| story-0070-0003 | Reescrever `_TEMPLATE-STORY.md` v2 | — | 0001 | 0005, 0007, 0008 | Pendente |
| story-0070-0004 | `_TEMPLATE-ARCHITECTURE-SYSTEM.md` (auto-fill via YAML) | — | 0001 | 0006, 0007, 0008 | Pendente |
| story-0070-0005 | Atualizar `x-epic-create`/`x-story-create` para v2 | — | 0002, 0003 | 0008 | Pendente |
| story-0070-0006 | Skill `/x-arch-system-update` | — | 0004 | 0008 | Pendente |
| story-0070-0007 | Skill `/x-template-migrate` (v1→v2) | — | 0002, 0003, 0004 | 0008 | Pendente |
| story-0070-0008 | Smoke + audit + CHANGELOG + supersede EPIC-0056 | — | 0005, 0006, 0007 | — | Pendente |

> **Valores de Status:** `Pendente` · `Refinada` · `Em Andamento` · `Concluída` · `Falha` · `Bloqueada` · `Parcial`

> **Nota:** EPIC-0056 (RA9) é marcado superseded em story-0070-0008 — dependência cross-epic implícita.

---

## 2. Fases de Implementação

```
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 0 — Governance + Decisão de Substituição          ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────┐               ║
║   │  story-0070-0001  Capability + Rule 30 + ADR-0019    │               ║
║   └──────────────────────┬───────────────────────────────┘               ║
╚══════════════════════════╪═══════════════════════════════════════════════╝
                           │
                           ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 1 — Templates v2 (paralelo, 3 stories)            ║
║                                                                          ║
║   ┌─────────────────┐   ┌─────────────────┐   ┌────────────────────┐     ║
║   │ story-0070-0002 │   │ story-0070-0003 │   │ story-0070-0004    │     ║
║   │ TEMPLATE-EPIC   │   │ TEMPLATE-STORY  │   │ TEMPLATE-           │     ║
║   │ v2 (foco valor) │   │ v2 (foco valor) │   │ ARCHITECTURE-       │     ║
║   │                 │   │                 │   │ SYSTEM (YAML auto-  │     ║
║   │                 │   │                 │   │ fill)               │     ║
║   └────────┬────────┘   └────────┬────────┘   └────────┬───────────┘     ║
╚════════════╪═════════════════════╪═════════════════════╪═════════════════╝
             │                     │                     │
             ▼                     ▼                     ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 2 — Skills (paralelo, 3 stories)                  ║
║                                                                          ║
║   ┌─────────────────────┐   ┌─────────────────────┐   ┌──────────────┐   ║
║   │ story-0070-0005     │   │ story-0070-0006     │   │ story-0070-  │   ║
║   │ x-epic-create /     │   │ x-arch-system-      │   │ 0007         │   ║
║   │ x-story-create v2   │   │ update              │   │ x-template-  │   ║
║   │                     │   │                     │   │ migrate      │   ║
║   └─────────┬───────────┘   └─────────┬───────────┘   └─────┬────────┘   ║
╚═════════════╪═════════════════════════╪═════════════════════╪════════════╝
              │                         │                     │
              └─────────────┬───────────┴─────────────────────┘
                            ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 3 — Verification + Supersedência                  ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────┐       ║
║   │  story-0070-0008  Smoke + audit + CHANGELOG + EPIC-0056      │       ║
║   │                   marked Superseded                          │       ║
║   └──────────────────────────────────────────────────────────────┘       ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Caminho Crítico

```
story-0070-0001 → story-0070-0004 → story-0070-0006 → story-0070-0008
   Phase 0          Phase 1            Phase 2            Phase 3
```

**4 fases no caminho crítico.** Cadeia mais longa (4 stories): 0001 → 0004 (system template) → 0006 (skill que atualiza system.md) → 0008 (smoke).

---

## 4. Grafo de Dependências (Mermaid)

```mermaid
graph TD
    S0001["story-0070-0001<br/>Governance Foundation"]
    S0002["story-0070-0002<br/>TEMPLATE-EPIC v2"]
    S0003["story-0070-0003<br/>TEMPLATE-STORY v2"]
    S0004["story-0070-0004<br/>TEMPLATE-ARCH-SYSTEM"]
    S0005["story-0070-0005<br/>x-epic-create v2"]
    S0006["story-0070-0006<br/>x-arch-system-update"]
    S0007["story-0070-0007<br/>x-template-migrate"]
    S0008["story-0070-0008<br/>Smoke + Supersede 0056"]

    S0001 --> S0002
    S0001 --> S0003
    S0001 --> S0004
    S0002 --> S0005
    S0003 --> S0005
    S0004 --> S0006
    S0002 --> S0007
    S0003 --> S0007
    S0004 --> S0007
    S0005 --> S0008
    S0006 --> S0008
    S0007 --> S0008

    classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
    classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff
    classDef fase2 fill:#533483,stroke:#e94560,color:#fff
    classDef fase3 fill:#e94560,stroke:#fff,color:#fff

    class S0001 fase0
    class S0002,S0003,S0004 fase1
    class S0005,S0006,S0007 fase2
    class S0008 fase3
```

---

## 5. Resumo por Fase

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Governance | 1 (sequencial) | — |
| 1 | 0002, 0003, 0004 | Templates | 3 paralelas | Fase 0 |
| 2 | 0005, 0006, 0007 | Skills (Adapter Inbound) | 3 paralelas | Fase 1 |
| 3 | 0008 | Test + Doc + Supersedência | 1 (sequencial) | Fase 2 |

**Total: 8 histórias em 4 fases.**

---

## 6. Detalhamento por Fase

### Fase 0 — Governance Foundation
| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0070-0001 | Capability `governance.value-driven-templates`, Rule (NN TBD — D-R3), ADR (NNNN TBD — D-R4), decisão D-R7 sobre rule única vs compartilhada com EPIC-0071, marker SUPERSEDED em EPIC-0056 (D-R9) | `capabilities/governance/value-driven-templates.yaml`, `java/src/main/resources/targets/claude/rules/<NN>-value-driven-templates.md` (D-R2), `docs/adr/ADR-<NNNN>-value-driven-templates.md` |

### Fase 1 — Templates v2
| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0070-0002 | TEMPLATE-EPIC v2: 9 seções pivotadas (Visão, Persona, Hipótese, Alternativas, Escopo, Riscos, Stories, Quality Gates, Origem) + frontmatter v3.0 (D-R6) | `java/src/main/resources/shared/templates/_TEMPLATE-EPIC.md` (rewrite) |
| 0070-0003 | TEMPLATE-STORY v2: 9 seções (Visão, Persona, Entrega, AC Gherkin 4 categorias, Contratos, Tasks, Dependências, Decision Rationale, Refinement Verdict) + frontmatter v3.0 (D-R6) | `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md` (rewrite) |
| 0070-0004 | TEMPLATE-ARCHITECTURE-SYSTEM: 11 seções, auto-fill via YAML (stack, persistência, comunicação, observabilidade, resilience, perf budget, security baseline, dep policy, doc targets, integrações, decision log) + frontmatter v3.0 + 1 golden de referência | `java/src/main/resources/shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md` (NEW), `DocsAssembler.renderSystemArchitecture` (extend), `java/src/test/resources/golden/<perfil>/system.md.golden` |

### Fase 2 — Skills (path D-R1: `targets/claude/skills/core/plan/`)
| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0070-0005 | `x-epic-create` e `x-story-create` emitem v2 por padrão; flag `--legacy-template-v1` para opt-out (Rule 19 deprecation 2 releases) + telemetry de uso da flag | SKILL.md de `x-epic-create` e `x-story-create` modificadas (path em `targets/claude/skills/core/<categoria>/`) |
| 0070-0006 | `/x-arch-system-update` atualiza `docs/architecture/system.md` incrementalmente (lê épico recém-implementado + ADRs novos + diff YAML); merge via `x-internal-report-write --append` para Decision Log + Edit cirúrgico para auto-fill; idempotente | NEW `java/src/main/resources/targets/claude/skills/core/plan/x-arch-system-update/SKILL.md` (D-R1) |
| 0070-0007 | `/x-template-migrate` assistida v1→v2 com diff + perguntas por bloco técnico (interativo + `--non-interactive` com defaults seguros) + recovery via state-file | NEW `java/src/main/resources/targets/claude/skills/core/plan/x-template-migrate/SKILL.md` (D-R1) |

### Fase 3 — Verification + Supersedência
| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| 0070-0008 | Smoke E2E (6 cenários: gerar epic v2, gerar story v2, system.md inicializado, x-arch-system-update incremental, migrar epic v1, EPIC-0056 marker) + audit `audit-template-version.sh` Rule 26-compliant (D-R5) + EPIC-0056 marker SUPERSEDED (D-R9) + CHANGELOG MAJOR sem versão fixa (D-R10) + regen 9 goldens | `java/src/test/java/dev/iadev/templates/Epic0070ValueTemplatesSmokeIT.java`, `java/src/main/resources/targets/claude/scripts/audit-template-version.sh`, `governance/baselines/template-version-baseline.txt`, `epic-0056.md` (marker D-R9), `CHANGELOG.md`, `CLAUDE.md`, `docs/audit-gates-catalog.md` |

---

## 7. Observações Estratégicas

### Gargalo Principal
**story-0070-0001** define decisões fundadoras (Rule única ou compartilhada com 0071). Investimento extra em design vale.

### Histórias Folha
**story-0070-0008** é a única folha (smoke + supersedência).

### Otimização de Tempo
- Fase 1: 3 paralelas (templates independentes).
- Fase 2: 3 paralelas (skills independentes — cada uma consome templates próprios).
- Total mínimo: 4 fases.

### Dependências Cruzadas
- story-0007 depende de 0002+0003+0004 (3 entradas) — concentra integração de templates.
- story-0008 (smoke) depende de 0005+0006+0007 — natural.

### Marco de Validação Arquitetural
**story-0070-0004** é o checkpoint — se TEMPLATE-ARCHITECTURE-SYSTEM ficar mal-modelado (auto-fill confuso, seções erradas), o `system.md` produzido nos projetos cliente fica ruim para sempre. Code-review extra recomendado.

---

## 8. Dependências entre Tasks (Cross-Story)

Cross-story task dependencies a serem detalhadas durante Phase 0 do `x-epic-implement` quando cada story for decomposta em tasks (`x-task-plan`). Pontos conhecidos pós-refinement:

- task-0070-0001-005 (criar Rule NN) → task-0070-0002-002, 0070-0003-002, 0070-0004-002 (declarar `requires-capabilities` precisa do capability ID estável da Rule).
- task-0070-0008-002 (`audit-template-version.sh`) consome o conjunto de épicos pós-rollout — depende de stories 0005-0007 já merged em `epic/0070`.
- task-0070-0007-006 (side-effect "mover para system.md") invoca `x-arch-system-update` da story 0070-0006 — sequência de Phase 2 deve respeitar essa dependência cruzada (0006 antes de 0007 OU stub do skill em 0006 disponível).

Refinement-driven deps acima foram derivadas das D-R1..D-R10 do `epic-0070.md §10`.

---

## 8.5 Restrições de Paralelismo

**Hotspots esperados:**
- `shared/templates/_TEMPLATE-*.md` — múltiplos arquivos novos/reescritos; sem colisão entre 0002/0003/0004 (arquivos diferentes).
- `targets/claude/skills/plan/*/SKILL.md` — 3 SKILLs distintas em Fase 2 (sem colisão).
- `CHANGELOG.md` (regen) — story-0008.
- `epic-0056.md` (write) — story-0008 toca para supersedência.
- Goldens regenerados (regen) — afeta os 9 perfis canônicos.

**Recomendação:** 3 paralelas em Fase 1 e Fase 2 são seguras (zero colisão entre stories).
