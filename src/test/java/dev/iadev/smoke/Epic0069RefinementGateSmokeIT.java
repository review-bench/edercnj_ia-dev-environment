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
import org.junit.jupiter.api.io.TempDir;

/**
 * E2E smoke tests for EPIC-0069 (Story Refinement & DoR Gate / Rule 29).
 *
 * <p>Validates the 6 canonical scenarios cooperatively across Camada 0 (PreToolUse hook {@code
 * enforce-refinement-gate.sh}) and Camada 2 (CI script {@code audit-refinement-gate.sh}).
 * Complements the bash unit suites {@code enforce_refinement_gate_test.sh} and {@code
 * audit_refinement_gate_test.sh}.
 */
@DisplayName("Epic0069RefinementGateSmokeIT — Refinement Gate E2E")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "POSIX bash hook + CI script")
class Epic0069RefinementGateSmokeIT {

    private static final Path HOOK =
            repoRoot()
                    .resolve(
                            "src/main/resources/targets/claude/hooks/"
                                    + "enforce-refinement-gate.sh");

    private static final Path AUDIT =
            repoRoot()
                    .resolve(
                            "src/main/resources/targets/claude/scripts/"
                                    + "audit-refinement-gate.sh");

    // ─── Scenario 1: approved verdict allows hook ──────────────────────────

    @Test
    @DisplayName("hook exits 0 when refinementVerdict.status=approved")
    void hook_allowsWhen_verdictApproved(@TempDir Path tmp) throws Exception {
        setupEpic(tmp, "0099", "4", "approved");
        ProcessResult r =
                runHookWithPayload(
                        tmp,
                        "{\"tool_name\":\"Skill\",\"tool_input\":{\"skill\":\"x-implement-story\","
                                + "\"args\":\"story-0099-0001\"}}");
        assertThat(r.exitCode).as("exit 0 on approved").isEqualTo(0);
    }

    // ─── Scenario 2: absent verdict blocks (REFINEMENT_REQUIRED) ───────────

    @Test
    @DisplayName("hook exits 33 when refinementVerdict absent on flowVersion=4")
    void hook_blocksWhen_verdictAbsent(@TempDir Path tmp) throws Exception {
        setupEpic(tmp, "0099", "4", null);
        ProcessResult r =
                runHookWithPayload(
                        tmp,
                        "{\"tool_name\":\"Skill\",\"tool_input\":{\"skill\":\"x-implement-story\","
                                + "\"args\":\"story-0099-0001\"}}");
        assertThat(r.exitCode).as("exit 33 REFINEMENT_REQUIRED").isEqualTo(33);
        assertThat(r.stderr).contains("REFINEMENT_REQUIRED");
    }

    // ─── Scenario 3: rejected verdict fails audit ──────────────────────────

    @Test
    @DisplayName("audit exits 1 REFINEMENT_GATE_VIOLATION when verdict=rejected")
    void audit_failsWhen_verdictRejected(@TempDir Path tmp) throws Exception {
        setupRepoForAudit(tmp, "0099", "rejected");
        ProcessResult r = runAudit(tmp, "--story", "story-0099-0001");
        assertThat(r.exitCode).as("exit 1 on rejected").isEqualTo(1);
        assertThat(r.stderr).contains("REFINEMENT_GATE_VIOLATION").contains("rejected-verdict");
    }

    // ─── Scenario 4: legacy flowVersion=1 is no-op ─────────────────────────

    @Test
    @DisplayName("hook exits 0 on flowVersion=1 (Rule 19 legacy fallback)")
    void hook_isNoOpOn_flowVersion1(@TempDir Path tmp) throws Exception {
        setupEpic(tmp, "0099", "1", null);
        ProcessResult r =
                runHookWithPayload(
                        tmp,
                        "{\"tool_name\":\"Skill\",\"tool_input\":{\"skill\":\"x-implement-story\","
                                + "\"args\":\"story-0099-0001\"}}");
        assertThat(r.exitCode).as("exit 0 on legacy flow").isEqualTo(0);
        assertThat(r.stderr).contains("Rule 19 legacy fallback");
    }

    // ─── Scenario 5: hotfix branch bypass (Rule 27 Exception 2) ────────────

    @Test
    @DisplayName("audit exits 0 on hotfix/* branch even with rejected verdict")
    void audit_isNoOpOn_hotfixBranch(@TempDir Path tmp) throws Exception {
        setupRepoForAudit(tmp, "0099", "rejected");
        runCommand(tmp, "git", "checkout", "-q", "-b", "hotfix/CRIT-001");
        ProcessResult r = runAudit(tmp);
        assertThat(r.exitCode).as("exit 0 on hotfix").isEqualTo(0);
    }

    // ─── Scenario 6: epic-scope approved + story-level absent → blocks story
    //                 but NOT epic ────────────────────────────────────────────

    @Test
    @DisplayName("epic-scope approved verdict allows x-epic-implement at epic level")
    void hook_allowsEpicWhen_epicScopeApproved(@TempDir Path tmp) throws Exception {
        Path epicDir = tmp.resolve("ai/epics/epic-0099-test");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                """
                {
                  "flowVersion": "4",
                  "epicId": "EPIC-0099",
                  "refinementVerdict": {"status": "approved", "scope": "epic"}
                }
                """);
        ProcessResult r =
                runHookWithPayload(
                        tmp,
                        "{\"tool_name\":\"Skill\",\"tool_input\":{\"skill\":\"x-epic-implement\","
                                + "\"args\":\"epic-0099\"}}");
        assertThat(r.exitCode).as("exit 0 on epic-scope approved").isEqualTo(0);
    }

    // ─── Self-checks ────────────────────────────────────────────────────────

    @Test
    @DisplayName("hook --self-check exits 0 with prerequisites in place")
    void hook_selfCheck_passes(@TempDir Path tmp) throws Exception {
        Files.createDirectories(tmp.resolve("governance/baselines"));
        Files.writeString(
                tmp.resolve("governance/baselines/refinement-gate-baseline.txt"), "# baseline\n");
        ProcessBuilder pb = new ProcessBuilder("bash", HOOK.toString(), "--self-check");
        pb.environment().put("CLAUDE_PROJECT_DIR", tmp.toString());
        ProcessResult r = waitProcess(pb);
        assertThat(r.exitCode).as("hook self-check exit 0").isEqualTo(0);
    }

    @Test
    @DisplayName("audit --self-check exits 0 with prerequisites in place")
    void audit_selfCheck_passes(@TempDir Path tmp) throws Exception {
        setupRepoForAudit(tmp, "0099", "approved");
        ProcessResult r = runAudit(tmp, "--self-check");
        assertThat(r.exitCode).as("audit self-check exit 0").isEqualTo(0);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private void setupEpic(Path tmp, String epicNum, String flowVersion, String verdictStatus)
            throws IOException {
        Path epicDir = tmp.resolve("ai/epics/epic-" + epicNum + "-test");
        Files.createDirectories(epicDir);
        String state;
        if (verdictStatus == null) {
            state =
                    String.format(
                            "{\"flowVersion\":\"%s\",\"epicId\":\"EPIC-%s\","
                                    + "\"taskTracking\":{\"enabled\":true}}",
                            flowVersion, epicNum);
        } else {
            state =
                    String.format(
                            "{\"flowVersion\":\"%s\",\"epicId\":\"EPIC-%s\","
                                    + "\"refinementVerdict\":{\"status\":\"%s\","
                                    + "\"scope\":\"story\",\"blockers\":[]},"
                                    + "\"taskTracking\":{\"enabled\":true}}",
                            flowVersion, epicNum, verdictStatus);
        }
        Files.writeString(epicDir.resolve("execution-state.json"), state);
    }

    private void setupRepoForAudit(Path tmp, String epicNum, String verdictStatus)
            throws Exception {
        runCommand(tmp, "git", "init", "-q");
        runCommand(
                tmp,
                "git",
                "-c",
                "user.email=t@t",
                "-c",
                "user.name=T",
                "commit",
                "--allow-empty",
                "-q",
                "-m",
                "init");
        Files.createDirectories(tmp.resolve(".claude/rules"));
        Files.createDirectories(tmp.resolve(".claude/hooks"));
        Files.createDirectories(tmp.resolve("governance/baselines"));
        Files.createDirectories(tmp.resolve("capabilities/governance"));
        Files.writeString(tmp.resolve(".claude/rules/29-refinement-gate.md"), "# Rule 29 stub\n");
        Files.writeString(
                tmp.resolve(".claude/hooks/enforce-refinement-gate.sh"), "#!/usr/bin/env bash\n");
        Files.writeString(
                tmp.resolve("capabilities/governance/refinement-gate.yaml"),
                "id: governance.refinement-gate\n");
        Files.writeString(
                tmp.resolve("governance/baselines/refinement-gate-baseline.txt"),
                "# refinement-gate-baseline.txt\n");
        setupEpic(tmp, epicNum, "4", verdictStatus);
    }

    private ProcessResult runHookWithPayload(Path projectDir, String payload) throws Exception {
        Path payloadFile = Files.createTempFile("payload-", ".json");
        Files.writeString(payloadFile, payload);
        try {
            ProcessBuilder pb =
                    new ProcessBuilder(
                            "bash",
                            "-c",
                            "cat \"$1\" | bash \"$2\"",
                            "--",
                            payloadFile.toString(),
                            HOOK.toString());
            pb.environment().put("CLAUDE_PROJECT_DIR", projectDir.toString());
            return waitProcess(pb);
        } finally {
            Files.deleteIfExists(payloadFile);
        }
    }

    private ProcessResult runAudit(Path workdir, String... args) throws Exception {
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("bash");
        cmd.add(AUDIT.toString());
        for (String a : args) cmd.add(a);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        return waitProcess(pb);
    }

    private ProcessResult waitProcess(ProcessBuilder pb) throws Exception {
        Process p = pb.start();
        byte[] stderr = p.getErrorStream().readAllBytes();
        byte[] stdout = p.getInputStream().readAllBytes();
        boolean finished = p.waitFor(20, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            p.waitFor(5, TimeUnit.SECONDS);
            throw new AssertionError("process exceeded 20s timeout");
        }
        return new ProcessResult(
                p.exitValue(),
                new String(stdout, StandardCharsets.UTF_8),
                new String(stderr, StandardCharsets.UTF_8));
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

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
