package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.PlanTemplateDefinitions;
import dev.iadev.application.assembler.SkillsSelection;
import dev.iadev.domain.model.QualityConfig;
import dev.iadev.testutil.TestConfigBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0072MutationSkillSmokeIT — x-test-mutation structural invariants")
class Epic0072MutationSkillSmokeIT {

    private static final Path SKILL_ROOT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-test-mutation");

    private static final Path KP_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "testing");

    private static final Path TEMPLATES_ROOT =
            Path.of("src", "main", "resources", "shared", "templates");

    @Nested
    @DisplayName("SKILL.md — file structure")
    class SkillStructure {

        @Test
        @DisplayName("scenario1_skillFile_exists")
        void scenario1_skillFile_exists() {
            assertThat(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario2_skillFile_containsStackDispatchMatrix")
        void scenario2_skillFile_containsStackDispatchMatrix() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must reference PIT for Java").contains("PIT");
            assertThat(content).as("must reference Stryker for JS/TS").contains("Stryker");
            assertThat(content).as("must reference mutmut for Python").contains("mutmut");
            assertThat(content).as("must reference go-mutesting for Go").contains("go-mutesting");
        }

        @Test
        @DisplayName("scenario3_skillFile_containsExitCodes")
        void scenario3_skillFile_containsExitCodes() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content)
                    .as("must document MUTATION_SCORE_BELOW_THRESHOLD")
                    .contains("MUTATION_SCORE_BELOW_THRESHOLD");
            assertThat(content)
                    .as("must document MUTATION_RUNTIME_CAP_EXCEEDED")
                    .contains("MUTATION_RUNTIME_CAP_EXCEEDED");
            assertThat(content)
                    .as("must document MUTATION_DISABLED exit code")
                    .contains("MUTATION_DISABLED");
        }

        @Test
        @DisplayName("scenario4_skillFile_containsThresholdConfig")
        void scenario4_skillFile_containsThresholdConfig() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content)
                    .as("must reference quality.mutation.threshold")
                    .contains("quality.mutation.threshold");
            assertThat(content).as("must document runtime-cap-min").contains("runtime-cap-min");
        }

        @Test
        @DisplayName("scenario5_skillFile_requiresCapabilityFrontmatter")
        void scenario5_skillFile_requiresCapabilityFrontmatter() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must declare requires-any").contains("requires-any:");
            assertThat(content)
                    .as("must list quality.mutation.java-pit")
                    .contains("quality.mutation.java-pit");
            assertThat(content)
                    .as("must list quality.mutation.stryker")
                    .contains("quality.mutation.stryker");
        }
    }

    @Nested
    @DisplayName("QualityGate skill selection")
    class GateSelection {

        @Test
        @DisplayName("scenario6_qualityMutationEnabled_selectsXTestMutation")
        void scenario6_qualityMutationEnabled_selectsXTestMutation() {
            var baseConfig = TestConfigBuilder.builder().build();
            var qualityConfig =
                    new QualityConfig(
                            QualityConfig.PerformanceConfig.DEFAULT,
                            new QualityConfig.MutationConfig(true, 80, 20, java.util.Map.of()),
                            QualityConfig.ContractConfig.DEFAULT);
            var configWithQuality =
                    new dev.iadev.domain.model.ProjectConfig(
                            baseConfig.core(),
                            baseConfig.tech(),
                            new dev.iadev.domain.model.Governance(
                                    baseConfig.governance().compliance(),
                                    baseConfig.governance().platforms(),
                                    baseConfig.governance().branchingModel(),
                                    baseConfig.governance().telemetryEnabled(),
                                    baseConfig.governance().documentation(),
                                    qualityConfig));
            List<String> skills = SkillsSelection.selectQualitySkills(configWithQuality);
            assertThat(skills).contains("x-test-mutation");
        }

        @Test
        @DisplayName("scenario7_qualityMutationDisabled_doesNotSelectXTestMutation")
        void scenario7_qualityMutationDisabled_doesNotSelectXTestMutation() {
            var config = TestConfigBuilder.builder().build();
            assertThat(config.quality().mutation().enabled()).isFalse();
            List<String> skills = SkillsSelection.selectQualitySkills(config);
            assertThat(skills).doesNotContain("x-test-mutation");
        }

        @Test
        @DisplayName("scenario8_qualityAllDisabled_selectsNoSkills")
        void scenario8_qualityAllDisabled_selectsNoSkills() {
            var config = TestConfigBuilder.builder().build();
            List<String> skills = SkillsSelection.selectQualitySkills(config);
            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("Knowledge Pack playbooks")
    class KnowledgePacks {

        @Test
        @DisplayName("scenario9_javaPlaybook_exists")
        void scenario9_javaPlaybook_exists() {
            assertThat(KP_ROOT.resolve("mutation-java.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario10_jsPlaybook_exists")
        void scenario10_jsPlaybook_exists() {
            assertThat(KP_ROOT.resolve("mutation-js.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario11_pythonPlaybook_exists")
        void scenario11_pythonPlaybook_exists() {
            assertThat(KP_ROOT.resolve("mutation-python.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario12_goPlaybook_exists")
        void scenario12_goPlaybook_exists() {
            assertThat(KP_ROOT.resolve("mutation-go.md").toAbsolutePath()).exists();
        }
    }

    @Nested
    @DisplayName("Template artifacts")
    class Artifacts {

        @Test
        @DisplayName("scenario13_mutationPlanTemplate_exists")
        void scenario13_mutationPlanTemplate_exists() {
            assertThat(TEMPLATES_ROOT.resolve("_TEMPLATE-MUTATION-PLAN.md").toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario14_planTemplateDefinitions_includesMutationPlan")
        void scenario14_planTemplateDefinitions_includesMutationPlan() {
            assertThat(PlanTemplateDefinitions.TEMPLATE_SECTIONS)
                    .as("must register _TEMPLATE-MUTATION-PLAN.md")
                    .containsKey("_TEMPLATE-MUTATION-PLAN.md");
        }

        @Test
        @DisplayName("scenario15_mutationPlanTemplate_containsMandatorySections")
        void scenario15_mutationPlanTemplate_containsMandatorySections() throws Exception {
            String content =
                    Files.readString(
                            TEMPLATES_ROOT.resolve("_TEMPLATE-MUTATION-PLAN.md").toAbsolutePath());
            assertThat(content).as("must have Header section").contains("## Header");
            assertThat(content).as("must have Summary section").contains("## Summary");
            assertThat(content)
                    .as("must have Surviving Mutants section")
                    .contains("Surviving Mutants");
            assertThat(content).as("must have Tooling section").contains("## Tooling");
        }
    }
}
