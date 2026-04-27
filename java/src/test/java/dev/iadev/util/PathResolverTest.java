package dev.iadev.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PathResolver — v3/v4 probe and path construction")
class PathResolverTest {

    @TempDir
    Path base;

    PathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PathResolver(base);
    }

    // ── Scenario 1: v3 probe negative ─────────────────────────────────────

    @Test
    @DisplayName("epicDir returns v3 path when no v4 directory exists")
    void epicDir_v3Layout_returnsLegacyPath() {
        Path result = resolver.epicDir("0001");

        assertThat(result).isEqualTo(base.resolve("plans/epic-0001").normalize());
    }

    // ── Scenario 2: v4 probe positive ─────────────────────────────────────

    @Test
    @DisplayName("epicDir returns v4 path when ai/epics/epic-0060-* directory exists")
    void epicDir_v4Layout_returnsNewPath() throws IOException {
        Path v4Dir = base.resolve("ai/epics/epic-0060-folder-reorganization");
        Files.createDirectories(v4Dir);

        Path result = resolver.epicDir("0060");

        assertThat(result.toString()).contains("epic-0060-folder-reorganization");
    }

    // ── Scenario 3: invalid epicId ─────────────────────────────────────────

    @Test
    @DisplayName("epicDir throws IllegalArgumentException for non-4-digit epicId")
    void epicDir_invalidEpicId_throwsException() {
        assertThatThrownBy(() -> resolver.epicDir("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid epicId format");
    }

    @Test
    @DisplayName("epicDir rejects traversal attempt via epicId")
    void epicDir_traversalAttempt_throwsException() {
        assertThatThrownBy(() -> resolver.epicDir("../x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("epicDir rejects null epicId")
    void epicDir_nullEpicId_throwsException() {
        assertThatThrownBy(() -> resolver.epicDir(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── UnitType enum ─────────────────────────────────────────────────────

    @Test
    @DisplayName("UnitType has exactly STORY, BUG, SPIKE, CHORE")
    void unitType_hasExpectedValues() {
        UnitType[] values = UnitType.values();

        assertThat(values).extracting(Enum::name)
                .containsExactlyInAnyOrder("STORY", "BUG", "SPIKE", "CHORE");
    }

    @Test
    @DisplayName("UnitType.STORY has correct sub-folder and prefix")
    void unitType_story_hasCorrectMetadata() {
        assertThat(UnitType.STORY.subFolder()).isEqualTo("stories");
        assertThat(UnitType.STORY.prefix()).isEqualTo("story");
    }

    // ── unitDir / planDir / reviewDir / reportDir ─────────────────────────

    @Test
    @DisplayName("unitDir returns nested work/<type> path under epic")
    void unitDir_returnsCorrectNestedPath() {
        Path result = resolver.unitDir("0060", UnitType.STORY, "0001");

        assertThat(result.toString())
                .contains("plans/epic-0060")
                .contains("work/stories/story-0060-0001");
    }

    @Test
    @DisplayName("unitDir uses correct prefix for each UnitType")
    void unitDir_eachUnitType_usesCorrectPrefix() {
        assertThat(resolver.unitDir("0060", UnitType.BUG, "0001").toString())
                .contains("work/bugs/bug-0060-0001");
        assertThat(resolver.unitDir("0060", UnitType.SPIKE, "0001").toString())
                .contains("work/spikes/spike-0060-0001");
        assertThat(resolver.unitDir("0060", UnitType.CHORE, "0001").toString())
                .contains("work/chores/chore-0060-0001");
    }

    @Test
    @DisplayName("unitDir throws when type is null")
    void unitDir_nullType_throws() {
        assertThatThrownBy(() -> resolver.unitDir("0060", null, "0001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UnitType cannot be null");
    }

    @Test
    @DisplayName("planDir, reviewDir, reportDir chain off unitDir")
    void subDirs_chainOffUnitDir() {
        Path unit = resolver.unitDir("0060", UnitType.STORY, "0001");

        assertThat(resolver.planDir("0060", UnitType.STORY, "0001"))
                .isEqualTo(unit.resolve("plans"));
        assertThat(resolver.reviewDir("0060", UnitType.STORY, "0001"))
                .isEqualTo(unit.resolve("reviews"));
        assertThat(resolver.reportDir("0060", UnitType.STORY, "0001"))
                .isEqualTo(unit.resolve("reports"));
    }

    // ── epicTelemetry / epicState ─────────────────────────────────────────

    @Test
    @DisplayName("epicTelemetry returns telemetry/events.ndjson under epic")
    void epicTelemetry_returnsExpectedPath() {
        Path result = resolver.epicTelemetry("0060");

        assertThat(result.toString()).endsWith("telemetry/events.ndjson");
        assertThat(result.toString()).contains("plans/epic-0060");
    }

    @Test
    @DisplayName("epicState returns execution-state.json under epic")
    void epicState_returnsExpectedPath() {
        Path result = resolver.epicState("0060");

        assertThat(result.toString()).endsWith("execution-state.json");
        assertThat(result.toString()).contains("plans/epic-0060");
    }

    // ── releasesDir / runsDir ─────────────────────────────────────────────

    @Test
    @DisplayName("releasesDir returns ai/releases under base")
    void releasesDir_returnsAiReleases() {
        assertThat(resolver.releasesDir())
                .isEqualTo(base.resolve("ai/releases").normalize());
    }

    @Test
    @DisplayName("runsDir returns ai/runs under base")
    void runsDir_returnsAiRuns() {
        assertThat(resolver.runsDir())
                .isEqualTo(base.resolve("ai/runs").normalize());
    }
}
