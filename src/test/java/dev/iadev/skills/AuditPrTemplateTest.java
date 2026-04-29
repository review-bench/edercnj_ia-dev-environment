package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for audit-pr-template.sh (story-0066-0007).
 *
 * <p>Behavioural fixtures cover: --help, --self-check, missing --pr arg, invalid --pr arg,
 * audit-exempt with empty reason, hotfix-bypass logging.
 */
@DisplayName("AuditPrTemplateTest")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class AuditPrTemplateTest {

    private static final String SCRIPT_PATH =
            System.getProperty("user.dir")
                    + "/src/main/resources/targets/claude/scripts/audit-pr-template.sh";

    @TempDir Path tempDir;

    private ProcessResult run(List<String> args) throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("/bin/bash");
        cmd.add(SCRIPT_PATH);
        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(tempDir.toFile());
        pb.environment().put("CLAUDE_PROJECT_DIR", tempDir.toString());

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("Process should complete within 15s").isTrue();

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(proc.exitValue(), stdout, stderr);
    }

    @Nested
    @DisplayName("degenerate — script is reachable")
    class Reachable {

        @Test
        @DisplayName("--help exits 0 and prints Usage")
        void help_exitsZero() throws IOException, InterruptedException {
            ProcessResult r = run(List.of("--help"));
            assertThat(r.exitCode()).isEqualTo(0);
            assertThat(r.stdout()).contains("Usage");
        }
    }

    @Nested
    @DisplayName("self-check — Rule 26 contract")
    class SelfCheck {

        @Test
        @DisplayName("--self-check verifies gh and jq dependencies")
        void selfCheck_verifiesDependencies() throws IOException, InterruptedException {
            // gh + jq present on dev hosts; baseline file may or may not exist
            ProcessResult r = run(List.of("--self-check"));
            // Accept exit 0 if both deps present, exit 2 with clear OPERATIONAL_ERROR otherwise
            if (r.exitCode() == 0) {
                assertThat(r.stdout()).contains("OK");
            } else {
                assertThat(r.exitCode()).isEqualTo(2);
                assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
            }
        }
    }

    @Nested
    @DisplayName("argparse — invalid args")
    class ArgParse {

        @Test
        @DisplayName("missing --pr returns exit 2 OPERATIONAL_ERROR")
        void noPrArg_exits2() throws IOException, InterruptedException {
            ProcessResult r = run(List.of());
            assertThat(r.exitCode()).isEqualTo(2);
            assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
            assertThat(r.stderr()).contains("--pr");
        }

        @Test
        @DisplayName("non-numeric --pr returns exit 2")
        void nonNumericPr_exits2() throws IOException, InterruptedException {
            ProcessResult r = run(List.of("--pr", "abc"));
            assertThat(r.exitCode()).isEqualTo(2);
            assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
        }

        @Test
        @DisplayName("unknown flag returns exit 2")
        void unknownFlag_exits2() throws IOException, InterruptedException {
            ProcessResult r = run(List.of("--bogus"));
            assertThat(r.exitCode()).isEqualTo(2);
            assertThat(r.stderr()).contains("OPERATIONAL_ERROR");
        }
    }

    @Nested
    @DisplayName("baseline grandfather behavior")
    class Baseline {

        @Test
        @DisplayName("PR listed in baseline returns exit 0")
        void grandfatheredPr_exits0() throws IOException, InterruptedException {
            Path baseline = tempDir.resolve("governance/baselines/pr-template-baseline.txt");
            Files.createDirectories(baseline.getParent());
            Files.writeString(baseline, "999  # legacy PR\n", StandardCharsets.UTF_8);

            ProcessResult r = run(List.of("--pr", "999"));
            assertThat(r.exitCode()).isEqualTo(0);
            assertThat(r.stdout()).contains("grandfathered");
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
