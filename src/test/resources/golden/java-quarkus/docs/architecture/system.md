<!--
  AUTO-FILL vs NARRATIVE — placeholder convention
  ================================================
  AUTO-FILL  → Pebble double-brace tokens (e.g. OPEN_BRACE key CLOSE_BRACE) resolved from
                project YAML via ContextBuilder.buildContext().
                Sections: Stack Resolvida, Persistência, Comunicação, Segurança Baseline.
                Partial auto-fill: Observabilidade (EPIC-0072), Performance Budget (EPIC-0072),
                                   Dependency Policy (EPIC-0074), Documentação Targets (EPIC-0071).
  NARRATIVE  → Free-text blocks marked < ... > that the operator fills manually.
                Sections: Resilience, Integrações Externas, Decision Log do Sistema.
  IDEMPOTENT → This file is created once; re-running ia-dev-env generate does NOT overwrite it.
               Incremental updates are performed by /x-update-system-architecture (story-0070-0006).
-->

# Arquitetura do Sistema — my-quarkus-service

> **Gerado por:** `ia-dev-env generate` · **Última atualização:** (ver Decision Log §11)
> **Gatilho de split modular:** arquivo > 800 linhas → abrir épico "split modular".

---

## 1. Stack Resolvida

| Dimensão | Valor |
| :--- | :--- |
| Linguagem | java 21 |
| Framework | quarkus 3.17 |
| Build Tool | maven |
| Estilo Arquitetural | microservice |
| Container | docker |
| Orquestrador | kubernetes |

---

## 2. Persistência


(não aplicável — projeto stateless)


---

## 3. Comunicação

| Canal | Tecnologia |
| :--- | :--- |
| Interfaces | rest, grpc, event-consumer, event-producer |
| Message Broker | kafka |


---

## 4. Observabilidade

> **Auto-fill disponível quando EPIC-0072 entrega `observability.standard` / `.backend`.**
> Preencher manualmente até lá.

| Dimensão | Valor |
| :--- | :--- |
| Standard | < ex: opentelemetry > |
| Backend | < ex: grafana-stack > |
| Traces | < ex: Jaeger, Tempo > |
| Métricas | < ex: Prometheus > |
| Logs | < ex: Loki, ELK > |

---

## 5. Resilience

> **Narrativo** — descreva as estratégias de resiliência adotadas.

- **Circuit Breaker:** < ex: Resilience4j, Hystrix, ou N/A >
- **Retry Policy:** < ex: exponential backoff com max 3 tentativas >
- **Timeout:** < ex: 2s para chamadas externas, 500ms para cache >
- **Bulkhead:** < ex: semaphore / thread pool isolado por parceiro >
- **Fallback:** < ex: resposta default, cache frio, circuit aberto >

---

## 6. Performance Budget

> **Auto-fill disponível quando EPIC-0072 entrega `quality.performance.slo`.**
> Preencher manualmente até lá.

| SLO | Target |
| :--- | :--- |
| Latência P95 (endpoint principal) | < ex: ≤ 200ms > |
| Latência P99 | < ex: ≤ 500ms > |
| Throughput mínimo | < ex: ≥ 500 req/s > |
| Availability | < ex: 99.9% > |

---

## 7. Segurança Baseline


- **Compliance:** N/A

- **Autenticação / Autorização:** < ex: OAuth 2.0 + OIDC / API Key + RBAC >
- **Criptografia em trânsito:** TLS 1.2+
- **Criptografia em repouso:** < declarar se aplicável >
- **Secrets management:** < ex: Vault, AWS Secrets Manager >
- **OWASP Top 10:** mitigações documentadas em `docs/security/owasp.md`

---

## 8. Dependency Policy

> **Auto-fill disponível quando EPIC-0074 entrega `dependencies.policy`.**
> Preencher manualmente até lá.

- **Versionamento:** < ex: pinned-versions / range-based >
- **Vulnerabilidades:** < ex: CVE scan via Dependabot / Trivy em CI >
- **Licenças permitidas:** < ex: Apache 2.0, MIT, BSD >
- **Atualização:** < ex: quarterly bump com regressão automática >

---

## 9. Documentação Targets

> **Auto-fill disponível quando EPIC-0071 entrega `docs.targets`.**
> Preencher manualmente até lá.

| Artefato | Caminho | Audiência |
| :--- | :--- | :--- |
| ADRs | `docs/adr/` | Arquitetos, Tech Leads |
| API Spec | `docs/specs/` | Consumidores da API |
| Runbook | `docs/runbook.md` | SRE / Ops |
| Este arquivo | `docs/architecture/system.md` | Time de Desenvolvimento |

---

## 10. Integrações Externas

> **Narrativo** — liste sistemas externos e dependências de terceiros.

| Sistema | Protocolo | Dono | SLA esperado |
| :--- | :--- | :--- | :--- |
| < nome do sistema > | < REST / gRPC / evento > | < time > | < ex: 99.5% / 200ms > |

---

## 11. Decision Log do Sistema

> Populado de forma incremental por `/x-update-system-architecture` (story-0070-0006).
> Cada entrada registra uma decisão arquitetural de impacto transversal.

| Data | Decisão | Motivo | Alternativa Descartada |
| :--- | :--- | :--- | :--- |
| (gerado) | Criação deste documento | Onboarding em ≤ 30min (EPIC-0070) | 60+ arquivos dispersos |
