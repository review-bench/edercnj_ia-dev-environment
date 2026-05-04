# Feature: MFA Support

**Feature ID:** mfa-support  
**Capability:** auth  
**Status:** Draft

---

## 1. Feature Statement

Como **Administrador de Segurança**, eu quero **exigir autenticação multifator (TOTP/SMS) para acesso a recursos críticos**, para que **o risco de comprometimento de conta seja reduzido mesmo com credenciais vazadas**.

### 1.1 Contexto

Clientes enterprise de ContractOS exigem controles de acesso mais rigorosos para operações financeiras e contratuais. MFA via TOTP (Authenticator apps) é o padrão mínimo exigido por frameworks como SOC 2 Type II e ISO 27001. A feature oferece MFA obrigatório por role ou por operação sensível.

### 1.2 Escopo

**In-scope:**
- TOTP (RFC 6238) via apps como Google Authenticator, Authy
- MFA obrigatório configurável por tenant (role-based)
- Backup codes de recuperação (8 codes de uso único)

**Out-of-scope:**
- SMS OTP (custo e vulnerabilidade SIM-swap)
- Hardware tokens (FIDO2/WebAuthn — feature separada)
- MFA para usuários consumer B2C

---

## 2. Casos de Uso

### UC-001: Configurar TOTP pela primeira vez

| Campo | Valor |
| :--- | :--- |
| **Ator** | Usuário com MFA requerido pelo tenant |
| **Ação** | I want to scan a QR code to register my authenticator app |
| **Benefício** | so that I can satisfy the MFA requirement without contacting support |

### UC-002: Login com MFA TOTP

| Campo | Valor |
| :--- | :--- |
| **Ator** | Usuário com MFA habilitado |
| **Ação** | I want to enter a 6-digit TOTP code after my password |
| **Benefício** | so that my account is protected even if my password is compromised |

### UC-003: Recuperação via backup code

| Campo | Valor |
| :--- | :--- |
| **Ator** | Usuário que perdeu acesso ao authenticator |
| **Ação** | I want to use a one-time backup code to regain access |
| **Benefício** | so that I'm not permanently locked out if I lose my phone |

---

## 3. Requisitos Funcionais

| ID | Requisito | Prioridade | UC |
| :--- | :--- | :--- | :--- |
| RF-001 | Gerar TOTP secret e QR code para setup inicial | Must | UC-001 |
| RF-002 | Verificar TOTP code com janela de 30s ±1 (clock drift) | Must | UC-002 |
| RF-003 | Gerar 8 backup codes de uso único no enrollment | Must | UC-003 |
| RF-004 | Invalidar backup code após primeiro uso | Must | UC-003 |
| RF-005 | Permitir admin configurar MFA como obrigatório por role | Should | UC-001, UC-002 |

---

## 4. Interfaces Exposed

### 4.1 Input Contract

| Campo | Tipo | M/O | Validações |
| :--- | :--- | :--- | :--- |
| `userId` | `String` | `M` | referência a usuário existente |
| `totpCode` | `String(6)` | `M` | dígitos numéricos, 6 chars |
| `backupCode` | `String(16)` | `O` | formato: XXXX-XXXX-XXXX-XXXX |

### 4.2 Output Contract

| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `verified` | `Boolean` | true se MFA passou |
| `remainingBackupCodes` | `Integer` | contagem de backup codes restantes |
| `mfaSessionToken` | `String` | token de sessão pós-MFA |

### 4.3 Events Emitted

| Evento | Trigger | Payload |
| :--- | :--- | :--- |
| `MFAEnrolled` | Setup completo | `{ userId, method: TOTP }` |
| `MFAVerified` | Login com MFA bem-sucedido | `{ userId, method }` |
| `MFAFailed` | Código inválido | `{ userId, attempts }` |
| `BackupCodeUsed` | Código de backup consumido | `{ userId, codesRemaining }` |

---

## 5. Acceptance Criteria Detalhados

### Happy Path

```gherkin
Cenário: Enrollment TOTP bem-sucedido
  DADO que usuário iniciou setup de MFA
  QUANDO escaneia o QR code e insere código TOTP correto
  ENTÃO MFA é habilitado na conta
  E 8 backup codes são exibidos para armazenamento seguro

Cenário: Login com TOTP válido
  DADO que usuário tem MFA habilitado
  QUANDO insere senha correta e TOTP válido
  ENTÃO acesso é concedido com mfaSessionToken
  E evento MFAVerified é emitido

Cenário: Recuperação via backup code
  DADO que usuário perdeu acesso ao authenticator
  QUANDO insere um dos 8 backup codes de forma correta
  ENTÃO acesso é concedido
  E backup code é invalidado (não pode ser reutilizado)
  E evento BackupCodeUsed é emitido com codesRemaining decrementado
```

### Error & Boundary

```gherkin
Cenário: TOTP code inválido
  DADO que usuário insere código TOTP incorreto
  QUANDO tentativa de verificação é feita
  ENTÃO erro "Invalid TOTP code" é retornado
  E evento MFAFailed é emitido com contagem de tentativas

Cenário: TOTP com clock drift acima de 60s é rejeitado
  DADO que relógio do device desviou > 60s
  QUANDO TOTP code fora da janela é submetido
  ENTÃO verificação falha com "Code expired or clock drift too large"

Cenário: Backup code já utilizado
  DADO que backup code foi previamente usado
  QUANDO mesmo código é submetido novamente
  ENTÃO erro "Backup code already used" é retornado

Cenário: Todos backup codes esgotados
  DADO que todos 8 backup codes foram utilizados
  QUANDO usuário tenta login sem authenticator
  ENTÃO mensagem "No backup codes remaining. Contact your admin." é exibida
```

### Performance & SLA

```gherkin
Cenário: Verificação TOTP dentro do SLA
  DADO que requisição de verificação válida é recebida
  QUANDO TOTP é verificado
  ENTÃO resposta em menos de 200ms (P99)
```

### Security & Auth

```gherkin
Cenário: Tentativa de brute-force é bloqueada
  DADO que 5 tentativas MFA inválidas em 60 segundos
  QUANDO 6ª tentativa é feita
  ENTÃO conta é temporariamente bloqueada por 15 minutos
  E alerta de segurança é emitido ao admin

Cenário: Endpoint MFA requer sessão autenticada
  DADO que requisição não tem token de sessão válido
  QUANDO /auth/mfa/verify é chamado diretamente
  ENTÃO HTTP 401 é retornado
```

---

## 6. Estimativa & Roadmap

| Componente | Esforço | Sprint |
| :--- | :--- | :--- |
| TOTP domain (secret, code generation/verification) | 3 pts | Sprint 1 |
| Backup codes (generation, one-time use) | 2 pts | Sprint 1 |
| MFA enrollment flow (QR code, setup verification) | 3 pts | Sprint 2 |
| Admin config: MFA required by role | 2 pts | Sprint 2 |
| Rate limiting / brute-force protection | 2 pts | Sprint 2 |
| Testes de integração | 3 pts | Sprint 2 |

**Total estimado:** 15 pts (2 sprints)

**Critério de release:** Todos 5 RFs, brute-force protection ativo, backup codes funcionais, cobertura ≥ 95%.

---

## 7. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação |
| :--- | :--- | :--- | :--- |
| Clock drift causando falsos negativos em TOTP | Média | Alto | Janela de ±1 período (±30s) conforme RFC 6238 |
| Usuário perde acesso ao authenticator e backup codes | Baixa | Alto | Admin reset flow com aprovação de segundo admin |
| Secret TOTP exposto em logs | Baixa | Crítico | TOTP secret nunca logado; masked em stack traces |
