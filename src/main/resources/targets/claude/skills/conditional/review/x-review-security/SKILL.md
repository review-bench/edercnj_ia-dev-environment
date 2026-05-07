---
name: x-review-security
description: "Reviews code changes for compliance with selected security frameworks. Verifies sensitive data handling, audit trails, and access control patterns."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash, Agent
argument-hint: "[PR number or file paths]"
requires-capabilities: []
fragment-slot: { slot: review-specialist, fragment-id: security, fragment-order: 10 }
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Security Compliance Review

## Purpose

Review code changes against the compliance frameworks selected in the project configuration. Verify sensitive data handling, audit trails, access control patterns, and cryptography usage per active framework requirements.

## Activation Condition

Include this skill when the project has compliance frameworks configured (PCI-DSS, LGPD, GDPR, HIPAA, SOX).

## Triggers

- `/x-review-security 42` -- review PR #42 for security compliance
- `/x-review-security src/main/java/com/example/auth/` -- review specific file paths
- `/x-review-security` -- review all current changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/application-security.md` | OWASP Top 10, security headers, secrets management |
| security | `skills/security/references/cryptography.md` | TLS, hashing, key management |
| compliance | `skills/compliance/SKILL.md` and `skills/compliance/references/` | Active framework requirements |

Read src/main/resources/targets/claude/knowledge/security/anti-patterns/index.md
Read src/main/resources/targets/claude/knowledge/checklists/hipaa-security.md
Read src/main/resources/targets/claude/knowledge/checklists/pci-dss-security.md
Read src/main/resources/targets/claude/knowledge/checklists/privacy-security.md
Read src/main/resources/targets/claude/knowledge/checklists/sox-security.md

## Workflow

### Step 1 -- Gather Context

Collect the review target: PR number or file paths from args. Run:
```bash
git diff --name-only HEAD~1..HEAD 2>/dev/null || git diff --name-only --cached
```

### Step 2 -- Dispatch to Security Engineer Agent

    Agent(
      subagent_type: "security-engineer",
      description: "Security specialist review for {target}",
      prompt: "Review the code changes for security compliance. Target: {target}. Run `git diff HEAD~1..HEAD` to get the diff. Read `skills/security/references/security-principles.md`, `skills/security/references/application-security.md`, and `skills/security/references/cryptography.md`. Read `skills/compliance/SKILL.md` to identify active compliance frameworks (PCI-DSS, LGPD, GDPR, HIPAA, SOX). Apply your full security checklist including sensitive data handling, input validation, auth/authz, defensive coding, and all active compliance framework checks. Produce output in this exact format:\n\nENGINEER: Security\nSTORY: {target}\nSCORE: XX/30\nSTATUS: Approved | Rejected | Partial\n---\nPASSED:\n- [SEC-XX] Description (2/2)\nFAILED:\n- [SEC-XX] Description (0/2) -- file:line -- Fix: suggestion [SEVERITY]\nPARTIAL:\n- [SEC-XX] Description (1/2) -- file:line -- Improvement: suggestion [SEVERITY]"
    )

## Output Format

```
## Compliance Review — [Change Description]

### Active Frameworks: [list]

### Per-Framework Results

#### [Framework Name]
- [x] Requirement met / [ ] Gap identified
- Finding: [description + remediation]

### Overall Verdict: COMPLIANT / NON-COMPLIANT / NEEDS REVIEW
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No compliance frameworks configured | Report INFO: no frameworks active, skip review |
| Compliance KP files missing | Warn and proceed with generic security review |
| PR number invalid or inaccessible | Report error with PR number and suggest checking access |
