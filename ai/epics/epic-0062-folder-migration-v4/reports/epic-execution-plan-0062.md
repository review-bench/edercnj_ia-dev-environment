# Plano de Execução — EPIC-0062: Migração Física v3→v4

**Gerado em:** 2026-04-28  
**flowVersion:** 4  
**epicBranch:** epic/0062  
**Modo:** sequential  

---

## Sumário Executivo

EPIC-0062 finaliza a migração física iniciada pelo EPIC-0060. São 8 stories em ordem topológica:
- **Fase Prep Paralela:** story-0062-0001 + story-0062-0003 (arquivos disjuntos, sequencializados)
- **Fase Movimentação:** story-0062-0002 + story-0062-0004 (dependentes das prep)
- **Fase Convergência:** story-0062-0005 (Java + regen, bloqueada pelas 3 anteriores)
- **Fase SKILLs:** story-0062-0006
- **Fase Rules:** story-0062-0007
- **Fase Cleanup:** story-0062-0008

---

## DAG de Dependências

```
story-0062-0001 ──→ story-0062-0002 ──┐
                                       ├──→ story-0062-0005 ──→ story-0062-0006 ──→ story-0062-0007 ──→ story-0062-0008
story-0062-0003 ──→ story-0062-0004 ──┘
         └──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Ordem de Execução (sequential)

| # | Story ID | Título | Status |
| :--- | :--- | :--- | :--- |
| 1 | story-0062-0001 | Parametrizar BASELINE_DIR nos audit scripts | PENDING |
| 2 | story-0062-0003 | Mover adr/*.md → docs/adr/ + atualizar refs raiz | PENDING |
| 3 | story-0062-0002 | Mover audits/*.txt → governance/baselines/ | PENDING |
| 4 | story-0062-0004 | Mover specs/*.md → docs/specs/ + atualizar refs | PENDING |
| 5 | story-0062-0005 | Java assemblers + FileCategorizer + regen 11 fixtures | PENDING |
| 6 | story-0062-0006 | Converter 14 SKILLs para PathResolver | PENDING |
| 7 | story-0062-0007 | Atualizar Rules 05/13/24/25/26/27/45 + regen fixtures | PENDING |
| 8 | story-0062-0008 | Cleanup: remover symlink + diretórios v3 vazios | PENDING |

---

## Critério de Merge (epic/0062 → develop)

- Todas 8 stories com Status: Concluída
- `mvn clean test` GREEN  
- 6 audit scripts em --self-check retornam 0
- `bash scripts/migrate-layout.sh --self-check` retorna 0
- `CHANGELOG.md` atualizado
- epic-execution-report-0062.md gerado
- Gate manual aprovado via /x-review-pr

---

## Restrições de Paralelismo

| Hotspot | Stories | Decisão |
| :--- | :--- | :--- |
| `CLAUDE.md`, `Conventions.md`, `README.md` | 0062-0003, 0062-0004 | Serial: 0003 → 0004 |
| `java/src/test/resources/golden/` | 0062-0005, 0062-0007 | Serial: 0005 → 0007 |
| `governance/baselines/` | 0062-0002, 0062-0008 | Serial: 0002 → 0008 |
