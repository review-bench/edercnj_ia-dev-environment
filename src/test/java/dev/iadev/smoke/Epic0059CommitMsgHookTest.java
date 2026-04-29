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
 * TASK-0059-0004-003: Integration tests for the commit-msg hook that protects {@code
 * execution-state.json} from manual edits.
 *
 * <p>Validates the three Gherkin scenarios from story-0059-0004:
 *
 * <ol>
 *   <li>Commit without execution-state.json passes (exit 0).
 *   <li>Commit with execution-state.json and valid trailer passes (exit 0).
 *   <li>Commit with execution-state.json without trailer is rejected (exit 1).
 * </ol>
 *
 * <p>Tests are skipped on Windows (POSIX hook + bash).
 *
 * @see <a href="plans/epic-0059/story-0059-0004.md">story-0059-0004</a>
 */
@DisplayName("Epic0059CommitMsgHookTest — TASK-0059-0004-003")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "POSIX bash hook — not applicable on Windows")
class Epic0059CommitMsgHookTest {

    private static final String HOOK_REPO_PATH = ".githooks/commit-msg";

    @Test
    @DisplayName("hookSourceExists — .githooks/commit-msg must exist " + "in the repository root")
    void hookSourceExists() {
        Path hook = repoRoot().resolve(HOOK_REPO_PATH);
        assertThat(hook).as(".githooks/commit-msg must exist (story-0059-0004)").isRegularFile();
        assertThat(hook.toFile().canExecute())
                .as(".githooks/commit-msg must be executable")
                .isTrue();
    }

    @Test
    @DisplayName("hookContent — contains canonical trailer pattern " + "and story reference")
    void hookContent_containsCanonicalTrailerPattern() throws IOException {
        Path hook = repoRoot().resolve(HOOK_REPO_PATH);
        String content = Files.readString(hook, StandardCharsets.UTF_8);

        assertThat(content).as("hook must reference story-0059-0004").contains("story-0059-0004");

        assertThat(content)
                .as("hook must validate the canonical Co-Authored-By trailer")
                .contains("x-internal-status-update@");

        assertThat(content).as("hook must use [0-9a-f]{40} SHA regex").contains("[0-9a-f]{40}");

        assertThat(content)
                .as("hook must check execution-state.json in staged files")
                .contains("execution-state.json");

        assertThat(content)
                .as("hook must use git interpret-trailers for " + "spec-compliant parsing")
                .contains("git interpret-trailers");

        assertThat(content)
                .as("hook must support CLAUDE_EXECUTION_STATE_HOOK_DISABLED " + "bypass variable")
                .contains("CLAUDE_EXECUTION_STATE_HOOK_DISABLED");
    }

    @Test
    @DisplayName(
            "hookScenario_noExecutionStateStaged_exits0 — "
                    + "commit without execution-state.json passes (Gherkin AC 1)")
    void hookScenario_noExecutionStateStaged_exits0(@TempDir Path fakeRepo) throws Exception {
        setupFakeRepo(fakeRepo);

        // Stage a regular file (no execution-state.json)
        Path regularFile = fakeRepo.resolve("src/Foo.java");
        Files.createDirectories(regularFile.getParent());
        Files.writeString(regularFile, "class Foo {}", StandardCharsets.UTF_8);
        runCommand(fakeRepo, "git", "add", "src/Foo.java");

        // Write a plain commit message (no trailer)
        Path commitMsg = fakeRepo.resolve(".git/COMMIT_EDITMSG");
        Files.writeString(commitMsg, "feat: add Foo\n", StandardCharsets.UTF_8);

        // Run the hook
        int exitCode = runHook(fakeRepo, commitMsg);
        assertThat(exitCode)
                .as("hook must exit 0 when no execution-state.json staged")
                .isEqualTo(0);
    }

    @Test
    @DisplayName(
            "hookScenario_executionStateWithValidTrailer_exits0 — "
                    + "commit with valid trailer passes (Gherkin AC 2)")
    void hookScenario_executionStateWithValidTrailer_exits0(@TempDir Path fakeRepo)
            throws Exception {
        setupFakeRepo(fakeRepo);

        // Stage execution-state.json
        Path stateDir = fakeRepo.resolve("plans/epic-0059");
        Files.createDirectories(stateDir);
        Files.writeString(
                stateDir.resolve("execution-state.json"),
                "{\"flowVersion\":\"2\"}",
                StandardCharsets.UTF_8);
        runCommand(fakeRepo, "git", "add", "plans/epic-0059/execution-state.json");

        // Build a valid SHA (40 hex chars)
        String fakeHeadSha = "a".repeat(40);
        String trailer = "Co-Authored-By: x-internal-status-update@" + fakeHeadSha;

        // Write commit message with trailer
        Path commitMsg = fakeRepo.resolve(".git/COMMIT_EDITMSG");
        Files.writeString(
                commitMsg,
                "chore: update execution state\n\n" + trailer + "\n",
                StandardCharsets.UTF_8);

        int exitCode = runHook(fakeRepo, commitMsg);
        assertThat(exitCode).as("hook must exit 0 when valid trailer present").isEqualTo(0);
    }

    @Test
    @DisplayName(
            "hookScenario_executionStateWithoutTrailer_exits1 — "
                    + "commit without trailer is rejected (Gherkin AC 3)")
    void hookScenario_executionStateWithoutTrailer_exits1(@TempDir Path fakeRepo) throws Exception {
        setupFakeRepo(fakeRepo);

        // Stage execution-state.json
        Path stateDir = fakeRepo.resolve("plans/epic-0059");
        Files.createDirectories(stateDir);
        Files.writeString(
                stateDir.resolve("execution-state.json"),
                "{\"flowVersion\":\"2\"}",
                StandardCharsets.UTF_8);
        runCommand(fakeRepo, "git", "add", "plans/epic-0059/execution-state.json");

        // Write commit message WITHOUT the canonical trailer
        Path commitMsg = fakeRepo.resolve(".git/COMMIT_EDITMSG");
        Files.writeString(commitMsg, "chore: update execution state\n", StandardCharsets.UTF_8);

        ProcessResult result = runHookWithStderr(fakeRepo, commitMsg);
        assertThat(result.exitCode).as("hook must exit 1 when trailer absent").isEqualTo(1);
        assertThat(result.stderr)
                .as("stderr must contain the canonical skill name")
                .contains("x-internal-status-update");
        assertThat(result.stderr)
                .as("stderr must reference execution-state.json constraint")
                .contains("execution-state.json");
    }

    @Test
    @DisplayName(
            "hookScenario_bypassDisabled_exits0 — "
                    + "CLAUDE_EXECUTION_STATE_HOOK_DISABLED=1 bypasses hook")
    void hookScenario_bypassDisabled_exits0(@TempDir Path fakeRepo) throws Exception {
        setupFakeRepo(fakeRepo);

        // Stage execution-state.json
        Path stateDir = fakeRepo.resolve("plans/epic-0059");
        Files.createDirectories(stateDir);
        Files.writeString(stateDir.resolve("execution-state.json"), "{}", StandardCharsets.UTF_8);
        runCommand(fakeRepo, "git", "add", "plans/epic-0059/execution-state.json");

        // Write commit message WITHOUT trailer
        Path commitMsg = fakeRepo.resolve(".git/COMMIT_EDITMSG");
        Files.writeString(commitMsg, "chore: bypass test\n", StandardCharsets.UTF_8);

        // Run hook with bypass variable set
        Path hookSrc = repoRoot().resolve(HOOK_REPO_PATH);
        ProcessBuilder pb = new ProcessBuilder("bash", hookSrc.toString(), commitMsg.toString());
        pb.directory(fakeRepo.toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("CLAUDE_EXECUTION_STATE_HOOK_DISABLED", "1");
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Hook timeout");
        }

        assertThat(p.exitValue()).as("hook must exit 0 when bypass variable is set").isEqualTo(0);
    }

    // --- helper methods ---

    private void setupFakeRepo(Path fakeRepo) throws Exception {
        runCommand(fakeRepo, "git", "init", "-q", "-b", "main");
        runCommand(
                fakeRepo,
                "git",
                "-c",
                "user.email=t@t",
                "-c",
                "user.name=t",
                "commit",
                "--allow-empty",
                "-q",
                "-m",
                "initial");
    }

    private int runCommand(Path workdir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Timeout: " + String.join(" ", cmd));
        }
        return p.exitValue();
    }

    private int runHook(Path fakeRepo, Path commitMsg) throws Exception {
        return runHookWithStderr(fakeRepo, commitMsg).exitCode;
    }

    private ProcessResult runHookWithStderr(Path fakeRepo, Path commitMsg) throws Exception {
        Path hookSrc = repoRoot().resolve(HOOK_REPO_PATH);
        ProcessBuilder pb = new ProcessBuilder("bash", hookSrc.toString(), commitMsg.toString());
        pb.directory(fakeRepo.toFile());
        pb.redirectErrorStream(false);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        String stderr = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Hook timeout");
        }
        return new ProcessResult(p.exitValue(), stderr);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }

    private record ProcessResult(int exitCode, String stderr) {}
}
