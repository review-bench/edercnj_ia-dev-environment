# QA Specialist Review — story-0061-0003

ENGINEER: QA
STORY: story-0061-0003 (Catálogo Dinâmico + DocsAssembler)
SCORE: 34/40
STATUS: PARTIAL

---

## PASSED

- [QA-02] Line coverage >= 95% — 6 tests cover all render paths in renderCatalog, renderAuditLoop, renderExitCodeLoop, renderConditionals
- [QA-03] Branch coverage >= 90% — isDefaultStack true/false, empty inventory, non-empty, nested loops all covered
- [QA-04] Test naming — `springBootInventory_containsActuatorSection`, `defaultStack_containsNoteAboutRuntimeAudits`, etc. all follow [method]_[scenario]_[expected]
- [QA-05] AAA pattern — build inventory (A) / renderCatalog (A) / assertThat (A) in every test
- [QA-08] No test interdependency — fresh `new DocsAssembler()` per test class
- [QA-09] Fixtures centralized — `modelSelectionAudit()` and `actuatorAudit()` are static helper methods shared within the test class
- [QA-10] Unique test data — each test builds its own inventory list
- [QA-12] Integration tests — `DocsAssembler()` default constructor reads real classpath template
- [QA-15] TPP progression — empty→1 audit→3 audits (simple to complex)
- [QA-16] No test-after — tests committed with implementation
- [QA-17] Acceptance tests — 4 of 5 Gherkin ACs validated end-to-end
- [QA-18] TDD coverage thresholds maintained — all 6 tests green

---

## FAILED

- [QA-07] Exception paths tested with specific assertions (0/2)
  - Finding: Story AC4 ("Placeholder não-resolvido falha o assembler") not implemented. `renderCatalog` with `{{UNKNOWN_PLACEHOLDER}}` leaves it intact (no throw). No test verifies this behavior.
  - Fix: Add test `unresolvedPlaceholder_inTemplate_isLeftIntact()` OR implement strict-mode throw and test it. Correlates with QA-01 partial.
  - Severity: MEDIUM

---

## PARTIAL

- [QA-01] Test for each AC (1/2)
  - Finding: AC3 (static catalog deletion) is only indirectly validated (build passes). AC4 (unresolved placeholder throws) not implemented or tested.

- [QA-06] Parametrized tests (1/2)
  - Finding: 6 separate @Test methods; could use @ParameterizedTest for the 5 different stacks rendering scenario.

- [QA-11] Edge cases (1/2)
  - Finding: null inventory, null stack not tested — could NPE in renderAuditLoop.

- [QA-13] TDD commits (1/2)
  - Finding: test+implementation in same commit per task, no separate RED commit.

- [QA-14] Refactor commits (1/2)
  - Finding: dead method `copy(List<AuditScript>)` at DocsAssembler:182 never called. Should be removed.

- [QA-19] Smoke tests (1/2)
  - Finding: `DocsAssemblerCatalogTest` serves as proxy for smoke; no explicit `*SmokeIT` class.
