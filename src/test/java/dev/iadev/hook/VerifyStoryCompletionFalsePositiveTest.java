package dev.iadev.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract test validating the fix for the false-positive storm in {@code
 * verify-story-completion.sh} (EPIC-0061 story-0061-0006 TASK-0061-0006-005).
 *
 * <p>The original bug: Signal A read from 500 telemetry entries (historical) + Signal B read from
 * HEAD commit (pre-session merge commit) → both false-positive on every Stop event, creating ~80
 * consecutive warnings even without any new commits in the session.
 *
 * <p>This class verifies the behavioral contract of the fix:
 *
 * <ul>
 *   <li>session-start.sh writes SESSION_EPOCH to .claude/state/session-start.txt
 *   <li>verify-story-completion.sh reads the file and scopes Signal A+B to session commits
 *   <li>Without session commits, both signals are 0 → exit 0 (no false positive)
 * </ul>
 */
@DisplayName("VerifyStoryCompletionFalsePositiveTest — session-scoped heuristic contract")
class VerifyStoryCompletionFalsePositiveTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"));

    private static final Path SESSION_START_HOOK =
            REPO_ROOT.resolve("src/main/resources/targets/claude/hooks/session-start.sh");
    private static final Path VERIFY_HOOK =
            REPO_ROOT.resolve("src/main/resources/targets/claude/hooks/verify-story-completion.sh");

    @Test
    @DisplayName("session-start.sh exists and writes epoch to session-start.txt path")
    void sessionStartHook_existsAndWritesEpoch() throws IOException {
        assertThat(SESSION_START_HOOK.toFile())
                .as("session-start.sh must exist as new Camada 0 SessionStart hook")
                .exists();

        String content = Files.readString(SESSION_START_HOOK);
        assertThat(content)
                .as("session-start.sh must write to .claude/state/session-start.txt")
                .contains("session-start.txt");
        assertThat(content)
                .as("session-start.sh must write epoch timestamp (date +%s)")
                .contains("date +%s");
    }

    @Test
    @DisplayName("verify-story-completion.sh reads session-start.txt for Signal A scoping")
    void verifyHook_readsSessionStartFile() throws IOException {
        String content = Files.readString(VERIFY_HOOK);

        assertThat(content)
                .as("verify-story-completion.sh must read session-start.txt")
                .contains("session-start.txt");

        assertThat(content)
                .as("verify-story-completion.sh Signal A must use git log --since")
                .contains("git log --since");
    }

    @Test
    @DisplayName("verify-story-completion.sh no longer tails 500 telemetry events (old bug)")
    void verifyHook_doesNotTailTelemetryFor500Lines() throws IOException {
        String content = Files.readString(VERIFY_HOOK);

        assertThat(content)
                .as(
                        "verify-story-completion.sh must NOT use 'tail -500' on telemetry "
                                + "(false-positive storm fix — TASK-0061-0006-005)")
                .doesNotContain("tail -500");
    }

    @Test
    @DisplayName(
            "verify-story-completion.sh Signal A is session-scoped (commits since SESSION_ISO)")
    void verifyHook_signalAScopedToSession() throws IOException {
        String content = Files.readString(VERIFY_HOOK);

        assertThat(content)
                .as("Signal A must use SESSION_ISO in git log --since")
                .contains("SESSION_ISO");
        assertThat(content)
                .as("Signal A must count session commits")
                .containsPattern("git log.*--since.*SESSION_ISO");
    }
}
