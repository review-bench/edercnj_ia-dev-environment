package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
 * E2E smoke tests for {@code enforce-continuous-flow.sh} (EPIC-0068).
 *
 * <p>Exercises the decision matrix (a)–(h) end-to-end: creates temp fixtures, invokes the bash
 * hook, and validates exit code + stderr. Complements the structural contract tests in {@code
 * EnforceContinuousFlowHookTest} and the bash unit tests in {@code
 * enforce_continuous_flow_test.sh}.
 */
@DisplayName("Epic0068ContinuousFlowSmokeTest — enforce-continuous-flow.sh E2E")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX bash hook; mirrors smoke gating convention.")
class Epic0068ContinuousFlowSmokeTest {

    private static final Path HOOK =
            repoRoot()
                    .resolve(
                            "src/main/resources/targets/claude/hooks/"
                                    + "enforce-continuous-flow.sh");

    // ─── Scenario (h): NUDGE emitted ───────────────────────────────────────

    @Test
    @DisplayName("hook emits CONTINUOUS_FLOW_INTERRUPT when phase open, non-interactive, last event=tool.result")
    void hook_emitsNudgeWhen_phaseOpenAndNonInteractive_andLastEventIsToolResult(
            @TempDir Path tmp) throws Exception {
        HookResult result = executeHook(tmp, "state-non-interactive-open-phase.json",
                "events-tool-result-last.ndjson");
        assertThat(result.exitCode).as("exit 2 for stall").isEqualTo(2);
        assertThat(result.stderr).contains("CONTINUOUS_FLOW_INTERRUPT");
    }

    // ─── Scenario (c): interactive mode suppresses nudge ───────────────────

    @Test
    @DisplayName("hook exits 0 when interactiveMode=interactive (Rule 19 guard)")
    void hook_skipsWhen_modeIsInteractive(@TempDir Path tmp) throws Exception {
        HookResult result = executeHook(tmp, "state-interactive.json",
                "events-tool-result-last.ndjson");
        assertThat(result.exitCode).as("exit 0 for interactive mode").isEqualTo(0);
    }

    // ─── Scenario (e): finding.high suppresses nudge ────────────────────────

    @Test
    @DisplayName("hook exits 0 when last NDJSON event is finding.high (legitimate pause)")
    void hook_skipsWhen_findingHighInLastNdjsonEvent(@TempDir Path tmp) throws Exception {
        HookResult result = executeHook(tmp, "state-non-interactive-open-phase.json",
                "events-finding-high.ndjson");
        assertThat(result.exitCode).as("exit 0 for finding.high").isEqualTo(0);
    }

    // ─── Scenario (d): empty openTasks suppresses nudge ─────────────────────

    @Test
    @DisplayName("hook exits 0 when taskTracking.openTasks is empty (orchestrator done)")
    void hook_skipsWhen_taskTrackingOpenTasksEmpty(@TempDir Path tmp) throws Exception {
        String emptyTasksState =
                """
                {
                  "epicId": "EPIC-TEST",
                  "interactiveMode": "non-interactive",
                  "taskTracking": {
                    "enabled": true,
                    "openTasks": [],
                    "phaseGateResults": [{"phase": "Phase 3", "mode": "pre", "passed": true}]
                  }
                }
                """;
        HookResult result = executeHookInline(tmp, emptyTasksState,
                "events-empty-tasks.ndjson");
        assertThat(result.exitCode).as("exit 0 for empty openTasks").isEqualTo(0);
    }

    // ─── Scenario (b): hotfix branch guard ─────────────────────────────────

    @Test
    @DisplayName("hook exits 0 on hotfix/* branch (Rule 27 Exception 2)")
    void hook_skipsWhen_currentBranchIsHotfix(@TempDir Path tmp) throws Exception {
        HookResult result = executeHookOnBranch(tmp, "state-non-interactive-open-phase.json",
                "events-tool-result-last.ndjson", "hotfix/critical-fix");
        assertThat(result.exitCode).as("exit 0 for hotfix branch").isEqualTo(0);
    }

    // ─── --self-check passes when jq available ──────────────────────────────

    @Test
    @DisplayName("--self-check exits 0 when jq is available and epic dirs exist")
    void hook_selfCheck_passesWhenJqAvailable(@TempDir Path tmp) throws Exception {
        Path epicDir = tmp.resolve("ai/epics");
        Files.createDirectories(epicDir);
        ProcessBuilder pb = new ProcessBuilder("bash", HOOK.toString(), "--self-check");
        pb.environment().put("CLAUDE_PROJECT_DIR", tmp.toString());
        pb.redirectErrorStream(false);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        p.waitFor(10, TimeUnit.SECONDS);
        assertThat(p.exitValue()).as("--self-check exits 0").isEqualTo(0);
    }

    // ─── --self-check emits OPERATIONAL_ERROR if jq unavailable ────────────

    @Test
    @DisplayName("--self-check emits OPERATIONAL_ERROR when jq missing from PATH")
    void hook_selfCheck_failsWhenJqMissing(@TempDir Path tmp) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", HOOK.toString(), "--self-check");
        pb.environment().put("CLAUDE_PROJECT_DIR", tmp.toString());
        pb.environment().put("PATH", "/nonexistent-bin");
        byte[] stderr = captureStderr(pb);
        String stderrStr = new String(stderr, StandardCharsets.UTF_8);
        assertThat(stderrStr).contains("OPERATIONAL_ERROR");
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private HookResult executeHook(Path tmp, String stateFixture, String ndjsonFixture)
            throws Exception {
        Path epicDir = setupEpicDir(tmp);
        copyFixture(stateFixture, epicDir.resolve("execution-state.json"));
        copyFixture(ndjsonFixture, epicDir.resolve("telemetry/events.ndjson"));
        return runHook(tmp);
    }

    private HookResult executeHookInline(Path tmp, String stateJson, String ndjsonFixture)
            throws Exception {
        Path epicDir = setupEpicDir(tmp);
        Files.writeString(epicDir.resolve("execution-state.json"), stateJson);
        copyFixture(ndjsonFixture, epicDir.resolve("telemetry/events.ndjson"));
        return runHook(tmp);
    }

    private HookResult executeHookOnBranch(Path tmp, String stateFixture, String ndjsonFixture,
            String branch) throws Exception {
        Path epicDir = setupEpicDir(tmp);
        copyFixture(stateFixture, epicDir.resolve("execution-state.json"));
        copyFixture(ndjsonFixture, epicDir.resolve("telemetry/events.ndjson"));
        initGitWithBranch(tmp, branch);
        return runHook(tmp);
    }

    private Path setupEpicDir(Path tmp) throws IOException {
        Path epicDir = tmp.resolve("ai/epics/epic-test");
        Files.createDirectories(epicDir.resolve("telemetry"));
        return epicDir;
    }

    private void copyFixture(String fixtureName, Path dest) throws IOException {
        URL url = getClass().getClassLoader()
                .getResource("fixtures/epic-0068/" + fixtureName);
        assertThat(url).as("fixture %s must exist", fixtureName).isNotNull();
        try (InputStream in = url.openStream()) {
            Files.copy(in, dest);
        }
    }

    private void initGitWithBranch(Path dir, String branch) throws Exception {
        runCommand(dir, "git", "init", "-q");
        runCommand(dir, "git", "-c", "user.email=t@t", "-c", "user.name=T",
                "commit", "--allow-empty", "-m", "init", "-q");
        runCommand(dir, "git", "checkout", "-q", "-b", branch);
    }

    private HookResult runHook(Path projectDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", HOOK.toString());
        pb.environment().put("CLAUDE_PROJECT_DIR", projectDir.toString());
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        byte[] stderr = p.getErrorStream().readAllBytes();
        p.getInputStream().readAllBytes();
        p.waitFor(10, TimeUnit.SECONDS);
        return new HookResult(p.exitValue(), new String(stderr, StandardCharsets.UTF_8));
    }

    private byte[] captureStderr(ProcessBuilder pb) throws Exception {
        Process p = pb.start();
        byte[] stderr = p.getErrorStream().readAllBytes();
        p.getInputStream().readAllBytes();
        p.waitFor(10, TimeUnit.SECONDS);
        return stderr;
    }

    private void runCommand(Path workdir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        p.waitFor(10, TimeUnit.SECONDS);
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }

    private record HookResult(int exitCode, String stderr) {}
}
