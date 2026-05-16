---
name: x-model-threats
description: "Generates STRIDE threat models: components, data flows, threats, severity, mitigations."
user-invocable: true
allowed-tools: Read, Write, Glob, Grep, Agent
argument-hint: "[architecture-plan-path] [--format stride|pasta|linddun] [--output results/security/]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Threat Model — STRIDE Analysis (slim — ADR-0012)

## Purpose

Generates automated threat models for {{PROJECT_NAME}} using STRIDE analysis. Identifies components, maps data flows, analyzes threats per STRIDE category, classifies severity, suggests mitigations, and produces a structured threat model document.

## Triggers

- `/x-model-threats` — analyze codebase and generate STRIDE threat model
- `/x-model-threats steering/plan.md` — generate from architecture plan
- `/x-model-threats --format stride` — STRIDE analysis (default)
- `/x-model-threats --format pasta` — PASTA analysis (risk-centric)
- `/x-model-threats --format linddun` — LINDDUN analysis (privacy-focused)
- `/x-model-threats --output results/security/` — specify output directory

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `path` | String | none | file path | Architecture plan path (optional) |
| `--format` | String | stride | stride, pasta, linddun | Analysis methodology |
| `--output` | String | results/security/ | directory path | Output directory for threat model |

## Output Contract

| Artifact | Path |
|----------|------|
| Threat model document | `results/security/threat-model.md` (or `--output` dir) |
| Structure | Executive Summary + System Overview + Threat Matrix + Detailed Findings + Risk Summary + Recommendations |
| Threat IDs | `TM-NNN` per finding with severity (CRITICAL / HIGH / MEDIUM / LOW) and Security KP cross-reference |

## STRIDE Categories (one row per component in the matrix)

| Category | Concern | Example Threats |
|----------|---------|-----------------|
| **S** — Spoofing | Identity / authentication | Token forgery, session hijack, credential stuffing |
| **T** — Tampering | Data integrity | SQL injection, MITM, request tampering |
| **R** — Repudiation | Audit / traceability | Insufficient logging, log tampering |
| **I** — Information Disclosure | Confidentiality | PII leak in logs, over-fetching, side-channel |
| **D** — Denial of Service | Availability | Resource exhaustion, DDoS, cascading failure |
| **E** — Elevation of Privilege | Authorization | IDOR, JWT claim manipulation, broken access control |

## Workflow Overview

```text
1. READ      -> Read architecture plan (or fallback to codebase scan)
2. IDENTIFY  -> Extract components (services, DBs, APIs, brokers, caches, gateways, auth, storage)
3. MAP       -> Data flows + trust boundaries (External↔Internal, User↔System, etc.)
4. ANALYZE   -> Apply STRIDE 6-category analysis per component
5. CLASSIFY  -> Severity (CRITICAL/HIGH/MEDIUM/LOW) via Impact × Probability
6. MITIGATE  -> Map each threat to security-KP section + concrete mitigations
7. GENERATE  -> Emit threat-model.md with Threat Matrix + detailed findings
```

Detailed Step 1–7 procedures, STRIDE per-category threat tables with mitigations, trust-boundary categories, severity classification with impact assessment criteria, Security KP cross-reference table, full document template, and PASTA/LINDDUN alternative-methodology summaries live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): explicit-path read vs auto-discover (`steering/` + ADRs) vs codebase-analysis fallback (package structure + config files + dependency declarations).
- **Step 2** (§Step 2): 8-row component discovery table (Services / Databases / External APIs / Message Brokers / Caches / API Gateway / Auth Service / File Storage).
- **Step 3** (§Step 3): protocol + data-sensitivity + trust-boundary + authentication taxonomy; 4-row trust-boundary risk table.
- **Step 4 (4.1–4.6)** (§Step 4): per-STRIDE threat tables (4 example threats per category) with affected components and mitigation recipes.
- **Step 5** (§Step 5): severity matrix (Impact × Probability); 4-factor impact assessment (data exposure, service impact, blast radius, regulatory).
- **Step 6** (§Step 6): STRIDE → Security-KP section crosswalk (Authentication, Input Validation, Logging/Audit, Data Protection, Resilience, Authorization).
- **Step 7** (§Step 7): full threat-model document template (Executive Summary + System Overview + Threat Matrix + Detailed Findings + Risk Summary + Recommendations).
- **Supported Formats** (§Supported Formats): PASTA 7-stage process; LINDDUN privacy categories (L/I/N/D/D/U/N).

## Error Handling

| Scenario | Action |
|----------|--------|
| No architecture plan found | Fallback to codebase analysis |
| Empty or invalid plan | Warn and attempt codebase analysis |
| No components identified | Report "No components found" with suggestions |
| Partial analysis | Generate partial threat model, note gaps |
| Unknown format requested | Default to STRIDE, warn user |

## Knowledge Pack References

| # | Knowledge Pack | Purpose |
|---|----------------|---------|
| 1 | `.claude/knowledge/security/index.md` | Mitigation recommendations and OWASP references |
| 2 | `.claude/knowledge/security/application-security.md` | Detailed security controls and patterns |
| 3 | `.claude/knowledge/security/security-principles.md` | Defense in depth, least privilege, fail secure |
| 4 | `.claude/knowledge/security/anti-patterns-java.md` | Java CWE-mapped anti-patterns for Tampering/Information Disclosure categories |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-plan-architecture` | invoked from | Threat model can be generated as part of architecture planning |
| `security-engineer` agent | delegates to | Uses security-engineer agent for in-depth analysis via Agent tool |
| `x-scan-owasp` | complements | Threat model informs A04 (Insecure Design) verification in OWASP scan |

## Full Protocol

Minimum viable contract above. Detailed STRIDE per-category threat tables, trust-boundary taxonomy, severity classification with impact assessment criteria, Security KP crosswalk, full threat-model document template, and PASTA/LINDDUN alternative-methodology summaries live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
