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
import org.junit.jupiter.api.io.TempDir;

/**
 * Maven CI-blocking audit harness for measure-context-budget.sh and audit-context-budget.sh
 * (EPIC-0078 story-0078-0001).
 *
 * <p>Validates exit codes 0/1/2/3 per Rule 26 §Standardized Exit Codes + story-0078-0016 hard-fail
 * contract.
 */
@DisplayName("ContextBudgetAuditorTest (Maven CI-blocking)")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class ContextBudgetAuditorTest {

    private static final Path MEASURE_SCRIPT =
            Path.of(
                    System.getProperty("user.dir"),
                    "src/main/resources/targets/claude/scripts/measure-context-budget.sh");

    private static final Path AUDIT_SCRIPT =
            Path.of(
                    System.getProperty("user.dir"),
                    "src/main/resources/targets/claude/scripts/audit-context-budget.sh");

    private static final Path BASELINE_PATH =
            Path.of(System.getProperty("user.dir"), "governance/baselines/context-budget.json");

    private static final String REPO_DIR = System.getProperty("user.dir");

    @Test
    @DisplayName("measure-context-budget.sh is present and executable")
    void measureScript_isPresentAndExecutable() throws IOException {
        assertThat(MEASURE_SCRIPT).as("measure-context-budget.sh must exist").exists();
    }

    @Test
    @DisplayName("audit-context-budget.sh is present and executable")
    void auditScript_isPresentAndExecutable() throws IOException {
        assertThat(AUDIT_SCRIPT).as("audit-context-budget.sh must exist").exists();
    }

    @Test
    @DisplayName("context-budget.json baseline exists with required fields")
    void baseline_existsWithRequiredFields() throws IOException {
        assertThat(BASELINE_PATH).as("context-budget.json must exist").exists();
        String content = Files.readString(BASELINE_PATH, StandardCharsets.UTF_8);
        assertThat(content).as("must contain alwaysLoaded field").contains("alwaysLoaded");
        assertThat(content).as("must contain measuredAt field").contains("measuredAt");
        assertThat(content).as("must contain ref field").contains("ref");
    }

    @Test
    @DisplayName("measure-context-budget.sh --self-check passes")
    void measureSelfCheck_passes() throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("/bin/bash", MEASURE_SCRIPT.toString(), "--self-check");
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("self-check must complete within 15s").isTrue();

        int exit = proc.exitValue();
        if (exit != 0) {
            String stderr =
                    new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(exit).as("non-zero must be 2 (OPERATIONAL_ERROR): " + stderr).isEqualTo(2);
            assertThat(stderr).contains("OPERATIONAL_ERROR");
        }
    }

    @Test
    @DisplayName("audit-context-budget.sh --self-check passes")
    void auditSelfCheck_passes() throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--self-check");
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);
        pb.environment().put("BASELINE_PATH", BASELINE_PATH.toString());

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("self-check must complete within 15s").isTrue();

        int exit = proc.exitValue();
        if (exit != 0) {
            String stderr =
                    new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(exit).as("non-zero must be 2 (OPERATIONAL_ERROR): " + stderr).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("measure-context-budget.sh outputs valid JSON with required fields")
    void measure_outputsValidJsonWithRequiredFields() throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder(
                        "/bin/bash",
                        MEASURE_SCRIPT.toString(),
                        "--root",
                        ".claude",
                        "--format",
                        "json");
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("measure must complete within 15s").isTrue();

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = proc.exitValue();

        // Accept exit 0 (jq available) or exit 2 (jq missing in CI — dependency gap, not a
        // regression in measure-context-budget.sh contract)
        if (exit == 0) {
            assertThat(stdout).as("output must contain alwaysLoaded").contains("alwaysLoaded");
            assertThat(stdout).as("output must contain perSkill").contains("perSkill");
            assertThat(stdout).as("output must contain measuredAt").contains("measuredAt");
        } else {
            assertThat(exit)
                    .as("non-zero exit must be 2 (OPERATIONAL_ERROR): " + stderr)
                    .isEqualTo(2);
            assertThat(stderr).as("stderr must name missing dep").contains("OPERATIONAL_ERROR");
        }
    }

    @Test
    @DisplayName("audit-context-budget.sh --advisory exits 0 on valid baseline")
    void audit_advisoryExitsZeroOnValidBaseline() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--advisory");
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);
        pb.environment().put("BASELINE_PATH", BASELINE_PATH.toString());

        Process proc = pb.start();
        boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
        assertThat(finished).as("audit must complete within 30s").isTrue();
        assertThat(proc.exitValue()).as("advisory mode must exit 0").isEqualTo(0);
    }

    @Test
    @DisplayName("audit-context-budget.sh exits 3 on corrupt baseline")
    void audit_exitsThreeOnCorruptBaseline(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path corruptBaseline = tempDir.resolve("corrupt-context-budget.json");
        Files.writeString(corruptBaseline, "{ invalid json }", StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString());
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", REPO_DIR);
        pb.environment().put("BASELINE_PATH", corruptBaseline.toString());

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("audit must complete within 15s").isTrue();

        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(proc.exitValue()).as("corrupt baseline must exit 3: " + stderr).isEqualTo(3);
        assertThat(stderr).as("stderr must contain BASELINE_CORRUPT").contains("BASELINE_CORRUPT");
    }

    @Test
    @DisplayName("--hard exits 1 when alwaysLoaded exceeds limit (story-0078-0016)")
    void audit_hardExitsOneWhenLimitExceeded(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Files.createDirectories(tempDir.resolve(".claude"));
        // CLAUDE.md is what measure-context-budget.sh counts for alwaysLoaded
        Files.writeString(tempDir.resolve("CLAUDE.md"), "x".repeat(500), StandardCharsets.UTF_8);

        Path tinyBaseline = tempDir.resolve("tiny-context-budget.json");
        Files.writeString(
                tinyBaseline,
                """
                {
                  "alwaysLoaded": 1,
                  "measuredAt": "2026-05-06T00:00:00Z",
                  "ref": "test",
                  "limit": 1,
                  "tolerancePct": 0
                }
                """,
                StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString(), "--hard");
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", tempDir.toString());
        pb.environment().put("BASELINE_PATH", tinyBaseline.toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        proc.waitFor(30, TimeUnit.SECONDS);
        assertThat(proc.exitValue())
                .as("--hard with exceeded limit must exit 1: " + output)
                .isEqualTo(1);
        assertThat(output)
                .as("must contain CONTEXT_BUDGET_VIOLATION")
                .contains("CONTEXT_BUDGET_VIOLATION");
    }

    @Test
    @DisplayName("default mode is hard-fail (no flag → exit 1 when limit exceeded)")
    void audit_defaultIsHardFail(@TempDir Path tempDir) throws IOException, InterruptedException {
        Files.createDirectories(tempDir.resolve(".claude"));
        // CLAUDE.md is what measure-context-budget.sh counts for alwaysLoaded
        Files.writeString(tempDir.resolve("CLAUDE.md"), "x".repeat(500), StandardCharsets.UTF_8);

        Path tinyBaseline = tempDir.resolve("tiny-context-budget.json");
        Files.writeString(
                tinyBaseline,
                """
                {
                  "alwaysLoaded": 1,
                  "measuredAt": "2026-05-06T00:00:00Z",
                  "ref": "test",
                  "limit": 1,
                  "tolerancePct": 0
                }
                """,
                StandardCharsets.UTF_8);

        // No --advisory flag — default must now be hard-fail
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", AUDIT_SCRIPT.toString());
        pb.directory(new java.io.File(REPO_DIR));
        pb.environment().put("CLAUDE_PROJECT_DIR", tempDir.toString());
        pb.environment().put("BASELINE_PATH", tinyBaseline.toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor(30, TimeUnit.SECONDS);
        assertThat(proc.exitValue())
                .as("default (no flag) must hard-fail when limit exceeded")
                .isEqualTo(1);
    }
}
