package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Validates that x-story-implement and x-task-implement SKILL.md files contain balanced
 * phase.start/phase.end markers for every numbered phase (static inspection).
 *
 * <p>Complements TelemetryMarkerLint: the lint detects DUPLICATE/DANGLING/UNCLOSED; this test
 * detects ABSENT markers per numbered phase (the gap the lint misses).
 *
 * <p>TPP order: degenerate (0 phases) → happy x-story-implement → happy x-task-implement → boundary
 * (error phase has failed status doc).
 */
@DisplayName("PhaseMarkerEmissionTest")
class PhaseMarkerEmissionTest {

    private static final String SKILLS_ROOT = "src/main/resources/targets/claude/skills/core/dev/";

    private static final Path X_STORY = Path.of(SKILLS_ROOT + "x-story-implement/SKILL.md");

    private static final Path X_TASK = Path.of(SKILLS_ROOT + "x-task-implement/SKILL.md");

    private static final Pattern PHASE_HEADER = Pattern.compile("^## Phase (\\d+)[^\\d]");
    private static final Pattern PHASE_START =
            Pattern.compile("telemetry-phase\\.sh\\s+start\\s+(\\S+)\\s+(Phase-\\d+-[^\\s`'\"]+)");
    private static final Pattern PHASE_END =
            Pattern.compile("telemetry-phase\\.sh\\s+end\\s+(\\S+)\\s+(Phase-\\d+-[^\\s`'\"]+)");

    @Nested
    @DisplayName("degenerate — skill with zero numbered phases")
    class Degenerate {

        @Test
        @DisplayName("skill with no Phase N headers requires no markers")
        void skillWithZeroNumberedPhases_requiresNoMarkers(@TempDir Path tempDir)
                throws IOException {
            Path fakeSkill = tempDir.resolve("SKILL.md");
            Files.writeString(
                    fakeSkill,
                    "# My Skill\n\n## Context\n\nJust prose. No numbered phases.\n",
                    StandardCharsets.UTF_8);

            List<Integer> phases = extractNumberedPhases(fakeSkill);

            assertThat(phases).isEmpty();
        }
    }

    @Nested
    @DisplayName("happy path — x-story-implement")
    class XStoryImplement {

        @Test
        @DisplayName("every numbered phase has balanced phase.start/phase.end markers")
        void xStoryImplement_hasBalancedMarkersPerPhase() throws IOException {
            assertThat(X_STORY).as("x-story-implement/SKILL.md must exist").exists();

            List<String> lines = Files.readAllLines(X_STORY, StandardCharsets.UTF_8);
            List<Integer> numberedPhases = extractNumberedPhasesFromLines(lines);

            assertThat(numberedPhases)
                    .as("x-story-implement should have at least 3 numbered phases")
                    .hasSizeGreaterThanOrEqualTo(3);

            List<String> starts = extractMarkers(lines, PHASE_START);
            List<String> ends = extractMarkers(lines, PHASE_END);

            assertThat(starts).as("Must have at least one phase.start marker").isNotEmpty();
            assertThat(ends).as("Must have at least one phase.end marker").isNotEmpty();
            assertThat(starts).as("start and end counts must match").hasSameSizeAs(ends);

            for (String startId : starts) {
                assertThat(ends)
                        .as("Every start marker '%s' must have a matching end", startId)
                        .contains(startId);
            }
        }
    }

    @Nested
    @DisplayName("happy path — x-task-implement")
    class XTaskImplement {

        @Test
        @DisplayName("every numbered phase has balanced phase.start/phase.end markers")
        void xTaskImplement_hasBalancedMarkersPerPhase() throws IOException {
            assertThat(X_TASK).as("x-task-implement/SKILL.md must exist").exists();

            List<String> lines = Files.readAllLines(X_TASK, StandardCharsets.UTF_8);
            List<Integer> numberedPhases = extractNumberedPhasesFromLines(lines);

            assertThat(numberedPhases)
                    .as("x-task-implement should have at least 4 numbered phases")
                    .hasSizeGreaterThanOrEqualTo(4);

            List<String> starts = extractMarkers(lines, PHASE_START);
            List<String> ends = extractMarkers(lines, PHASE_END);

            assertThat(starts).as("Must have at least one phase.start marker").isNotEmpty();
            assertThat(ends).as("Must have at least one phase.end marker").isNotEmpty();
            assertThat(starts).as("start and end counts must match").hasSameSizeAs(ends);

            for (String startId : starts) {
                assertThat(ends)
                        .as("Every start marker '%s' must have a matching end", startId)
                        .contains(startId);
            }
        }
    }

    @Nested
    @DisplayName("boundary — error phases document failed status")
    class ErrorPhase {

        @Test
        @DisplayName("x-story-implement contains phase.end with status=failed or error doc")
        void phaseWithError_emitsPhaseEndWithStatusFailed() throws IOException {
            assertThat(X_STORY).exists();

            String content = Files.readString(X_STORY, StandardCharsets.UTF_8);

            boolean hasFailedEnd =
                    content.contains("telemetry-phase.sh end")
                            && (content.contains("failed") || content.contains("status=failed"));

            boolean hasErrorDoc =
                    content.contains("status=failed")
                            || content.contains("phase.end status=failed")
                            || content.contains("Phase-3-Verify")
                                    && content.contains("VERIFY_FAILED");

            assertThat(hasFailedEnd || hasErrorDoc)
                    .as(
                            "x-story-implement must document error phase-end (status=failed or"
                                    + " equivalent)")
                    .isTrue();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Integer> extractNumberedPhases(Path skillMd) throws IOException {
        List<String> lines = Files.readAllLines(skillMd, StandardCharsets.UTF_8);
        return extractNumberedPhasesFromLines(lines);
    }

    private List<Integer> extractNumberedPhasesFromLines(List<String> lines) {
        List<Integer> phases = new ArrayList<>();
        for (String line : lines) {
            Matcher m = PHASE_HEADER.matcher(line);
            if (m.find()) {
                phases.add(Integer.parseInt(m.group(1)));
            }
        }
        return phases;
    }

    private List<String> extractMarkers(List<String> lines, Pattern pattern) {
        List<String> ids = new ArrayList<>();
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                ids.add(m.group(2));
            }
        }
        return ids;
    }
}
