# Plataforma de Gestão de Assinaturas para SaaS B2B

> **Template:** _TEMPLATE-IDEATION.md v1.0  
> **Ideation ID:** ideation-saas-subscription-mgmt-2026  
> **Status:** Aprovada

---

## 1. Visão & Escopo

**Título:** SubscribeOps — Gestão Unificada de Assinaturas e Revenue Expansion para SaaS B2B

**Visão:**

SaaS B2B com planos escalonados enfrentam um problema crítico de revenue leakage: upgrades que deveriam ser automáticos ficam presos em aprovações manuais, downgrades acontecem sem análise de churn risk, e add-ons são vendidos reativamente (quando o cliente pede) em vez de proativamente (quando o sistema detecta oportunidade). O resultado: 15-25% de MRR potencial não é capturado (análise interna de 50 SaaS BR 2025).

Esta ideação propõe uma plataforma de gestão de assinaturas que combina billing automation (integrada com Stripe/PagarME) com um motor de expansion revenue: detecta quando clientes estão prontos para upsell (via sinais de uso), orquestra o processo de aprovação, e mede o impacto em Net Revenue Retention.

O produto é voltado para SaaS B2B no Brasil com 100-1.000 clientes ativos, MRR de R$200K-R$2M, e times de CS com 2-10 pessoas que atualmente gerenciam assinaturas via spreadsheet + Stripe dashboard manual.

**Público-Alvo Principal:** Head of Customer Success em SaaS B2B (equipe de 3-8 CSMs), responsável por churn, upsell e NRR.

**Escopo desta ideação:** [x] Produto novo

---

## 2. Stakeholders & Personas

| Persona | Papel | Decisões Chave | Frequência de Uso |
| :--- | :--- | :--- | :--- |
| Head of CS | Comprador + Operador principal | Configura estratégias de expansão, aprova/rejeita upsells, analisa NRR | Diário |
| CSM (Customer Success Manager) | Operador | Executa playbooks de expansão, contata clientes para upgrades | Diário |
| CFO / VP Finance | Aprovador | Aprova billing model, reconciliação com contabilidade | Mensal |
| Engenheiro de Integração | Implementador | Integra com Stripe, configura webhooks, mapeia planos | Uma vez (setup) |
| Cliente SaaS (usuário do produto gerenciado) | Usuário indireto | Aprova upgrades do próprio plano | Por upgrade |

**Stakeholders internos:** Produto, Engenharia, Finance, CS da empresa que adopta o produto.

**Decisores:** Head of CS (decisão técnica e operacional); CFO (aprovação de budget).

---

## 3. Requisitos de Negócio

| ID | Tipo | Requisito | Prioridade | Verificável? |
| :--- | :--- | :--- | :--- | :--- |
| BIZ-001 | Funcional | Sincronização bidirecional com Stripe: mudanças de assinatura refletem em < 30s | Alta | Sim — testes de integração com Stripe test mode |
| BIZ-002 | Funcional | Motor de expansão detecta clientes em threshold de upgrade (ex: uso > 80% do limite do plano por 7 dias) | Alta | Sim — testes unitários com dados de uso simulados |
| BIZ-003 | Funcional | Playbook de upsell automatizado: sequência de touchpoints (email, in-app, CSM task) configurável por segmento | Média | Sim — testes E2E de playbook |
| BIZ-004 | Não-funcional | Dashboard de NRR/GRR/Churn atualizado em < 5 min (refresh rate aceitável para análise estratégica) | Média | Sim — testes de latência de pipeline |
| BIZ-005 | Funcional | Alerta de churn risk: detecção de downgrades não planejados com scoring de propensidade | Alta | Sim — modelo de scoring validado com dados históricos |
| BIZ-006 | Não-funcional | Auditoria completa de mudanças de assinatura (quem aprovou, quando, qual justificativa) | Alta | Sim — testes de audit log |
| BIZ-007 | Funcional | Multi-moeda: suporte a BRL e USD com conversão automática para relatórios consolidados | Média | Sim — testes com assinaturas em ambas as moedas |

---

## 4. Restrições & Assunções

### 4.1 Restrições

| Tipo | Descrição | Impacto |
| :--- | :--- | :--- |
| Regulatório | LGPD: dados de uso de clientes dos SaaS são dados de terceiros — requerem DPA (Data Processing Agreement) | Cada cliente precisa assinar DPA antes de sincronizar dados de uso |
| Técnico | Dependência crítica do Stripe: se Stripe API muda breaking change, sincronização quebra | Manter versão fixada da API Stripe; monitorar changelog com alerta automático |
| Técnico | Modelos de assinatura variam muito entre clientes (per-seat, usage-based, flat-rate) — difícil normalizar | MVP cobre per-seat e flat-rate; usage-based em Fase 2 |
| Orçamentário | Custo de infra ≤ R$3.000/mês para 30 SaaS clientes (cada com até 500 assinaturas) | Limita processamento em batch; sem ML em tempo real no MVP |

### 4.2 Assunções

- [x] Clientes usam Stripe como gateway principal (70% do mercado SaaS BR)
- [x] Dados de uso do produto dos clientes são acessíveis via API ou webhook (a validar por cliente)
- [ ] Clientes aceitam assinar DPA para compartilhar dados de uso (risco jurídico a validar)
- [x] Head of CS tem autonomia para aprovar upgrades < R$10K/mês sem aprovação do CFO

---

## 5. Critérios de Sucesso

| Métrica | Baseline Atual | Meta | Prazo | Método de Medição |
| :--- | :--- | :--- | :--- | :--- |
| Net Revenue Retention (NRR) dos clientes que adotam | 95% (média setor) | 115% (upsell supera churn) | 6 meses | Relatório NRR mensal da plataforma |
| Tempo médio para aprovar upgrade | 5 dias (manual, via email/spreadsheet) | < 4 horas (automatizado) | 3 meses | Audit log: timestamp criação → aprovação |
| Revenue expansion capturado (vs. baseline manual) | R$0 adicional capturado proativamente | R$30K MRR incremental por cliente médio | 6 meses | Comparação MRR antes/após adoção |
| Churn prevenido | 0% de churns identificados proativamente | 30% dos churns previstos evitados com intervenção | 6 meses | Clientes em churn risk que renovaram após touchpoint |

**OKR Vinculado:** OKR 2026-H2: "Lançar produto de revenue operations com NRR de clientes piloto > 110%"

**Critério de sucesso mínimo (MVP):** 2 clientes piloto com NRR aumentado em 10pp em 90 dias.

---

## 6. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação | Responsável |
| :--- | :--- | :--- | :--- | :--- |
| Clientes não compartilham dados de uso (resistência ao DPA) | Média | Alto — motor de expansão fica cego sem dados de uso | Oferecer alternativa: manual input de métricas de uso via CSV; feature roadmap DPA simplificado | CS + Jurídico |
| Modelos de billing muito customizados (per-unit, hybrid) não suportados no MVP | Alta | Médio — exclui clientes com billing complexo | Pré-qualificar clientes piloto: somente per-seat ou flat-rate; comunicar limitação claramente | Produto |
| Integração Stripe envolve dados financeiros sensíveis — risco de vazamento | Baixa | Crítico | Stripe Connect para isolamento de dados; criptografia AES-256 em repouso; pen test antes de go-live | Segurança + Engenharia |
| Head of CS não adota o produto (prefere planilha conhecida) | Média | Alto — adoção zero = 0 NRR | Onboarding white-glove nas primeiras 30 dias; migration wizard para importar planilhas existentes | CS |

**Plano de contingência:** Se adoção < 2 clientes pagantes em 60 dias, reduzimos escopo para "dashboard de NRR" sem automação de playbook (menor complexidade, proposição de valor mais clara).

---

## 7. Roadmap

| Fase | Descrição | Duração Estimada | Milestone | Entregável |
| :--- | :--- | :--- | :--- | :--- |
| Fase 1 — MVP | Sincronização Stripe + motor de expansão baseado em thresholds + playbook básico (email + CSM task) + dashboard NRR/GRR | 10 semanas | 2026-07-15 | Beta para 2 clientes pagantes |
| Fase 2 — Expansão | Churn scoring ML + playbook avançado (in-app, Slack) + usage-based billing + multi-gateway (PagarME) | 8 semanas | 2026-09-15 | GA para 10 clientes |
| Fase 3 — Escala | Integrações CRM (HubSpot, Salesforce) + revenue forecasting + benchmarks por vertical | 10 semanas | 2026-12-01 | 30+ clientes, feature parity com Chargebee BR |

**Dependências externas:** Stripe API stability (monitorado via status page); aprovação jurídica do DPA template (4 semanas com advogados).

**Próximos passos imediatos:** Entrevistas de descoberta com 8 Heads of CS (identificados via LinkedIn); spike técnico de Stripe Connect para avaliar isolamento de dados por cliente.

---

*Ideation criada em: 2026-05-04 | Autor: Product Team EPIC-0077 | Status: Aprovada*
