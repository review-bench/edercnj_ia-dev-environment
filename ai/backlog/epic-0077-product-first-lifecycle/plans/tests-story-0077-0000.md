# Test Plan — story-0077-0000

**Story:** ADR Amendment — Rule 14 Extension for Product-First Runtime Domain

## Test Strategy

No Java unit tests — normative/documentation story.

## Verification Scenarios

| Scenario | Verification Method |
| :--- | :--- |
| ADR exists with status Accepted | grep "Status.*Accepted" docs/adr/ADR-0030-*.md |
| ADR indexed in README | grep "ADR-0030" docs/adr/README.md |
| Rule 14 contains extension section | grep "Product-First Domain Extension" src/.../rules/14-project-scope.md |
| Authorized packages listed | grep "domain/products/" src/.../rules/14-project-scope.md |
| Existing Rule 14 sections unchanged | diff baseline against amended file |

## Smoke Test

`bash src/test/bash/audit-flow-version-v5.sh` — unrelated but confirms baseline tests still pass.
