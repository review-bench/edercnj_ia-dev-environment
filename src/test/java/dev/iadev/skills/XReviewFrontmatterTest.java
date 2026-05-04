package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TDD tests for Phase 5 (Emit Frontmatter) of x-review/SKILL.md (story-0067-0002).
 *
 * <p>Validates that the Phase 5 section is present with the required structural markers: telemetry
 * phase.start/end pair, MANDATORY TOOL CALL block, TaskCreate, pre- and post-gate invocations, and
 * TaskUpdate.
 */
@DisplayName("XReviewFrontmatterTest — Phase 5 Emit Frontmatter contract")
class XReviewFrontmatterTest {

    private static final Path SKILL_PATH =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/review/x-review-codebase/SKILL.md");

    @Test
    @DisplayName("phase5_section_present_in_skillmd")
    void phase5_section_present_in_skillmd() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("x-review SKILL.md must contain Phase 5 header")
                .contains("## Phase 5");
    }

    @Test
    @DisplayName("phase5_telemetry_markers_balanced")
    void phase5_telemetry_markers_balanced() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must have telemetry phase.start marker")
                .contains("telemetry-phase.sh start x-review-codebase Phase-5-Frontmatter");
        assertThat(content)
                .as("Phase 5 must have telemetry phase.end marker")
                .contains("telemetry-phase.sh end x-review-codebase Phase-5-Frontmatter ok");
    }

    @Test
    @DisplayName("phase5_mandatory_tool_call_block_present")
    void phase5_mandatory_tool_call_block_present() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must have MANDATORY TOOL CALL block (Rule 24 §Camada-1)")
                .contains("MANDATORY TOOL CALL (Rule 24 §Camada-1)");
    }

    @Test
    @DisplayName("phase5_phase_gate_pre_and_post_invocations_present")
    void phase5_phase_gate_pre_and_post_invocations_present() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must invoke x-internal-verify-phase-gates with --mode pre")
                .containsPattern(
                        "x-internal-verify-phase-gates.*--mode pre.*--phase.*Phase 5.*--skill x-review-codebase");
        assertThat(content)
                .as("Phase 5 must invoke x-internal-verify-phase-gates with --mode post")
                .containsPattern(
                        "x-internal-verify-phase-gates.*--mode post.*--phase.*Phase 5.*--skill x-review-codebase");
    }

    @Test
    @DisplayName("phase5_task_create_and_update_present")
    void phase5_task_create_and_update_present() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content).as("Phase 5 must emit TaskCreate").contains("TaskCreate(");
        assertThat(content)
                .as("Phase 5 must emit TaskUpdate with status: \"completed\"")
                .contains("TaskUpdate(");
    }
}
