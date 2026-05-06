# Sistema de Agendamento Inteligente para Clínicas de Saúde

> **Template:** _TEMPLATE-IDEATION.md v1.0  
> **Ideation ID:** pilot-ideation-002-healthcare-scheduling  
> **Status:** Rascunho (Piloto de Validação)

---

## 1. Visão & Escopo

**Título:** ClinicaFlow — Agendamento Inteligente com Redução de No-Show para Clínicas

**Visão:**

Clínicas de saúde de médio porte (5-30 consultórios) perdem em média 18% da receita por cancelamentos e no-shows de última hora. As soluções atuais são ou muito simples (Google Calendar) sem lembretes proativos, ou muito complexas (HIS completos) com implementação de 6+ meses.

Esta ideação propõe um sistema de agendamento especializado em saúde que combina agendamento online com pacientes, lembretes inteligentes (WhatsApp + SMS + email), lista de espera automatizada para reaproveitamento de slots cancelados, e analytics de no-show por médico/especialidade.

**Público-Alvo Principal:** Gerente de Recepção ou Sócio-Clínico de clínica médica de especialidade (5-30 consultórios, 200-2.000 pacientes/mês).

**Escopo desta ideação:** [x] Produto novo

---

## 2. Stakeholders & Personas

| Persona | Papel | Decisões Chave | Frequência de Uso |
| :--- | :--- | :--- | :--- |
| Gerente de Recepção | Operadora principal | Gerenciar agenda, confirmar consultas, acionar lista de espera | Diário (8h/dia) |
| Médico / Especialista | Usuário da agenda | Ver agenda do dia, confirmar disponibilidade, bloquear slots | Diário |
| Paciente | Usuário final | Agendar, remarcar, receber lembretes | Por consulta |
| Sócio-Clínico / Administrador | Comprador + Aprovador | Aprovar assinatura, analisar relatórios de receita e no-show | Semanal |

**Stakeholders internos:** Produto, Engenharia, CS (onboarding de clínicas), Suporte.

**Decisores:** Sócio-Clínico (aprovação de budget e sistema); Gerente de Recepção (aceitação operacional — influenciador chave).

---

## 3. Requisitos de Negócio

| ID | Tipo | Requisito | Prioridade | Verificável? |
| :--- | :--- | :--- | :--- | :--- |
| BIZ-001 | Funcional | Agendamento online para pacientes (link público por médico/especialidade, sem login obrigatório) | Alta | Sim — testes E2E de agendamento público |
| BIZ-002 | Funcional | Lembretes automáticos: WhatsApp (preferência), SMS (fallback), email — configuráveis por clínica | Alta | Sim — testes de integração com Twilio/Meta |
| BIZ-003 | Funcional | Lista de espera: quando consulta é cancelada, próximo da lista é notificado automaticamente em < 5min | Alta | Sim — testes de evento de cancelamento |
| BIZ-004 | Funcional | Bloqueio de agenda por médico: férias, horários, procedimentos longos | Média | Sim — testes de conflito de agenda |
| BIZ-005 | Não-funcional | LGPD: dados de saúde são dados sensíveis — consentimento, anonimização para analytics | Alta | Sim — audit de dados com DPO |
| BIZ-006 | Funcional | Dashboard de no-show: taxa por médico, especialidade, horário, e tendência semanal | Média | Sim — testes de cálculo de métricas |
| BIZ-007 | Não-funcional | Integração com Google Calendar para sincronização bidirecional da agenda do médico | Baixa | Sim — testes de integração Google API |

---

## 4. Restrições & Assunções

### 4.1 Restrições

| Tipo | Descrição | Impacto |
| :--- | :--- | :--- |
| Regulatório | LGPD: dados de saúde são categoria especial — requerem DPO designado e RIPD | Aumenta custo de compliance; DPO pode ser terceirizado |
| Regulatório | CFM (Conselho Federal de Medicina): prontuário eletrônico tem requisitos específicos — não tocamos prontuário | Limitar escopo a agendamento; NÃO incluir informações clínicas no sistema |
| Técnico | WhatsApp Business API requer aprovação Meta (4-8 semanas); SMS é fallback imediato | MVP usa SMS + email; WhatsApp integrado na Fase 2 |
| Timeline | Clínicas têm baixa tolerância para downtime — migração deve ser gradual | Importação de agendas existentes; período de operação paralela de 30 dias |

### 4.2 Assunções

- [x] Clínicas têm smartphone/tablet disponível para recepcionistas (não requerem hardware especializado)
- [x] Pacientes têm WhatsApp ou celular para SMS (cobertura > 95% no Brasil)
- [x] Gerente de Recepção aceita treinamento de 2h para novo sistema
- [ ] Google Calendar é o calendário principal dos médicos (a validar — alguns usam Outlook)

---

## 5. Critérios de Sucesso

| Métrica | Baseline Atual | Meta | Prazo | Método de Medição |
| :--- | :--- | :--- | :--- | :--- |
| Taxa de no-show | 18% (média setor) | < 8% (-10pp) | 6 meses | Dashboard de no-show (confirmações vs comparecimentos) |
| Slots reaproveitados via lista de espera | 0% (processo manual atual) | 60% dos cancelamentos com < 24h aviso | 3 meses | Relatório de lista de espera |
| Tempo médio de agendamento (recepcionista) | 4 min (telefone) | < 1 min (online self-service) | 3 meses | Analytics de duração da sessão de agendamento |
| NPS do paciente | N/A (sem sistema formal) | > 70 | 6 meses | Survey pós-consulta via WhatsApp |

**OKR Vinculado:** OKR 2026-Q3: "Adquirir 20 clínicas com MRR médio de R$800/clínica"

**Critério de sucesso mínimo (MVP):** 3 clínicas piloto com taxa de no-show reduzida em 5pp em 60 dias.

---

## 6. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação | Responsável |
| :--- | :--- | :--- | :--- | :--- |
| Recepcionistas resistem à mudança (preferem telefone) | Alta | Alto — adoção zero se não há adesão operacional | Treinamento presencial de 2h com "campeão interno" (recepcionista que lidera adoção); suporte CS em 30 dias pós-implementação | CS |
| Aprovação WhatsApp Business API atrasada (> 8 semanas) | Média | Médio — MVP depende de SMS (menor taxa de abertura) | SMS como canal primário no MVP; WhatsApp como upgrade; comunicar expectativa de timeline para clínicas | Produto |
| Dados de pacientes expostos (breach LGPD) | Baixa | Crítico — multa ANPD + dano reputacional | Criptografia AES-256; no logging de dados de saúde; pen test antes de go-live; DPO externo | Segurança + Jurídico |
| Médico bloqueia implantação por não querer "sistema novo" | Média | Médio — clínica não consegue implantar sem adesão do corpo clínico | Demonstração rápida (30min) para médicos mostrando que não muda a rotina deles; mantém Google Calendar como "fonte de verdade" | CS + Produto |

**Plano de contingência:** Se adoção < 2 clínicas em 60 dias, investigar blocker com entrevistas qualitativas e iterar no onboarding antes de escalar marketing.

---

## 7. Roadmap

| Fase | Descrição | Duração Estimada | Milestone | Entregável |
| :--- | :--- | :--- | :--- | :--- |
| Fase 1 — MVP | Agendamento online + lembretes SMS/email + lista de espera + dashboard básico de no-show | 10 semanas | 2026-07-30 | 3 clínicas piloto |
| Fase 2 — Expansão | WhatsApp Business + Google Calendar sync + relatórios avançados + app mobile para médico | 8 semanas | 2026-10-01 | 15 clínicas ativas |
| Fase 3 — Escala | Integração com planos de saúde (Unimed, Bradesco Saúde) + telemedicina básica + prontuário parceiro | 12 semanas | 2027-01-15 | 50+ clínicas |

**Dependências externas:** Aprovação WhatsApp Business Meta (4-8 semanas); parceria com DPO externo para compliance LGPD (2 semanas).

**Próximos passos imediatos:** Entrevistas com 5 gerentes de recepção de clínicas de médio porte; avaliação jurídica de requisitos LGPD para dados de saúde.

---

*Ideation criada em: 2026-05-04 | Autor: Product Team EPIC-0077 (Piloto 002) | Status: Rascunho*
