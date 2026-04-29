package dev.iadev.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("MigrateLayoutScriptIT — scripts/migrate-layout.sh")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class MigrateLayoutScriptIT {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
    private static final Path SCRIPT = REPO_ROOT.resolve("scripts/migrate-layout.sh");

    @Test
    @DisplayName("script file exists and is executable")
    void script_existsAndExecutable() throws IOException {
        assertThat(SCRIPT).exists().isRegularFile();
        assertThat(Files.isExecutable(SCRIPT)).isTrue();
    }

    @Test
    @DisplayName("--self-check passes when jq and git are available")
    void selfCheck_passes() throws Exception {
        ProcessResult r = run("--self-check");
        assertThat(r.exitCode).isZero();
        assertThat(r.stdout).contains("self-check: OK");
    }

    @Test
    @DisplayName("--unknown-flag exits 3 with INVALID_ARGS")
    void invalidFlag_exits3() throws Exception {
        ProcessResult r = run("--bogus");
        assertThat(r.exitCode).isEqualTo(3);
        assertThat(r.stderr).contains("INVALID_ARGS");
    }

    @Test
    @DisplayName("--epic with non-4-digit value rejected")
    void invalidEpic_exits3() throws Exception {
        ProcessResult r = run("--epic", "abc");
        assertThat(r.exitCode).isEqualTo(3);
        assertThat(r.stderr).contains("INVALID_ARGS");
    }

    @Test
    @DisplayName("--dry-run on a legacy v2 epic emits skipped message")
    void dryRun_legacyEpic_skipped() throws Exception {
        // Use existing legacy epic in the repo (every flowVersion-absent epic
        // is treated as legacy v0/v1/v2 and skipped). epic-0042 has no
        // execution-state.json, so flowVersion resolves to 0 → skip.
        ProcessResult r = run("--dry-run", "--epic", "0042");
        assertThat(r.exitCode).isZero();
        assertThat(r.stdout).contains("skipped");
        assertThat(r.stdout).contains("legacy");
    }

    @Test
    @DisplayName("--dry-run produces no filesystem changes for target epic")
    void dryRun_noFilesystemChanges() throws Exception {
        Path target = REPO_ROOT.resolve("ai/epics/epic-0042-test-noop");
        boolean preExists = Files.exists(target);
        run("--dry-run", "--epic", "0042");
        assertThat(Files.exists(target)).isEqualTo(preExists);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private record ProcessResult(int exitCode, String stdout, String stderr) {}

    private ProcessResult run(String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("bash");
        command.add(SCRIPT.toString());
        command.addAll(List.of(args));
        pb.command(command);
        pb.directory(REPO_ROOT.toFile());
        Process p = pb.start();
        String stdout = new String(p.getInputStream().readAllBytes());
        String stderr = new String(p.getErrorStream().readAllBytes());
        int exit = p.waitFor();
        return new ProcessResult(exit, stdout, stderr);
    }
}
