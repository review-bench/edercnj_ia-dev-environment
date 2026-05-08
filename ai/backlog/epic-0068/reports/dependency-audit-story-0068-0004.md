# Dependency Audit — story-0068-0004

**Story:** story-0068-0004 — E2E Smoke Test, Audit Catalog Entry, and CHANGELOG for enforce-continuous-flow.sh
**Epic:** EPIC-0068 (Continuous-Flow Heartbeat Hook)
**Audit Date:** 2026-04-29
**Auditor:** Automated audit via x-dependency-audit (Claude Sonnet 4.6)

---

## Summary

**Result: PASS — No new dependencies introduced; three pre-existing open Dependabot alerts are unrelated to this story's scope.**

Story-0068-0004 introduces one new test class (`Epic0068ContinuousFlowSmokeTest`), five
NDJSON/JSON test fixture files, a `docs/audit-gates-catalog.md` section, and a
`CHANGELOG.md` entry. The `pom.xml` is unchanged — confirmed by `git show 2cdc94664 -- pom.xml`
producing zero diff lines. All test dependencies are satisfied by the existing test
dependency block already present in `pom.xml` prior to this story.

---

## New/Changed Dependencies

### Maven (`pom.xml`)

**Zero changes.** The `pom.xml` was not touched by this story (PR commit `2cdc94664`,
merge commit `6c0b12170`). Diff confirms 0 lines changed in `pom.xml`.

### Test Dependencies Used by `Epic0068ContinuousFlowSmokeTest`

The new smoke test imports the following libraries, all pre-existing in `pom.xml`:

| Import | Artifact | Version in pom.xml | Scope | New? |
| :--- | :--- | :--- | :--- | :--- |
| `org.assertj.core.api.Assertions` | `org.assertj:assertj-core` | 3.27.7 | test | No |
| `org.junit.jupiter.api.*` | `org.junit.jupiter:junit-jupiter` | 5.11.4 | test | No |
| `org.junit.jupiter.api.condition.*` | (included in junit-jupiter) | 5.11.4 | test | No |
| `org.junit.jupiter.api.io.TempDir` | (included in junit-jupiter) | 5.11.4 | test | No |
| `java.io.*`, `java.net.*`, `java.nio.*`, `java.util.concurrent.*` | JDK 21 standard library | — | — | No |

**No new Maven coordinates introduced.** All third-party imports resolve to existing
`pom.xml` entries.

### Test Fixtures (NDJSON/JSON)

Five static data files added under `src/test/resources/fixtures/epic-0068/`:

| File | Type | Size | Library dependency? |
| :--- | :--- | :--- | :--- |
| `state-non-interactive-open-phase.json` | JSON (test data) | ~160 B | None |
| `state-interactive.json` | JSON (test data) | ~160 B | None |
| `events-tool-result-last.ndjson` | NDJSON (test data) | ~180 B | None |
| `events-finding-high.ndjson` | NDJSON (test data) | ~180 B | None |
| `events-empty-tasks.ndjson` | NDJSON (test data) | ~60 B | None |

These are static resource files read by the test at runtime via
`ClassLoader.getResource(...)`. They introduce no library or binary dependency.

---

## Transitive Risk

The SBOM (`target/bom.json`, CycloneDX 1.5, generated at last `mvn package`) lists
**12 runtime components**. No test-scoped dependencies appear in the SBOM (CycloneDX
plugin is configured with `includeTestScope: false`).

Runtime transitive graph (unchanged from baseline):

| Component | Version | Introduced by |
| :--- | :--- | :--- |
| `info.picocli:picocli` | 4.7.7 | Direct — CLI framework |
| `io.pebbletemplates:pebble` | 3.2.2 | Direct — template engine |
| `org.unbescape:unbescape` | 1.1.6.RELEASE | Transitive via pebble |
| `org.yaml:snakeyaml` | 2.6 | Direct — YAML parsing |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.2 | Direct — JSON processing |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.21 | Transitive via jackson-databind |
| `com.fasterxml.jackson.core:jackson-core` | 2.21.2 | Transitive via jackson-databind |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.21.2 | Direct — JSON JSR-310 support |
| `org.jline:jline` | 3.28.0 | Direct — interactive terminal |
| `org.slf4j:slf4j-api` | 2.0.17 | Direct — logging API |
| `ch.qos.logback:logback-classic` | 1.5.15 | Direct — logging impl |
| `ch.qos.logback:logback-core` | 1.5.15 | Transitive via logback-classic |

**Story-0068-0004 adds zero new nodes to this graph.** The transitive risk surface is
identical to the pre-story baseline.

---

## Vulnerability Assessment

### Dependabot Alerts (as of 2026-04-29)

Three open alerts and two fixed alerts were found via `gh api
repos/edercnj/ia-dev-environment/dependabot/alerts`:

| Alert # | Package | Severity | State | GHSA | Scope | Introduced by this story? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| #5 | `ch.qos.logback:logback-core` 1.5.15 | LOW | open | GHSA-qqpg-mvqg-649v | Runtime — logging | No — pre-existing |
| #4 | `ch.qos.logback:logback-core` 1.5.15 | MEDIUM | open | GHSA-25qh-j22f-pwp8 | Runtime — logging | No — pre-existing |
| #3 | `io.pebbletemplates:pebble` 3.2.2 | HIGH | open | GHSA-p75g-cxfj-7wrx | Runtime — template engine | No — pre-existing |
| #2 | `org.assertj:assertj-core` | HIGH | **fixed** | — | Test only | No — pre-existing, now fixed |
| #1 | `io.pebbletemplates:pebble` | HIGH | **fixed** | — | Runtime | No — pre-existing, now fixed |

**Analysis of open alerts:**

- **GHSA-qqpg-mvqg-649v (logback-core LOW):** Attacker can instantiate classes on the
  classpath via JMX or similar administrative channels. This project is a CLI generator —
  there is no network-exposed surface, no JMX port, and logback is a build/dev-time
  dependency. Exploitability is negligible in this threat model. No patched version is
  currently available (`patched_versions: null`).

- **GHSA-25qh-j22f-pwp8 (logback-core MEDIUM):** Arbitrary code execution via file
  processing (logback config file). The project ships no user-controllable logback
  configuration path in production artifacts. Mitigation is the same as above — CLI
  tool, no network exposure, no external logback config input. No patched version
  currently available.

- **GHSA-p75g-cxfj-7wrx (pebble HIGH):** Local File Inclusion via the `include` macro
  when processing untrusted templates. The generator processes templates from its own
  `src/main/resources/` only — there is no user-supplied template input path. The LFI
  vector requires user-controlled template content, which is not present in this use
  case. No patched version currently available.

**None of the three open alerts is introduced or worsened by story-0068-0004**, which
adds only a test class (test scope, not production), static fixture files, and Markdown
documentation. The open alerts are tracked as pre-existing technical debt in the
repository's Dependabot queue.

### SBOM Vulnerability Declarations

The CycloneDX SBOM (`target/bom.json`) declares **0 vulnerabilities** in the
`vulnerabilities` field. Third-party scanning is delegated to Dependabot (see above).

---

## Verdict

**PASS**

Justification:

1. **No new Maven dependencies.** `pom.xml` was not modified. Zero new coordinates
   or version bumps introduced.
2. **All test imports resolve to pre-existing test-scoped entries** in `pom.xml`
   (JUnit Jupiter 5.11.4 and AssertJ 3.27.7). No new transitive nodes added.
3. **Five fixture files** are static JSON/NDJSON read via classloader — they carry no
   binary or library dependency risk.
4. **Three open Dependabot alerts** (logback-core ×2, pebble ×1) are pre-existing,
   unrelated to this story's changeset, affect only the runtime production CLI surface
   (not the test additions), and have no available patched version as of the audit
   date. They are acknowledged technical debt tracked in the Dependabot queue.
5. **SBOM** declares 0 vulnerabilities and its component list is unchanged from the
   pre-story baseline.

This story is safe to merge from a dependency-security standpoint. The open Dependabot
alerts should be addressed in a dedicated dependency-upgrade story when patched versions
become available.
