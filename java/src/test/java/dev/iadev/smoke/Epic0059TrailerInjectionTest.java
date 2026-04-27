package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0004-002: Verify trailer injection contract in x-internal-status-update SKILL.md.
 *
 * <p>Validates the Acceptance Criterion of story-0059-0004 TASK-002: the {@code
 * x-internal-status-update} skill documents the canonical trailer format and injection pattern
 * required by the {@code .githooks/commit-msg} hook (surface F guard of EPIC-0059).
 *
 * @see <a href="plans/epic-0059/story-0059-0004.md">story-0059-0004</a>
 */
@DisplayName("Epic0059TrailerInjectionTest — TASK-0059-0004-002")
class Epic0059TrailerInjectionTest {

    private static final String CANONICAL_SKILL_PATH =
            "java/src/main/resources/targets/claude/skills/core/"
                    + "internal/ops/x-internal-status-update/SKILL.md";

    @Test
    @DisplayName(
            "trailerInjection_skillMdDocumentsCanonicalFormat — "
                    + "x-internal-status-update SKILL.md must contain "
                    + "the canonical Co-Authored-By trailer format "
                    + "(story-0059-0004 TASK-002 acceptance criterion)")
    void trailerInjection_skillMdDocumentsCanonicalFormat() throws IOException {
        Path skillMd = resolveRepoRoot().resolve(CANONICAL_SKILL_PATH);
        assertThat(skillMd).as("x-internal-status-update SKILL.md must exist").isRegularFile();

        String content = Files.readString(skillMd, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "SKILL.md must document the Trailer Injection "
                                + "Contract section (story-0059-0004)")
                .contains("## Trailer Injection Contract");

        assertThat(content)
                .as("SKILL.md must document canonical " + "Co-Authored-By trailer format")
                .contains("Co-Authored-By: x-internal-status-update@");

        assertThat(content)
                .as("SKILL.md must document git rev-parse HEAD " + "for SHA capture")
                .contains("git rev-parse HEAD");

        assertThat(content)
                .as("SKILL.md must document --trailer argument " + "in commit invocation pattern")
                .contains("--trailer");
    }

    @Test
    @DisplayName(
            "trailerInjection_skillMdDocumentsRegexPattern — "
                    + "SKILL.md must contain the validation regex "
                    + "used by the commit-msg hook "
                    + "(story-0059-0004 TASK-002)")
    void trailerInjection_skillMdDocumentsRegexPattern() throws IOException {
        Path skillMd = resolveRepoRoot().resolve(CANONICAL_SKILL_PATH);
        String content = Files.readString(skillMd, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "SKILL.md must document the trailer regex "
                                + "pattern for [0-9a-f]{40} SHA validation")
                .contains("[0-9a-f]{40}");

        assertThat(content)
                .as("SKILL.md must reference git interpret-trailers " + "for parse-safe validation")
                .contains("git interpret-trailers");
    }

    private Path resolveRepoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
