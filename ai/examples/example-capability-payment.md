# Capability: Payment Processing

**Product:** ContractOS B2B  
**Capability ID:** `payment`  
**Version:** 1.0  
**Status:** Planejada

---

## 1. Definição & Escopo

### 1.1 Propósito

Processar pagamentos de assinaturas e cobrança por uso na plataforma ContractOS B2B. Suporta boleto bancário, cartão de crédito (PCI-DSS Nível 1), e Pix. Integra com gateway de pagamento externo e emite notas fiscais eletrônicas (NF-e).

### 1.2 Escopo

| Incluído | Excluído |
| :--- | :--- |
| Cobrança de assinaturas mensais/anuais | Pagamentos entre usuários (P2P) |
| Boleto, cartão de crédito, Pix | Criptomoedas |
| Emissão de NF-e | Gestão de planos e pricing |
| Estorno e reembolso | Crédito rotativo / parcelamento |
| Webhook de confirmação de pagamento | Conciliação bancária manual |

### 1.3 Decomposição do Produto

Esta capability representa 1 de 5 pillars do produto `ContractOS B2B`.

---

## 2. RNFs Herdadas (no-relax override)

| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| PERFORMANCE | P99 < 3s para geração de PDF 30pg | true | — | — | — | — |
| SCALABILITY | Suportar 10x pico sem degradação | true | — | — | — | — |
| RELIABILITY | 99.9% uptime por mês | false | 99.5% uptime por mês | Gateway externo (Stripe) tem SLA 99.9%; downtime do gateway impacta a capability; SLA realista dado a dependência | pending | cfo@contractos.com.br |
| SECURITY | ICP-Brasil Nível 2 + MFA obrigatório | true | — | — | — | — |
| COMPLIANCE | LGPD Art.46 + Lei 14.063/2020 | true | — | — | — | — |
| OBSERVABILITY | OpenTelemetry trace_id em todos os logs | true | — | — | — | — |

---

## 3. Requisitos Funcionais da Capability

| RF-ID | Descrição | Critério de Aceite | Prioridade |
| :--- | :--- | :--- | :--- |
| RF-PAY-001 | Cobrança via boleto bancário | Boleto gerado com código de barras válido, vence em D+3 | Alta |
| RF-PAY-002 | Cobrança via Pix instantâneo | QR Code gerado; confirmação em ≤ 5s após pagamento | Alta |
| RF-PAY-003 | Cobrança via cartão de crédito | Tokenização via gateway; número do cartão nunca armazenado | Alta |
| RF-PAY-004 | Emissão automática de NF-e | NF-e emitida em ≤ 30min após confirmação do pagamento | Média |
| RF-PAY-005 | Estorno parcial e total | Estorno processado em ≤ 2 dias úteis | Baixa |

---

## 4. Interfaces & Portas

### 4.1 Portas de Entrada (Inbound)

| Porta | Tipo | Contrato |
| :--- | :--- | :--- |
| `PaymentPort` | REST | `POST /payments`, `GET /payments/{id}`, `POST /payments/{id}/refund` |
| `WebhookPort` | REST | `POST /webhooks/payment-gateway` |

### 4.2 Portas de Saída (Outbound)

| Porta | Tipo | Destino |
| :--- | :--- | :--- |
| `PaymentGatewayPort` | HTTPS | Stripe API v3 |
| `NFEPort` | SOAP/REST | SEFAZ — emissão NF-e |
| `PaymentRepositoryPort` | JPA / JDBC | PostgreSQL — tabela `payments` |

### 4.3 Eventos Produzidos / Consumidos

| Evento | Tipo | Schema |
| :--- | :--- | :--- |
| `PaymentConfirmed` | produced | `{paymentId, amount, method, timestamp}` |
| `PaymentFailed` | produced | `{paymentId, reason, timestamp}` |
| `RefundProcessed` | produced | `{paymentId, refundAmount, timestamp}` |

---

## 5. Constraints Técnicos Locais

| Constraint | Valor | Motivo |
| :--- | :--- | :--- |
| Latência interna máxima | 200ms (excluindo gateway) | Resposta ao usuário percebida como instantânea |
| Tamanho máximo de payload | 8KB | Dados de pagamento + metadata |
| Dependências externas | Stripe, SEFAZ, PostgreSQL 16+ | Alta disponibilidade requerida |
| Tecnologias proibidas | Armazenamento de PAN/CVV | PCI-DSS — dados de cartão via tokenização apenas |

---

## 6. Plano de Testes

| Tipo | Cobertura | Ferramenta |
| :--- | :--- | :--- |
| Unit | ≥ 95% linha, ≥ 90% branch | JUnit 5 / AssertJ |
| Integração | Happy path + error path + webhook | JUnit IT + Stripe Test Mode |
| Contract | OpenAPI diff | 0 breaking changes |
| Performance | k6 | P99 < 3s sob carga de 500 rps |

---

## 7. Roadmap de Entrega

| Sprint | Entregável | Dependência |
| :--- | :--- | :--- |
| Sprint 1 | Boleto bancário + webhook confirmação | — |
| Sprint 2 | Pix + NF-e automática | Sprint 1 |
| Sprint 3 | Cartão de crédito (PCI-DSS) + estorno | Sprint 2 |
