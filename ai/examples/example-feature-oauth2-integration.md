# Feature: OAuth2 Integration

**Feature ID:** oauth2-integration  
**Capability:** auth  
**Status:** Draft

---

## 1. Feature Statement

Como **Desenvolvedor de Produto**, eu quero **autenticar usuários via OAuth2 com providers externos (Google, GitHub, Microsoft)**, para que **os usuários não precisem criar uma senha separada e a taxa de cadastro aumente**.

### 1.1 Contexto

ContractOS B2B atualmente oferece autenticação local (email + senha). Para aumentar a taxa de conversão no onboarding B2B, precisamos suportar OAuth2 via provedores enterprise (Google Workspace, Microsoft Entra, GitHub Organizations). O fluxo segue o padrão Authorization Code com PKCE.

### 1.2 Escopo

**In-scope:**
- Authorization Code flow com PKCE para Google, GitHub, Microsoft
- Mapping de claims do provider para User entity local
- Criação automática de conta no primeiro login OAuth2

**Out-of-scope:**
- OAuth2 como servidor (emitir tokens para terceiros)
- SAML / LDAP federation
- Social login para usuários consumer (B2C)

---

## 2. Casos de Uso

### UC-001: Login com Google Workspace

| Campo | Valor |
| :--- | :--- |
| **Ator** | Engenheiro de Software (empresa cliente) |
| **Ação** | I want to click "Sign in with Google" and be authenticated |
| **Benefício** | so that I don't need to remember a separate password for ContractOS |

### UC-002: Login com GitHub Organizations

| Campo | Valor |
| :--- | :--- |
| **Ator** | Tech Lead (empresa cliente) |
| **Ação** | I want to sign in with my GitHub account linked to my organization |
| **Benefício** | so that access is automatically scoped to my GitHub org membership |

### UC-003: Auto-provisioning de conta no primeiro login

| Campo | Valor |
| :--- | :--- |
| **Ator** | Novo usuário OAuth2 |
| **Ação** | I want my account to be created automatically on first OAuth2 login |
| **Benefício** | so that I can access the platform without manual admin provisioning |

---

## 3. Requisitos Funcionais

| ID | Requisito | Prioridade | UC |
| :--- | :--- | :--- | :--- |
| RF-001 | Suportar Google, GitHub e Microsoft como OAuth2 providers | Must | UC-001, UC-002 |
| RF-002 | Implementar Authorization Code + PKCE (sem client_secret no frontend) | Must | UC-001, UC-002 |
| RF-003 | Criar conta local automaticamente no primeiro login OAuth2 | Must | UC-003 |
| RF-004 | Mapear claims do provider: sub → userId, email, name, picture | Must | UC-003 |
| RF-005 | Renovar access_token via refresh_token silenciosamente | Should | UC-001, UC-002 |

---

## 4. Interfaces Exposed

### 4.1 Input Contract

| Campo | Tipo | M/O | Validações |
| :--- | :--- | :--- | :--- |
| `provider` | `String` | `M` | enum: google, github, microsoft |
| `code` | `String` | `M` | authorization code from OAuth callback |
| `state` | `String` | `M` | CSRF state nonce |
| `redirectUri` | `String` | `M` | must match registered URI |

### 4.2 Output Contract

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `accessToken` | `String` | JWT de sessão ContractOS |
| `expiresIn` | `Integer` | segundos até expiração |
| `userId` | `String` | ID do usuário na plataforma |
| `isNewUser` | `Boolean` | true se conta foi criada neste login |

### 4.3 Events Emitted

| Evento | Trigger | Payload |
| :--- | :--- | :--- |
| `UserOAuth2LoggedIn` | Login bem-sucedido | `{ userId, provider, isNewUser }` |
| `UserOAuth2AccountCreated` | Primeiro login | `{ userId, email, provider }` |

---

## 5. Acceptance Criteria Detalhados

### Happy Path

```gherkin
Cenário: Login bem-sucedido com Google Workspace
  DADO que usuário clica em "Sign in with Google"
  QUANDO completa a autorização no consent screen do Google
  ENTÃO é redirecionado ao dashboard com sessão ativa
  E JWT ContractOS é emitido com claims: userId, email, provider=google

Cenário: Auto-provisioning no primeiro login Google
  DADO que usuário nunca fez login antes
  QUANDO completa OAuth2 flow com email corporativo
  ENTÃO conta é criada automaticamente com role=VIEWER
  E evento UserOAuth2AccountCreated é emitido

Cenário: Segundo login reutiliza conta existente
  DADO que usuário tem conta provisionada via OAuth2
  QUANDO faz login novamente com mesmo provider
  ENTÃO conta existente é retornada (não cria duplicata)
  E isNewUser=false no response

Cenário: Login com GitHub Organizations
  DADO que usuário pertence a uma GitHub Organization autorizada
  QUANDO autentica via GitHub OAuth2
  ENTÃO é logado com acesso vinculado à organização
```

### Error & Boundary

```gherkin
Cenário: State inválido (CSRF attempt)
  DADO que estado CSRF não corresponde ao armazenado
  QUANDO callback é recebido com state adulterado
  ENTÃO erro "Invalid state parameter" é retornado com HTTP 400

Cenário: Provider não suportado
  DADO que provider=facebook é passado
  QUANDO callback é processado
  ENTÃO erro "Unsupported OAuth2 provider: facebook" é retornado

Cenário: Authorization code expirado
  DADO que authorization code expirou (> 5 minutos)
  QUANDO troca pelo access_token é tentada
  ENTÃO erro "Authorization code expired" com HTTP 400

Cenário: Email domain não autorizado para tenant
  DADO que tenant configurou whitelist de domains
  QUANDO usuário com domain não autorizado tenta login OAuth2
  ENTÃO acesso é negado com "Domain not authorized for this tenant"
```

### Performance & SLA

```gherkin
Cenário: OAuth2 callback processado dentro do SLA
  DADO que authorization code válido é recebido
  QUANDO troca pelo JWT ContractOS é processada
  ENTÃO resposta é retornada em menos de 500ms (P99)
```

### Security & Auth

```gherkin
Cenário: Callback sem autenticação é rejeitado
  DADO que requisição não tem state ou code
  QUANDO /auth/oauth2/callback é chamado diretamente
  ENTÃO acesso é negado com HTTP 401

Cenário: PKCE code_verifier inválido
  DADO que code_verifier não corresponde ao code_challenge
  QUANDO troca de token é tentada
  ENTÃO provider rejeita e ContractOS retorna HTTP 400
```

---

## 6. Estimativa & Roadmap

| Componente | Esforço | Sprint |
| :--- | :--- | :--- |
| OAuth2 client abstraction (domain) | 3 pts | Sprint 1 |
| Provider adapters (Google, GitHub, Microsoft) | 5 pts | Sprint 1 |
| Auto-provisioning + claims mapping | 3 pts | Sprint 2 |
| Token refresh silencioso | 2 pts | Sprint 2 |
| Testes de integração + E2E | 3 pts | Sprint 2 |

**Total estimado:** 16 pts (2 sprints)

**Critério de release:** Todos 5 RFs implementados, cobertura ≥ 95%, todos ACs passando.

---

## 7. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação |
| :--- | :--- | :--- | :--- |
| Provider muda API de claims sem aviso | Média | Alto | Adapter pattern; testes de contrato contra provider sandbox |
| Conta duplicada por race condition no auto-provisioning | Baixa | Alto | Unique constraint em email + provider; upsert atômico |
| Refresh token expirado silenciosamente | Média | Médio | Re-auth flow com indicador UX; rotação de refresh token |
