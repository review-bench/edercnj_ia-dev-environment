package dev.iadev.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates structural invariants of the {@code x-arch-system-update} SKILL.md (EPIC-0070 /
 * story-0070-0006).
 *
 * <p>Checks: frontmatter contract (model: sonnet, requires-capabilities, user-invocable),
 * idempotency contract documentation, error codes, and ## Examples section.
 */
@DisplayName("ArchSystemUpdateSkillTest")
class ArchSystemUpdateSkillTest {

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
                    "x-arch-system-update",
                    "SKILL.md");

    @Test
    @DisplayName("skill_exists_atExpectedPath")
    void skill_exists_atExpectedPath() {
        assertThat(SKILL_FILE.toAbsolutePath())
                .as("x-arch-system-update SKILL.md must exist at core/plan/ (story-0070-0006)")
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("skill_frontmatter_hasModelSonnet")
    void skill_frontmatter_hasModelSonnet() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-arch-system-update MUST declare model: sonnet (Rule 23 — Reviewer tier)")
                .contains("model: sonnet");
    }

    @Test
    @DisplayName("skill_frontmatter_hasRequiresCapabilities")
    void skill_frontmatter_hasRequiresCapabilities() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-arch-system-update MUST declare requires-capabilities"
                                + " [governance.value-driven-templates] (Rule 28)")
                .contains("requires-capabilities")
                .contains("governance.value-driven-templates");
    }

    @Test
    @DisplayName("skill_frontmatter_isUserInvocable")
    void skill_frontmatter_isUserInvocable() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-arch-system-update is a public skill (Rule 22) — must NOT set user-invocable: false")
                .doesNotContain("user-invocable: false");
    }

    @Test
    @DisplayName("skill_documentsIdempotencyContract")
    void skill_documentsIdempotencyContract() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-arch-system-update MUST document idempotency contract (story-0070-0006 AC: Boundary)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("Idempotency"),
                        c -> assertThat(c).contains("idempoten"));
    }

    @Test
    @DisplayName("skill_documentsSystemMdMissingError")
    void skill_documentsSystemMdMissingError() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-arch-system-update MUST document SYSTEM_MD_MISSING error (story-0070-0006 AC: Error)")
                .contains("SYSTEM_MD_MISSING");
    }

    @Test
    @DisplayName("skill_documentsNoDryRunNoModifyCase")
    void skill_documentsNoDryRunNoModifyCase() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "x-arch-system-update MUST document degenerate case"
                                + " (no architectural decisions → system.md unchanged)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("no architectural decisions"),
                        c -> assertThat(c).contains("system.md not modified"),
                        c -> assertThat(c).contains("system.md unchanged"));
    }

    @Test
    @DisplayName("skill_hasExamplesSection")
    void skill_hasExamplesSection() throws IOException {
        String content = Files.readString(SKILL_FILE.toAbsolutePath(), StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-arch-system-update MUST have ## Examples section (story-0070-0006)")
                .contains("## Examples");
    }
}
