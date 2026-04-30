package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * E2E smoke test for EPIC-0067 Review YAML Frontmatter chain (story-0067-0004).
 *
 * <p>Validates: schema v1.0 is well-formed, specialist review fixture passes audit, tech-lead
 * review fixture passes audit, missing decision exits 1, invalid decision enum exits 1.
 *
 * <p>Last story of EPIC-0067 — gate that stories 0067-0001/0002/0003 all landed coherently.
 */
@DisplayName("Epic0067ReviewFrontmatterSmokeTest (E2E)")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class Epic0067ReviewFrontmatterSmokeTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path SCHEMA_PATH =
            REPO_ROOT.resolve("governance/schemas/review-frontmatter-1.0.json");
    private static final String SCRIPT_PATH =
            REPO_ROOT + "/src/main/resources/targets/claude/scripts/audit-review-frontmatter.sh";

    @TempDir Path tempDir;

    @BeforeEach
    void setUpFakeRepo() throws IOException, InterruptedException {
        ProcessBuilder git = new ProcessBuilder("git", "init", "-q");
        git.directory(tempDir.toFile());
        git.start().waitFor(10, TimeUnit.SECONDS);

        Path schemaDir = tempDir.resolve("governance/schemas");
        Files.createDirectories(schemaDir);
        Files.copy(SCHEMA_PATH, schemaDir.resolve("review-frontmatter-1.0.json"));

        Path baselineDir = tempDir.resolve("governance/baselines");
        Files.createDirectories(baselineDir);
        Files.writeString(
                baselineDir.resolve("review-frontmatter-baseline.txt"),
                "# empty\n",
                StandardCharsets.UTF_8);
    }

    private ProcessResult runAudit(List<String> args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("/bin/bash");
        cmd.add(SCRIPT_PATH);
        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(tempDir.toFile());

        Process proc = pb.start();
        boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
        assertThat(finished).as("Script must complete within 30s").isTrue();

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(proc.exitValue(), stdout, stderr);
    }

    private void writeReviewFile(String path, String content) throws IOException {
        Path file = tempDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    // ── Scenario 1 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("schema10_isWellFormed_requiredFieldsPresent")
    void schema10_isWellFormed_requiredFieldsPresent() throws IOException {
        assertThat(SCHEMA_PATH).as("review-frontmatter-1.0.json must exist").exists();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schema = mapper.readTree(SCHEMA_PATH.toFile());

        assertThat(schema.has("$schema")).as("schema must have $schema").isTrue();
        assertThat(schema.has("required")).as("schema must have required array").isTrue();
        assertThat(schema.get("required").isArray()).isTrue();

        List<String> required = new ArrayList<>();
        schema.get("required").forEach(n -> required.add(n.asText()));

        assertThat(required)
                .containsExactlyInAnyOrder(
                        "schema-version",
                        "generated-by",
                        "story-id",
                        "epic-id",
                        "date",
                        "decision",
                        "score",
                        "score-max",
                        "severity-counts",
                        "blocking-findings");
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("specialistReviewFixture_passesAuditValidation")
    void specialistReviewFixture_passesAuditValidation() throws IOException, InterruptedException {
        writeReviewFile(
                "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: story-0067-0002
                epic-id: EPIC-0067
                date: 2026-04-29T12:00:00Z
                decision: GO
                score: 48
                score-max: 50
                severity-counts:
                  critical: 0
                  high: 0
                  medium: 1
                  low: 0
                  info: 0
                blocking-findings: []
                reviewers:
                  - QA
                ---
                # Specialist Review

                ## Decision

                GO
                """);

        ProcessResult r = runAudit(List.of("--story", "story-0067-0002"));
        assertThat(r.exitCode())
                .as("Specialist review fixture must pass audit, stderr=%s".formatted(r.stderr()))
                .isEqualTo(0);
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("techLeadReviewFixture_passesAuditValidation_hasChecklist")
    void techLeadReviewFixture_passesAuditValidation_hasChecklist()
            throws IOException, InterruptedException {
        writeReviewFile(
                "ai/epics/epic-0067/plans/techlead-review-story-0067-0003.md",
                """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review-pr@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: story-0067-0003
                epic-id: EPIC-0067
                date: 2026-04-29T12:00:00Z
                decision: GO
                score: 53
                score-max: 55
                severity-counts:
                  critical: 0
                  high: 0
                  medium: 1
                  low: 0
                  info: 0
                blocking-findings: []
                checklist:
                  passed: 44
                  total: 45
                  failed-sections: []
                ---
                # Tech Lead Review

                ## Decision

                GO
                """);

        ProcessResult r = runAudit(List.of("--story", "story-0067-0003"));
        assertThat(r.exitCode())
                .as("Tech-lead review fixture must pass audit, stderr=%s".formatted(r.stderr()))
                .isEqualTo(0);
    }

    // ── Scenario 4 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reviewFixture_missingDecision_auditExitsOneWithViolation")
    void reviewFixture_missingDecision_auditExitsOneWithViolation()
            throws IOException, InterruptedException {
        writeReviewFile(
                "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: story-0067-0002
                epic-id: EPIC-0067
                date: 2026-04-29T12:00:00Z
                score: 47
                score-max: 50
                severity-counts:
                  critical: 0
                  high: 0
                  medium: 0
                  low: 0
                  info: 0
                blocking-findings: []
                ---
                # Review without decision field
                """);

        ProcessResult r = runAudit(List.of("--story", "story-0067-0002"));
        assertThat(r.exitCode()).isEqualTo(1);
        assertThat(r.stderr()).contains("REVIEW_FRONTMATTER_VIOLATION");
        assertThat(r.stderr()).contains("decision");
    }

    // ── Scenario 5 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reviewFixture_invalidDecisionValue_auditExitsOne")
    void reviewFixture_invalidDecisionValue_auditExitsOne()
            throws IOException, InterruptedException {
        writeReviewFile(
                "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: story-0067-0002
                epic-id: EPIC-0067
                date: 2026-04-29T12:00:00Z
                decision: APPROVED
                score: 47
                score-max: 50
                severity-counts:
                  critical: 0
                  high: 0
                  medium: 0
                  low: 0
                  info: 0
                blocking-findings: []
                ---
                # Review with invalid decision value
                """);

        ProcessResult r = runAudit(List.of("--story", "story-0067-0002"));
        assertThat(r.exitCode()).isEqualTo(1);
        assertThat(r.stderr()).contains("REVIEW_FRONTMATTER_VIOLATION");
        assertThat(r.stderr()).contains("APPROVED");
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
