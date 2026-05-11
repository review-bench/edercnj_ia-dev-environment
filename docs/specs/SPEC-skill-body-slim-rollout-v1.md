# SPEC — Skill Body Slim Rollout (Waves 0–5)

**Status:** Draft
**Versão:** 1.0
**Data:** 2026-05-11
**Autor:** Eder Celeste Nunes Junior + Claude (Opus 4.7)
**Epic:** TBD (a ser criado via `/x-create-feature` após aceite)
**Branch de origem:** `docs/spec-skill-body-slim-rollout`

> Spec consolidando o **plano de rollout multi-wave** que vinha sendo descrito apenas em corpos de PR (#1113, #1114, #1115). Persiste a estratégia, o inventário e os critérios de pronto/concluído num único artefato auditável, de modo que crashes de sessão não percam o contexto.

---

## 1. Contexto

### 1.1 Por que existe

Cada invocação `Skill(skill: "<name>", ...)` no harness do Claude Code **re-injeta o corpo completo de `SKILL.md`** na janela de contexto. Para skills orquestradoras (`x-orchestrate-epic`, `x-implement-epic`, `x-implement-story`, `x-orchestrate-task`) que disparam N sub-skills por execução, o custo é multiplicativo: `N × tamanho-do-corpo`. Em rodadas reais de `/x-implement-epic`, sessões inteiras já foram observadas perdendo 50–80K tokens **só de re-injeção redundante** das mesmas SKILL.md.

### 1.2 O que já decidimos

Duas decisões arquiteturais já estão **aceitas** e cobrem a estratégia técnica:

| ADR | Decisão | Alcance |
|-----|---------|---------|
| [ADR-0011](../adr/ADR-0011-shared-snippets-inclusion-strategy.md) | Snippets cross-skill compartilhados via **link markdown relativo** para `_shared/` (não placeholder substitution, não symlink). | Deduplicação cross-skill (pre-commit matrix, exit-codes families, TDD glossary, etc.). |
| [ADR-0012](../adr/ADR-0012-skill-body-slim-by-default.md) | `SKILL.md` é **contrato mínimo viável** (≤200–250 linhas, 5 seções canônicas: Triggers, Parameters, Output Contract, Error Envelope, Full Protocol pointer). Detalhe verboso vai para `references/full-protocol.md`, lido **on-demand**. | Compressão por-skill do corpo always-injected. |

Os dois ADRs são **complementares**:
- ADR-0011 ataca o eixo **cross-skill** (mesmo texto em muitos arquivos).
- ADR-0012 ataca o eixo **per-skill** (corpo verboso re-injetado a cada chamada).

### 1.3 Onde paramos

| Marco | Status | PR | Notas |
|-------|--------|----|----|
| EPIC-0047 — piloto ADR-0012 em 5 skills (`x-drive-tdd`, `x-implement-story`, `x-commit-changes`, `x-format-code`, `x-lint-code`) | ✅ Merged | #539 | Estabeleceu o padrão `references/full-protocol.md`. |
| Wave 0 — remover 3 duplicatas noun-first (`x-feature-create`, `x-feature-ideate`, `x-internal-pr-body-render`) | ✅ Merged | #1113 | −1054 linhas. 139 → 136 SKILL.md. |
| Wave 1 — infra `_shared/` (4 arquivos movidos + 3 novos snippets) | ✅ Merged | #1114 | `_shared/orchestrator-prelude.md`, `_shared/error-handling-orchestrator.md`, `_shared/error-handling-review.md`. |
| Wave 2.1 — slim `x-orchestrate-epic` (792 → 128 linhas, −83%) | ✅ Merged | #1115 | Primeiro consumo real de `_shared/` em produção. |
| Wave 2.2+ | ⏳ Pendente | — | Esta spec persiste o backlog restante. |

### 1.4 Spec irmão

A [SPEC-context-budget-optimization-v1](./SPEC-context-budget-optimization-v1.md) (EPIC-0078) trata da camada **always-loaded** (CLAUDE.md + `.claude/rules/*.md`), com alvo de ≤25K tokens. Esta spec é complementar — trata da camada **per-invocation** (corpos de SKILL.md). Não há sobreposição; um ataca o que sempre carrega, o outro o que recarrega N vezes por sessão.

---

## 2. Escopo

### 2.1 Incluído

- Aplicar contrato slim de ADR-0012 em **todas as skills com `SKILL.md` ≥ 500 linhas** (corte herdado de EPIC-0047 e do `SkillSizeLinter`).
- Onde houver oportunidade clara de delegação cross-skill, consumir snippets de `_shared/` conforme ADR-0011 (sem forçar — só quando o conteúdo é genuinamente cross-cutting).
- Cinco Waves sequenciais:
  - **Wave 0** — limpar duplicatas que poluem o cálculo de baseline.
  - **Wave 1** — construir infra `_shared/` antes de qualquer slim.
  - **Wave 2** — refactor pesado em skills ≥500 linhas (sub-waves 2.1 a 2.5+).
  - **Wave 3** — trim médio em skills 300–500 linhas.
  - **Wave 4** — strip de boilerplate em 150–300 linhas.
  - **Wave 5** — consolidação de Global Output Policy (109/139 skills) — requer decisão no gerador Java.
- Cada wave é entregue em **PRs separados, atômicos por skill** (um SKILL.md → um PR, com exceção da Wave 0 que é puramente de remoção).
- Validação por wave: `mvn -B test` verde, links `_shared/` resolvem, comportamento preservado (revisor confirma via `references/full-protocol.md`).

### 2.2 Excluído

- **Reorganização semântica de skills** (renomes, splits, merges) — pertence a EPIC-0036/0076.
- **Mudança de comportamento ou contrato externo** — esta epic é **estritamente refactor**. Triggers, Parameters, Output Contract, Error Envelope, Exit Codes permanecem byte-equivalentes ao consumidor.
- **Reescrita do `SkillsAssembler`** ou de outros assemblers — Wave 1 já provou que zero código Java é necessário para o padrão slim+references.
- **Aplicação a `knowledge/`** (KPs) — KPs já são lazy-loaded por design; não há ganho marginal.
- **Migração de capability frontmatter** (escopo EPIC-0064 / Rule 28).
- **Otimização de hooks Bash** (latência PreToolUse/PostToolUse) — domínio operacional, fora do alvo de contexto LLM.

---

## 3. Decisão / Contrato

### 3.1 Estrutura slim canônica (ADR-0012)

Toda `SKILL.md` ≥500 linhas DEVE migrar para:

```
skills/<name>/
├── SKILL.md                 (≤250 linhas: Triggers, Parameters, Output
│                             Contract, Error Envelope, Full Protocol pointer)
└── references/
    └── full-protocol.md     (verboso: phases, telemetry, schemas,
                              checkpoint/resume, integration notes)
```

**Cinco seções canônicas em `SKILL.md` slim:**

1. `## Triggers` — 1–3 linhas, quando invocar.
2. `## Parameters` — tabela de args + flags.
3. `## Output Contract` — artefatos produzidos, exit code, state-file path.
4. `## Error Envelope` — tabela de códigos de erro. Códigos transversais delegam a `_shared/exit-codes-common.md`.
5. `## Full Protocol` — uma frase + link para `references/full-protocol.md`.

### 3.2 Delegação cross-skill (ADR-0011)

Sempre que o conteúdo cair em uma das categorias abaixo, **substituir o bloco inline por link** para `_shared/`:

| Categoria | Arquivo `_shared/` | Consumidores típicos |
|-----------|--------------------|----------------------|
| Pre-commit error matrix | `_shared/error-handling-pre-commit.md` | `x-format-code`, `x-lint-code`, `x-commit-changes` |
| Orchestrator abort codes | `_shared/error-handling-orchestrator.md` | `x-orchestrate-epic`, `x-implement-epic`, `x-implement-story` |
| Review failure handling | `_shared/error-handling-review.md` | `x-review-pr`, `x-review-codebase` |
| Common exit codes (DEP_, STATE_, RULE_) | `_shared/exit-codes-common.md` | `x-release`, todos os orquestradores |
| TDD-tag glossary | `_shared/tdd-tags-glossary.md` | `x-drive-tdd`, `x-implement-task`, `x-implement-story` |
| Worktree-detect + epic-branch-ensure preludes | `_shared/orchestrator-prelude.md` | Orquestradores que precisam de Step P1+P2 |

### 3.3 O que NÃO move para `references/`

- Frontmatter YAML.
- Lista de parameters/flags (faz parte do contrato externo).
- Códigos de erro **específicos da skill** (apenas os cross-cutting delegam para `_shared/`).
- `Triggers` e `Output Contract` (são o "viewport" auditável do contrato).

---

## 4. Inventário (estado em 2026-05-11)

Medido contra `src/main/resources/claude/skills/*/SKILL.md` após merge de PR #1115:

### 4.1 Wave 2 — skills ≥500 linhas (10 alvos)

| Ordem | Skill | Linhas | `references/` existente? | Sub-wave proposta |
|-------|-------|--------|--------------------------|-------------------|
| 2.2 | `x-internal-create-story` | 818 | ❌ (criar do zero) | 2.2 |
| 2.3 | `x-plan-task` | 665 | ✅ (fundir inline em existente) | 2.3 |
| 2.4 | `x-review-codebase` | 659 | ✅ | 2.4 |
| 2.5 | `x-refine-epic` | 621 | ✅ | 2.5a |
| 2.5 | `x-refine-story` | 584 | ✅ | 2.5b |
| 2.5 | `x-internal-write-story-report` | 529 | ✅ | 2.5c |
| 2.5 | `x-internal-write-report` | 511 | ✅ | 2.5d |
| 2.5 | `x-review-pr` | 510 | ✅ | 2.5e |
| 2.5 | `x-internal-build-epic-plan` | 510 | ✅ | 2.5f |
| 2.5 | `x-internal-resume-story` | 503 | ✅ | 2.5g |

> A sub-wave 2.5 pode rodar **em paralelo** por sub-PR (mesma estrutura, mesmo padrão, sem dependência mútua) — a serialização de 2.2 → 2.3 → 2.4 antes existe apenas para validar o padrão em casos com particularidades (criar refs/ do zero em 2.2; fundir conteúdo inline com refs/ pré-existentes em 2.3).

### 4.2 Wave 3 — skills 300–500 linhas (≈18 alvos)

Trim médio: extrair seções que extrapolem o contrato canônico (workflow detail, exemplos longos, schemas extensos). Pacote será inventariado quando Wave 2 for concluída e o `SkillSizeLinter` baseline for re-medido.

### 4.3 Wave 4 — skills 150–300 linhas (light boilerplate)

Strip de boilerplate (Global Output Policy, repeat-yourself entre Triggers/Output, exemplos triviais). Candidatos suspeitos identificados em PR #1114 mas inventário fica para depois de Wave 3.

### 4.4 Wave 5 — Global Output Policy consolidation

109 de 139 skills repetem o mesmo `## Output Policy` (escopo levantado em PR #1114). Possíveis caminhos:

- **a)** Mover para `_shared/output-policy.md` + link em cada SKILL.md (ADR-0011 puro).
- **b)** Gerar pelo `SkillsAssembler` (template Java) — requer decisão sobre como tratar profile-specific output rules.
- **c)** Promover para `.claude/rules/` (sempre carregado) e remover por inteiro do corpo das skills.

Decisão da via fica para uma análise dedicada no início da Wave 5 (incluir mini-ADR).

---

## 5. Cronograma de execução

> Sequencial entre waves; sub-PRs dentro de uma sub-wave podem ser paralelos se não houver overlap em `_shared/`.

```
Wave 0  ✅ #1113 (merged)
Wave 1  ✅ #1114 (merged)
Wave 2  ⏳ em andamento
  ├─ 2.1 ✅ #1115 (merged) — x-orchestrate-epic
  ├─ 2.2     — x-internal-create-story (próximo)
  ├─ 2.3     — x-plan-task
  ├─ 2.4     — x-review-codebase
  └─ 2.5 a-g — 7 skills em paralelo
Wave 3  ⏸  (inventário pós Wave 2)
Wave 4  ⏸
Wave 5  ⏸  (precisa mini-ADR primeiro)
```

---

## 6. Métricas e Targets

| Métrica | Baseline (pré-Wave 0) | Pós-Wave 0+1 (atual) | Target pós-Wave 2 | Target final (pós-Wave 5) |
|---------|----------------------|----------------------|-------------------|---------------------------|
| Total `SKILL.md` linhas (corpo always-injectável) | ~50,191 | 34,044 | ~28,000 | ≤25,000 |
| Skills ≥500 linhas | 14 | 10 | 0 | 0 |
| Skills ≥250 linhas | ~40 | ~38 | ~20 | ≤10 |
| Skills com `references/` | ~12 | ~14 | ≥24 | ≥40 |
| Snippets em `_shared/` | 0 | 7 | 7 | 8–10 |

Medição: `wc -l` recursivo sobre `src/main/resources/claude/skills/**/SKILL.md` (a SoT). O output gerado em `.claude/skills/` é byte-equivalente.

---

## 7. DoR / DoD

### 7.1 Definition of Ready (por wave)

- [ ] Inventário da wave congelado em commit (ou item desta spec atualizado).
- [ ] PR template padronizado contendo: skill alvo, baseline linhas, target linhas, snippets `_shared/` consumidos.
- [ ] Wave anterior **merged e em verde** no `develop` (Waves não pisam umas nas outras).
- [ ] Nenhuma epic ativa renomeando os mesmos skill-paths (verificar `ai/epics/` em andamento).

### 7.2 Definition of Done (por sub-wave / PR)

- [ ] `SKILL.md` ≤250 linhas e contém exatamente as 5 seções canônicas de §3.1.
- [ ] `references/full-protocol.md` existe e contém o detalhe migrado **byte-equivalente em comportamento** (parafraseado é permitido se preservar contratos).
- [ ] Snippets `_shared/` referenciados via link relativo resolvem na árvore source-of-truth E na árvore gerada `.claude/skills/`.
- [ ] `mvn -B test` exit 0 (golden tests podem regenerar; revisor confirma diff intencional).
- [ ] `mvn -B package -DskipTests` exit 0.
- [ ] `audits/skill-size-baseline.txt` **remove** a entrada da skill migrada (não adiciona). Se a entrada não existia (skill já estava abaixo do gate), audit segue verde sem mudança.
- [ ] PR body lista: skill, antes/depois (linhas), `_shared/` consumidos, e nota explícita de "nenhuma mudança de contrato externo".
- [ ] Revisor humano confirma via diff que (a) Triggers/Parameters/Output Contract/Error Envelope intactos, (b) detalhe migrado preserva fases e contratos.

### 7.3 Definition of Done (da epic toda)

- [ ] Todas Waves 0–4 merged.
- [ ] Decisão sobre Wave 5 documentada (mini-ADR ou cancelamento explícito).
- [ ] `wc -l` total ≤25,000 linhas (alvo conservador; objetivo interno: 22,000).
- [ ] Telemetria de uma rodada `/x-implement-epic` end-to-end confirma redução de tokens consumidos vs baseline pré-Wave 0.
- [ ] CHANGELOG.md documenta as 5 waves sob `## Changed`.

---

## 8. Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| **Slim agressivo remove detalhe que o LLM precisa em runtime e perdemos behavior na borda.** | Alto | DoD exige revisor humano confirmando que `references/full-protocol.md` preserva fases/contratos. Smoke test pós-merge invoca a skill em modo `--dry-run` quando aplicável. Reverter por wave é cheap (PR atômico). |
| **Link `_shared/` quebra em algum perfil gerado (path resolution diferente em `.claude/skills/`).** | Alto | Wave 1 já validou o padrão `../_shared/...` em `x-orchestrate-epic`. CI valida que `_shared/` está em `target/classes/claude/skills/_shared/` (classpath visibility). Audit `audit-skill-shared-references.sh` pode ser adicionado se houver regressão. |
| **Waves se sobrepõem com epic renomeando skills (EPIC-0076, EPIC-0036).** | Médio | DoR §7.1 verifica ausência de epic ativa nos mesmos skill-paths. Se conflito surgir, esperar o rename mergear antes de iniciar a sub-wave da skill afetada. |
| **Sub-wave 2.5 paralela gera conflitos em `_shared/` se duas skills mexerem no mesmo snippet.** | Médio | Padrão da sub-wave 2.5 é **só consumir** `_shared/`, não criar/editar. Edições em `_shared/` ficam para uma wave dedicada (não 2.5). |
| **`references/full-protocol.md` cresce demais e perde o ponto.** | Baixo | `full-protocol.md` é lido **on-demand** (`Read`), não always-injected. Tamanho importa só para tempo do `Read`; não há gate de tamanho proposto. |
| **EPIC-0078 (rules slim) e esta epic competem por janelas de release.** | Baixo | Specs são **independentes** (atacam camadas diferentes). Podem rodar em paralelo no `develop`. Conflito de arquivo é improvável (rules ≠ skills). |
| **Crashes de sessão LLM perdem o plano (caso que motivou esta spec).** | Baixo (após esta spec) | Esta spec **é** a mitigação. Cada PR vincula a esta spec e a wave/sub-wave correspondente. Backlog persiste em disco, não em memória de chat. |

---

## 9. Próximos passos

1. Revisar este spec via PR `docs/spec-skill-body-slim-rollout` → `develop`.
2. Após merge, iniciar **Wave 2.2** — slim `x-internal-create-story` (818 linhas, sem `references/`).
3. Sub-waves 2.3, 2.4 sequenciais; 2.5 (a–g) em paralelo após 2.4 mergear.
4. Pós-Wave 2: re-medir baseline e congelar inventário de Wave 3.
5. Wave 5 começa com um mini-ADR escolhendo entre as três opções de §4.4.

---

## 10. Referências

- [ADR-0011 — Shared Snippets Inclusion Strategy](../adr/ADR-0011-shared-snippets-inclusion-strategy.md)
- [ADR-0012 — Skill Body Slim by Default](../adr/ADR-0012-skill-body-slim-by-default.md)
- [SPEC-context-budget-optimization-v1](./SPEC-context-budget-optimization-v1.md) — spec irmão (camada always-loaded)
- PR #1113 — Wave 0 (remover duplicatas noun-first)
- PR #1114 — Wave 1 (infra `_shared/`)
- PR #1115 — Wave 2.1 (slim `x-orchestrate-epic`)
- PR #539 — EPIC-0047 piloto ADR-0012 (5 skills)
- `audits/skill-size-baseline.txt` — baseline do `SkillSizeLinter`
