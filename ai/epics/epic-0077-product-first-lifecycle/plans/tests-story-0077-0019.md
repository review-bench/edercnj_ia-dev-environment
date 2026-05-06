# Test Plan — story-0077-0019

## Unit Tests: PentestAlwaysOnCapabilityTest

| Test | Expected |
| :--- | :--- |
| capability_yaml_hasCorrectId | id = "quality.pentest-always-on" |
| capability_yaml_isStableStatus | status = "stable" |
| capability_yaml_hasDescription | description is non-empty |

## Smoke Tests: PentestAlwaysOnSmokeTest

| Test | Expected |
| :--- | :--- |
| pentestEngineer_inCore_charterReadable | core/pentest-engineer.md non-empty, contains "Pentest Engineer" |
| capability_pentestAlwaysOn_exists | capability yaml readable from classpath |
