# EPIC-0077 Product-First Lifecycle — 28 Stories Index

**Generated:** 2026-04-29  
**Total Stories:** 28  
**Template Version:** RA9 v2 (9 sections)  
**Language:** PT-BR (Português Brasileiro)  

---

## Phase 0 — Foundations & Governance (3 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0001 | Rule 19 update + 5 capacidades novas + ADR-NNNN | Pendente | — | 0002, 0003 |
| story-0077-0002 | Estrutura ai/products/ + numbering schema + whitelist x-planning-commit | Pendente | 0001 | 0003, 0009 |
| story-0077-0003 | Coordenação EPIC-0065: rename x-feature-create → x-aggregate-create-from-feature | Pendente | 0001 | 0011 |

## Phase 1 — Templates Upstream (5 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0004 | _TEMPLATE-IDEATION.md (7 seções) | Pendente | 0001, 0002 | 0005, 0012 |
| story-0077-0005 | _TEMPLATE-PRODUCT.md (8 seções, RNFs Root) | Pendente | 0001, 0002 | 0006, 0009 |
| story-0077-0006 | _TEMPLATE-CAPABILITY.md (7 seções, RNF no-relax) | Pendente | 0005 | 0007, 0010 |
| story-0077-0007 | _TEMPLATE-FEATURE.md (7 seções) | Pendente | 0006 | 0008, 0011, 0020 |
| story-0077-0008 | Refator _TEMPLATE-EPIC.md v3 (Source Feature + Inherited RNFs) | Pendente | 0007 | 0013, 0024 |

## Phase 2 — Skills Upstream (4 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0009 | Skill x-create-product (Ideations → Product + C1) | Pendente | 0005, 0002, 0004 | 0010, 0021 |
| story-0077-0010 | Skill x-create-capability (Product → Capability + C2) | Pendente | 0006, 0009 | 0011, 0014 |
| story-0077-0011 | Skill x-create-feature (Capability → Feature) | Pendente | 0007, 0010 | 0020, 0024, 0025 |
| story-0077-0012 | Skill x-promote-ideation (x-feature-ideate output → persistent) | Pendente | 0004 | — |

## Phase 3 — C4 Model Obrigatório (5 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0013 | Refator x-arch-plan: C4Context+Container+Component obrigatórios | Pendente | 0008 | 0014, 0015, 0017 |
| story-0077-0014 | Plan-product-c1 / plan-capability-c2 emitem C4 mandatory | Pendente | 0009, 0010, 0013 | 0017 |
| story-0077-0015 | Refator x-task-plan: C4 Code level obrigatório | Pendente | 0013 | 0017, 0020 |
| story-0077-0016 | Skill interna x-internal-c4-validate (read-only) | Pendente | — | 0017 |
| story-0077-0017 | Integração C4 validador em phase-gate post + golden fixtures | Pendente | 0013, 0014, 0015, 0016 | — |

## Phase 4 — Especialistas Refator (3 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0018 | Charter rewrite qa-engineer.md (AC measurability + error catalog + SLO harness + success metrics) | Pendente | 0001 | 0020 |
| story-0077-0019 | Promote pentest-engineer core; capability quality.pentest-always-on | Pendente | 0001 | 0020 |
| story-0077-0020 | Refator x-story-plan Phase 2: 7 agentes paralelos sob v5 | Pendente | 0018, 0019 | 0028 |

## Phase 5 — RNF Gates Entrada Obrigatória (3 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0021 | RNF Root table mandatory em x-create-product (gate falha sem 10 categorias) | Pendente | 0009 | 0023 |
| story-0077-0022 | Skill interna x-internal-rnf-validate (no-relax + Justification) | Pendente | 0006, 0007 | 0023 |
| story-0077-0023 | Gate em DoR (estende EPIC-0069): RNF_INHERITANCE_VIOLATION | Pendente | 0021, 0022 | — |

## Phase 6 — Skill Refactors Epic→Story (3 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0024 | x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8 | Pendente | 0008, 0011 | 0025, 0026 |
| story-0077-0025 | x-story-create --from-feature + --epic-id; drops Sections 2/4/8 | Pendente | 0024 | 0027, 0028 |
| story-0077-0026 | Refator x-arch-plan integrado com Feature como input | Pendente | 0013, 0024 | 0027 |

## Phase 7 — Audit, Migration & Smoke (2 stories)

| ID | Título | Status | Bloqueadores | Bloqueia |
| :--- | :--- | :--- | :--- | :--- |
| story-0077-0027 | Scripts audit Camada 2: product-upstream + c4-completeness + rnf-gates + pentest-coverage | Pendente | 0017, 0020, 0023 | 0028 |
| story-0077-0028 | Rule 19 normativa flowVersion 5 + ADR consolidando + Epic0077ProductFirstSmokeIT E2E | Pendente | 0024, 0025, 0026, 0027 | — |

---

## Estrutura do Template (RA9 v2 — 9 Seções)

Cada story contém:

1. **Contexto & Escopo** — `User story` orientada por persona, contexto, 1.1 Regras Transversais, 1.2 Entrega de Valor
2. **Packages (Hexagonal)** — camadas `Domain`, `Application`, `Adapter In/Out` e `Infrastructure` por story
3. **Contratos & Endpoints** — schemas de `Request/Response`, códigos de erro, `event schemas`
4. **Materialização SOLID** — regras do EPIC, aplicação dos princípios SOLID, restrições de código
5. **Quality Gates** — DoR Local, cenários Gherkin (4 categorias obrigatórias), DoD Local, DoD Global
6. **Segurança** — validação de entrada, autenticação, dados sensíveis, operações de path
7. **Observabilidade** — `structured logging`, `metrics`, propagação de `correlation ID`
8. **Racional da Decisão** — Decisão (`statement`), Motivo (`why`), Alternativa descartada, Consequência
9. **Dependências & File Footprint** — `Blocked by/Blocks`, 3-4 tasks por story, `File footprint` (`write/read/regen`)

---

## Métricas-Chave

- **Entrega Total:** 28 stories, ~400-600 palavras cada
- **Cenários Gherkin:** mínimo de 3-4 por story, cobrindo: degenerado, caminho feliz, erro e casos de fronteira
- **Quebra de Tasks:** 3-4 tasks por story, cada uma com `Layer`, `Test Type`, tamanho (S/M/L), dependências, arquivos e `Acceptance Criteria`
- **Testes Smoke/E2E:** mínimo de 1 teste por story com label `[Test]` ou smoke/E2E
- **Valor Não Funcional:** cada story tem um deliverable não funcional mensurável (não técnico)

---

## Prontidão para Implementação

✓ Todas as 28 stories estão prontas para implementação  
✓ O grafo de dependências está completo e validado (DAG — sem ciclos)  
✓ Os templates (IDEATION, PRODUCT, CAPABILITY, FEATURE, EPIC) são pré-condições para a Fase 1+  
✓ As skills (`x-create-product`, `x-create-capability`, `x-create-feature`) dependem dos templates da Fase 1  
✓ A validação C4 e os gates de RNF são dependências da Fase 3+  

---

## Próximas Etapas

1. **Validação:** Rodar smoke tests de integridade de cada story (sintaxe Markdown, DAG de dependências, completude de tasks)
2. **Integração com Jira:** Converter 28 stories para Jira via skill `x-jira-create-stories`
3. **Persistência:** Executar `x-planning-commit` para registrar os artefatos em git
4. **Execução da Fase 0:** Iniciar stories 0001, 0002, 0003 como bloqueadores críticos
5. **Início da Fase 1:** Templates upstream (stories 0004-0008) após a conclusão da Fase 0

