# Compliance Assessment — story-0073-0001

**Story:** Schema YAML + capabilities + ADR
**Compliance:** none (project default)

## Assessment

No PCI-DSS or LGPD-specific requirements apply to this story.
The story creates governance artifacts (capability YAML, Java records, ADR) that are
design-time only. No runtime data processing, no PII, no payment data.

## D-R9 Addendum Alignment

The D-R9 addendum explicitly documents: the generator (`ia-dev-env`) uses `active-scan-policy: default`
in its own DAST baseline — does NOT require PCI/LGPD payloads. Custom compliance payloads are for
generated projects that declare `quality.dast.compliance: [pci|lgpd|hipaa]`. This story implements
that distinction in the schema (`DastConfig.compliance` field, default empty list).
