# Capability: {{CAPABILITY_NAME}}

**Product:** {{PRODUCT_NAME}}  
**Capability ID:** `{{CAPABILITY_ID}}`  
**Version:** {{VERSION}}  
**Status:** {{STATUS}}

---

## 1. Definição & Escopo

### 1.1 Propósito

{{CAPABILITY_PURPOSE}}

### 1.2 Escopo

| Incluído | Excluído |
| :--- | :--- |
| {{INCLUDED_1}} | {{EXCLUDED_1}} |
| {{INCLUDED_2}} | {{EXCLUDED_2}} |

### 1.3 Decomposição do Produto

Esta capability representa um dos {{TOTAL_CAPABILITIES}} pilares do produto `{{PRODUCT_NAME}}`.

---

## 2. RNFs Herdadas (no-relax override)

> **Regra:** Uma Capability NÃO pode relaxar RNFs marcadas como `mandatory=true` sem aprovação executiva.
> Overrides de RNFs não-mandatory requerem justificação obrigatória.

| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| PERFORMANCE | {{PERF_ORIGINAL}} | {{PERF_NORELAX}} | {{PERF_OVERRIDE}} | {{PERF_JUSTIFICATION}} | {{PERF_APPROVAL}} | {{PERF_APPROVER}} |
| SCALABILITY | {{SCALABILITY_ORIGINAL}} | {{SCALABILITY_NORELAX}} | — | — | — | — |
| RELIABILITY | {{RELIABILITY_ORIGINAL}} | {{RELIABILITY_NORELAX}} | — | — | — | — |
| SECURITY | {{SECURITY_ORIGINAL}} | true | — | — | — | — |
| COMPLIANCE | {{COMPLIANCE_ORIGINAL}} | true | — | — | — | — |
| OBSERVABILITY | {{OBSERVABILITY_ORIGINAL}} | {{OBSERVABILITY_NORELAX}} | — | — | — | — |

> **Regra Inegociável:** `SECURITY` e `COMPLIANCE` são sempre `no-relax=true`. Override é bloqueado pelo validador de domínio.

---

## 3. Requisitos Funcionais da Capability

| RF-ID | Descrição | Critério de Aceite | Prioridade |
| :--- | :--- | :--- | :--- |
| RF-{{CAP_ID}}-001 | {{RF_001_DESC}} | {{RF_001_AC}} | Alta |
| RF-{{CAP_ID}}-002 | {{RF_002_DESC}} | {{RF_002_AC}} | Média |
| RF-{{CAP_ID}}-003 | {{RF_003_DESC}} | {{RF_003_AC}} | Baixa |

---

## 4. Interfaces & Portas

### 4.1 Portas de Entrada (Inbound)

| Porta | Tipo | Contrato |
| :--- | :--- | :--- |
| {{INBOUND_PORT_1}} | {{INBOUND_TYPE_1}} | {{INBOUND_CONTRACT_1}} |

### 4.2 Portas de Saída (Outbound)

| Porta | Tipo | Destino |
| :--- | :--- | :--- |
| {{OUTBOUND_PORT_1}} | {{OUTBOUND_TYPE_1}} | {{OUTBOUND_DEST_1}} |

### 4.3 Eventos Produzidos / Consumidos

| Evento | Tipo | Schema |
| :--- | :--- | :--- |
| `{{EVENT_1}}` | produced/consumed | `{{EVENT_1_SCHEMA}}` |

---

## 5. Constraints Técnicos Locais

| Constraint | Valor | Motivo |
| :--- | :--- | :--- |
| Latência interna máxima | {{MAX_LATENCY}} | SLA da capability |
| Tamanho máximo de payload | {{MAX_PAYLOAD}} | Limite de memória |
| Dependências externas | {{EXTERNAL_DEPS}} | Disponibilidade requerida |
| Tecnologias proibidas | {{FORBIDDEN_TECH}} | Policy de plataforma |

---

## 6. Plano de Testes

| Tipo | Cobertura | Ferramenta |
| :--- | :--- | :--- |
| Unit | ≥ 95% linha, ≥ 90% branch | JUnit 5 / AssertJ |
| Integração | Happy path + error path | JUnit IT |
| Contract | {{CONTRACT_TOOL}} | {{CONTRACT_SCOPE}} |
| Performance | {{PERF_TOOL}} | Validar SLA da capability |

---

## 7. Roadmap de Entrega

| Sprint | Entregável | Dependência |
| :--- | :--- | :--- |
| {{SPRINT_1}} | {{DELIVERABLE_1}} | — |
| {{SPRINT_2}} | {{DELIVERABLE_2}} | Sprint {{SPRINT_1}} |
| {{SPRINT_3}} | {{DELIVERABLE_3}} | Sprint {{SPRINT_2}} |
