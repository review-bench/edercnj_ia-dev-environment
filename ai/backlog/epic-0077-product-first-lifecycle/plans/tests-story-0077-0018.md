# Test Plan — story-0077-0018

## Unit Tests: SLOHarnessTest

| Test | Scenario | Expected |
| :--- | :--- | :--- |
| `validate_aboveTarget_passes` | observed=99.99, target=99.95 | passed=true, delta=+0.04 |
| `validate_exactlyTarget_passes` | observed=99.95, target=99.95 | passed=true, delta=0.0 |
| `validate_belowTarget_fails` | observed=99.90, target=99.95 | passed=false, delta=-0.05 |
| `validate_zeroObserved_fails` | observed=0.0, target=99.95 | passed=false |
| `validate_nullSpec_throwsException` | spec=null | IllegalArgumentException |

## Smoke Tests: QaCharterSmokeTest

| Test | Scenario | Expected |
| :--- | :--- | :--- |
| `sloHarness_uptimeSlo_passes` | uptime=99.99 vs target=99.95 | passed=true |
| `sloHarness_latencySlo_fails` | latency=250 vs target=200 (lower=better, so observed > target fails) | passed=false |
| `errorCatalog_yaml_isReadable` | read ErrorCatalog.yaml from classpath | non-empty content containing "InvalidToken" |
| `qaCharter_measurableAc_example` | AC template constructed | contains Unit, Target, Measurement keys |

## Coverage Target
- Line ≥ 97%, Branch ≥ 93%
