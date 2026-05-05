# Implementation Plan — story-0077-0019

## TASK-0077-0019-001: pentest-engineer promotion + capability
- Create `capabilities/quality/pentest/pentest-always-on.yaml`
- Copy pentest-engineer.md content to `src/main/resources/targets/claude/agents/core/pentest-engineer.md`
- Update: remove `Condition` section (no longer conditional), add always-on section
- Java test: `PentestAlwaysOnCapabilityTest` verifying capability yaml is readable

## TASK-0077-0019-002: Smoke test
- `PentestAlwaysOnSmokeTest.java`: verify pentest-engineer.md in core, capability yaml exists on classpath
