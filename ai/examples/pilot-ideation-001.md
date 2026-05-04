# Plataforma de Pagamentos Instantâneos para Fintechs

> **Template:** _TEMPLATE-IDEATION.md v1.0  
> **Ideation ID:** pilot-ideation-001-fintech-payments  
> **Status:** Rascunho (Piloto de Validação)

---

## 1. Visão & Escopo

**Título:** PayFast — API de Pagamentos PIX para Fintechs com SLA Garantido

**Visão:**

Fintechs de médio porte que processam pagamentos via PIX enfrentam dois problemas críticos: latência imprevisível na integração com o BACEN (variando de 200ms a 8s em picos) e falta de observabilidade granular quando transações falham. O resultado é churn de clientes B2B e penalidades contratuais por SLA violado.

Esta ideação propõe uma API de pagamentos PIX com SLA contratual garantido (99.9% das transações em < 1s) mediada por uma camada de retry inteligente, circuit breaker com banco BACEN, e observabilidade em tempo real (webhook + dashboard de status).

**Público-Alvo Principal:** CTO de Fintech de médio porte (50-500 funcionários) que processa 10K-500K transações PIX/mês.

**Escopo desta ideação:** [x] Produto novo

---

## 2. Stakeholders & Personas

| Persona | Papel | Decisões Chave | Frequência de Uso |
| :--- | :--- | :--- | :--- |
| CTO Fintech | Comprador técnico | Aprovar integração, avaliar SLA contratual | Mensal (revisão) |
| Engenheiro Backend | Implementador | Integrar SDK, configurar webhooks, monitorar alertas | Diário |
| CFO Fintech | Aprovador orçamentário | Aprovar pricing, analisar ROI de SLA garantido | Trimestral |
| Compliance Officer | Validador regulatório | Validar conformidade com BACEN, LGPD | Semestral |

**Stakeholders internos:** Produto, Engenharia, Jurídico/Compliance, Financeiro.

**Decisores:** CTO (decisão técnica); CFO (aprovação de budget).

---

## 3. Requisitos de Negócio

| ID | Tipo | Requisito | Prioridade | Verificável? |
| :--- | :--- | :--- | :--- | :--- |
| BIZ-001 | Não-funcional | 99.9% das transações PIX concluídas em < 1s (P99) | Alta | Sim — load tests com k6 + SLA dashboard |
| BIZ-002 | Funcional | Retry automático com backoff exponencial em falhas BACEN (max 3 tentativas) | Alta | Sim — testes de integração com mock BACEN |
| BIZ-003 | Funcional | Circuit breaker: detecta indisponibilidade BACEN em < 5s e ativa modo degradado | Alta | Sim — testes de chaos engineering |
| BIZ-004 | Funcional | Webhook em tempo real para sucesso/falha com payload tipado (JSON Schema validado) | Alta | Sim — testes E2E de webhook delivery |
| BIZ-005 | Funcional | SDK em Java, Node.js, Python — documentado com exemplos funcionais | Média | Sim — smoke tests por SDK |
| BIZ-006 | Não-funcional | Conformidade BACEN: respeitar limites de transação, horários de janela PIX | Alta | Sim — testes contra regras BACEN 2026 |
| BIZ-007 | Funcional | Dashboard operacional: taxa de sucesso, latência P50/P95/P99, volume por hora | Média | Sim — testes de integração de métricas |

---

## 4. Restrições & Assunções

### 4.1 Restrições

| Tipo | Descrição | Impacto |
| :--- | :--- | :--- |
| Regulatório | BACEN: mudanças de API sem notice de 30 dias podem quebrar integração | Monitorar changelog BACEN; versão fixa da API com fallback |
| Regulatório | LGPD: dados de CPF/CNPJ de pagadores são dados pessoais sensíveis | Criptografia em trânsito e repouso; retenção máxima 5 anos; DPA obrigatório |
| Técnico | SLA de 99.9% depende de infraestrutura multi-AZ com auto-failover | Custo de infra maior; mínimo 3 AZs na AWS São Paulo |
| Timeline | Certificação BACEN para PSP leva 6-12 meses | Operar como sub-adquirente via parceiro certificado no MVP |

### 4.2 Assunções

- [x] Parceiro PSP certificado aceita acordo de sub-adquirência com SLA de 99.5%
- [x] Clientes têm CNPJ ativo e conta bancária para receber PIX
- [ ] BACEN não muda API PIX durante janela de desenvolvimento de 6 meses (monitorar)
- [x] 99.9% SLA é diferencial suficiente para atrair primeiros 5 clientes pagantes

---

## 5. Critérios de Sucesso

| Métrica | Baseline Atual | Meta | Prazo | Método de Medição |
| :--- | :--- | :--- | :--- | :--- |
| Latência PIX P99 | 3-8s (concorrentes genéricos) | < 1s | Antes de GA | Load test com k6 (100 TPS) |
| Taxa de sucesso de transação | 97% (concorrentes) | 99.9% | 6 meses após GA | Dashboard operacional |
| Tempo médio de integração (TTV) | 2-4 semanas (concorrentes) | < 5 dias com SDK | 3 meses | Onboarding logs |
| NPS de desenvolvedores | N/A | > 60 | 6 meses | Survey pós-integração |

**OKR Vinculado:** OKR 2026-H2: "Adquirir 5 clientes Fintech com MRR > R$20K cada"

**Critério de sucesso mínimo (MVP):** 2 clientes integrados processando transações reais com SLA 99.9% por 30 dias consecutivos.

---

## 6. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação | Responsável |
| :--- | :--- | :--- | :--- | :--- |
| BACEN muda API PIX sem notice adequado | Média | Alto — integração quebra em produção | Monitorar changelog BACEN semanalmente; wrapper com abstração de versão | Engenharia |
| Fraude: transações PIX falsificadas (phishing de endkeys) | Alta | Crítico — responsabilidade legal e financeira | Validação de chave PIX contra API BACEN antes de processar; rate limiting por CPF | Segurança |
| Parceiro PSP viola SLA de 99.5%, causando cascata de violação do nosso 99.9% | Baixa | Crítico — perda de contratos | Múltiplos PSPs com failover automático; cláusula de SLA no contrato com PSP | Produto + Jurídico |
| Regulação muda: BACEN proíbe sub-adquirência não certificada | Baixa | Alto — modelo de negócio inviabilizado | Iniciar processo de certificação própria PSP paralelamente ao MVP | Compliance |

**Plano de contingência:** Se SLA 99.9% não for atingível com sub-adquirente, pivotar para SLA 99.5% (mais realista) e renegociar contratos com clientes piloto antes de publicar.

---

## 7. Roadmap

| Fase | Descrição | Duração Estimada | Milestone | Entregável |
| :--- | :--- | :--- | :--- | :--- |
| Fase 1 — MVP | API PIX via sub-adquirente + retry/circuit-breaker + webhook + SDK Java | 8 semanas | 2026-07-15 | 2 clientes beta pagantes |
| Fase 2 — Expansão | SDK Node.js + Python + dashboard operacional + multi-PSP failover | 6 semanas | 2026-09-01 | 5 clientes pagantes |
| Fase 3 — Certificação | PSP próprio BACEN + certificação regulatória + SLA contratual 99.99% | 12 meses | 2027-10-01 | Operação independente PSP |

**Dependências externas:** Acordo sub-adquirência com PSP parceiro (negociação 4 semanas); certificação BACEN PSP próprio (12+ meses, Fase 3).

**Próximos passos imediatos:** Negociação com 2 PSPs parceiros para acordo sub-adquirência; spike técnico de 5 dias para validar latência P99 com mock BACEN.

---

*Ideation criada em: 2026-05-04 | Autor: Product Team EPIC-0077 (Piloto 001) | Status: Rascunho*
