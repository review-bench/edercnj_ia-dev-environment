# [Nome do Produto]

> **Template:** _TEMPLATE-PRODUCT.md v1.0  
> **Product ID:** product-[id]-[slug]  
> **Status:** [Rascunho / Em Revisão / Aprovado]  
> **Ideation Origin:** [pilot-ideation-XXX.md ou ideation ID]

---

## 1. Propósito & Contexto

**Título do Produto:**

*[Descreva o produto em 2-3 parágrafos: problema raiz, mercado-alvo, e por que agora. Conecte ao produto pai se existir.]*

**Proposta de Valor Central:**

*[Uma frase que resume o valor diferencial do produto.]*

**Mercado-Alvo Refinado:**

| Segmento | Tamanho Estimado | Critérios de Qualificação |
| :--- | :--- | :--- |
| *[Segmento 1]* | *[ex: 5.000 empresas]* | *[ex: 50-500 funcionários, SaaS B2B]* |
| *[Segmento 2]* | *[ex: 50.000 PMEs]* | *[ex: receita >R$1M/ano]* |

---

## 2. Personas Refinadas

*[Refine as personas da ideação com mais detalhe sobre comportamento, dores profundas, e critérios de sucesso.]*

| Persona | Papel | Dor Principal | Job-to-be-Done | Critério de Sucesso |
| :--- | :--- | :--- | :--- | :--- |
| *[Persona 1]* | *[Comprador/Usuário/Influenciador]* | *[Dor específica]* | *[Objetivo primário]* | *[Como mede sucesso]* |
| *[Persona 2]* | *[...]* | *[...]* | *[...]* | *[...]* |

**Anti-personas** (quem NÃO é nosso cliente):

*[Liste 2-3 perfis que parecem nosso cliente mas não são — previne desperdício de vendas.]*

---

## 3. Requisitos Funcionais Épicos

*[Lista de épicos funcionais de alto nível. Cada épico vira uma capability ou sub-produto. Não detalhar aqui — apenas nomear e justificar prioridade.]*

| ID | Épico Funcional | Prioridade | Justificativa |
| :--- | :--- | :--- | :--- |
| F-001 | *[Nome do épico]* | *[Alta/Média/Baixa]* | *[Por que essa prioridade?]* |
| F-002 | *[...]* | *[...]* | *[...]* |
| F-003 | *[...]* | *[...]* | *[...]* |

---

## 4. RNFs Root (Non-Functional Requirements Raiz)

*[Estes RNFs são herdados por TODAS as capabilities do produto. Nenhuma capability pode relaxar um RNF marcado como MANDATORY. Mínimo de 6 categorias obrigatórias: PERFORMANCE, SCALABILITY, RELIABILITY, SECURITY, COMPLIANCE, OBSERVABILITY.]*

| Categoria | Requisito | Target Mensurável | Mandatory | Verificação |
| :--- | :--- | :--- | :--- | :--- |
| PERFORMANCE | *[ex: Latência de API]* | *[ex: P99 < 200ms sob 1000 RPS]* | *[Sim/Não]* | *[ex: load test k6]* |
| SCALABILITY | *[ex: Escalonamento horizontal]* | *[ex: suporta 10x pico sem redeployment]* | *[Sim/Não]* | *[ex: chaos test auto-scaling]* |
| RELIABILITY | *[ex: Disponibilidade]* | *[ex: SLA 99.9% por mês]* | *[Sim/Não]* | *[ex: uptime monitor]* |
| SECURITY | *[ex: Autenticação]* | *[ex: OAuth 2.0 + MFA obrigatório para admins]* | *[Sim/Não]* | *[ex: pen test semestral]* |
| COMPLIANCE | *[ex: LGPD]* | *[ex: DPA assinado, dados criptografados AES-256]* | *[Sim/Não]* | *[ex: audit ANPD]* |
| OBSERVABILITY | *[ex: Tracing distribuído]* | *[ex: 100% requests com trace_id propagado]* | *[Sim/Não]* | *[ex: Grafana dashboard]* |
| DATA_INTEGRITY | *[ex: Consistência transacional]* | *[ex: zero perda de dados em falha de nó]* | *[Não]* | *[ex: chaos monkey]* |
| MAINTAINABILITY | *[ex: Cobertura de testes]* | *[ex: ≥95% line, ≥90% branch]* | *[Não]* | *[ex: jacoco gate]* |
| PORTABILITY | *[ex: Cloud-agnostic]* | *[ex: deploy em AWS, GCP, Azure sem mudança de código]* | *[Não]* | *[ex: smoke test multi-cloud]* |
| USABILITY | *[ex: Tempo de onboarding]* | *[ex: <1 hora para first value para novo cliente]* | *[Não]* | *[ex: onboarding analytics]* |

---

## 5. Constraints Técnicos

*[Limitações que o produto inteiro deve respeitar — tecnologia, regulatório, orçamentário, timeline.]*

| Tipo | Constraint | Impacto | Workaround |
| :--- | :--- | :--- | :--- |
| Técnico | *[ex: Sem dependência de cloud provider]* | *[ex: vendor lock-in proibido]* | *[ex: abstrações via ports]* |
| Regulatório | *[ex: LGPD + GDPR]* | *[ex: dados EU não podem sair da EU]* | *[ex: data residency por região]* |
| Orçamentário | *[ex: Custo infra < R$0,50/transação]* | *[ex: limite margem]* | *[ex: cache agressivo]* |
| Timeline | *[ex: MVP em 3 meses]* | *[ex: escopo reduzido]* | *[ex: feature flags]* |

---

## 6. KPIs de Produto

*[Métricas de negócio que provam que o produto entrega valor. Cada KPI deve ser mensurável com método explícito.]*

| KPI | Baseline Atual | Meta (6 meses) | Meta (12 meses) | Método de Medição |
| :--- | :--- | :--- | :--- | :--- |
| *[KPI 1]* | *[Baseline]* | *[Meta 6m]* | *[Meta 12m]* | *[Como medir]* |
| *[KPI 2]* | *[...]* | *[...]* | *[...]* | *[...]* |

**OKR Vinculado:** *[OKR da empresa que este produto serve.]*

---

## 7. Arquitetura de Alto Nível

*[Diagrama C4 Context (nível 1) — mermaid ou descrição. Não entrar em detalhes de componentes aqui.]*

```
[Ator Externo 1] --→ [PRODUTO] --→ [Sistema Externo 1]
                                 --→ [Sistema Externo 2]
[Ator Externo 2] --→ [PRODUTO]
```

**Dependências Externas Críticas:**

| Sistema | Tipo | Criticidade | Fallback |
| :--- | :--- | :--- | :--- |
| *[Sistema]* | *[API/DB/Mensageria]* | *[Alta/Média/Baixa]* | *[Fallback se indisponível]* |

---

## 8. Roadmap Multi-Ano

*[Fases de longo prazo do produto — não sprints, mas horizontes estratégicos.]*

| Fase | Horizonte | Objetivo | Entregáveis Chave | Marco |
| :--- | :--- | :--- | :--- | :--- |
| Fase 1 — Validação | *[ex: 0-6 meses]* | *[Provar hipótese central]* | *[MVP + 3 clientes piloto]* | *[Data]* |
| Fase 2 — Escala | *[ex: 6-18 meses]* | *[Crescer base de clientes]* | *[Integrações + self-service]* | *[Data]* |
| Fase 3 — Expansão | *[ex: 18-36 meses]* | *[Expandir para novos mercados]* | *[Versão enterprise + API pública]* | *[Data]* |

**Dependências Externas (Roadmap):** *[Parceiros, certificações, regulações que afetam o roadmap.]*

---

*Product criado em: [YYYY-MM-DD] | Autor: [Nome/Time] | Status: [Rascunho/Em Revisão/Aprovado]*
