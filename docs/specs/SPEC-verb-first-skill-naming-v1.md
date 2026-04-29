# SPEC — Verb-First Skill Naming Refactor

**Status:** Refined  
**Versão:** 1.1  
**Data:** 2026-04-29  
**Autor:** GitHub Copilot CLI + Eder Junior  
**Epic:** EPIC-0076

> **v1.1 — Refinamento (2026-04-29):**
> - 11 skills do catálogo atual ausentes na v1.0 foram adicionadas à matriz canônica nas seções 6.1 e 6.4.
> - Reordenadas seções para refletir presença real de `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-story-create`, `x-parallel-eval`, `x-spec-drift`, `x-mcp-recommend`, `x-frontmatter-migrate`, `x-ci-generate`, `x-setup-env`, `x-setup-stack`.
> - Esclarecido que `x-setup-env` e `x-setup-stack` já estão em forma verb-first aceitável (não renomear).

> SPEC funcional para a segunda onda de renomeação de skills.
> A baseline considerada é o **catálogo efetivo após a execução dos épicos anteriores**.
> Portanto, quando um épico anterior já prevê rename ou criação de skill, esta SPEC parte **do nome futuro planejado**, não do nome histórico atual.

---

## 1. Contexto

O repositório já passou por uma primeira padronização de skills em EPIC-0036, que consolidou principalmente o formato **substantivo + ação** (`x-epic-decompose`, `x-story-implement`, `x-pr-fix`). A partir dos épicos posteriores, especialmente EPIC-0065, o catálogo continuará mudando com novas skills públicas e internas (`x-feature-create`, `x-feature-ideate`, `x-internal-epic-create`, `x-internal-epic-map`, `x-internal-story-create`).

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
| `x-epic-implement` | `x-implement-epic` |
| `x-pr-fix` | `x-fix-pr` |
| `x-test-run` | `x-execute-tests` |
| `x-internal-story-load-context` | `x-internal-load-story-context` |

---

## 5. Baseline futura considerada

Esta SPEC assume que, antes de EPIC-0076 entrar em execução, os seguintes épicos terão sido concluídos:

| Epic | Impacto no catálogo |
| :--- | :--- |
| EPIC-0065 | introduz `x-feature-ideate`, `x-feature-create`, `x-internal-epic-create`, `x-internal-epic-map`, `x-internal-story-create` |
| EPIC-0066 | introduz surface de renderização de PR body |
| EPIC-0069 | introduz skills de refinement (`x-story-refine`, `x-epic-refine`) |
| EPIC-0072 | amplia catálogo de testes avançados |
| EPIC-0073 | amplia catálogo de testes de regressão |
| EPIC-0075 | introduz `x-memory-search` e `x-internal-epic-summary` |

Regra prática: quando um nome futuro já estiver definido por um épico anterior, EPIC-0076 renomeia **esse nome futuro**, não o predecessor histórico.

---

## 6. Matriz Canônica de Renomeação

> A coluna “Baseline considerada” representa o nome esperado **no momento de execução** de EPIC-0076.

### 6.1 Criação, planejamento e implementação

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-feature-ideate | x-ideate-feature |
| x-feature-create | x-create-feature |
| x-epic-create | x-create-epic |
| x-story-create | x-create-story |
| x-epic-decompose | x-decompose-epic |
| x-epic-map | x-map-epic |
| x-epic-orchestrate | x-orchestrate-epic |
| x-arch-plan | x-plan-architecture |
| x-arch-update | x-update-architecture |
| x-adr-generate | x-generate-adr |
| x-story-plan | x-plan-story |
| x-task-plan | x-plan-task |
| x-epic-implement | x-implement-epic |
| x-story-implement | x-implement-story |
| x-task-implement | x-implement-task |
| x-story-refine | x-refine-story |
| x-epic-refine | x-refine-epic |
| x-threat-model | x-model-threats |
| x-spec-drift | x-detect-spec-drift |
| x-parallel-eval | x-evaluate-parallelism |

### 6.2 Testes

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-test-plan | x-plan-tests |
| x-test-run | x-execute-tests |
| x-test-tdd | x-drive-tdd |
| x-test-e2e | x-execute-e2e-tests |
| x-test-contract | x-execute-contract-tests |
| x-test-contract-lint | x-lint-contract-tests |
| x-test-smoke-api | x-execute-api-smoke-tests |
| x-test-smoke-socket | x-execute-socket-smoke-tests |
| x-test-perf | x-execute-performance-tests |
| x-test-performance *(se EPIC-0072 introduzir nome expandido)* | x-execute-performance-tests |
| x-test-mutation | x-execute-mutation-tests |
| x-test-property | x-execute-property-tests |
| x-test-quality | x-assess-test-quality |
| x-test-regression-shell | x-execute-shell-regression-tests |
| x-test-regression-service | x-execute-service-regression-tests |
| x-test-regression-self | x-execute-self-regression-tests |

### 6.3 Review

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-review | x-review-codebase |
| x-review-pr | x-review-pr |
| x-review-qa | x-review-qa |
| x-review-perf | x-review-performance |
| x-review-api | x-review-api |
| x-review-db | x-review-database |
| x-review-devops | x-review-devops |
| x-review-events | x-review-events |
| x-review-obs | x-review-observability |
| x-review-security | x-review-security |
| x-review-graphql | x-review-graphql |
| x-review-grpc | x-review-grpc |
| x-review-gateway | x-review-gateway |
| x-review-compliance | x-review-compliance |
| x-review-data-modeling | x-review-data-modeling |
| x-code-audit | x-audit-code |

### 6.4 Code, docs, templates e setup de ambiente

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-code-format | x-format-code |
| x-code-lint | x-lint-code |
| x-doc-generate | x-generate-docs |
| x-doc-generate-v2 | x-generate-docs-v2 |
| x-doc-validate | x-validate-docs |
| x-template-migrate | x-migrate-templates |
| x-frontmatter-migrate | x-migrate-frontmatter |
| x-ci-generate | x-generate-ci |
| x-mcp-recommend | x-recommend-mcp |
| x-setup-env | x-setup-env *(já em verb-first; sem renome)* |
| x-setup-stack | x-setup-stack *(já em verb-first; sem renome)* |

### 6.5 Git, PR e worktree

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-git-branch | x-create-git-branch |
| x-git-cleanup-branches | x-cleanup-git-branches |
| x-git-commit | x-commit-changes |
| x-git-merge | x-merge-branches |
| x-git-push | x-push-branch |
| x-git-worktree | x-manage-worktrees |
| x-planning-commit | x-commit-planning |
| x-pr-create | x-create-pr |
| x-pr-fix | x-fix-pr |
| x-pr-fix-epic | x-fix-epic-pr |
| x-pr-merge | x-merge-pr |
| x-pr-merge-train | x-manage-pr-merge-train |
| x-pr-watch-ci | x-watch-pr-ci |
| x-pr-body-render | x-render-pr-body |

### 6.6 Operações, release e telemetria

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-ops-incident | x-handle-incident |
| x-ops-troubleshoot | x-troubleshoot-operations |
| x-perf-profile | x-profile-performance |
| x-release | x-release |
| x-release-changelog | x-generate-release-changelog |
| x-status-reconcile | x-reconcile-status |
| x-telemetry-analyze | x-analyze-telemetry |
| x-telemetry-trend | x-analyze-telemetry-trends |
| x-obs-instrument | x-instrument-observability |
| x-memory-search | x-search-memory |

### 6.7 Segurança, dependências e compliance técnica

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-dependency-audit | x-audit-dependencies |
| x-supply-chain-audit | x-audit-supply-chain |
| x-hardening-eval | x-evaluate-hardening |
| x-runtime-eval | x-evaluate-runtime |
| x-owasp-scan | x-scan-owasp |
| x-security-dashboard | x-generate-security-dashboard |
| x-security-pipeline | x-generate-security-pipeline |
| x-security-secrets | x-scan-secrets |
| x-security-sast | x-run-sast |
| x-security-dast | x-run-dast |
| x-security-container | x-scan-container-security |
| x-security-pentest | x-run-pentest |
| x-security-infra | x-assess-infrastructure-security |
| x-security-sonar | x-run-sonar-security |
| x-license-check | x-check-licenses |
| x-dep-policy-validate | x-validate-dependency-policy |
| x-dep-validate-with-policy | x-validate-dependencies-with-policy |

### 6.8 Jira

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-jira-create-epic | x-create-jira-epic |
| x-jira-create-stories | x-create-jira-stories |

### 6.9 Internas

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-internal-args-normalize | x-internal-normalize-args |
| x-internal-epic-branch-ensure | x-internal-ensure-epic-branch |
| x-internal-epic-build-plan | x-internal-build-epic-plan |
| x-internal-epic-integrity-gate | x-internal-verify-epic-integrity |
| x-internal-epic-create | x-internal-create-epic |
| x-internal-epic-map | x-internal-map-epic |
| x-internal-epic-summary | x-internal-summarize-epic |
| x-internal-phase-gate | x-internal-verify-phase-gates |
| x-internal-report-write | x-internal-write-report |
| x-internal-status-update | x-internal-update-status |
| x-internal-story-build-plan | x-internal-build-story-plan |
| x-internal-story-create | x-internal-create-story |
| x-internal-story-load-context | x-internal-load-story-context |
| x-internal-story-report | x-internal-write-story-report |
| x-internal-story-resume | x-internal-resume-story |
| x-internal-story-verify | x-internal-verify-story |
| x-internal-worktree-precheck | x-internal-precheck-worktree |
| x-internal-pr-body-render | x-internal-render-pr-body |
| x-internal-pr-body-render-backlog | x-internal-render-backlog-pr-body |
| x-internal-pr-body-render-impl | x-internal-render-implementation-pr-body |
| x-internal-pr-backlog-render | x-internal-render-pr-backlog |
| x-internal-doc-generate-step | x-internal-generate-doc-step |
| x-internal-doc-validate-step | x-internal-validate-doc-step |

### 6.10 Libs internas

| Baseline considerada | Nome canônico proposto |
| :--- | :--- |
| x-lib-audit-rules | x-lib-audit-rules |
| x-lib-group-verifier | x-lib-verify-group |
| x-lib-task-decomposer | x-lib-decompose-task |

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
