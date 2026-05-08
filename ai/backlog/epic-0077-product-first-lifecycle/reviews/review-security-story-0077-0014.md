ENGINEER: Security
STORY: story-0077-0014
SCORE: 24/30
STATUS: Partial
---
PASSED:
- [SEC-01] HTML escaping correctly implemented (2/2): Both ProductC4Planner.escape() and CapabilityC4Planner.escape() replace `&`, `<`, `>`, `"` — four-pass, correct order (ampersand first). XArchPlanC4SmokeTest.mermaidProduct_htmlInjectionAttempt and mermaidCapability_htmlInjectionAttempt validate escaping end-to-end.
- [SEC-02] No hardcoded secrets or credentials (2/2): No tokens, passwords, or API keys found anywhere in the new classes. PlantUML stdlib URL is a public reference, not a credential.
- [SEC-03] Input validation at domain boundary (2/2): C4Diagram compact constructor validates all four fields non-null/non-blank; planners validate productId/capabilityId before building content. No null propagation into string operations.
- [SEC-04] No external network calls at runtime (2/2): PlantUML `!include` URLs appear only in the generated diagram CONTENT string — they are rendered text for the user's PlantUML processor, not executed by the Java process itself.
- [SEC-05] No file system operations (2/2): All output goes to PrintWriter (picocli's `spec.commandLine().getOut()`). No temp files, no directory creation, no path operations.

PARTIAL:
- [SEC-06] Generic Exception catch in CLI (1/2) -- adapter/inbound/cli/XArchPlanProductCommand.java:51, XArchPlanCapabilityCommand.java:51 -- `catch (Exception e)` catches all runtime exceptions. If an unexpected RuntimeException (e.g., OOME, StackOverflow) occurs, `e.getMessage()` may return null (NPE risk on `out.println("Error: " + null)`). Severity: LOW. Fix: narrow to `catch (IllegalArgumentException e)` and let unexpected exceptions propagate (picocli has its own uncaught exception handler). [LOW]
- [SEC-07] ContainerDb PostgreSQL hardcoded in CapabilityC4Planner (1/2) -- domain/architecture/CapabilityC4Planner.java:42 -- Template generates `ContainerDb(store, "Store", "PostgreSQL", "Persistence")` for all capabilities regardless of the project's actual database stack. This project has `database: none`. The generated diagram is misleading and could be used to make incorrect architectural assumptions. Severity: LOW (output is planning artifact, not runtime config). Fix: make db container conditional on project profile or use a neutral placeholder. [LOW]
