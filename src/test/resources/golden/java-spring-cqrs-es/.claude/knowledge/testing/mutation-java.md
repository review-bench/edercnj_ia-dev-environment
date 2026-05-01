---
name: mutation-java
description: "PIT mutation testing playbook for Java/Maven and Java/Gradle projects"
requires-capabilities: [quality.mutation.java-pit]
---

# Knowledge Pack: Mutation Testing — Java (PIT)

## Tool Matrix

| Build Tool | Plugin | Min Version | Container Image |
|------------|--------|-------------|----------------|
| Maven | `pitest-maven` | 1.15 | `maven:3.9-eclipse-temurin-21` |
| Gradle | `gradle-pitest` | 1.15 | `eclipse-temurin:21` |

## Maven Configuration

Add to `pom.xml`:

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.15.3</version>
  <configuration>
    <targetClasses>
      <param>dev.iadev.domain.*</param>
      <param>dev.iadev.application.*</param>
    </targetClasses>
    <excludedClasses>
      <param>**test**</param>
      <param>**generated**</param>
      <param>**/infrastructure/adapter/**</param>
    </excludedClasses>
    <mutators>DEFAULTS</mutators>
    <threads>4</threads>
  </configuration>
</plugin>
```

## Run Command

```bash
timeout $((runtime_cap_min * 60)) mvn pitest:mutationCoverage \
  -DtargetClasses="dev.iadev.domain.*,dev.iadev.application.*" \
  -DexcludedClasses="$(exclusions_csv)" \
  -Dthreads=4 \
  --no-transfer-progress
```

## Result Parsing

Report location: `target/pit-reports/*/mutations.xml`

```bash
# Extract score
KILLED=$(xmllint --xpath "count(//mutation[@status='KILLED'])" target/pit-reports/*/mutations.xml)
TOTAL=$(xmllint --xpath "count(//mutation)" target/pit-reports/*/mutations.xml)
SCORE=$(echo "scale=2; $KILLED * 100 / $TOTAL" | bc)
```

Per-package override example in config:
```yaml
quality:
  mutation:
    thresholds:
      per-package:
        "dev.iadev.domain": 90
        "dev.iadev.adapter": 70
```

## Surviving Mutant Report

From `mutations.xml`, find `<mutation status="SURVIVED">`:
- `<sourceFile>` → class
- `<mutatedMethod>` → method
- `<lineNumber>` → line
- `<mutator>` → mutation type (CONDITIONALS_BOUNDARY, NEGATE_CONDITIONALS, etc.)

## Exit Code Mapping

| PIT outcome | Skill exit code |
|-------------|----------------|
| Score ≥ threshold | 0 SUCCESS |
| Score < threshold | 1 MUTATION_SCORE_BELOW_THRESHOLD |
| Timeout | 2 MUTATION_RUNTIME_CAP_EXCEEDED |
| Plugin not in classpath | 3 TOOL_NOT_FOUND |

## Default Exclusions Rationale

- `**/test/**` — test code generates trivially surviving mutants (assertions are not production logic)
- `**/generated/**` — generated code is not hand-authored; mutations are not meaningful
- `**/infrastructure/adapter/**` — JDBC/HTTP adapters are integration-tested, not unit-tested; mocks kill few mutants
