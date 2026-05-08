# Mapa de Implementação — EPIC-0062: Migração Física v3→v4

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0062.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0062-0001 | Parametrizar `BASELINE_DIR` nos audit scripts | — | — | story-0062-0002 | Concluída |
| story-0062-0002 | Mover `audits/*.txt` → `governance/baselines/` | — | story-0062-0001 | story-0062-0005, story-0062-0008 | Concluída |
| story-0062-0003 | Mover `adr/*.md` → `docs/adr/` + refs raiz | — | — | story-0062-0004, story-0062-0005, story-0062-0007 | Concluída |
| story-0062-0004 | Mover `specs/*.md` → `docs/specs/` + refs | — | story-0062-0003 | story-0062-0005, story-0062-0007 | Concluída |
| story-0062-0005 | Java assemblers + `FileCategorizer` para v4 | — | story-0062-0002, story-0062-0003, story-0062-0004 | story-0062-0006 | Concluída |
| story-0062-0006 | 14 SKILLs → `PathResolver` | — | story-0062-0005 | story-0062-0007 | Concluída |
| story-0062-0007 | Rules 05/13/24/25/26/27/45 + regen 11 fixtures | — | story-0062-0006 | story-0062-0008 | Concluída |
| story-0062-0008 | Cleanup: remover symlink + dirs v3 vazios | — | story-0062-0007 | — | Concluída |

> **Valores de Status:** `Pendente` (padrão) · `Em Andamento` · `Concluída` · `Falha` · `Bloqueada` · `Parcial`

> **Convergência crítica:** story-0062-0005 ← {0062-0002, 0062-0003, 0062-0004}. As três precedentes podem rodar em paralelo (tocam arquivos disjuntos: scripts vs ADRs vs specs), mas convergem em 0062-0005 que atualiza Java contra os novos paths físicos.

---

## 2. Fases de Implementação

> Histórias agrupadas em fases. Dentro de uma fase, podem rodar em paralelo. Fase só inicia quando todas as dependências das anteriores estão `Concluída`.

```
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 0 — Preparação Paralela (2 stories em paralelo)            ║
║                                                                          ║
║   ┌──────────────────────────────────┐  ┌──────────────────────────┐    ║
║   │  story-0062-0001                 │  │  story-0062-0003         │    ║
║   │  Parametrizar BASELINE_DIR       │  │  git mv adr → docs/adr   │    ║
║   │  (sem dependência)               │  │  (sem dependência)       │    ║
║   └──────────────┬───────────────────┘  └──────────┬───────────────┘    ║
╚══════════════════╪══════════════════════════════════╪════════════════════╝
                   │                                  │
                   ▼                                  ▼
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 1 — Movimentação Física Sequencial                          ║
║                                                                          ║
║  ┌──────────────────────────────────┐  ┌──────────────────────────────┐ ║
║  │  story-0062-0002                 │  │  story-0062-0004              │ ║
║  │  git mv audits → governance      │  │  git mv specs → docs/specs    │ ║
║  │  (← story-0062-0001)             │  │  (← story-0062-0003)          │ ║
║  └──────────────┬───────────────────┘  └──────────┬───────────────────┘ ║
╚══════════════════╪══════════════════════════════════╪════════════════════╝
                   │                                  │
                   └──────────────┬───────────────────┘
                                  ▼
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 2 — Java + FileCategorizer (convergência)                   ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────────┐  ║
║   │  story-0062-0005                                                 │  ║
║   │  Atualizar 5 assemblers + FileCategorizer + regen 11 fixtures   │  ║
║   │  (← stories 0062-0002, 0062-0003, 0062-0004)                    │  ║
║   └───────────────────────────────┬──────────────────────────────────┘  ║
╚═══════════════════════════════════╪══════════════════════════════════════╝
                                    ▼
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 3 — SKILLs (sequencial)                                     ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────────┐  ║
║   │  story-0062-0006                                                 │  ║
║   │  14 SKILLs → PathResolver (lotes de 3)                          │  ║
║   │  (← story-0062-0005)                                             │  ║
║   └───────────────────────────────┬──────────────────────────────────┘  ║
╚═══════════════════════════════════╪══════════════════════════════════════╝
                                    ▼
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 4 — Rules + Regen final                                     ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────────┐  ║
║   │  story-0062-0007                                                 │  ║
║   │  Rules 05/13/24/25/26/27/45 + regen golden 11 perfis            │  ║
║   │  (← story-0062-0006)                                             │  ║
║   └───────────────────────────────┬──────────────────────────────────┘  ║
╚═══════════════════════════════════╪══════════════════════════════════════╝
                                    ▼
╔══════════════════════════════════════════════════════════════════════════╗
║         FASE 5 — Cleanup                                                 ║
║                                                                          ║
║   ┌──────────────────────────────────────────────────────────────────┐  ║
║   │  story-0062-0008                                                 │  ║
║   │  Remover symlink audits/ + diretórios v3 vazios                 │  ║
║   │  (← story-0062-0007)                                             │  ║
║   └──────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Restrições de Paralelismo (RULE-004 do EPIC-0041)

| Hotspot | Stories que tocam | Recomendação |
| :--- | :--- | :--- |
| `CLAUDE.md`, `Conventions.md`, `README.md` | 0062-0003 (refs ADR), 0062-0004 (refs spec) | **Serializar 0062-0003 → 0062-0004** (mesmas linhas/blocos prováveis) |
| `java/src/main/resources/targets/claude/skills/` | 0062-0006 (14 SKILLs) | Lote único, sem paralelismo interno |
| `java/src/test/resources/golden/` (11 perfis) | 0062-0005, 0062-0007 | Serializar (regen colide entre PRs) |
| `scripts/audit-*.sh` | 0062-0001 | Lote único |
| `governance/baselines/` | 0062-0002 (cria), 0062-0008 (cleanup) | Serializar fim-a-fim |

> Stories 0062-0001 e 0062-0003 são as únicas seguras para paralelismo (tocam arquivos disjuntos). Demais convergem ou serializam por hotspot compartilhado.

---

## 4. Critério de Pronto Para Merge (`epic/0062` → `develop`)

- [ ] Todas as 8 histórias com `Status: Concluída` em seu story-*.md
- [ ] AC1-AC10 do epic-0062.md §6 satisfeitos
- [ ] `mvn clean test` GREEN (≈4000 testes)
- [ ] Os 8 audit scripts em `--self-check` retornam 0
- [ ] `bash scripts/migrate-layout.sh --self-check` retorna 0
- [ ] `CHANGELOG.md` atualizado com entrada `[Unreleased]` listando os 8 PRs
- [ ] Tag `pre-layout-v4` ainda disponível (rollback)
- [ ] `epic-execution-report-0062.md` gerado em `ai/epics/epic-0062-folder-migration-v4/reports/`
- [ ] Gate manual de revisão (`/x-review-pr` no PR final `epic/0062 → develop`)

---

## 5. Próximos Épicos (Pós-EPIC-0062)

| Epic | Escopo | Pré-requisito |
| :--- | :--- | :--- |
| **EPIC-0063** | Hook `forbid-writes-to-legacy-plans` (freeze write em `plans/epic-N/` para `flowVersion ≤ 2`) | EPIC-0062 mergeado em `develop` |
| **EPIC-0064** | Remover probe v3 do `PathResolver` + bump MAJOR | EPIC-0063 mergeado + 2 sprints de janela de coexistência sem incidentes |
