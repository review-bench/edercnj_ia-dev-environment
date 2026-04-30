# SPEC-context-budget-optimization-v1 — Context Budget Optimization (Rules → Knowledge Packs)

> **Status:** Draft
> **Author:** Eder Celeste Nunes Junior
> **Date:** 2026-04-30
> **Branch:** docs/feature-context-budget-optimization

---

## Sistema

O contexto sempre-carregado a cada turno do LLM (CLAUDE.md raiz + `.claude/rules/*.md`) cresceu para ~52k tokens medidos neste worktree (CLAUDE.md ~5.5k + 25 rules ~43k + SKILL.md de orquestrador ~3.8k quando invocado). Boa parte desse volume é **conteúdo de referência** (matrizes de fallback, exemplos de código vulnerável, BNF de marcadores, decision-trees, listas exaustivas de superfícies) que pertence semanticamente a Knowledge Packs lazy-loaded — não a contratos normativos sempre ativos.

Este epic **não remove nenhum gate, fase, hook, audit ou política**. A separação é puramente estrutural:

- **Rules** ficam como contratos curtos (≤80 linhas): Purpose, Invariantes, Forbidden, Enforcement (1 tabela), Reference (link para o KP).
- **Knowledge Packs** absorvem o detalhe operacional (exemplos, matrizes, BNF, regex, decision-trees), e são lidos via `Read` apenas pelas skills que precisam deles.

Layers de aplicação:

- **Camada 1 (Normativa):** rules permanecem sempre ativas, mas slim.
- **Camada 4 (Observabilidade):** novo `audit-context-budget.sh` mede tokens always-loaded em CI; advisory inicialmente, hard-fail após estabilização.
- **EPIC-0064 Capability Composer:** rules de domínio específico (ex.: Rule 12 = Java) ganham `requires-capabilities`, sendo podadas em projetos cujo perfil não as requer.

Stakeholders: Tech Leads (token budget), LLM agents (todo orquestrador se beneficia de contexto menor), Maintainers (rules ficam mais legíveis), Auditores de governança (contratos ficam mais claros sem ruído de exemplo).

Meta quantitativa: contexto sempre-carregado de **~52k → ~22k tokens (–58%)**, sem regredir nenhum gate.

---

## Escopo

### Incluído

- **Baseline e instrumentação:** novo `scripts/measure-context-budget.sh` (output JSON `{alwaysLoaded, perSkill}`), baseline commitado em `governance/baselines/context-budget.json`, audit `scripts/audit-context-budget.sh` (advisory primeiro, hard-fail depois).
- **Limpeza imediata (sem refactor):** remoção de `02-domain.md` do `RulesAssembler` (template não-preenchido com `{DOMAIN_NAME}`/`{ENTITIES_TABLE}`), renumeração da rule duplicada `28-tool-call-grammar.md` → `30-tool-call-grammar.md`, extração do histórico de epics concluídos do CLAUDE.md raiz para `docs/epics-history.md` (substituído por link).
- **Promoção de Rule 12 para KP:** os 8 exemplos Java vulneráveis/fixed (262 linhas) saem para `knowledge/security/anti-patterns/<j1..j8>.md`. Rule 12 reduz para ~30 linhas (lista CWE+severity+1-line + link). `KnowledgePacksAssembler` ganha categoria `security/anti-patterns`. Skills consumidoras (`x-review-security`, `x-owasp-scan`, `x-threat-model`) atualizadas para `Read` o KP.
- **Slim Rule 25 (task-hierarchy):** regex BNF do `subject`, tabelas `metadata`/`activeForm`, exemplos válidos/inválidos vão para `knowledge/lifecycle/task-hierarchy.md`. Rule 25 fica com Invariantes + tabela Enforcement + link. Alvo ≤60 linhas (de 179).
- **Slim Rule 26 (audit-gate-lifecycle):** decision tree, descrição extensa de Camada 0, naming completo vão para `knowledge/governance/audit-gate-lifecycle.md`. Rule mantém tabela das 5 camadas, naming convention e exit codes. Alvo ≤50 linhas (de 201).
- **Slim Rule 28-capability + Rule 30-tool-call-grammar:** YAML examples extensos de capability composition vão para `knowledge/governance/capability-composition.md`; BNF + exemplos longos de tool-call grammar vão para `knowledge/governance/tool-call-grammar.md`. Cada rule alvo ≤60 linhas.
- **Consolidação Rules 19/24/27/29/45 em "Lifecycle Integrity Contract":** Rule 19 reescrita como contrato canônico unificado (4-5 invariantes, tabela das 12 superfícies, tabela das 5 camadas, exit code matrix unificada, exceções `--legacy-flow`/`hotfix/*`/`CLAUDE_RECOVERY_MODE`). Detalhes movidos para 5 KPs: `knowledge/lifecycle/{backward-compatibility,execution-integrity,zero-bypass,refinement-gate,ci-watch-integrity}.md`. Rules 24, 27, 29, 45 reduzidas a stubs de 10 linhas durante 2 releases (deprecation window), removidas após.
- **Anotação `requires-capabilities`:** cada rule restante recebe declaração explícita; rules não-universais (12 → `language.java.*`, 25 → `governance.task-hierarchy`, 28 → `governance.capability-composition`, 30 → `governance.tool-call-grammar`) são podadas pelo `CapabilityAwareComposer` em projetos sem a capability.
- **Hardening:** `audit-context-budget.sh` vira hard-fail (limite 25.000 tokens always-loaded), `_TEMPLATE-RULE.md` documenta os 4 blocos canônicos (Purpose / Invariants / Enforcement / Reference), ADR registra decisão "Rules são contratos curtos; KPs carregam exemplos".
- **Atualização de skills consumidoras:** cada skill que dependia de exemplo agora movido para KP recebe `Read <KP-path>` explícito antes da remoção do conteúdo da rule.
- **Auditoria de orfandade:** novo `scripts/audit-kp-references.sh` faz grep reverso e falha em KP sem referência por skill alguma.

### Excluído

- **Remoção, simplificação ou desativação de qualquer gate, hook, audit, fase ou política existente.** Todos os 4 layers de enforcement permanecem operacionais; apenas o conteúdo de referência migra de localização.
- **Reescrita do `CapabilityResolver` ou do `CompositionEngine`** (EPIC-0064) — esta epic apenas anota artefatos, não muda o composer.
- **Migração de skills/agents para frontmatter v3.0** — escopo de EPIC-0064 Phase 2.
- **Mudança no fluxo de execução de orquestradores** (`x-epic-implement`, `x-story-implement`, `x-task-implement` continuam idênticos).
- **Otimização de hooks Bash** (latência de Stop/PreToolUse/PostToolUse não é alvo desta epic).
- **Reformulação de CLAUDE.md raiz além da extração de histórico de epics** — texto operacional permanece.
- **Suporte a outros idiomas ou stacks não-Java na Rule 12 KP** (apenas anti-patterns Java existentes são migrados; novas linguagens ficam para epic futura).

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-TBD-01 | **Rules são contratos curtos:** toda rule em `.claude/rules/` MUST caber em ≤80 linhas e conter apenas Purpose, Invariants/Forbidden, Enforcement (tabela curta) e Reference (link para KP). Conteúdo extenso (matrizes, exemplos, BNF, decision-trees) vai para Knowledge Packs. | Rule 26 (audit-gate-lifecycle), `_TEMPLATE-RULE.md`, `RulesAssembler`, golden files |
| RULE-TBD-02 | **Knowledge Packs são lazy-loaded:** todo KP em `.claude/knowledge/` MUST ser invocado via `Read <path>` por pelo menos uma skill. KPs órfãos são detectados por `audit-kp-references.sh` (exit 1 `KP_ORPHAN`). | `KnowledgePacksAssembler`, audit catalog, Rule 22 (visibility) |
| RULE-TBD-03 | **Context budget audit:** novo gate Camada 4 (`scripts/audit-context-budget.sh`) mede `wc -c` somado de CLAUDE.md + `.claude/rules/*.md` + opcional `<skill>/SKILL.md`, converte para tokens (`bytes/4`), e falha se always-loaded exceder limite (advisory primeiro, hard-fail 25.000 tokens após estabilização). | Rule 26 (Camada 4), `governance/baselines/context-budget.json`, CI workflow |
| RULE-TBD-04 | **Lifecycle Integrity Contract canonical:** Rule 19 absorve invariantes de Rules 24/27/29/45. Rules antigas viram stubs apontando para Rule 19 + KP correspondente durante 2 releases (Rule 19 §Deprecation Window), depois são removidas. Hooks/audits seguem operacionais (referenciam KPs e Rule 19 unificada). | Rules 19/24/27/29/45, todos os hooks `enforce-*` e `verify-*`, `audit-execution-integrity.sh`, `audit-bypass-flags.sh`, `audit-refinement-gate.sh` |
| RULE-TBD-05 | **Capability-aware rule pruning:** rules com `requires-capabilities ≠ []` são podadas pelo `CapabilityAwareComposer` em projetos sem a capability declarada. Pruning é advisory por 1 release antes de virar hard-prune. | EPIC-0064 composer, `audit-capability-graph.sh`, profile YAMLs |
| RULE-TBD-06 | **Renumeração 28→30 atômica:** `28-tool-call-grammar.md` → `30-tool-call-grammar.md` em commit único que atualiza CLAUDE.md, ADRs, baselines, hooks e qualquer skill que mencione o nome. `grep -r "28-tool-call-grammar"` deve retornar zero matches após o commit. | Rule 28 numbering, ADR cross-references, audit catalog |

---

## Histórias

Índice preliminar (a ser refinado por `/x-feature-create`):

| # | Título | Stakeholder | Justificativa |
|---|--------|-------------|---------------|
| 1 | Baseline tooling: `measure-context-budget.sh` + `audit-context-budget.sh` (advisory) | SRE / Governance | Sem baseline objetiva não há como medir economia de tokens das fases seguintes |
| 2 | Remove `02-domain.md` do `RulesAssembler` | Maintainer | Template com placeholders não-preenchidos consumindo ~5k tokens/turn |
| 3 | Renumerar `28-tool-call-grammar.md` → `30-tool-call-grammar.md` (atômico) | Maintainer | Rule 28 está duplicada; CLAUDE.md já lista apenas Rule 28 capability |
| 4 | Extrair histórico de epics concluídos do CLAUDE.md para `docs/epics-history.md` | Tech Writer | ~14 blocos `> Concluded — EPIC-XXXX` consumindo ~3-4k tokens sem afetar comportamento futuro |
| 5 | Slim Rule 12 (Security Anti-Patterns) → KP `knowledge/security/anti-patterns/` | Architect / Security | 262 linhas de exemplos Java só usadas em revisão; carga always-loaded desnecessária |
| 6 | Slim Rule 25 (task-hierarchy) → KP `knowledge/lifecycle/task-hierarchy.md` | Architect | Regex BNF + tabelas só relevantes para skills que validam subject |
| 7 | Slim Rule 26 (audit-gate-lifecycle) → KP `knowledge/governance/audit-gate-lifecycle.md` | Architect | Decision tree + naming completo só relevantes para autores de novos gates |
| 8 | Slim Rule 28-capability → KP `knowledge/governance/capability-composition.md` | Architect | YAML examples só relevantes durante autoria de novos artefatos |
| 9 | Slim Rule 30-tool-call-grammar → KP `knowledge/governance/tool-call-grammar.md` | Architect | BNF + exemplos só relevantes para auditoria de SKILL.md |
| 10 | Atualizar skills consumidoras com `Read <KP-path>` explícito (pré-requisito de slimming) | Architect | Garantir que nenhuma skill perca acesso ao detalhe quando rule for reduzida |
| 11 | Reescrever Rule 19 como "Lifecycle Integrity Contract" canônico (consolida 24/27/29/45) | Architect / Governance | Eliminar sobreposição massiva de "Forbidden / Audit / Self-check" entre 5 rules |
| 12 | Criar 5 KPs lifecycle (`backward-compatibility`, `execution-integrity`, `zero-bypass`, `refinement-gate`, `ci-watch-integrity`) | Architect | Hospedar matrizes de fallback, 12 superfícies, exit codes detalhados, dimensões de refinement |
| 13 | Stub Rules 24/27/29/45 (10 linhas, apontando Rule 19 + KP) com deprecation window de 2 releases | Architect | Permitir removal limpo sem quebrar audit scripts |
| 14 | Anotar `requires-capabilities` em todas rules restantes; ativar pruning advisory | Architect / EPIC-0064 | Permitir podar Rule 12 (Java-only) e similares em perfis minimalistas |
| 15 | Audit `audit-kp-references.sh` (KP órfão) | SRE / Governance | Garantir que todo KP novo é efetivamente referenciado por skill |
| 16 | Hard-fail `audit-context-budget.sh` em 25.000 tokens; ADR + `_TEMPLATE-RULE.md` documentando convenção | Tech Lead | Cristalizar princípio "Rules são contratos curtos, KPs carregam exemplos" |
| 17 | Remover stubs de Rules 24/27/29/45 após 2 releases (release n+2) | Maintainer | Liberar ~18k tokens da camada always-loaded permanentemente |

---

## DoR / DoD

### Definition of Ready

- [ ] `scripts/measure-context-budget.sh` implementado e baseline (`governance/baselines/context-budget.json`) commitado em commit pré-epic.
- [ ] Mapeamento completo de skills consumidoras de cada rule a ser slimming feito: `grep -r "12-security-anti-patterns\|25-task-hierarchy\|26-audit-gate-lifecycle\|28-capability\|28-tool-call-grammar"` em `targets/claude/skills/` cataloga todos os pontos que precisam de update para `Read <KP-path>`.
- [ ] Inventário das 12 superfícies de Rule 27 + 8 exit codes de Rule 45 + 7 dimensões de Rule 29 + matriz `flowVersion`/`taskTracking`/`interactiveMode`/`refinementVerdict` de Rule 19 consolidados em planilha de migração para o Lifecycle Integrity KP.
- [ ] ADR de "Rules como contratos curtos / KPs como referência lazy" reservado (número TBD pendente de epics em voo); rascunho da rationale escrito.
- [ ] Convenção de path para KPs nova confirmada (`knowledge/{security|lifecycle|governance}/<topic>.md`) e validada contra `KnowledgePacksAssembler`.
- [ ] Estratégia de deprecation window confirmada (2 releases conforme Rule 19 §Deprecation Window) e documentada no Implementation Map.

### Definition of Done

- [ ] `scripts/audit-context-budget.sh` implementado, hard-fail em 25.000 tokens always-loaded, com `--self-check` contract conforme Rule 26 (exit 0/1/2/3); registrado em `docs/audit-gates-catalog.md`.
- [ ] `02-domain.md` removido de `RulesAssembler`; profile YAMLs marcam `domain: skip` ou equivalente; golden files regenerados; `GoldenFileTest` verde.
- [ ] Renumeração `28-tool-call-grammar.md` → `30-tool-call-grammar.md` aplicada em commit atômico; `grep -r "28-tool-call-grammar"` em todo o repo retorna zero matches; ADRs/baselines/hooks atualizados.
- [ ] Histórico de epics concluídos extraído de CLAUDE.md raiz para `docs/epics-history.md`; CLAUDE.md mantém apenas link; tamanho de CLAUDE.md cai para ≤200 linhas.
- [ ] Rule 12 reduzida para ≤30 linhas; KP `knowledge/security/anti-patterns/<j1..j8>.md` criado; `x-review-security`, `x-owasp-scan`, `x-threat-model` lêem o KP via `Read` explícito; smoke test valida.
- [ ] Rules 25, 26, 28-capability, 30-tool-call-grammar reduzidas para ≤60 linhas cada; KPs correspondentes em `knowledge/lifecycle/` e `knowledge/governance/` criados; audits associados (`audit-task-hierarchy.sh`, `audit-tool-call-grammar.sh`, `audit-capability-graph.sh`) continuam green.
- [ ] Rule 19 reescrita como "Lifecycle Integrity Contract" canônico; 5 KPs lifecycle criados; Rules 24/27/29/45 reduzidas a stubs de 10 linhas apontando Rule 19 + KP; `LifecycleIntegrityAuditTest`, `audit-execution-integrity.sh`, `audit-bypass-flags.sh`, `audit-refinement-gate.sh` continuam green.
- [ ] Todas rules restantes anotadas com `requires-capabilities`; pruning advisory em release atual; `audit-capability-graph.sh --self-check` green.
- [ ] `audit-kp-references.sh` implementado; zero KPs órfãos detectados.
- [ ] `_TEMPLATE-RULE.md` documenta os 4 blocos canônicos (Purpose / Invariants / Enforcement / Reference); ADR-NNNN publicado.
- [ ] Métrica final: `scripts/measure-context-budget.sh` reporta always-loaded ≤25.000 tokens (alvo interno: 22.000); telemetria confirma redução em runs reais de `/x-epic-implement`.
- [ ] CHANGELOG.md sob `## Changed` documenta consolidação de rules + criação de KPs; sob `## Deprecated` lista Rules 24/27/29/45 com sunset date (release n+2).
- [ ] Cobertura: line ≥95%, branch ≥90% no projeto Java do gerador (Rule 05); todos os assemblers afetados (`RulesAssembler`, `KnowledgePacksAssembler`, `PlanTemplatesAssembler`) com testes atualizados.

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| **Skill perde acesso a exemplo movido para KP:** se uma skill consumia conteúdo da rule via contexto sempre-ativo (sem `Read` explícito), a redução da rule deixará a skill cega ao detalhe. | Alto | Cada story de slimming (5-9, 11-13) tem como **pré-requisito** a story 10 (atualizar skills consumidoras com `Read <KP-path>` explícito). Implementation Map ordena: primeiro skills, depois rules. Smoke tests validam que cada skill consumidora produz output equivalente antes/depois. |
| **Renumeração 28→30 quebra ADRs/baselines/hooks:** referências espalhadas a `28-tool-call-grammar` em ADRs, baselines `governance/baselines/*.txt`, hooks Bash, e CLAUDE.md de outros perfis. | Alto | Story 3 é commit atômico que: (a) renomeia o arquivo, (b) `grep -rln '28-tool-call-grammar' \| xargs sed -i '' 's/28-tool-call-grammar/30-tool-call-grammar/g'`, (c) regenera goldens, (d) executa `mvn verify` antes de commit. CI block até `grep -r '28-tool-call-grammar'` retornar zero. |
| **Consolidação de Rules 19/24/27/29/45 confunde audit scripts em produção:** scripts hoje fazem `grep` em rules específicas; se contrato move sem atualização, audits falsam-positivam ou negativam. | Alto | Stub de 10 linhas em cada rule deprecada durante 2 releases (Rule 19 §Deprecation Window) preserva grep-paths antigos. Audits continuam encontrando "Rule 24" + cross-link para Rule 19 unificada. Após release n+2, audits são atualizados para apontar Rule 19 antes da remoção dos stubs. Story 17 é gated em "release n+2 reached". |
| **KPs ficam órfãos:** ao mover conteúdo, é possível esquecer de adicionar `Read <KP-path>` na skill consumidora, deixando KP carregado em disco mas nunca lido. | Médio | Story 15 (`audit-kp-references.sh`) é gate Camada 4: para cada `.md` em `knowledge/`, faz `grep -rl '<kp-path>' targets/claude/skills/` e exige ≥1 match. CI hard-fail em órfão. |
| **Pruning por capability remove rule esperada em perfil novo:** se um perfil futuro for adicionado sem declarar capability necessária (ex.: `language.java.*`), Rule 12 some silenciosamente e código Java novo passa por revisão sem checklist de anti-patterns. | Médio | Story 14 é advisory em release atual (warn-only no audit), virando hard-prune apenas em release seguinte. ADR documenta lista canônica de capabilities por linguagem; profile YAMLs novos são validados pelo `audit-capability-graph.sh` que lista capabilities ausentes esperadas. |
| **Latência do audit `audit-context-budget.sh`:** somar `wc -c` + tokenização de 25 rules + CLAUDE.md em cada PR pode adicionar 1-2s ao CI. | Baixo | Audit roda em job paralelo no CI workflow, não bloqueia outros jobs. Tokenização é heurística simples (`bytes/4`), não chamada de tokenizer real. |
| **Perda de discoverability:** rules slim com link para KP podem deixar leitor humano sem contexto suficiente para entender o "porquê" durante revisão de PR. | Baixo | Cada rule slim mantém **Purpose** (1 parágrafo de motivação) + **Reference** (link explícito com âncora). `_TEMPLATE-RULE.md` documenta convenção. ADR explica princípio editorial. |
| **Conflito com epics em voo (EPIC-0064 capability composition, EPIC-0069 refinement gate):** se este epic merge antes de EPIC-0064 estabilizar, pruning por capability pode behave diferente do esperado. | Médio | Story 14 (capability annotation) é gated em "EPIC-0064 Phase 2 stable"; Implementation Map declara dependência via DAG. Se EPIC-0064 estiver pendente, story 14 vira no-op (anota mas não poda) até unblock. |

---

## Próximos passos (após revisão humana)

1. Revisar o spec e iterar conforme necessário via PR (`docs/feature-context-budget-optimization` → `develop`).
2. Invocar `/x-feature-create docs/specs/SPEC-context-budget-optimization-v1.md --epic-id <NNNN>` para gerar Epic + Stories + Implementation Map.
3. Executar `/x-epic-implement epic-NNNN` para iniciar Phase 1 (planning) das 17 stories propostas.
4. Após release n+2 (deprecation window cumprida), executar story 17 (remoção dos stubs) como follow-up isolado.

---

**Spec prepared by:** Eder Celeste Nunes Junior (assisted by analysis of `.claude/` always-loaded layer in worktree `awesome-austin-c3703b`)
**Date:** 2026-04-30
