package dev.iadev.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@code enforce-continuous-flow.sh} (EPIC-0068 story-0068-0002/0003).
 *
 * <p>Validates structural contracts of the Stop hook source-of-truth file — does not execute the
 * script (shell execution is covered by {@code enforce_continuous_flow_test.sh} + {@code
 * derive_next_mandatory_call_test.sh} in {@code src/test/bash/}).
 */
@DisplayName("EnforceContinuousFlowHookTest — EPIC-0068 hook structural contract")
class EnforceContinuousFlowHookTest {

    private static final Path HOOK =
            Path.of("src/main/resources/targets/claude/hooks/enforce-continuous-flow.sh");

    @Test
    @DisplayName("hook source-of-truth exists")
    void hook_sourceOfTruth_exists() {
        assertThat(HOOK)
                .as("enforce-continuous-flow.sh must exist in source-of-truth hooks dir")
                .exists();
    }

    @Test
    @DisplayName("hook has set -euo pipefail for safety")
    void hook_hasSetEuoPipefail() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must have 'set -euo pipefail' for bash safety")
                .contains("set -euo pipefail");
    }

    @Test
    @DisplayName("hook implements --self-check (Rule 26)")
    void hook_hasSelfCheck() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must implement --self-check for Rule 26 contract")
                .contains("--self-check");
        assertThat(content)
                .as("--self-check must verify jq is available")
                .contains("OPERATIONAL_ERROR: jq required");
    }

    @Test
    @DisplayName("hook emits CONTINUOUS_FLOW_INTERRUPT on stderr (exit 2 contract)")
    void hook_emitsContinuousFlowInterrupt() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must emit CONTINUOUS_FLOW_INTERRUPT nudge (story-0068-0002 §3.4)")
                .contains("CONTINUOUS_FLOW_INTERRUPT");
        assertThat(content)
                .as("nudge must include Next mandatory tool call line")
                .contains("Next mandatory tool call:");
    }

    @Test
    @DisplayName("hook respects interactiveMode fallback (Rule 19)")
    void hook_respectsInteractiveModeFallback() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must fall back to 'interactive' when field absent (Rule 19)")
                .contains("interactiveMode // \"interactive\"");
        assertThat(content)
                .as("hook must check for non-interactive to emit nudge")
                .contains("non-interactive");
    }

    @Test
    @DisplayName("hook implements hotfix branch guard (Rule 27 Exception 2)")
    void hook_hasHotfixGuard() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must exit 0 for hotfix/* branches (Rule 27 Exception 2)")
                .contains("hotfix/*");
    }

    @Test
    @DisplayName("hook contains derive_next_mandatory_call function (story-0068-0003)")
    void hook_containsDeriveNextMandatoryCallFunction() throws IOException {
        String content = Files.readString(HOOK);
        assertThat(content)
                .as("hook must contain derive_next_mandatory_call function (story-0068-0003)")
                .contains("derive_next_mandatory_call");
        assertThat(content)
                .as("function must search for [required] markers in SKILL.md")
                .contains("[required]");
        assertThat(content)
                .as("function must return PHASE_COMPLETE when all required calls emitted")
                .contains("PHASE_COMPLETE");
    }

    @Test
    @DisplayName("hook is under 200 lines (Rule 03 / story-0068-0002 §4.2)")
    void hook_isUnder200Lines() throws IOException {
        long lineCount = Files.lines(HOOK).count();
        assertThat(lineCount)
                .as("hook must be ≤ 200 lines (story-0068-0002 §4.2 coding constraint)")
                .isLessThanOrEqualTo(200);
    }

    @Test
    @DisplayName("derive_next_mandatory_call function is under 25 lines (Rule 03)")
    void deriveFunction_isUnder25Lines() throws IOException {
        String content = Files.readString(HOOK);
        int startIdx = content.indexOf("derive_next_mandatory_call()");
        assertThat(startIdx)
                .as("derive_next_mandatory_call function must be present")
                .isGreaterThan(0);
        String fromStart = content.substring(startIdx);
        int endIdx = fromStart.indexOf("\n}\n");
        assertThat(endIdx)
                .as("function must have closing brace")
                .isGreaterThan(0);
        String funcBody = fromStart.substring(0, endIdx + 3);
        long lineCount = funcBody.lines().count();
        assertThat(lineCount)
                .as("derive_next_mandatory_call must be ≤ 25 lines (Rule 03)")
                .isLessThanOrEqualTo(25);
    }
}
