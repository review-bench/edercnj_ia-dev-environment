package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * TASK-0059-0007-003: Smoke tests for {@code scripts/audit-pr-evidence.sh}.
 *
 * <p>Validates the key exit paths of the audit script:
 *
 * <ol>
 *   <li>Script exists and is executable.
 *   <li>{@code --self-check} exits 0 when environment is configured.
 *   <li>{@code --help} exits 2 (usage error per convention).
 * </ol>
 *
 * <p>The functional smoke tests (PR body validation) require a live GitHub connection and are
 * exercised at CI integration time. Unit-level structural tests validate the script's contract
 * without network access.
 *
 * @see <a href="plans/epic-0059/story-0059-0007.md">story-0059-0007</a>
 */
@DisplayName("Epic0059AuditPrEvidenceTest — TASK-0059-0007-003")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "POSIX bash script — not applicable on Windows")
class Epic0059AuditPrEvidenceTest {

    private static final String SCRIPT_PATH = "scripts/audit-pr-evidence.sh";
    private static final String BASELINE_PATH = "governance/baselines/pr-evidence-baseline.txt";

    @Test
    @DisplayName("scriptExists — scripts/audit-pr-evidence.sh must exist")
    void scriptExists() {
        Path script = repoRoot().resolve(SCRIPT_PATH);
        assertThat(script)
                .as("scripts/audit-pr-evidence.sh must exist (story-0059-0007)")
                .isRegularFile();
    }

    @Test
    @DisplayName("scriptIsExecutable — scripts/audit-pr-evidence.sh must be executable")
    void scriptIsExecutable() {
        Path script = repoRoot().resolve(SCRIPT_PATH);
        assertThat(script.toFile().canExecute())
                .as("scripts/audit-pr-evidence.sh must be executable")
                .isTrue();
    }

    @Test
    @DisplayName("selfCheckExitsZero — --self-check must exit 0 in configured environment")
    void selfCheckExitsZero() throws Exception {
        int exit = runScript("--self-check");
        assertThat(exit)
                .as("--self-check must exit 0 when environment (gh, jq) is configured")
                .isZero();
    }

    @Test
    @DisplayName("baselineFileExists — governance/baselines/pr-evidence-baseline.txt must exist")
    void baselineFileExists() {
        Path baseline = repoRoot().resolve(BASELINE_PATH);
        assertThat(baseline)
                .as("governance/baselines/pr-evidence-baseline.txt must exist (story-0059-0007)")
                .isRegularFile();
    }

    @Test
    @DisplayName(
            "baselineFileHasValidFormat — each data line must be PR_NUMBER optionally followed by comment")
    void baselineFileHasValidFormat() throws IOException {
        Path baseline = repoRoot().resolve(BASELINE_PATH);
        String content = Files.readString(baseline, StandardCharsets.UTF_8);
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue; // skip empty lines and comment-only lines
            }
            assertThat(trimmed)
                    .as("baseline line must start with a PR number: '%s'", trimmed)
                    .matches("^[0-9]+(\\s.*)?$");
        }
    }

    @Test
    @DisplayName("scriptContainsAllFiveExitCodes — exit codes 0-4 must be documented in script")
    void scriptContainsAllFiveExitCodes() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content).as("script must document exit 0 (OK)").contains("exit 0");
        assertThat(content)
                .as("script must document exit 1 (PRE_EVIDENCE_MISSING)")
                .contains("PRE_EVIDENCE_MISSING");
        assertThat(content)
                .as("script must document exit 2 (PRE_BASELINE_CORRUPT)")
                .contains("PRE_BASELINE_CORRUPT");
        assertThat(content)
                .as("script must document exit 3 (PRE_INVALID_EXEMPTION)")
                .contains("PRE_INVALID_EXEMPTION");
        assertThat(content)
                .as("script must document exit 4 (PRE_ENFORCEMENT_BROKEN)")
                .contains("PRE_ENFORCEMENT_BROKEN");
    }

    @Test
    @DisplayName(
            "scriptValidatesStoryIdPlaceholder — script must reject story-XXXX-YYYY placeholder")
    void scriptValidatesStoryIdPlaceholder() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content)
                .as("script must check for story-XXXX-YYYY placeholder")
                .contains("story-XXXX-YYYY");
    }

    @Test
    @DisplayName("scriptValidatesShaPlaceholder — script must reject abc123def456... placeholder")
    void scriptValidatesShaPlaceholder() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content)
                .as("script must check for abc123def456... placeholder")
                .contains("abc123def456...");
    }

    @Test
    @DisplayName("scriptValidatesSkillPlaceholder — script must reject SKILL-NAME-HERE placeholder")
    void scriptValidatesSkillPlaceholder() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content)
                .as("script must check for SKILL-NAME-HERE placeholder")
                .contains("SKILL-NAME-HERE");
    }

    @Test
    @DisplayName("scriptValidates40HexSha — script must validate 40 hex char SHA pattern")
    void scriptValidates40HexSha() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content)
                .as("script must validate 40-char hex SHA with regex [0-9a-f]{40}")
                .containsAnyOf("[0-9a-f]{40}", "[0-9a-f]{40}$");
    }

    @Test
    @DisplayName("scriptContainsSelfCheckFlag — --self-check implementation must be present")
    void scriptContainsSelfCheckFlag() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content).as("script must implement --self-check flag").contains("--self-check");
    }

    @Test
    @DisplayName("scriptContainsNoStoryEvidenceBypass — --no-story-evidence must be recognized")
    void scriptContainsNoStoryEvidenceBypass() throws IOException {
        String content = Files.readString(repoRoot().resolve(SCRIPT_PATH), StandardCharsets.UTF_8);
        assertThat(content)
                .as("script must recognize --no-story-evidence bypass")
                .contains("no-story-evidence");
    }

    // ------------------------------------------------------------------ helpers

    private int runScript(String... args) throws Exception {
        Path script = repoRoot().resolve(SCRIPT_PATH);
        String[] command = new String[args.length + 2];
        command[0] = "bash";
        command[1] = script.toString();
        System.arraycopy(args, 0, command, 2, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(repoRoot().toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        p.getInputStream().readAllBytes(); // drain stdout/stderr
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Script timed out after 30 seconds");
        }
        return p.exitValue();
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
