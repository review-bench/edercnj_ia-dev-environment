---
epic-id: EPIC-0078
slug: context-budget-optimization
summary-version: "1.0"
created: "2026-05-06"
last-updated: "2026-05-07"
indexable: true
archived: false
superseded-by: null
tags:
  - context-budget
  - rules-slimming
  - knowledge-packs
  - capability-composition
  - governance
  - token-optimization
capabilities-affected:
  - governance.capability-frontmatter
  - governance.audit-gate-lifecycle
  - governance.ai-memory
rules-affected:
  - Rule-03
  - Rule-04
  - Rule-05
  - Rule-06
  - Rule-07
  - Rule-08
  - Rule-09
  - Rule-12
  - Rule-19
  - Rule-24
  - Rule-25
  - Rule-26
  - Rule-27
  - Rule-28
  - Rule-29
  - Rule-30
  - Rule-45
adrs-referenced:
  - ADR-0033
patterns-introduced:
  - stub-pointer-rule
  - lifecycle-kp-split
  - requires-capabilities-frontmatter
antipatterns-rejected:
  - rules-as-reference-documents
  - verbatim-rule-content-in-tests
dependencies-of:
  - EPIC-0064
  - EPIC-0077
dependencies-for: []
---

# EPIC-0078 — Context Budget Optimization

## Why this epic existed

O conjunto de regras always-loaded consumia **59,591 tokens** por conversa LLM — 2.4× o target de 25,000. As regras cresceram de contratos concisos para documentos de referência completos ao longo de múltiplos épicos. Cada épico adicionava "mais uma linha" a tabelas e matrizes sem auditar o tamanho total. O resultado: toda conversa injetava matrizes de fallback inteiras, exemplos de código completos e seções de rationale extensas que raramente são necessárias inline.

## Hypothesis tested

Se dividirmos regras em contratos stub compactos (≤50 linhas) apontando para KPs de lifecycle para detalhes completos, e adicionarmos frontmatter `requires-capabilities` para habilitar pruning por capability, então o contexto always-loaded cai abaixo de 25,000 tokens sem perder nenhum enforcement de governança.

## Decisions taken (with why)

- **D-001 KP split sobre inline compression**: Comprimir o texto das regras perderia precisão. Extrair para KPs preserva fidelidade enquanto remove conteúdo da camada always-loaded. KPs são carregados sob demanda por skills que precisam do detalhe.
- **D-002 5 lifecycle KPs + 5 governance KPs**: Conteúdo agrupado por fase do lifecycle (task-hierarchy, backward-compat, exec-integrity, zero-bypass, refinement-gate, ci-watch) e tipo de governança (audit-gate-lifecycle, capability-composition, tool-call-grammar) para retrieval direcionado.
- **D-003 `requires-capabilities` frontmatter em modo advisory primeiro**: Hard-fail no pruning de capability arriscaria remover conteúdo em projetos que não declararam capabilities. Modo advisory (WARN_ONLY) vai primeiro; hard-fail segue em release posterior após adoção validar.
- **D-004 Rule 02 (Domain Template) removida completamente**: O domain template é um esqueleto que times customizam imediatamente — navegar como regra always-loaded não adicionava valor. Deletada do output do generator por inteiro.
- **D-005 `audit-context-budget.sh` como gate CI hard-fail**: Previne regressão futura de budget. Baseline commitada no token count pós-EPIC-0078; qualquer PR que aumente o count falha no CI.

## Alternatives rejected (with why)

- **Per-profile rule trimming**: Exigiria arquivos de regra separados por perfil de capability. A abordagem de KP pointer funciona para todos os perfis com um único arquivo de regra.
- **Context window increase**: Tratamento de sintoma, não causa raiz. Não ajuda custo de tokens nem melhora qualidade de resposta em problemas mais curtos.
- **Compressão inline de prosa**: Perderia precisão dos contratos; o conteúdo detalhado ficaria inacessível sem re-expandir nas regras.

## Reusable patterns produced

- **Stub-pointer pattern**: `## Full Detail → [lifecycle KP](../knowledge/lifecycle/X.md)` em uma regra permite que regras compactas permaneçam normativamente completas. Qualquer regra futura que crescer demais pode usar o mesmo padrão.
- **YAML frontmatter em regras**: `requires-capabilities: []` marca regras universalmente carregadas; não-vazio habilita pruning. Padrão agora standard para todos os arquivos de regra.
- **KP naming convention**: `knowledge/lifecycle/` para KPs de fluxo de execução; `knowledge/governance/` para KPs de meta-governança.
- **Test adaptation guide para rule-slimming**: Testes checando tabelas de evidência/seções detalhadas → redirecionar para o KP correspondente. Testes checando presença de seção → adaptar para a nova lista (mais curta) de seções na regra slim.

## Anti-patterns observed

- **Rules-as-reference-documents**: Regras crescem de contratos concisos para documentos de referência completos ao longo de múltiplos épicos, cada um adicionando "mais uma linha" sem auditar tamanho total. `audit-context-budget.sh` previne a reincidência.
- **Verbatim rule content in tests**: Testes que verificam conteúdo de regras verbatim quebram quando regras são slimadas. Quando rules.slim, 14 test classes falharam localmente antes do fix. Padrão correto: testar KP para detalhe, stub para ponteiro/cabeçalhos de seção.
- **Memory summary format mismatch**: Primeiro draft da memory summary usou formato livre em vez do schema canônico com seções `## Why this epic existed`, `## Hypothesis tested`, etc. — detectado por `RetroSeedSmokeIT` no CI.

## Links

- [Epic document](../epics/epic-0078-context-budget-optimization/epic-0078.md)
- [ADR-0033 Context Budget Optimization](../docs/adr/ADR-0033-context-budget-optimization.md)
- [Integrity gate report](../epics/epic-0078-context-budget-optimization/reports/epic-completion-report-0078.md)
- [EPIC-0064 Capability-Driven Composition](epic-0064-summary.md)
- [EPIC-0077 Product-First Lifecycle](epic-0077-summary.md)
