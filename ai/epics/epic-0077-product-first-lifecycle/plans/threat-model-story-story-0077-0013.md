# Threat Model — story-0077-0013

**Story:** story-0077-0013 — Refator x-arch-plan: C4 obrigatórios  
**Modeled At:** 2026-05-05T14:30:00Z

## Trust Boundaries

```
[Architect (human/CI)] → [x-arch-plan SKILL] → [XArchPlanProductCommand / XArchPlanCapabilityCommand]
                                                         ↓
                                              [ProductC4Planner / CapabilityC4Planner]
                                                         ↓
                                              [C4Diagram (generated content)]
                                                         ↓
                                              [Documentation portal / file system]
```

## Threats Identified

| ID | STRIDE | Threat | Likelihood | Impact | Mitigation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| T-01 | Tampering | HTML/script injection via `--product-id` or `--capability-id` labels embedded in generated diagram content | Low | High (XSS if diagram rendered in browser) | `ProductC4Planner.escape()` and `CapabilityC4Planner.escape()` apply HTML entity encoding (`&`, `<`, `>`, `"`) before embedding in diagram content. Verified by `product_htmlEscapingInDiagrams` and `capability_htmlEscapingInDiagrams` in `XArchPlanC4SmokeTest` |
| T-02 | Tampering | PlantUML directive injection (`!include`, `!define`, `@startjson`) via user-supplied ID altering diagram semantics or triggering SSRF in CI that renders PlantUML | Low | High | `escape()` strips `!` and `@` characters outside the canonical `@startuml`/`@enduml` wrapper that is hard-coded in `buildContext`/`buildContainer`. User-supplied ID is only embedded as a label string, never as a raw PlantUML directive |
| T-03 | Information Disclosure | Exception messages from `XArchPlanProductCommand.call()` or `XArchPlanCapabilityCommand.call()` exposing internal stack traces via `out.println("Error: " + e.getMessage())` | Low | Medium | Exception message is captured and printed via `e.getMessage()` only (no stack trace). For `EXIT_EXECUTION`, the raw message may reveal internal implementation details; future hardening should map exceptions to user-safe messages |
| T-04 | Denial of Service | Extremely long `--product-id` or `--capability-id` causing large diagram content allocation | Very Low | Low | picocli validates argument presence but not length; content is bounded by string builder + fixed template structure. No loop over user input size |

## Residual Risks

- T-03 (partial): exception message passthrough to CLI output is accepted for MVP scope; hardening deferred to a future story.
- Downstream rendering surfaces (Backstage, Confluence, GitHub Pages) must independently sanitize diagram content — escaping at generation time is necessary but not sufficient for all rendering contexts.

## Verdict

**Acceptable** — T-01 and T-02 (highest priority) are mitigated by `escape()` implementation. Residual risk T-03 is low-severity and accepted for this story scope.
