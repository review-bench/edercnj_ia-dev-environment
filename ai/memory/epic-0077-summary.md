---
epic-id: EPIC-0077
slug: product-first-lifecycle
summary-version: "1.0"
created: "2026-05-05"
last-updated: "2026-05-05"
indexable: true
archived: false
superseded-by: null
tags:
  - product-first
  - lifecycle
  - c4-model
  - hierarchy
  - rnf-gate
  - flow-version-5
  - ideation
  - capability
  - planning
capabilities-affected:
  - governance.product-first
  - governance.refinement-gate
  - governance.ai-memory
rules-affected:
  - Rule-14
  - Rule-19
  - Rule-23
  - Rule-24
  - Rule-27
  - Rule-29
adrs-referenced:
  - ADR-0030
patterns-introduced:
  - product-first-hierarchy
  - rnf-inheritance
  - c4-before-decompose
antipatterns-rejected:
  - epic-without-product-parent
  - cli-command-unregistered
dependencies-of:
  - EPIC-0064
  - EPIC-0069
dependencies-for:
  - EPIC-0078
  - EPIC-0079
---

# EPIC-0077 — Product-First Lifecycle & Planning C4 Model

## Why this epic existed

A plataforma planejava bem de Epic→Story→Task mas não tinha cadeia persistente conectando intenção de produto a artefatos técnicos. Decisões de arquitetura, qualidade e segurança entravam tarde no fluxo — gaps de C4 e RNF eram descobertos pós-decomposição, forçando retrofits. Não havia rastreabilidade upstream de Ideação a Feature.

## Hypothesis tested

Introduzir **Ideation → Product → Capability → Feature → Epic → Story → Task** como hierarquia canônica com C4 obrigatório e RNF gate no-relax eliminaria descoberta tardia de gaps e reduziria retrofits de governança.

## Decisions taken (with why)

- **D-001 Hierarquia de 7 níveis**: Cada nível cria artefato persistente em `ai/{level}s/`. Rastreabilidade end-to-end de ideação a task.
- **D-002 `flowVersion: "5"`**: Registrado na Rule 19 Fallback Matrix com `productFirstLifecycle: true`. Backward-compatible por Rule 19 §Field Additions.
- **D-003 C4 Model como gate visual**: `x-arch-plan-product/capability/feature` geram Mermaid/PlantUML C1+C2+C3 antes de qualquer decomposição downstream.
- **D-004 RNF no-relax imutável**: Marcadores em `## Inherited RNFs` propagados de Product → Capability → Feature sem possibilidade de relaxamento.
- **D-005 Naming `x-{verb}-{noun}`**: Skills user-invocable seguem prefixo `x-` com verbo ativo; internal com `x-internal-`.
- **D-ADR-0030 Rule 14 Extension**: `domain/products/`, `domain/capabilities/`, `domain/features/`, `domain/planning/rnf-validation/` autorizados como extensão do pipeline de geração.

## Alternatives rejected (with why)

- **Manter hierarquia Epic→Story→Task apenas**: Decisões de produto continuariam chegando tarde; retrofits persistiriam.
- **C4 como documentação opcional**: Sem enforcement os diagramas ficam desatualizados; gate obrigatório é necessário.
- **RNF inline nas stories sem herança**: Duplicação em 30+ stories inviável e inconsistente; herança centraliza a fonte de verdade.

## Reusable patterns produced

- **Product-First Hierarchy Pattern**: Cada nível tem artefato `ai/{level}s/{id}.md` com `parent-id` em frontmatter para rastreabilidade.
- **RNF Inheritance Pattern**: Capability herda RNFs do Product-pai; Feature herda de Capability; `no-relax` é imutável downstream.
- **C4-before-Decompose Pattern**: `x-arch-plan-*` deve rodar antes de `x-create-capability` ou `x-create-feature`.

## Anti-patterns observed

- **CLI command unregistered**: 10 de 11 comandos novos compilaram mas não foram adicionados ao `IaDevEnvApplication.subcommands` — apareciam no bytecode mas não no `--help`. Corrigido pós-merge em `fix/epic-0077-post-merge-fixes`.
- **SKILL.md ausente em skills entregues**: 6 skills criaram Java+CLI sem SKILL.md correspondente; Claude Code não listava os slash-commands.
- **Artifact naming mismatch**: story-0077-0029 usou naming sem prefixo `story-` triggering hook Camada 2. Corrigido criando aliases com naming canônico.

## Links

- [Epic document](../epics/epic-0077-product-first-lifecycle/epic-0077-product-first-lifecycle.md)
- [ADR-0030 Rule 14 Extension](../docs/adr/ADR-0030-rule14-product-first-domain.md)
- [Rule 19 §flowVersion 5](../.claude/rules/19-backward-compatibility.md)
- [EPIC-0064 Capability-Driven Composition](epic-0064-summary.md)
- [EPIC-0069 Refinement Gate](epic-0069-summary.md)
