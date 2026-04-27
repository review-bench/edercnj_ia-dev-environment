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
}
