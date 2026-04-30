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
                    "core/dev/x-epic-implement",
                    "core/dev/x-story-implement",
                    "core/dev/x-task-implement",
                    "core/ops/x-release",
                    "core/plan/x-epic-orchestrate",
                    "core/review/x-review",
                    "core/review/x-review-pr",
                    "core/pr/x-pr-merge-train");

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "core/dev/x-epic-implement",
                "core/dev/x-story-implement",
                "core/dev/x-task-implement",
                "core/ops/x-release",
                "core/plan/x-epic-orchestrate",
                "core/review/x-review",
                "core/review/x-review-pr",
                "core/pr/x-pr-merge-train"
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
    @DisplayName("Rule 19 source-of-truth contains interactiveMode fallback matrix entry")
    void rule19_containsInteractiveModeFallbackMatrix() throws IOException {
        Path rule19 =
                Path.of("src/main/resources/targets/claude/rules/19-backward-compatibility.md");

        assertThat(rule19).as("Rule 19 source-of-truth must exist").exists();

        String content = Files.readString(rule19);

        assertThat(content)
                .as("Rule 19 must document interactiveMode fallback matrix (EPIC-0068)")
                .contains("`interactiveMode` Field (EPIC-0068)");

        assertThat(content)
                .as(
                        "Rule 19 interactiveMode section must cover absent-field fallback to interactive")
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
