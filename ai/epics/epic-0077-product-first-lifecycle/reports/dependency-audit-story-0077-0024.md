# Dependency Audit — story-0077-0024

**Story:** story-0077-0024 — x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8
**Audited At:** 2026-05-05T22:00:00Z
**Result:** PASS

## New Dependencies

None. story-0077-0024 introduces no new Maven dependencies. All production classes use
only the Java standard library and project-internal types:

- `XEpicCreateCommand.java` — picocli annotations (`@Command`, `@Option`, `@Parameters`)
  from the already-declared `info.picocli:picocli:4.7.6` dependency
- `CreateEpicFromFeatureUseCase.java` — `java.util.*` only
- `FeatureMarkdownParser.java` — `java.nio.file.*`, `java.util.*`
- `FeatureEpicSourceLoader.java` — `java.nio.file.*`, `java.util.*`
- `EpicFromFeatureArtifactWriter.java` — `java.nio.file.*`, `java.util.*`, `java.time.*`
- `FeatureEpicSource.java` — pure Java record
- `InheritedRnfLine.java` — pure Java record
- `CreateEpicFromFeatureResult.java` — pure Java record

No changes to `pom.xml`. No new `<dependency>` declarations. No NPM, PyPI, or Go
module changes.

## Existing Dependency Versions (unchanged)

| GroupId | ArtifactId | Version | License | CVEs |
| :--- | :--- | :--- | :--- | :--- |
| `info.picocli` | `picocli` | `4.7.6` | Apache-2.0 | None known |
| `org.junit.jupiter` | `junit-jupiter` | `5.11.4` | EPL-2.0 | None known |
| `org.assertj` | `assertj-core` | `3.27.3` | Apache-2.0 | None known |

All versions comply with project policy. No CVEs detected for used versions.

## Transitive Dependency Impact

No new transitive dependencies introduced. picocli `4.7.6` has zero transitive
dependencies (self-contained JAR). The Java standard library modules used
(`java.nio.file`, `java.util`, `java.time`) are part of the JDK — no additional
classpath entries required.

## Skill Files (non-JAR)

SKILL.md files and golden fixtures are text resources — no binary dependencies, no
classpath impact, no runtime artifact loading.

## Verdict

**PASS** — No new Maven or ecosystem dependencies introduced. Existing dependency
versions comply with project policy. No CVEs detected. The implementation relies
exclusively on the Java standard library and the already-approved `info.picocli:picocli`
dependency, which is declared in `pom.xml` and has no known CVEs at version `4.7.6`.
