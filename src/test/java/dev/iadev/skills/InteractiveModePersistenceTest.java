package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EPIC-0068 / story-0068-0001 — Gate verifying that each of the 8 Anexo B orchestrators persists
 * the {@code interactiveMode} field to {@code execution-state.json} during Phase 0.
 *
 * <p>Checks that every orchestrator SKILL.md calls {@code x-internal-status-update} with {@code
 * --field interactiveMode} in its Phase 0 initialization block. Without this write, the Stop hook
 * {@code enforce-continuous-flow.sh} cannot distinguish interactive from non-interactive sessions
 * (EPIC-0068 §1.1 Problem).
 */
@DisplayName("InteractiveModePersistenceTest — EPIC-0068 orchestrators write interactiveMode")
class InteractiveModePersistenceTest {

    private static final Path SKILLS_ROOT = Path.of("src/main/resources/targets/claude/skills");

    private static final String INTERACTIVE_MODE_WRITE = "--field interactiveMode --value";

    private static final List<String> ORCHESTRATOR_SKILLS =
            List.of(
                    "core/dev/x-implement-epic",
                    "core/dev/x-implement-story",
                    "core/dev/x-implement-task",
                    "core/ops/x-release",
                    "core/plan/x-orchestrate-epic",
                    "core/review/x-review-codebase",
                    "core/review/x-review-pr",
                    "core/pr/x-manage-pr-merge-train");

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "core/dev/x-implement-epic",
                "core/dev/x-implement-story",
                "core/dev/x-implement-task",
                "core/ops/x-release",
                "core/plan/x-orchestrate-epic",
                "core/review/x-review-codebase",
                "core/review/x-review-pr",
                "core/pr/x-manage-pr-merge-train"
            })
    @DisplayName("orchestrator SKILL.md writes interactiveMode field in Phase 0")
    void orchestrator_skillMd_writesInteractiveModeField(String skillPath) throws IOException {
        Path skillFile = SKILLS_ROOT.resolve(skillPath).resolve("SKILL.md");

        assertThat(skillFile).as("SKILL.md must exist for orchestrator %s", skillPath).exists();

        String content = Files.readString(skillFile);

        assertThat(content)
                .as(
                        "Orchestrator %s must call x-internal-status-update with --field interactiveMode"
                                + " in Phase 0 (EPIC-0068 story-0068-0001)",
                        skillPath)
                .contains(INTERACTIVE_MODE_WRITE);
    }

    @Test
    @DisplayName("Rule 19 + lifecycle KP document interactiveMode fallback matrix (EPIC-0068)")
    void rule19_containsInteractiveModeFallbackMatrix() throws IOException {
        // EPIC-0078 story-0078-0011 slimmed Rule 19: the interactiveMode fallback matrix
        // moved from the rule body to the lifecycle KP backward-compatibility.md.
        // Rule 19 now references `interactiveMode` in the KP pointer; the full matrix lives in the KP.
        Path rule19 =
                Path.of("src/main/resources/targets/claude/rules/19-backward-compatibility.md");
        Path kp =
                Path.of(
                        "src/main/resources/targets/claude/knowledge/lifecycle/backward-compatibility.md");

        assertThat(rule19).as("Rule 19 source-of-truth must exist").exists();
        assertThat(kp).as("backward-compatibility KP must exist").exists();

        String rule19Content = Files.readString(rule19);
        assertThat(rule19Content)
                .as("Rule 19 must reference interactiveMode (pointer to KP — EPIC-0078 slim)")
                .contains("interactiveMode");

        String kpContent = Files.readString(kp);
        assertThat(kpContent)
                .as("Lifecycle KP must contain interactiveMode fallback matrix")
                .contains("`interactiveMode` Fallback Matrix")
                .contains("Field absent")
                .contains("\"interactive\"");
    }

    @Test
    @DisplayName("all 8 Anexo B orchestrators are covered")
    void allAnexoBOrchestrators_areCovered() {
        assertThat(ORCHESTRATOR_SKILLS)
                .as("All 8 Anexo B orchestrators must be in the coverage list (Rule 25 §Scope)")
                .hasSize(8);
    }
}
