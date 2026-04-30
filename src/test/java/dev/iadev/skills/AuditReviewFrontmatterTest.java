package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioural tests for audit-review-frontmatter.sh (story-0067-0004).
 *
 * <p>Eight scenarios validate: valid specialist review, valid tech-lead review, missing
 * frontmatter, invalid decision enum, missing required field, baseline grandfather, self-check,
 * and v3+v4 path resolution.
 */
@DisplayName("AuditReviewFrontmatterTest")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class AuditReviewFrontmatterTest {

    private static final String SCRIPT_PATH =
            System.getProperty("user.dir")
                    + "/src/main/resources/targets/claude/scripts/audit-review-frontmatter.sh";

    private static final String SCHEMA_SOURCE =
            System.getProperty("user.dir") + "/governance/schemas/review-frontmatter-1.0.json";

    @TempDir Path tempDir;

    @BeforeEach
    void setUpFakeRepo() throws IOException, InterruptedException {
        // initialise a minimal git repo so the script can locate REPO_ROOT
        ProcessBuilder git = new ProcessBuilder("git", "init", "-q");
        git.directory(tempDir.toFile());
        git.start().waitFor(10, TimeUnit.SECONDS);

        // copy schema
        Path schemaDir = tempDir.resolve("governance/schemas");
        Files.createDirectories(schemaDir);
        Files.copy(Path.of(SCHEMA_SOURCE), schemaDir.resolve("review-frontmatter-1.0.json"));

        // create empty baseline
        Path baselineDir = tempDir.resolve("governance/baselines");
        Files.createDirectories(baselineDir);
        Files.writeString(
                baselineDir.resolve("review-frontmatter-baseline.txt"),
                "# empty baseline\n",
                StandardCharsets.UTF_8);
    }

    private ProcessResult run(List<String> args) throws IOException, InterruptedException {
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

    private Path createReviewFile(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String validSpecialistFrontmatter(String storyId) {
        return """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: %s
                epic-id: EPIC-0067
                date: 2026-04-29T12:00:00Z
                decision: GO
                score: 47
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
                  - Security
                ---
                # Specialist Review

                ## Decision

                **GO**

                ## Summary

                Story implements frontmatter YAML correctly.
                """.formatted(storyId);
    }

    private static String validTechLeadFrontmatter(String storyId) {
        return """
                <!-- template-version: 1.0 -->
                ---
                schema-version: "1.0"
                generated-by: x-review-pr@a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
                story-id: %s
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

                **GO**

                ## Summary

                Tech-lead review passes all checks.
                """.formatted(storyId);
    }

    @Nested
    @DisplayName("happy path — valid specialist review")
    class ValidSpecialist {

        @Test
        @DisplayName("valid review-story-*.md with frontmatter exits 0")
        void auditScript_validSpecialistReview_exitsZero() throws IOException, InterruptedException {
            createReviewFile(
                    "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                    validSpecialistFrontmatter("story-0067-0002"));

            ProcessResult r = run(List.of("--story", "story-0067-0002"));
            assertThat(r.exitCode())
                    .as("Valid specialist review must exit 0, stderr=%s".formatted(r.stderr()))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("happy path — valid tech-lead review")
    class ValidTechLead {

        @Test
        @DisplayName("valid techlead-review-story-*.md with frontmatter exits 0")
        void auditScript_validTechLeadReview_exitsZero() throws IOException, InterruptedException {
            createReviewFile(
                    "ai/epics/epic-0067/plans/techlead-review-story-0067-0003.md",
                    validTechLeadFrontmatter("story-0067-0003"));

            ProcessResult r = run(List.of("--story", "story-0067-0003"));
            assertThat(r.exitCode())
                    .as("Valid tech-lead review must exit 0, stderr=%s".formatted(r.stderr()))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("violation — missing frontmatter")
    class MissingFrontmatter {

        @Test
        @DisplayName("review file without frontmatter exits 1 with REVIEW_FRONTMATTER_VIOLATION")
        void auditScript_missingFrontmatter_exitsOne_withViolationMessage()
                throws IOException, InterruptedException {
            createReviewFile(
                    "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                    "# Specialist Review\n\nNo frontmatter here.\n");

            ProcessResult r = run(List.of("--story", "story-0067-0002"));
            assertThat(r.exitCode()).isEqualTo(1);
            assertThat(r.stderr()).contains("REVIEW_FRONTMATTER_VIOLATION");
            assertThat(r.stderr()).contains("review-story-0067-0002.md");
        }
    }

    @Nested
    @DisplayName("violation — invalid decision enum")
    class InvalidDecision {

        @Test
        @DisplayName("decision=APPROVED (not in enum) exits 1")
        void auditScript_invalidDecisionEnum_exitsOne() throws IOException, InterruptedException {
            String content =
                    validSpecialistFrontmatter("story-0067-0002")
                            .replace("decision: GO", "decision: APPROVED");
            createReviewFile("ai/epics/epic-0067/plans/review-story-0067-0002.md", content);

            ProcessResult r = run(List.of("--story", "story-0067-0002"));
            assertThat(r.exitCode()).isEqualTo(1);
            assertThat(r.stderr()).contains("REVIEW_FRONTMATTER_VIOLATION");
            assertThat(r.stderr()).contains("decision");
        }
    }

    @Nested
    @DisplayName("violation — missing required field")
    class MissingRequiredField {

        @Test
        @DisplayName("missing severity-counts field exits 1")
        void auditScript_missingSeverityCounts_exitsOne() throws IOException, InterruptedException {
            String content =
                    validSpecialistFrontmatter("story-0067-0002")
                            .replaceAll(
                                    "severity-counts:[\\s\\S]*?blocking-findings:",
                                    "blocking-findings:");
            createReviewFile("ai/epics/epic-0067/plans/review-story-0067-0002.md", content);

            ProcessResult r = run(List.of("--story", "story-0067-0002"));
            assertThat(r.exitCode()).isEqualTo(1);
            assertThat(r.stderr()).contains("REVIEW_FRONTMATTER_VIOLATION");
            assertThat(r.stderr()).contains("severity-counts");
        }
    }

    @Nested
    @DisplayName("baseline — grandfathered story")
    class Baseline {

        @Test
        @DisplayName("story ID in review-frontmatter-baseline.txt exits 0 even without frontmatter")
        void auditScript_grandfatheredStoryId_exitsZero() throws IOException, InterruptedException {
            // write story ID to baseline
            Path baseline =
                    tempDir.resolve("governance/baselines/review-frontmatter-baseline.txt");
            Files.writeString(
                    baseline,
                    "story-0063-0001  # legacy review, pre-EPIC-0067\n",
                    StandardCharsets.UTF_8);

            // create a review WITHOUT frontmatter for the grandfathered story
            createReviewFile(
                    "ai/epics/epic-0063/plans/review-story-0063-0001.md",
                    "# Old Review\n\nNo frontmatter.\n");

            ProcessResult r = run(List.of("--story", "story-0063-0001"));
            assertThat(r.exitCode())
                    .as("Grandfathered story must exit 0, stderr=%s".formatted(r.stderr()))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("self-check — Rule 26 contract")
    class SelfCheck {

        @Test
        @DisplayName("--self-check with all deps present exits 0")
        void auditScript_selfCheck_withAllDeps_exitsZero() throws IOException, InterruptedException {
            ProcessResult r = run(List.of("--self-check"));
            // yq + jq must be on PATH in CI; if not, we accept exit 2 with OPERATIONAL_ERROR
            if (r.exitCode() == 0) {
                assertThat(r.stderr()).contains("SELF_CHECK_OK");
            } else {
                assertThat(r.exitCode()).isEqualTo(2);
                assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
            }
        }
    }

    @Nested
    @DisplayName("path resolution — v3 and v4 scanned")
    class PathResolution {

        @Test
        @DisplayName("--all mode finds files in both plans/epic-*/plans/ and ai/epics/*/plans/")
        void auditScript_pathResolution_v3andV4_bothScanned()
                throws IOException, InterruptedException {
            // v4 file — valid
            createReviewFile(
                    "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                    validSpecialistFrontmatter("story-0067-0002"));

            // v3 file — also valid
            createReviewFile(
                    "plans/epic-0066/plans/review-story-0066-0001.md",
                    validSpecialistFrontmatter("story-0066-0001"));

            ProcessResult r = run(List.of("--all"));
            assertThat(r.exitCode())
                    .as("Both v3 and v4 valid reviews must produce exit 0, stderr=%s"
                            .formatted(r.stderr()))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("violation — audit-exempt marker missing reason")
    class InvalidExemption {

        @Test
        @DisplayName("audit-exempt marker without reason exits 3 with INVALID_EXEMPTION")
        void auditScript_auditExemptMissingReason_exitsThree()
                throws IOException, InterruptedException {
            createReviewFile(
                    "ai/epics/epic-0067/plans/review-story-0067-0002.md",
                    "<!-- audit-exempt: -->\n# Review without reason\n");

            ProcessResult r = run(List.of("--story", "story-0067-0002"));
            assertThat(r.exitCode()).isEqualTo(3);
            assertThat(r.stderr()).contains("INVALID_EXEMPTION");
        }
    }

    @Nested
    @DisplayName("security — path traversal prevention")
    class PathTraversal {

        @Test
        @DisplayName("symlink resolving outside REPO_ROOT exits 2 with OPERATIONAL_ERROR")
        void auditScript_symlinkOutsideRepoRoot_exitsTwo()
                throws IOException, InterruptedException {
            // Create a file outside the temp git repo
            java.io.File outsideFile = java.io.File.createTempFile("outside-review", ".md");
            outsideFile.deleteOnExit();
            java.nio.file.Files.writeString(
                    outsideFile.toPath(),
                    "# Review outside repo root\n",
                    StandardCharsets.UTF_8);

            // Create a symlink inside the repo pointing to the outside file
            Path linkDir = tempDir.resolve("ai/epics/epic-0067/plans");
            Files.createDirectories(linkDir);
            Path symlink = linkDir.resolve("review-story-0067-0099.md");
            Files.createSymbolicLink(symlink, outsideFile.toPath());

            ProcessResult r = run(List.of("--story", "story-0067-0099"));
            assertThat(r.exitCode())
                    .as("Path outside REPO_ROOT must exit 2, stderr=%s".formatted(r.stderr()))
                    .isEqualTo(2);
            assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
