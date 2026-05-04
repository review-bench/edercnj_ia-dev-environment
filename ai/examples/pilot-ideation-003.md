# Plataforma de Aprendizado Adaptativo para EdTechs

> **Template:** _TEMPLATE-IDEATION.md v1.0  
> **Ideation ID:** pilot-ideation-003-edtech-adaptive-learning  
> **Status:** Rascunho (Piloto de Validação)

---

## 1. Visão & Escopo

**Título:** LearnPath — Engine de Aprendizado Adaptativo para Plataformas EdTech B2B

**Visão:**

Plataformas EdTech B2B (treinamento corporativo, cursos técnicos) perdem engajamento rapidamente: 70% dos alunos não completam cursos online (taxa de abandono média do setor). O problema central é conteúdo linear que ignora o ritmo individual: alunos avançados ficam entediados em conteúdo básico, iniciantes ficam sobrecarregados com ritmo acelerado.

Esta ideação propõe um engine de aprendizado adaptativo como serviço (API + SDK) que EdTechs integram em suas plataformas: analisa padrão de respostas, tempo por questão, e sequências de erro para personalizar a trilha de aprendizado em tempo real. O engine não substitui o conteúdo da EdTech — personaliza a *sequência* de entrega desse conteúdo.

**Público-Alvo Principal:** CTO ou Head de Produto de EdTech B2B com 500-50.000 alunos ativos na plataforma e problema comprovado de conclusão de cursos.

**Escopo desta ideação:** [x] Produto novo

---

## 2. Stakeholders & Personas

| Persona | Papel | Decisões Chave | Frequência de Uso |
| :--- | :--- | :--- | :--- |
| CTO EdTech | Comprador técnico | Avaliar arquitetura de integração, modelo de pricing por MAU, SLA | Uma vez (decisão de compra) |
| Head de Produto EdTech | Co-comprador | Validar que personalização melhora métricas de conclusão | Mensal (revisão de métricas) |
| Engenheiro Backend EdTech | Implementador | Integrar API, configurar eventos de tracking, testar sandbox | 2 semanas (integração) |
| Aluno final (B2C da EdTech) | Usuário indireto | Experimenta trilha personalizada sem saber da camada adaptativa | Por sessão de estudo |
| L&D Manager (cliente corporativo da EdTech) | Stakeholder indireto | Avalia se taxa de conclusão melhorou; apresenta ROI para RH | Mensal |

**Stakeholders internos:** Produto, Engenharia ML, Data Science, CS, Vendas.

**Decisores:** CTO (decisão técnica de integração); Head de Produto (validação de valor); CFO EdTech (aprovação de budget).

---

## 3. Requisitos de Negócio

| ID | Tipo | Requisito | Prioridade | Verificável? |
| :--- | :--- | :--- | :--- | :--- |
| BIZ-001 | Funcional | Engine processa evento de resposta do aluno e retorna próximo conteúdo recomendado em < 100ms P99 | Alta | Sim — load tests com k6 |
| BIZ-002 | Funcional | Modelo adaptativo considera: taxa de acerto por tópico, tempo por questão, padrões de erro consecutivo | Alta | Sim — testes unitários do modelo de scoring |
| BIZ-003 | Funcional | SDK disponível em JavaScript/TypeScript e Python — integração em < 1 dia para devs experientes | Alta | Sim — dogfooding interno em plataforma de teste |
| BIZ-004 | Funcional | Dashboard para EdTech: taxa de conclusão antes/depois por coorte, tempo médio de estudo, módulos com maior abandono | Média | Sim — testes de cálculo de métricas de coorte |
| BIZ-005 | Não-funcional | API com 99.9% disponibilidade — aluno nunca vê erro por indisponibilidade do engine (fallback para trilha linear) | Alta | Sim — chaos tests + fallback automático |
| BIZ-006 | Funcional | Suporte a múltiplos modelos de conteúdo: vídeo, quiz, texto, exercício prático — engine é agnóstico ao tipo | Média | Sim — testes com 4 tipos de conteúdo |
| BIZ-007 | Não-funcional | Modelo adaptativo melhora após 50+ respostas por aluno (cold start aceitável nos primeiros 10 eventos) | Alta | Sim — A/B test em 30 dias com grupo controle |

---

## 4. Restrições & Assunções

### 4.1 Restrições

| Tipo | Descrição | Impacto |
| :--- | :--- | :--- |
| Técnico | Engine não substitui plataforma de conteúdo da EdTech — integra via API de eventos e retorna sequência | Escopo restrito: engine recebe evento, retorna próximo_item_id; EdTech renderiza conteúdo |
| Regulatório | LGPD: dados de aprendizado são dados pessoais — histórico de respostas de aluno identificável | Pseudonimização obrigatória; aluno_id como UUID opaco; sem correlação com email/CPF no engine |
| Orçamentário | Custo de ML inference (GPU) deve ser < R$0,001/evento para viabilidade econômica a R$5/MAU | Modelo leve (decision tree ou logistic regression) no MVP; LLM apenas na pesquisa avançada |
| Técnico | EdTechs têm stacks variados (React, Vue, Angular, mobile nativo) — SDK deve ser framework-agnostic | SDK JavaScript puro (vanilla); sem dependências de framework |

### 4.2 Assunções

- [x] EdTechs têm eventos de interação do aluno já coletados (ou podem implementar em < 1 semana)
- [x] Taxa de conclusão de cursos é uma métrica monitorada pelas EdTechs (senão, não percebem o valor)
- [x] CTOs de EdTech aceitam modelo de pricing por MAU (aluno ativo/mês)
- [ ] Modelo adaptativo leve (< 10ms de inferência) é suficientemente eficaz — a validar com experimento

---

## 5. Critérios de Sucesso

| Métrica | Baseline Atual | Meta | Prazo | Método de Medição |
| :--- | :--- | :--- | :--- | :--- |
| Taxa de conclusão de cursos (clientes piloto) | 30% (baseline EdTech típica) | 50% (+20pp) | 6 meses | A/B test: grupo com engine vs controle (trilha linear) |
| Latência de recomendação P99 | N/A (feature nova) | < 100ms | Antes de GA | Load test com k6 (1.000 eventos/s) |
| Tempo de integração por EdTech | N/A | < 2 dias (dev experiente + sandbox) | 3 meses | Onboarding logs (primeiro evento enviado ao engine) |
| Retenção de alunos (dias 7 e 30) | 15% no dia 30 (baseline) | 35% no dia 30 (+20pp) | 6 meses | Coorte com/sem engine no dashboard EdTech |

**OKR Vinculado:** OKR 2026-H2: "Provar que engine adaptativo aumenta taxa de conclusão em 15pp em 3 EdTechs piloto"

**Critério de sucesso mínimo (MVP):** 2 EdTechs integradas, A/B test mostrando +10pp de conclusão em 30 dias.

---

## 6. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação | Responsável |
| :--- | :--- | :--- | :--- | :--- |
| Modelo adaptativo não melhora conclusão (hipótese inválida) | Média | Alto — proposta de valor central falha | A/B test rigoroso antes de investir em escala; critério de sucesso claro (10pp em 30 dias) | Data Science + Produto |
| EdTech não tem eventos de interação estruturados (sem tracking implementado) | Alta | Médio — engine fica cego sem dados | Fornecer SDK de tracking como parte do pacote; onboarding inclui implementação de eventos em 1 dia | Engenharia + CS |
| Custo de ML inference aumenta com escala (GPU caro) | Média | Alto — margem negativa acima de 10K MAU | MVP com modelo leve (sem GPU); validar hipótese de negócio antes de otimizar infraestrutura ML | Engenharia + Finanças |
| CTO EdTech prefere construir in-house após ver a ideia | Alta | Médio — perda de cliente potencial | Velocidade de entrega (6 meses vs 18 meses in-house); dados de múltiplas EdTechs melhoram modelo (network effect) | Vendas + Produto |

**Plano de contingência:** Se A/B test não mostra 10pp de melhora em 45 dias, investigar qual tipo de conteúdo/aluno se beneficia mais da personalização e estreitar ICP (Ideal Customer Profile) antes de iterar no modelo.

---

## 7. Roadmap

| Fase | Descrição | Duração Estimada | Milestone | Entregável |
| :--- | :--- | :--- | :--- | :--- |
| Fase 1 — MVP | API de eventos + modelo adaptativo leve (decision tree) + SDK JS/Python + dashboard básico | 8 semanas | 2026-07-30 | 2 EdTechs piloto em A/B test |
| Fase 2 — Validação | A/B test results (30 dias) + iteração de modelo + dashboard avançado + SDK mobile (React Native) | 6 semanas | 2026-09-15 | Prova de valor documentada + 5 EdTechs |
| Fase 3 — Escala | Modelo ML avançado (neural network lightweight) + multi-tenant isolation + SLA 99.95% contratual | 10 semanas | 2026-12-01 | 20+ EdTechs, network effect do modelo multi-cliente |

**Dependências externas:** Acesso a dados de treino de alunos reais de EdTechs parceiras (para calibrar modelo antes de lançar); parceria com 1 EdTech para dogfooding em produção.

**Próximos passos imediatos:** Recrutar 2 EdTechs como parceiras de co-desenvolvimento (acesso a dados anonimizados + feedback de produto); protótipo de modelo adaptativo em Python com dados sintéticos.

---

*Ideation criada em: 2026-05-04 | Autor: Product Team EPIC-0077 (Piloto 003) | Status: Rascunho*
