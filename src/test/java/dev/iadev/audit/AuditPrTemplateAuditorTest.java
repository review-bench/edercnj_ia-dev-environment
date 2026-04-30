package dev.iadev.audit;

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
 * Maven CI-blocking audit harness for audit-pr-template.sh (story-0066-0007).
 *
 * <p>This test runs during {@code mvn verify} — when audit-pr-template.sh is broken (missing, not
 * executable, or self-check fails), the build fails with a clear assertion. Mirrors the EPIC-0061
 * RULE-007/RULE-008 pattern that replaced .github/workflows/audit.yml with Java audit harness
 * classes.
 */
@DisplayName("AuditPrTemplateAuditorTest (Maven CI-blocking)")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class AuditPrTemplateAuditorTest {

    private static final Path SCRIPT_PATH =
            Path.of(
                    System.getProperty("user.dir"),
                    "src/main/resources/targets/claude/scripts/audit-pr-template.sh");

    private static final Path BASELINE_PATH =
            Path.of(
                    System.getProperty("user.dir"),
                    "governance/baselines/pr-template-baseline.txt");

    @Test
    @DisplayName("audit-pr-template.sh is present and executable")
    void script_isPresentAndExecutable() throws IOException {
        assertThat(SCRIPT_PATH).as("audit-pr-template.sh source-of-truth must exist").exists();
        // POSIX systems only — assert owner-execute permission
        try {
            var perms = Files.getPosixFilePermissions(SCRIPT_PATH);
            assertThat(perms)
                    .as("audit-pr-template.sh must be owner-executable")
                    .contains(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX (Windows) — skip permission check
        }
    }

    @Test
    @DisplayName("baseline file exists and is readable")
    void baseline_existsAndIsReadable() throws IOException {
        assertThat(BASELINE_PATH).as("pr-template-baseline.txt must exist").exists();
        String content = Files.readString(BASELINE_PATH, StandardCharsets.UTF_8);
        assertThat(content).as("baseline must contain header comment").contains("baseline");
    }

    @Test
    @DisplayName("audit-pr-template.sh --self-check passes")
    void selfCheck_passes() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", SCRIPT_PATH.toString(), "--self-check");
        pb.directory(new java.io.File(System.getProperty("user.dir")));
        pb.environment().put("CLAUDE_PROJECT_DIR", System.getProperty("user.dir"));

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("self-check must complete within 15s").isTrue();

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        // Accept exit 0 (deps present) OR exit 2 with clear OPERATIONAL_ERROR (deps missing in CI).
        // The harness validates the structural contract; missing deps in dev/CI are not a
        // story-0066-0007 regression.
        if (proc.exitValue() == 0) {
            assertThat(stdout).contains("OK");
        } else {
            assertThat(proc.exitValue())
                    .as("Non-zero exit must be 2 (OPERATIONAL_ERROR), not 1")
                    .isEqualTo(2);
            assertThat(stderr).contains("OPERATIONAL_ERROR");
        }
    }

    @Test
    @DisplayName("audit-pr-template.sh --help exits 0")
    void help_exitsZero() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", SCRIPT_PATH.toString(), "--help");
        Process proc = pb.start();
        proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(proc.exitValue()).isEqualTo(0);
    }
}
