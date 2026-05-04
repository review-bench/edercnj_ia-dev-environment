package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TDD tests for Phase 5 (Emit Frontmatter) of x-review-pr/SKILL.md (story-0067-0003).
 *
 * <p>Mirrors XReviewFrontmatterTest but validates the tech-lead review variant: score-max 55,
 * checklist field (not reviewers), generated-by prefix x-review-pr.
 */
@DisplayName("XReviewPrFrontmatterTest — Phase 5 Emit Frontmatter contract")
class XReviewPrFrontmatterTest {

    private static final Path SKILL_PATH =
            Path.of("src/main/resources/targets/claude/skills/core/review/x-review-pr/SKILL.md");

    @Test
    @DisplayName("phase5_section_present_in_skillmd")
    void phase5_section_present_in_skillmd() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("x-review-pr SKILL.md must contain Phase 5 header")
                .contains("## Phase 5");
    }

    @Test
    @DisplayName("phase5_telemetry_markers_balanced")
    void phase5_telemetry_markers_balanced() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must have telemetry phase.start marker for x-review-pr")
                .contains("telemetry-phase.sh start x-review-pr Phase-5-Frontmatter");
        assertThat(content)
                .as("Phase 5 must have telemetry phase.end marker for x-review-pr")
                .contains("telemetry-phase.sh end x-review-pr Phase-5-Frontmatter ok");
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
    @DisplayName("phase5_checklist_field_emitted_not_reviewers")
    void phase5_checklist_field_emitted_not_reviewers() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must emit checklist field with total: 45")
                .contains("checklist:")
                .contains("total: 45");
        assertThat(content)
                .as("Phase 5 must emit score-max: 55 (tech-lead constant)")
                .contains("score-max: 55");
        assertThat(content)
                .as(
                        "Phase 5 MUST NOT document a YAML field 'reviewers:' — Tech Lead is sole"
                                + " reviewer; checklist replaces it")
                .doesNotContain("reviewers:");
        assertThat(content)
                .as("Phase 5 must document generated-by prefix 'x-review-pr@' (schema pattern)")
                .contains("x-review-pr@");
    }

    @Test
    @DisplayName("phase5_phase_gate_pre_and_post_invocations_present")
    void phase5_phase_gate_pre_and_post_invocations_present() throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("Phase 5 must invoke x-internal-verify-phase-gates with --mode pre")
                .containsPattern(
                        "x-internal-verify-phase-gates.*--mode pre.*--phase.*Phase 5.*--skill x-review-pr");
        assertThat(content)
                .as("Phase 5 must invoke x-internal-verify-phase-gates with --mode post")
                .containsPattern(
                        "x-internal-verify-phase-gates.*--mode post.*--phase.*Phase 5.*--skill x-review-pr");
    }
}
