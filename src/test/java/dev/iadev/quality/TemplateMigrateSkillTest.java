package dev.iadev.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates structural invariants of the {@code x-migrate-templates} SKILL.md (EPIC-0070 /
 * story-0070-0007).
 *
 * <p>Checks: frontmatter contract (model: sonnet, requires-capabilities), error codes,
 * idempotency/dry-run support, recovery contract, and ## Examples section.
 */
@DisplayName("TemplateMigrateSkillTest")
class TemplateMigrateSkillTest {

    private static final Path SKILL_FILE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "plan",
                    "x-migrate-templates",
                    "SKILL.md");

    @Test
    @DisplayName("skill_exists_atExpectedPath")
    void skill_exists_atExpectedPath() {
        assertThat(SKILL_FILE.toAbsolutePath())
                .as("x-migrate-templates SKILL.md must exist at core/plan/ (story-0070-0007)")
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("skill_frontmatter_hasModelSonnet")
    void skill_frontmatter_hasModelSonnet() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST declare model: sonnet (Rule 23 — parser + diff render)")
                .contains("model: sonnet");
    }

    @Test
    @DisplayName("skill_frontmatter_hasRequiresCapabilities")
    void skill_frontmatter_hasRequiresCapabilities() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST declare requires-capabilities"
                                + " [governance.value-driven-templates] (Rule 28)")
                .contains("requires-capabilities")
                .contains("governance.value-driven-templates");
    }

    @Test
    @DisplayName("skill_documentsParserErrorCode")
    void skill_documentsParserErrorCode() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST document PARSER_ERROR code (story-0070-0007 AC: Error)")
                .contains("PARSER_ERROR");
    }

    @Test
    @DisplayName("skill_documentsDryRunMode")
    void skill_documentsDryRunMode() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST document --dry-run flag (story-0070-0007 AC: Boundary)")
                .contains("--dry-run");
    }

    @Test
    @DisplayName("skill_documentsAtomicWrite")
    void skill_documentsAtomicWrite() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST document atomic write (story-0070-0007 AC: Error — file not written on parse failure)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("atomic"),
                        c -> assertThat(c).contains("Atomic"));
    }

    @Test
    @DisplayName("skill_documentsRecoveryStateFile")
    void skill_documentsRecoveryStateFile() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST document recovery state-file (story-0070-0007 §4 task-007-008)")
                .contains("template-migrate-")
                .contains(".json");
    }

    @Test
    @DisplayName("skill_documentsV2AlreadyDetectedDegenerate")
    void skill_documentsV2AlreadyDetectedDegenerate() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-migrate-templates MUST handle already-v2 epic"
                                + " (story-0070-0007 AC: Degenerate)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("already in v2"),
                        c -> assertThat(c).contains("already v2"),
                        c -> assertThat(c).contains("épico já em v2"));
    }

    @Test
    @DisplayName("skill_hasExamplesSection")
    void skill_hasExamplesSection() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-migrate-templates MUST have ## Examples section (story-0070-0007)")
                .contains("## Examples");
    }
}
