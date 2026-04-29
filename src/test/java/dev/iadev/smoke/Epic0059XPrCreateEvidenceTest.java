package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0007-002: Verification tests confirming that {@code x-pr-create} SKILL.md declares the
 * {@code ## Orchestrator Evidence} injection (Phase 3.5).
 *
 * <p>These tests verify the structural requirements of the extended {@code x-pr-create} SKILL.md
 * under the source-of-truth directory. They act as regression guards ensuring the Phase 3.5
 * Orchestrator Evidence section is not accidentally removed.
 *
 * <p>Scenarios validated:
 *
 * <ol>
 *   <li>SKILL.md exists in source-of-truth ({@code src/main/resources/}).
 *   <li>Phase 3.5 section header is present.
 *   <li>Orchestrator Evidence bash block is present (Story IDs extraction).
 *   <li>{@code --no-story-evidence} flag is declared in the parameters table.
 *   <li>{@code Invocation Skill | x-story-implement} canonical value is present.
 *   <li>Both Phase 1 and Phase 3 artifact collection loops are present.
 * </ol>
 *
 * @see <a href="plans/epic-0059/story-0059-0007.md">story-0059-0007</a>
 */
@DisplayName("Epic0059XPrCreateEvidenceTest — TASK-0059-0007-002")
class Epic0059XPrCreateEvidenceTest {

    private static final String SOURCE_SKILL_PATH =
            "src/main/resources/targets/claude/skills/core/pr/x-pr-create/SKILL.md";

    private static final String GENERATED_SKILL_PATH = ".claude/skills/x-pr-create/SKILL.md";

    @Test
    @DisplayName("sourceSkillExists — source-of-truth x-pr-create/SKILL.md must exist")
    void sourceSkillExists() {
        Path skill = repoRoot().resolve(SOURCE_SKILL_PATH);
        assertThat(skill).as("source-of-truth x-pr-create SKILL.md must exist").isRegularFile();
    }

    @Test
    @DisplayName("sourceSkillContainsPhase35Header — Phase 3.5 Orchestrator Evidence section")
    void sourceSkillContainsPhase35Header() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as("source SKILL.md must contain Phase 3.5 EVIDENCE section header")
                .contains("Phase 3.5 -- Inject Orchestrator Evidence");
    }

    @Test
    @DisplayName("sourceSkillContainsOrchestratorEvidenceBlock — ## Orchestrator Evidence table")
    void sourceSkillContainsOrchestratorEvidenceBlock() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as("source SKILL.md must contain ## Orchestrator Evidence block")
                .contains("## Orchestrator Evidence");
    }

    @Test
    @DisplayName("sourceSkillContainsNoStoryEvidenceFlag — --no-story-evidence parameter")
    void sourceSkillContainsNoStoryEvidenceFlag() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as("source SKILL.md must declare --no-story-evidence flag in parameters table")
                .contains("--no-story-evidence");
    }

    @Test
    @DisplayName(
            "sourceSkillContainsCanonicalInvocationSkill — x-story-implement as canonical value")
    void sourceSkillContainsCanonicalInvocationSkill() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as(
                        "source SKILL.md must set 'Invocation Skill | x-story-implement' (not a placeholder)")
                .contains("Invocation Skill | x-story-implement");
    }

    @Test
    @DisplayName("sourceSkillContainsPhase1ArtifactLoop — Phase 1 artifact collection loop")
    void sourceSkillContainsPhase1ArtifactLoop() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as(
                        "source SKILL.md must contain Phase 1 artifact collection (arch/plan/tests/tasks/security/compliance)")
                .contains("arch-story-")
                .contains("plan-story-")
                .contains("tests-story-")
                .contains("tasks-story-");
    }

    @Test
    @DisplayName("sourceSkillContainsPhase3ArtifactLoop — Phase 3 artifact collection loop")
    void sourceSkillContainsPhase3ArtifactLoop() throws IOException {
        String content = readSourceSkill();
        assertThat(content)
                .as(
                        "source SKILL.md must contain Phase 3 artifact collection (verify-envelope/story-completion-report)")
                .contains("verify-envelope-")
                .contains("story-completion-report-");
    }

    @Test
    @DisplayName("generatedSkillContainsPhase35 — generated .claude/ SKILL.md also has Phase 3.5")
    void generatedSkillContainsPhase35() throws IOException {
        Path generated = repoRoot().resolve(GENERATED_SKILL_PATH);
        if (!Files.exists(generated)) {
            // .claude/ is gitignored — skip when running in isolated env
            return;
        }
        String content = Files.readString(generated, StandardCharsets.UTF_8);
        assertThat(content)
                .as("generated .claude/skills/x-pr-create/SKILL.md must also contain Phase 3.5")
                .contains("Phase 3.5 -- Inject Orchestrator Evidence");
    }

    @Test
    @DisplayName(
            "generatedSkillContainsNoStoryEvidenceFlag — generated SKILL.md has --no-story-evidence")
    void generatedSkillContainsNoStoryEvidenceFlag() throws IOException {
        Path generated = repoRoot().resolve(GENERATED_SKILL_PATH);
        if (!Files.exists(generated)) {
            return;
        }
        String content = Files.readString(generated, StandardCharsets.UTF_8);
        assertThat(content)
                .as("generated SKILL.md must declare --no-story-evidence flag")
                .contains("--no-story-evidence");
    }

    // ------------------------------------------------------------------ helpers

    private String readSourceSkill() throws IOException {
        return Files.readString(repoRoot().resolve(SOURCE_SKILL_PATH), StandardCharsets.UTF_8);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
