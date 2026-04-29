package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0007-001: Verification tests for the GitHub PR template that mandates the {@code ##
 * Orchestrator Evidence} section.
 *
 * <p>Validates the structural requirements of {@code .github/pull_request_template.md}:
 *
 * <ol>
 *   <li>Template file exists and is readable.
 *   <li>Template contains the mandatory {@code ## Orchestrator Evidence} section.
 *   <li>Template contains all required fields (Story IDs, Orchestrator Commit SHA, Invocation
 *       Skill, Phase 1 Artifacts, Phase 3 Artifacts).
 *   <li>Template contains placeholder values that {@code audit-pr-evidence.sh} will detect as
 *       unfilled (proof of rejection surface).
 * </ol>
 *
 * @see <a href="plans/epic-0059/story-0059-0007.md">story-0059-0007</a>
 */
@DisplayName("Epic0059PrTemplateTest — TASK-0059-0007-001")
class Epic0059PrTemplateTest {

    private static final String TEMPLATE_PATH = ".github/pull_request_template.md";

    @Test
    @DisplayName("templateExists — .github/pull_request_template.md must exist")
    void templateExists() {
        Path template = repoRoot().resolve(TEMPLATE_PATH);
        assertThat(template)
                .as(".github/pull_request_template.md must exist (story-0059-0007)")
                .isRegularFile();
    }

    @Test
    @DisplayName("templateContainsOrchestratorEvidenceSection — mandatory section header")
    void templateContainsOrchestratorEvidenceSection() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain ## Orchestrator Evidence section")
                .contains("## Orchestrator Evidence");
    }

    @Test
    @DisplayName("templateContainsStoryIdsField — Story IDs campo obrigatório")
    void templateContainsStoryIdsField() throws IOException {
        String content = readTemplate();
        assertThat(content).as("template must contain 'Story IDs' field").contains("Story IDs");
    }

    @Test
    @DisplayName(
            "templateContainsOrchestratorCommitShaField — Orchestrator Commit SHA campo obrigatório")
    void templateContainsOrchestratorCommitShaField() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'Orchestrator Commit SHA' field")
                .contains("Orchestrator Commit SHA");
    }

    @Test
    @DisplayName("templateContainsInvocationSkillField — Invocation Skill campo obrigatório")
    void templateContainsInvocationSkillField() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'Invocation Skill' field")
                .contains("Invocation Skill");
    }

    @Test
    @DisplayName("templateContainsPhase1ArtifactsField — Phase 1 Artifacts campo obrigatório")
    void templateContainsPhase1ArtifactsField() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'Phase 1 Artifacts' field")
                .contains("Phase 1 Artifacts");
    }

    @Test
    @DisplayName("templateContainsPhase3ArtifactsField — Phase 3 Artifacts campo obrigatório")
    void templateContainsPhase3ArtifactsField() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'Phase 3 Artifacts' field")
                .contains("Phase 3 Artifacts");
    }

    @Test
    @DisplayName("templateContainsStoryIdPlaceholder — audit must detect unfilled Story IDs")
    void templateContainsStoryIdPlaceholder() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'story-XXXX-YYYY' placeholder for audit detection")
                .contains("story-XXXX-YYYY");
    }

    @Test
    @DisplayName(
            "templateContainsShaPlaceholder — audit must detect unfilled Orchestrator Commit SHA")
    void templateContainsShaPlaceholder() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'abc123def456...' placeholder for audit detection")
                .contains("abc123def456...");
    }

    @Test
    @DisplayName("templateContainsSkillPlaceholder — audit must detect unfilled Invocation Skill")
    void templateContainsSkillPlaceholder() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain 'SKILL-NAME-HERE' placeholder for audit detection")
                .contains("SKILL-NAME-HERE");
    }

    @Test
    @DisplayName("templateContainsDoNotEditComment — x-pr-create instrumentation note")
    void templateContainsDoNotEditComment() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("template must contain the x-pr-create instrumentation comment")
                .contains("Filled automatically by x-pr-create");
    }

    // ------------------------------------------------------------------ helpers

    private String readTemplate() throws IOException {
        return Files.readString(repoRoot().resolve(TEMPLATE_PATH), StandardCharsets.UTF_8);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
