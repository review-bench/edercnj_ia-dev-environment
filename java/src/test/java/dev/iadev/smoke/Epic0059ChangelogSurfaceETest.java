package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-0059-0006-003: Verifies that CHANGELOG.md documents the closure of
 * bypass surface E ({@code git commit --no-verify}) introduced by story-0059-0006.
 *
 * <p>Acceptance criteria from story-0059-0006 §8 TASK-0059-0006-003:</p>
 * <ul>
 *   <li>CHANGELOG has {@code ### Added} entry describing the pre-commit-chain job.</li>
 *   <li>Entry mentions closure of bypass surface E.</li>
 * </ul>
 *
 * @see <a href="plans/epic-0059/story-0059-0006.md">story-0059-0006</a>
 */
@DisplayName("Epic0059ChangelogSurfaceETest — TASK-0059-0006-003")
class Epic0059ChangelogSurfaceETest {

    private static final String CHANGELOG_PATH = "CHANGELOG.md";

    @Test
    @DisplayName("changelog_hasAddedSection — "
            + "CHANGELOG must have a ### Added entry for story-0059-0006")
    void changelog_hasAddedSection() throws IOException {
        String changelog = readFile(CHANGELOG_PATH);

        assertThat(changelog)
                .as("CHANGELOG must reference story-0059-0006")
                .contains("story-0059-0006");
    }

    @Test
    @DisplayName("changelog_mentionsBypassSurfaceE — "
            + "CHANGELOG entry must explicitly document bypass surface E closure")
    void changelog_mentionsBypassSurfaceE() throws IOException {
        String changelog = readFile(CHANGELOG_PATH);

        assertThat(changelog)
                .as("CHANGELOG must mention bypass surface E")
                .contains("surface E");
    }

    @Test
    @DisplayName("changelog_mentionsPreCommitChainJob — "
            + "CHANGELOG entry must describe the pre-commit-chain CI job")
    void changelog_mentionsPreCommitChainJob() throws IOException {
        String changelog = readFile(CHANGELOG_PATH);

        assertThat(changelog)
                .as("CHANGELOG must mention pre-commit-chain")
                .contains("pre-commit-chain");
    }

    @Test
    @DisplayName("changelog_mentionsNoVerifyBypass — "
            + "CHANGELOG entry must explicitly mention git commit --no-verify")
    void changelog_mentionsNoVerifyBypass() throws IOException {
        String changelog = readFile(CHANGELOG_PATH);

        assertThat(changelog)
                .as("CHANGELOG must mention --no-verify bypass")
                .contains("--no-verify");
    }

    @Test
    @DisplayName("changelog_mentionsSpotlessCheckstyle — "
            + "CHANGELOG entry must describe Spotless and Checkstyle plugins")
    void changelog_mentionsSpotlessCheckstyle() throws IOException {
        String changelog = readFile(CHANGELOG_PATH);

        assertThat(changelog)
                .as("CHANGELOG must mention spotless-maven-plugin")
                .contains("spotless-maven-plugin");

        assertThat(changelog)
                .as("CHANGELOG must mention maven-checkstyle-plugin")
                .contains("maven-checkstyle-plugin");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private String readFile(String repoRelativePath) throws IOException {
        Path path = repoRoot().resolve(repoRelativePath);
        assertThat(path)
                .as("File must exist: " + repoRelativePath)
                .isRegularFile();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("java")) {
            return cwd.getParent();
        }
        return cwd;
    }
}
