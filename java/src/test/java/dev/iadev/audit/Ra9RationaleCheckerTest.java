package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Ra9RationaleChecker} (TASK-0056-0007-002).
 *
 * <p>Checks that section 8 (Decision Rationale) is filled correctly in Epic/Story artifacts. Task
 * files accept N/A.
 */
@DisplayName("Ra9RationaleCheckerTest")
class Ra9RationaleCheckerTest {

    private static final String SECTION_HEADER = "## 8. Decision Rationale\n\n";

    private static final String VALID_RATIONALE =
            SECTION_HEADER
                    + "**Decisão:** Use JPA.\n"
                    + "**Motivo:** Reduces boilerplate.\n"
                    + "**Alternativa descartada:** JDBC.\n"
                    + "**Consequência:** Spring dependency.\n";

    private final Ra9RationaleChecker checker = new Ra9RationaleChecker();

    @Test
    @DisplayName("check_validRationale_noViolations")
    void check_validRationale_noViolations() {
        List<String> violations = checker.check(VALID_RATIONALE, "epic-0060.md");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("check_emptySection8_flagsViolation")
    void check_emptySection8_flagsViolation() {
        String content = SECTION_HEADER + "\n\n";
        List<String> violations = checker.check(content, "story-0060-0001.md");
        assertThat(violations).as("Empty section 8 must flag RA9_RATIONALE_EMPTY").isNotEmpty();
        assertThat(violations.get(0)).contains("RA9_RATIONALE_EMPTY");
    }

    @Test
    @DisplayName("check_placeholderBody_flagsViolation")
    void check_placeholderBody_flagsViolation() {
        String content = SECTION_HEADER + "{{DECISION_RATIONALE}}\n";
        List<String> violations = checker.check(content, "story-0060-0001.md");
        assertThat(violations).as("Placeholder body must flag RA9_RATIONALE_EMPTY").isNotEmpty();
    }

    @Test
    @DisplayName("check_todoBody_flagsViolation")
    void check_todoBody_flagsViolation() {
        String content = SECTION_HEADER + "TODO: fill this.\n";
        List<String> violations = checker.check(content, "epic-0060.md");
        assertThat(violations).as("TODO body must flag RA9_RATIONALE_EMPTY").isNotEmpty();
    }

    @Test
    @DisplayName("check_taskWithNa_noViolations")
    void check_taskWithNa_noViolations() {
        String content = SECTION_HEADER + "N/A — VO imutável, sem trade-off relevante.\n";
        List<String> violations = checker.check(content, "task-0060-0001-001.md");
        assertThat(violations).as("Task with N/A rationale must not flag violation").isEmpty();
    }

    @Test
    @DisplayName("check_storyWithNa_flagsViolation")
    void check_storyWithNa_flagsViolation() {
        String content = SECTION_HEADER + "N/A — sem trade-off.\n";
        List<String> violations = checker.check(content, "story-0060-0001.md");
        assertThat(violations)
                .as(
                        "N/A is accepted only on Task artifacts."
                                + " Story MUST use the 4-line micro-template.")
                .isNotEmpty();
    }

    @Test
    @DisplayName("check_epicWithNa_flagsViolation")
    void check_epicWithNa_flagsViolation() {
        String content = SECTION_HEADER + "N/A — sem trade-off.\n";
        List<String> violations = checker.check(content, "plans/epic-0060/epic-0060.md");
        assertThat(violations)
                .as("Epic must use the 4-line micro-template," + " not N/A")
                .isNotEmpty();
    }

    @Test
    @DisplayName("check_noSection8_noViolation")
    void check_noSection8_noViolation() {
        String content = "## 1. Contexto & Escopo\ncontent\n";
        List<String> violations = checker.check(content, "story.md");
        assertThat(violations)
                .as("Missing section 8 is caught by Ra9SectionsChecker," + " not this checker")
                .isEmpty();
    }

    @Test
    @DisplayName("check_auditExempt_noViolations")
    void check_auditExempt_noViolations() {
        String content = "<!-- audit-exempt -->\n" + SECTION_HEADER + "\n\n";
        List<String> violations = checker.check(content, "legacy.md");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("check_taskWithLowercaseNa_noViolations")
    void check_taskWithLowercaseNa_noViolations() {
        String content = SECTION_HEADER + "n/a — immutable VO, no trade-off.\n";
        List<String> violations = checker.check(content, "task-0060-0001-001.md");
        assertThat(violations).as("Task with lowercase n/a must not flag violation").isEmpty();
    }

    @Test
    @DisplayName("check_sectionFollowedByAnotherSection_bodyTruncatedCorrectly")
    void check_sectionFollowedByAnotherSection_bodyTruncatedCorrectly() {
        String content =
                SECTION_HEADER
                        + "**Decisão:** Use X.\n"
                        + "**Motivo:** Reason.\n"
                        + "**Alternativa descartada:** Y.\n"
                        + "**Consequência:** Z.\n"
                        + "\n## 9. Next Section\n\nContent here.\n";
        List<String> violations = checker.check(content, "epic-0060.md");
        assertThat(violations)
                .as("Section 8 body truncated at next ## — valid rationale must pass")
                .isEmpty();
    }

    @Test
    @DisplayName("check_taskWithValidRationale_noViolations")
    void check_taskWithValidRationale_noViolations() {
        // Task artifact with full 4-field rationale (not N/A):
        // isTaskArtifact=true, isNaAccepted=false → continues to full rationale check
        List<String> violations = checker.check(VALID_RATIONALE, "task-0060-0001-001.md");
        assertThat(violations).as("Task with full rationale must pass").isEmpty();
    }

    @Test
    @DisplayName("check_section8HeaderWithNoNewline_treatedAsEmpty")
    void check_section8HeaderWithNoNewline_treatedAsEmpty() {
        // Section 8 header at end of string with no newline — bodyStart < 0 path returns ""
        // empty body is treated as RA9_RATIONALE_EMPTY violation for a story
        String content = "## 8. Decision Rationale";
        List<String> violations = checker.check(content, "story-0060-0001.md");
        assertThat(violations)
                .as("Section 8 with no body (no newline after header) treated as empty → violation")
                .isNotEmpty();
    }
}
