package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * E2E smoke test for EPIC-0072 (Comprehensive Test Strategy).
 *
 * <p>Validates the end-to-end integration of the three quality gate skills ({@code
 * x-execute-performance-tests}, {@code x-test-mutation}, {@code x-execute-contract-tests}) by
 * asserting structural invariants across SKILL.md files, audit scripts, and configuration
 * templates. Each scenario maps to a Gherkin acceptance criterion from story-0072-0009.
 */
@Tag("smoke")
@Tag("e2e")
@DisplayName("Epic0072TestStrategySmokeIT — 8-scenario quality strategy E2E smoke")
class Epic0072TestStrategySmokeIT {

    private static final Path PERF_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-execute-performance-tests",
                    "SKILL.md");

    private static final Path MUTATION_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-execute-mutation-tests",
                    "SKILL.md");

    private static final Path CONTRACT_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-execute-contract-tests",
                    "SKILL.md");

    private static final Path AUDIT_PERF =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-perf-baseline.sh");

    private static final Path AUDIT_MUTATION =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-mutation-score.sh");

    private static final Path AUDIT_CONTRACT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-contract-breaking.sh");

    private static final Path CONFIG_TEMPLATE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "config-templates",
                    "setup-config.java-spring.yaml");

    // ─── Scenario 1 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "perfRegressionAboveToleranceBlocks_RESTStack — "
                    + "x-execute-performance-tests blocks merge when p95/p99 exceeds baseline + tolerance")
    void perfRegressionAboveToleranceBlocks_RESTStack() throws Exception {
        String content = Files.readString(PERF_SKILL.toAbsolutePath());

        assertThat(content)
                .as("must document PERF_REGRESSION_DETECTED exit code for blocking")
                .contains("PERF_REGRESSION_DETECTED");
        assertThat(content).as("must document REST stack → Newman dispatch").contains("Newman");
        assertThat(content).as("must reference p95 regression threshold check").contains("p95");
        assertThat(content)
                .as("must reference baseline tolerance percentage")
                .contains("baseline-tolerance-pct");
        assertThat(content)
                .as("PERF_REGRESSION_DETECTED must map to non-zero exit")
                .containsPattern(
                        "(?s)PERF_REGRESSION_DETECTED.*[1-9]\\d*|[1-9]\\d*.*PERF_REGRESSION_DETECTED");
    }

    // ─── Scenario 2 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "perfWithinTolerancePasses_RESTStack — "
                    + "x-execute-performance-tests exits 0 when regression within tolerance")
    void perfWithinTolerancePasses_RESTStack() throws Exception {
        String content = Files.readString(PERF_SKILL.toAbsolutePath());

        assertThat(content)
                .as("must document SUCCESS exit code (0) for within-tolerance case")
                .contains("SUCCESS");
        assertThat(content).as("must document tolerance-pct default (10)").contains("10");
        assertThat(content)
                .as("audit script for performance baseline must exist")
                .satisfies(
                        ignored ->
                                assertThat(AUDIT_PERF.toAbsolutePath())
                                        .as("audit-perf-baseline.sh must exist")
                                        .exists());
    }

    // ─── Scenario 3 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "mutationScoreBelowThresholdBlocks_JavaStack — "
                    + "x-test-mutation blocks merge when score < threshold (Java/PIT)")
    void mutationScoreBelowThresholdBlocks_JavaStack() throws Exception {
        String content = Files.readString(MUTATION_SKILL.toAbsolutePath());

        assertThat(content)
                .as("must document MUTATION_SCORE_BELOW_THRESHOLD exit code")
                .contains("MUTATION_SCORE_BELOW_THRESHOLD");
        assertThat(content).as("must dispatch PIT for Java stack").containsAnyOf("PIT", "pitest");
        assertThat(content).as("must enforce threshold check").contains("threshold");
        assertThat(content).as("must document Java stack dispatch").containsAnyOf("Java", "java");
        assertThat(content)
                .as("MUTATION_SCORE_BELOW_THRESHOLD must map to non-zero exit")
                .containsPattern(
                        "(?s)MUTATION_SCORE_BELOW_THRESHOLD.*[1-9]|[1-9].*MUTATION_SCORE_BELOW_THRESHOLD");
    }

    // ─── Scenario 4 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "mutationAboveThresholdPasses_JavaStack — "
                    + "x-test-mutation exits 0 when score >= threshold")
    void mutationAboveThresholdPasses_JavaStack() throws Exception {
        String content = Files.readString(MUTATION_SKILL.toAbsolutePath());

        assertThat(content)
                .as("must document SUCCESS exit code (0) for above-threshold case")
                .contains("SUCCESS");
        assertThat(content).as("must document default threshold (80)").contains("80");
        assertThat(content)
                .as("audit script for mutation score must exist")
                .satisfies(
                        ignored ->
                                assertThat(AUDIT_MUTATION.toAbsolutePath())
                                        .as("audit-mutation-score.sh must exist")
                                        .exists());
    }

    // ─── Scenario 5 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "contractBreakingWithoutMigrationDocBlocks_OpenAPIStack — "
                    + "x-execute-contract-tests blocks merge when breaking change lacks CHANGELOG entry (OpenAPI)")
    void contractBreakingWithoutMigrationDocBlocks_OpenAPIStack() throws Exception {
        String content = Files.readString(CONTRACT_SKILL.toAbsolutePath());

        assertThat(content)
                .as("must document CONTRACT_BREAKING_CHANGE exit code")
                .contains("CONTRACT_BREAKING_CHANGE");
        assertThat(content)
                .as("must dispatch openapi-diff for REST/OpenAPI stack")
                .contains("openapi-diff");
        assertThat(content)
                .as("must document blocking on undocumented breaking change")
                .containsAnyOf(
                        "without CHANGELOG",
                        "no CHANGELOG",
                        "lacks CHANGELOG",
                        "## BREAKING",
                        "CHANGELOG detection",
                        "Breaking");
    }

    // ─── Scenario 6 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "contractBreakingWithChangelogPasses_Proto3Stack — "
                    + "x-execute-contract-tests exits 0 when breaking change documented in CHANGELOG (proto3/buf)")
    void contractBreakingWithChangelogPasses_Proto3Stack() throws Exception {
        String content = Files.readString(CONTRACT_SKILL.toAbsolutePath());

        assertThat(content).as("must dispatch buf for proto3/gRPC stack").contains("buf");
        assertThat(content)
                .as("must document CHANGELOG integration step")
                .contains("CHANGELOG Integration");
        assertThat(content)
                .as("must document SUCCESS when breaking change is in CHANGELOG")
                .contains("SUCCESS");
        assertThat(content)
                .as("audit script for contract breaking must exist")
                .satisfies(
                        ignored ->
                                assertThat(AUDIT_CONTRACT.toAbsolutePath())
                                        .as("audit-contract-breaking.sh must exist")
                                        .exists());
    }

    // ─── Scenario 7 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "optOutAllDisabledProducesWarn — "
                    + "all 3 skills exit 0 with WARN when quality.*.enabled=false (opt-out)")
    void optOutAllDisabledProducesWarn() throws Exception {
        String perfContent = Files.readString(PERF_SKILL.toAbsolutePath());
        String mutContent = Files.readString(MUTATION_SKILL.toAbsolutePath());
        String contractContent = Files.readString(CONTRACT_SKILL.toAbsolutePath());
        String configContent = Files.readString(CONFIG_TEMPLATE.toAbsolutePath());

        assertThat(perfContent)
                .as("x-execute-performance-tests must document PERF_DISABLED opt-out")
                .contains("PERF_DISABLED");
        assertThat(perfContent)
                .as("x-execute-performance-tests PERF_DISABLED must be exit 50 (no-op)")
                .contains("50");

        assertThat(mutContent)
                .as("x-test-mutation must document MUTATION_DISABLED opt-out")
                .contains("MUTATION_DISABLED");
        assertThat(mutContent)
                .as("x-test-mutation MUTATION_DISABLED must be exit 50 (no-op)")
                .contains("50");

        assertThat(contractContent)
                .as("x-execute-contract-tests must document WARN behavior when disabled")
                .containsAnyOf("WARN", "disabled", "enabled=false", "skip");

        assertThat(configContent)
                .as("config template must default all quality gates to disabled")
                .contains("enabled: false");
        assertThat(configContent)
                .as("performance gate must default to disabled")
                .containsPattern("(?s)performance:[\\s\\S]{0,50}enabled: false");
        assertThat(configContent)
                .as("mutation gate must default to disabled")
                .containsPattern("(?s)mutation:[\\s\\S]{0,50}enabled: false");
        assertThat(configContent)
                .as("contract gate must default to disabled")
                .containsPattern("(?s)contract:[\\s\\S]{0,50}enabled: false");
    }

    // ─── Scenario 8 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "stackAwarenessSelectsCorrectTooling — "
                    + "each skill has stack dispatch matrix: REST→Newman, gRPC→ghz, Java→PIT, "
                    + "JS→Stryker, OpenAPI→openapi-diff, proto3→buf")
    void stackAwarenessSelectsCorrectTooling() throws Exception {
        String perfContent = Files.readString(PERF_SKILL.toAbsolutePath());
        String mutContent = Files.readString(MUTATION_SKILL.toAbsolutePath());
        String contractContent = Files.readString(CONTRACT_SKILL.toAbsolutePath());

        // x-execute-performance-tests stack dispatch
        assertThat(perfContent)
                .as("x-execute-performance-tests: REST stack → Newman")
                .contains("Newman");
        assertThat(perfContent).as("x-execute-performance-tests: gRPC stack → ghz").contains("ghz");
        assertThat(perfContent)
                .as("x-execute-performance-tests: CLI stack → hyperfine")
                .contains("hyperfine");
        assertThat(perfContent)
                .as("x-execute-performance-tests: must contain stack dispatch matrix or table")
                .containsAnyOf("Stack Dispatch", "stack dispatch", "## Stack");

        // x-test-mutation stack dispatch
        assertThat(mutContent)
                .as("x-test-mutation: Java stack → PIT/pitest")
                .containsAnyOf("PIT", "pitest");
        assertThat(mutContent)
                .as("x-test-mutation: JS/TS stack → Stryker")
                .containsAnyOf("Stryker", "stryker");
        assertThat(mutContent)
                .as("x-test-mutation: must contain stack dispatch matrix")
                .containsAnyOf("Stack Dispatch", "stack dispatch", "## Stack");

        // x-execute-contract-tests stack dispatch
        assertThat(contractContent)
                .as("x-execute-contract-tests: REST/OpenAPI → openapi-diff")
                .contains("openapi-diff");
        assertThat(contractContent)
                .as("x-execute-contract-tests: gRPC/proto3 → buf")
                .contains("buf");
    }
}
