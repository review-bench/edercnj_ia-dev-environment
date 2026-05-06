package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Repo-level smoke test for Rule 45 (CI-Watch Integrity).
 *
 * <p>Reads the committed reference golden ({@code java-spring} profile) and asserts the loadable
 * artifact contains both {@code CI_PASSED} (resolved as {@code SUCCESS} in the canonical contract)
 * and {@code CI_FAILED}, plus the canonical Exit Codes Matrix header.
 *
 * <p>Independent of {@code @TempDir} / pipeline regeneration; serves as a fast guard against drift
 * between source-of-truth and reference golden.
 */
@DisplayName("Rule45SmokeTest — reference golden carries CI_FAILED + SUCCESS")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX path resolution; mirrors sibling smoke tests.")
class Rule45SmokeTest {

    private static final String REFERENCE_GOLDEN_PATH =
            "src/test/resources/golden/java-spring/" + ".claude/rules/45-ci-watch-integrity.md";

    @Test
    @DisplayName("reference golden Rule 45 references SUCCESS and CI_FAILED")
    void smoke_referenceGolden_referencesCanonicalCodes() throws IOException {
        Path rule = repoRoot().resolve(REFERENCE_GOLDEN_PATH);
        assertThat(rule).as("reference golden Rule 45 must exist").exists();

        String body = Files.readString(rule, StandardCharsets.UTF_8);

        // EPIC-0078 story-0078-0013 slimmed Rule 45: exit codes now appear in compact form
        // "0=SUCCESS ... 20=CI_FAILED ..." and the header is "## Exit Codes (8 Stable — ...)".
        assertThat(body).as("Rule 45 must reference SUCCESS exit code").contains("SUCCESS");
        assertThat(body).as("Rule 45 must reference CI_FAILED exit code").contains("CI_FAILED");
        assertThat(body)
                .as("Rule 45 must declare the Exit Codes section header")
                .contains("## Exit Codes");
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
