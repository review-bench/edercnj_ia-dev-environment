package dev.iadev.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Validates that x-internal-epic-create and x-internal-story-create emit v2 templates by default
 * and declare the --legacy-template-v1 flag (EPIC-0070 / story-0070-0005).
 */
@DisplayName("PlanSkillsV2TemplateDefaultTest")
class PlanSkillsV2TemplateDefaultTest {

    private static final Path INTERNAL_PLAN_SKILLS_ROOT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "internal",
                    "plan");

    @ParameterizedTest(name = "{0} declares --legacy-template-v1 parameter")
    @ValueSource(strings = {"x-internal-epic-create", "x-internal-story-create"})
    @DisplayName("createSkill_hasLegacyTemplateV1Flag_inParametersTable")
    void createSkill_hasLegacyTemplateV1Flag_inParametersTable(String skillName)
            throws IOException {
        Path skillFile =
                INTERNAL_PLAN_SKILLS_ROOT.resolve(skillName).resolve("SKILL.md").toAbsolutePath();

        assertThat(skillFile).as("SKILL.md for %s must exist", skillName).exists();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("Skill %s MUST declare --legacy-template-v1 in Parameters table", skillName)
                .contains("--legacy-template-v1");
    }

    @ParameterizedTest(name = "{0} references v2 template structure")
    @ValueSource(strings = {"x-internal-epic-create", "x-internal-story-create"})
    @DisplayName("createSkill_referencesV2TemplateStructure")
    void createSkill_referencesV2TemplateStructure(String skillName) throws IOException {
        Path skillFile =
                INTERNAL_PLAN_SKILLS_ROOT.resolve(skillName).resolve("SKILL.md").toAbsolutePath();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("Skill %s MUST reference v2 value-driven template (EPIC-0070)", skillName)
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("v2 value-driven"),
                        c -> assertThat(c).contains("EPIC-0070"),
                        c -> assertThat(c).contains("v2 sections"));
    }

    @ParameterizedTest(name = "{0} contains deprecation warning text")
    @ValueSource(strings = {"x-internal-epic-create", "x-internal-story-create"})
    @DisplayName("createSkill_hasDeprecationWarning_forLegacyFlag")
    void createSkill_hasDeprecationWarning_forLegacyFlag(String skillName) throws IOException {
        Path skillFile =
                INTERNAL_PLAN_SKILLS_ROOT.resolve(skillName).resolve("SKILL.md").toAbsolutePath();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "Skill %s MUST include deprecation warning text for --legacy-template-v1",
                        skillName)
                .contains("WARN [legacy-template]")
                .contains("DEPRECATED");
    }

    @ParameterizedTest(name = "{0} has ## Examples section")
    @ValueSource(strings = {"x-internal-epic-create", "x-internal-story-create"})
    @DisplayName("createSkill_hasExamplesSection")
    void createSkill_hasExamplesSection(String skillName) throws IOException {
        Path skillFile =
                INTERNAL_PLAN_SKILLS_ROOT.resolve(skillName).resolve("SKILL.md").toAbsolutePath();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("Skill %s MUST have ## Examples section (story-0070-0005)", skillName)
                .contains("## Examples");
    }
}
