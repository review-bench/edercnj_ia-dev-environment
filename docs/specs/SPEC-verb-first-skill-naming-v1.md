# SPEC — Verb-First Skill Naming Refactor

**Status:** Accepted  
**Versão:** 1.2  
**Data:** 2026-05-03  
**Autor:** GitHub Copilot CLI + Eder Junior  
**Epic:** EPIC-0076

> **v1.2 — Inventário real (2026-05-03):**
> - Auditoria completa do catálogo em disco (`src/main/resources/targets/claude/skills/**`) contra a matriz v1.1.
> - 2 skills encontradas no disco mas ausentes da SPEC: `x-update-system-architecture` (seção 6.4) e `x-run-dynamic-pentest` (seção 6.7) — adicionadas.
> - 17 skills fantasmas (na SPEC mas não materializadas no disco) marcadas com nota `[não materializada — skip rename]`. As skills públicas `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-story-create` existiam como skills públicas mas foram convertidas a internals por EPIC-0065 (hard-cut). As demais (`x-test-property`, `x-test-quality`, `x-test-regression-service`, `x-test-regression-self`, `x-doc-generate-v2`, `x-pr-body-render`, `x-license-check`, `x-dep-validate-with-policy`, internals do pr-body e doc) não foram materializadas por épicos predecessores.
> - Matriz v1.2 é a fonte canônica para execução das stories 0003–0005.

> **v1.1 — Refinamento (2026-04-29):**
> - 11 skills do catálogo atual ausentes na v1.0 foram adicionadas à matriz canônica nas seções 6.1 e 6.4.
> - Reordenadas seções para refletir presença real de `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-story-create`, `x-evaluate-parallelism`, `x-detect-spec-drift`, `x-recommend-mcp`, `x-migrate-frontmatter`, `x-generate-ci`, `x-setup-env`, `x-setup-stack`.
> - Esclarecido que `x-setup-env` e `x-setup-stack` já estão em forma verb-first aceitável (não renomear).

> SPEC funcional para a segunda onda de renomeação de skills.
> A baseline considerada é o **catálogo efetivo após a execução dos épicos anteriores**.
> Portanto, quando um épico anterior já prevê rename ou criação de skill, esta SPEC parte **do nome futuro planejado**, não do nome histórico atual.

---

## 1. Contexto

O repositório já passou por uma primeira padronização de skills em EPIC-0036, que consolidou principalmente o formato **substantivo + ação** (`x-epic-decompose`, `x-implement-story`, `x-fix-pr`). A partir dos épicos posteriores, especialmente EPIC-0065, o catálogo continuará mudando com novas skills públicas e internas (`x-create-feature`, `x-ideate-feature`, `x-internal-create-epic`, `x-internal-map-epic`, `x-internal-create-story`).

O problema remanescente é semântico: ainda há muitos nomes que não representam a ação principal da skill logo no prefixo verbal. Para operadores, isso reduz previsibilidade. Para autores de docs, rules e templates, isso força memorizar exceções. Para futuras sessões LLM, a descoberta do comando correto fica menos natural do que deveria.

Esta SPEC propõe um novo padrão:

- **skills públicas:** `x-<verbo>-<objeto>`
- **skills internas:** `x-internal-<verbo>-<objeto>`
- **libs internas:** `x-lib-<verbo>-<objeto>`

Objetivo: tornar o catálogo mais intencional, com o verbo aparecendo primeiro e o objeto real da ação vindo em seguida.

---

## 2. Objetivos

1. Padronizar o catálogo público em torno de **verbo primeiro**.
2. Aplicar a mesma regra às skills internas, preservando o namespace `x-internal-`.
3. Aplicar o mesmo raciocínio às libs internas `x-lib-*`.
4. Considerar, desde já, nomes **já previstos** em épicos 0065+ para evitar renomear duas vezes a mesma intenção.
5. Atualizar todas as superfícies que dependem de nomes de skills:
   - source of truth em `java/src/main/resources/targets/claude/skills/**`
   - referências cruzadas entre SKILL.md
   - rules, templates e docs
   - Java que conhece nomes/caminhos
   - testes e guards

---

## 3. Fora do escopo

- Knowledge packs não invocáveis (`*-kp`)
- Renomear retrospectivamente artefatos históricos de épicos já concluídos
- Criar aliases permanentes
- Preservar dupla nomenclatura por janela de transição longa

---

## 4. Convenção Canônica

### 4.1 Gramática

| Tipo | Formato |
| :--- | :--- |
| Pública | `x-<verbo>-<objeto>` |
| Interna | `x-internal-<verbo>-<objeto>` |
| Lib interna | `x-lib-<verbo>-<objeto>` |

### 4.2 Regras

1. **Verbo primeiro** — a leitura do nome deve responder imediatamente “o que esta skill faz?”.
2. **Objeto intencional** — o objeto deve refletir o alvo real da ação, não necessariamente a categoria técnica onde a skill vive.
3. **Sem inversão cega** — a troca não é mecânica; clareza vence simetria.
4. **Sem aliases** — o rename é hard-cut por cluster.
5. **Internas preservam namespace** — `internal` continua imediatamente após `x-`.
6. **Plural intencional** — usar plural quando a ação opera naturalmente sobre coleção (`tests`, `docs`, `branches`, `dependencies`).

### 4.3 Exemplos

| Ruim / ambíguo | Melhor |
| :--- | :--- |
| `x-implement-epic` | `x-implement-epic` |
| `x-fix-pr` | `x-fix-pr` |
| `x-execute-tests` | `x-execute-tests` |
| `x-internal-load-story-context` | `x-internal-load-story-context` |

---

## 5. Baseline futura considerada

Esta SPEC assume que, antes de EPIC-0076 entrar em execução, os seguintes épicos terão sido concluídos:

| Epic | Impacto no catálogo |
| :--- | :--- |
| EPIC-0065 | introduz `x-ideate-feature`, `x-create-feature`, `x-internal-create-epic`, `x-internal-map-epic`, `x-internal-create-story` |
| EPIC-0066 | introduz surface de renderização de PR body |
| EPIC-0069 | introduz skills de refinement (`x-refine-story`, `x-refine-epic`) |
| EPIC-0072 | amplia catálogo de testes avançados |
| EPIC-0073 | amplia catálogo de testes de regressão |
| EPIC-0075 | introduz `x-search-memory` e `x-internal-summarize-epic` |

Regra prática: quando um nome futuro já estiver definido por um épico anterior, EPIC-0076 renomeia **esse nome futuro**, não o predecessor histórico.

---

## 6. Matriz Canônica de Renomeação

> A coluna “Baseline considerada” representa o nome esperado **no momento de execução** de EPIC-0076.

### 6.1 Criação, planejamento e implementação

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-ideate-feature | x-ideate-feature |
| x-create-feature | x-create-feature |
| x-epic-create | ~~x-create-epic~~ *[não materializada — skip rename: convertida a x-internal-create-epic por EPIC-0065]* |
| x-story-create | ~~x-create-story~~ *[não materializada — skip rename: convertida a x-internal-create-story por EPIC-0065]* |
| x-epic-decompose | ~~x-decompose-epic~~ *[não materializada — skip rename: removida hard-cut por EPIC-0065]* |
| x-epic-map | ~~x-map-epic~~ *[não materializada — skip rename: convertida a x-internal-map-epic por EPIC-0065]* |
| x-orchestrate-epic | x-orchestrate-epic |
| x-plan-architecture | x-plan-architecture |
| x-update-architecture | x-update-architecture |
| x-update-system-architecture | x-update-system-architecture |
| x-generate-adr | x-generate-adr |
| x-plan-story | x-plan-story |
| x-plan-task | x-plan-task |
| x-implement-epic | x-implement-epic |
| x-implement-story | x-implement-story |
| x-implement-task | x-implement-task |
| x-refine-story | x-refine-story |
| x-refine-epic | x-refine-epic |
| x-model-threats | x-model-threats |
| x-detect-spec-drift | x-detect-spec-drift |
| x-evaluate-parallelism | x-evaluate-parallelism |

### 6.2 Testes

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-plan-tests | x-plan-tests |
| x-execute-tests | x-execute-tests |
| x-drive-tdd | x-drive-tdd |
| x-execute-e2e-tests | x-execute-e2e-tests |
| x-execute-contract-tests | x-execute-contract-tests |
| x-lint-contract-tests | x-lint-contract-tests |
| x-execute-api-smoke-tests | x-execute-api-smoke-tests |
| x-execute-socket-smoke-tests | x-execute-socket-smoke-tests |
| x-run-perf-tests | x-execute-performance-tests |
| x-execute-performance-tests *(se EPIC-0072 introduzir nome expandido)* | x-execute-performance-tests |
| x-execute-mutation-tests | x-execute-mutation-tests |
| x-test-property | ~~x-execute-property-tests~~ *[não materializada — skip rename]* |
| x-test-quality | ~~x-assess-test-quality~~ *[não materializada — skip rename]* |
| x-execute-shell-regression-tests | x-execute-shell-regression-tests |
| x-test-regression-service | ~~x-execute-service-regression-tests~~ *[não materializada — skip rename]* |
| x-test-regression-self | ~~x-execute-self-regression-tests~~ *[não materializada — skip rename]* |

### 6.3 Review

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-review-codebase | x-review-codebase |
| x-review-pr | x-review-pr |
| x-review-qa | x-review-qa |
| x-review-performance | x-review-performance |
| x-review-api | x-review-api |
| x-review-database | x-review-database |
| x-review-devops | x-review-devops |
| x-review-events | x-review-events |
| x-review-observability | x-review-observability |
| x-review-security | x-review-security |
| x-review-graphql | x-review-graphql |
| x-review-grpc | x-review-grpc |
| x-review-gateway | x-review-gateway |
| x-review-compliance | x-review-compliance |
| x-review-data-modeling | x-review-data-modeling |
| x-audit-code | x-audit-code |

### 6.4 Code, docs, templates e setup de ambiente

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-format-code | x-format-code |
| x-lint-code | x-lint-code |
| x-generate-docs | x-generate-docs |
| x-doc-generate-v2 | ~~x-generate-docs-v2~~ *[não materializada — skip rename]* |
| x-validate-docs | x-validate-docs |
| x-migrate-templates | x-migrate-templates |
| x-migrate-frontmatter | x-migrate-frontmatter |
| x-generate-ci | x-generate-ci |
| x-recommend-mcp | x-recommend-mcp |
| x-setup-env | x-setup-env *(já em verb-first; sem renome)* |
| x-setup-stack | x-setup-stack *(já em verb-first; sem renome)* |

### 6.5 Git, PR e worktree

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-create-git-branch | x-create-git-branch |
| x-cleanup-git-branches | x-cleanup-git-branches |
| x-commit-changes | x-commit-changes |
| x-merge-branches | x-merge-branches |
| x-push-branch | x-push-branch |
| x-manage-worktrees | x-manage-worktrees |
| x-commit-planning | x-commit-planning |
| x-create-pr | x-create-pr |
| x-fix-pr | x-fix-pr |
| x-fix-epic-pr | x-fix-epic-pr |
| x-merge-pr | x-merge-pr |
| x-manage-pr-merge-train | x-manage-pr-merge-train |
| x-watch-pr-ci | x-watch-pr-ci |
| x-pr-body-render | ~~x-render-pr-body~~ *[não materializada — skip rename]* |

### 6.6 Operações, release e telemetria

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-handle-incident | x-handle-incident |
| x-troubleshoot-operations | x-troubleshoot-operations |
| x-profile-performance | x-profile-performance |
| x-release | x-release |
| x-generate-release-changelog | x-generate-release-changelog |
| x-reconcile-status | x-reconcile-status |
| x-analyze-telemetry | x-analyze-telemetry |
| x-analyze-telemetry-trends | x-analyze-telemetry-trends |
| x-instrument-observability | x-instrument-observability |
| x-search-memory | x-search-memory |

### 6.7 Segurança, dependências e compliance técnica

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-audit-dependencies | x-audit-dependencies |
| x-audit-supply-chain | x-audit-supply-chain |
| x-evaluate-hardening | x-evaluate-hardening |
| x-evaluate-runtime | x-evaluate-runtime |
| x-scan-owasp | x-scan-owasp |
| x-generate-security-dashboard | x-generate-security-dashboard |
| x-generate-security-pipeline | x-generate-security-pipeline |
| x-scan-secrets | x-scan-secrets |
| x-run-sast | x-run-sast |
| x-run-dast | x-run-dast |
| x-scan-container-security | x-scan-container-security |
| x-run-dynamic-pentest | x-run-dynamic-pentest |
| x-run-pentest | x-run-pentest |
| x-assess-infrastructure-security | x-assess-infrastructure-security |
| x-run-sonar-security | x-run-sonar-security |
| x-license-check | ~~x-check-licenses~~ *[não materializada — skip rename]* |
| x-validate-dependency-policy | x-validate-dependency-policy |
| x-dep-validate-with-policy | ~~x-validate-dependencies-with-policy~~ *[não materializada — skip rename]* |

### 6.8 Jira

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-create-jira-epic | x-create-jira-epic |
| x-create-jira-stories | x-create-jira-stories |

### 6.9 Internas

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-internal-normalize-args | x-internal-normalize-args |
| x-internal-ensure-epic-branch | x-internal-ensure-epic-branch |
| x-internal-build-epic-plan | x-internal-build-epic-plan |
| x-internal-verify-epic-integrity | x-internal-verify-epic-integrity |
| x-internal-create-epic | x-internal-create-epic |
| x-internal-map-epic | x-internal-map-epic |
| x-internal-summarize-epic | x-internal-summarize-epic |
| x-internal-verify-phase-gates | x-internal-verify-phase-gates |
| x-internal-write-report | x-internal-write-report |
| x-internal-update-status | x-internal-update-status |
| x-internal-build-story-plan | x-internal-build-story-plan |
| x-internal-create-story | x-internal-create-story |
| x-internal-load-story-context | x-internal-load-story-context |
| x-internal-write-story-report | x-internal-write-story-report |
| x-internal-resume-story | x-internal-resume-story |
| x-internal-verify-story | x-internal-verify-story |
| x-internal-precheck-worktree | x-internal-precheck-worktree |
| x-internal-render-pr-body | x-internal-render-pr-body |
| x-internal-pr-body-render-backlog | ~~x-internal-render-backlog-pr-body~~ *[não materializada — skip rename]* |
| x-internal-pr-body-render-impl | ~~x-internal-render-implementation-pr-body~~ *[não materializada — skip rename]* |
| x-internal-pr-backlog-render | ~~x-internal-render-pr-backlog~~ *[não materializada — skip rename]* |
| x-internal-doc-generate-step | ~~x-internal-generate-doc-step~~ *[não materializada — skip rename]* |
| x-internal-doc-validate-step | ~~x-internal-validate-doc-step~~ *[não materializada — skip rename]* |

### 6.10 Libs internas

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-lib-audit-rules | x-lib-audit-rules |
| x-lib-verify-group | x-lib-verify-group |
| x-lib-decompose-task | x-lib-decompose-task |

---

## 7. Estratégia de rollout

1. Congelar gramática e critérios de exceção.
2. Fechar a matriz canônica pós-EPIC-0075.
3. Executar o rename por clusters para reduzir conflito de merge.
4. Atualizar superfícies dependentes na mesma história:
   - source of truth
   - docs e templates
   - Java e testes
   - guards e audits
5. Adicionar guard CI para bloquear nomes legados.
6. Publicar tabela de migração no CHANGELOG e docs do projeto.

---

## 8. Critérios de aceite da SPEC

- A convenção pública, interna e lib está definida sem ambiguidade.
- A baseline futura pós-EPIC-0065/0069/0075 está explicitada.
- A matriz canônica cobre skills públicas, internas e `x-lib-*`.
- Knowledge packs ficam explicitamente fora do escopo.
- O rollout inclui update de referências, testes e guard anti-legado.
