---
name: claude-md-fragment-security-pci
description: PCI DSS compliance section — active when PCI compliance capability is configured.
fragment-slot: { slot: domain-specific, fragment-id: security-pci, fragment-order: 30 }
requires-capabilities: [compliance.pci.dss]
---

## PCI Compliance

This project processes payment card data and is subject to PCI DSS requirements.

- **Cardholder data:** Never log, persist, or transmit PANs in plaintext; use tokenization (Vault Transit) or truncation (last 4 digits only in logs).
- **Audit trail:** Every state change on a payment entity must emit an immutable audit log entry (`payment_audit_log` table) with user, timestamp (UTC), action, before/after values.
- **Encryption at rest:** All columns containing cardholder data must be encrypted at the database level using AES-256; keys stored in HSM or secrets manager, never in code.
- **Access control:** Production DB credentials must follow least-privilege; application role has only the permissions required for its operation (no DDL rights in prod).
- **Key rotation:** Encryption keys must be rotatable without downtime; use envelope encryption (data key encrypted by master key).
- **Compliance reports:** Maintain artifacts under `reports/compliance/` that map controls to code evidence; regenerate on every release.
