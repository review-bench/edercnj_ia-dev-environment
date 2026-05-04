# Capability: Authentication & Authorization

**Product:** ContractOS B2B  
**Capability ID:** `auth`  
**Version:** 1.0  
**Status:** Planejada

---

## 1. Definição & Escopo

### 1.1 Propósito

Gerenciar identidade, autenticação (quem é você?) e autorização (o que pode fazer?) para todos os usuários e sistemas da plataforma ContractOS. Inclui SSO empresarial via SAML 2.0 e OAuth 2.0, MFA obrigatório para usuários com papel de Gestor ou superior, e controle de acesso baseado em papéis (RBAC).

### 1.2 Escopo

| Incluído | Excluído |
| :--- | :--- |
| Login / Logout de usuários humanos | Autenticação de dispositivos IoT |
| SSO via SAML 2.0 e OAuth 2.0 | Gestão de licenças de usuários |
| MFA (TOTP + WebAuthn) | Billing e planos de assinatura |
| RBAC (5 papéis: Viewer, Editor, Gestor, Admin, TI) | Analytics de comportamento |
| Auditoria de sessões (rastro completo) | Gestão de chaves criptográficas (HSM) |

### 1.3 Decomposição do Produto

Esta capability representa 1 de 5 pillars do produto `ContractOS B2B`.

---

## 2. RNFs Herdadas (no-relax override)

| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| PERFORMANCE | P99 < 3s para geração de PDF 30pg | false | P99 < 500ms para login | Auth é caminho crítico pré-qualquer operação; SLA mais restrito é necessário | approved | cto@contractos.com.br |
| SCALABILITY | Suportar 10x pico sem degradação | true | — | — | — | — |
| RELIABILITY | 99.9% uptime por mês | false | 99.95% uptime por mês | Auth indisponível = plataforma indisponível; SLA mais restrito necessário | approved | cto@contractos.com.br |
| SECURITY | ICP-Brasil Nível 2 + MFA obrigatório para Gestores | true | — | — | — | — |
| COMPLIANCE | LGPD Art.46 + Lei 14.063/2020 | true | — | — | — | — |
| OBSERVABILITY | OpenTelemetry trace_id em todos os logs | true | — | — | — | — |

---

## 3. Requisitos Funcionais da Capability

| RF-ID | Descrição | Critério de Aceite | Prioridade |
| :--- | :--- | :--- | :--- |
| RF-AUTH-001 | Login com email/senha + MFA | Usuário com MFA ativo não consegue login sem 2º fator | Alta |
| RF-AUTH-002 | SSO via SAML 2.0 para empresas | IdP corporativo consegue logar usuário sem criar senha local | Alta |
| RF-AUTH-003 | RBAC — controle de papéis | Viewer não consegue criar/editar contratos | Alta |
| RF-AUTH-004 | Auditoria de sessões | Cada login gera evento auditável com IP, device, timestamp | Média |
| RF-AUTH-005 | Revogação de sessão | Admin consegue revogar sessão de qualquer usuário em ≤ 5s | Média |

---

## 4. Interfaces & Portas

### 4.1 Portas de Entrada (Inbound)

| Porta | Tipo | Contrato |
| :--- | :--- | :--- |
| `AuthenticationPort` | REST + gRPC | `POST /auth/login`, `POST /auth/saml/callback`, `POST /auth/mfa/verify` |
| `AuthorizationPort` | gRPC | `CheckPermission(userId, resource, action) → bool` |

### 4.2 Portas de Saída (Outbound)

| Porta | Tipo | Destino |
| :--- | :--- | :--- |
| `UserRepositoryPort` | JPA / JDBC | PostgreSQL — tabela `auth_users` |
| `AuditEventPort` | Event | Kafka — topic `auth.audit.v1` |
| `MFAProviderPort` | HTTPS | TOTP / WebAuthn provider |

### 4.3 Eventos Produzidos / Consumidos

| Evento | Tipo | Schema |
| :--- | :--- | :--- |
| `UserLoggedIn` | produced | `{userId, sessionId, ip, device, timestamp}` |
| `UserLoggedOut` | produced | `{userId, sessionId, timestamp}` |
| `MFAChallengeIssued` | produced | `{userId, method, timestamp}` |

---

## 5. Constraints Técnicos Locais

| Constraint | Valor | Motivo |
| :--- | :--- | :--- |
| Latência interna máxima | 100ms | Login P99 < 500ms; 100ms cobre domínio sem incluir I/O |
| Tamanho máximo de payload | 2KB | JWT + metadata de sessão |
| Dependências externas | PostgreSQL 16+, Kafka 3.6+ | Disponibilidade 99.95% required |
| Tecnologias proibidas | MD5, SHA-1 para senhas | Vulnerabilidades conhecidas (NIST 800-63B) |

---

## 6. Plano de Testes

| Tipo | Cobertura | Ferramenta |
| :--- | :--- | :--- |
| Unit | ≥ 95% linha, ≥ 90% branch | JUnit 5 / AssertJ |
| Integração | Happy path + error path | JUnit IT + Testcontainers (PostgreSQL) |
| Contract | OpenAPI diff | 0 breaking changes entre versões |
| Performance | k6 | P99 < 500ms sob 1000 rps concorrentes |

---

## 7. Roadmap de Entrega

| Sprint | Entregável | Dependência |
| :--- | :--- | :--- |
| Sprint 1 | Login básico email/senha + RBAC | — |
| Sprint 2 | MFA (TOTP) + auditoria de sessões | Sprint 1 |
| Sprint 3 | SSO SAML 2.0 + WebAuthn | Sprint 2 |
