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

@DisplayName("Epic0072ContractSkillSmokeIT — x-test-contract structural invariants")
class Epic0072ContractSkillSmokeIT {

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
                    "x-test-contract");

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
            assertThat(content).as("must reference openapi-diff for REST").contains("openapi-diff");
            assertThat(content).as("must reference buf for gRPC/proto3").contains("buf");
            assertThat(content)
                    .as("must reference Spring Cloud Contract")
                    .contains("Spring Cloud Contract");
            assertThat(content)
                    .as("must reference schema registry for events")
                    .contains("schema registry");
        }

        @Test
        @DisplayName("scenario3_skillFile_containsExitCodes")
        void scenario3_skillFile_containsExitCodes() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content)
                    .as("must document CONTRACT_BREAKING_CHANGE")
                    .contains("CONTRACT_BREAKING_CHANGE");
            assertThat(content).as("must document OPERATIONAL_ERROR").contains("OPERATIONAL_ERROR");
            assertThat(content)
                    .as("must document CONTRACT_ARTIFACT_INVALID")
                    .contains("CONTRACT_ARTIFACT_INVALID");
        }

        @Test
        @DisplayName("scenario4_skillFile_containsPactOptIn")
        void scenario4_skillFile_containsPactOptIn() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must reference Pact opt-in").contains("Pact");
            assertThat(content)
                    .as("must reference quality.contract.pact config")
                    .contains("quality.contract.pact");
        }

        @Test
        @DisplayName("scenario5_skillFile_requiresAnyFrontmatter")
        void scenario5_skillFile_requiresAnyFrontmatter() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must declare requires-any").contains("requires-any:");
            assertThat(content)
                    .as("must list quality.contract.openapi-diff")
                    .contains("quality.contract.openapi-diff");
            assertThat(content)
                    .as("must list quality.contract.buf")
                    .contains("quality.contract.buf");
            assertThat(content)
                    .as("must list quality.contract.scc")
                    .contains("quality.contract.scc");
            assertThat(content)
                    .as("must list quality.contract.pact")
                    .contains("quality.contract.pact");
        }
    }

    @Nested
    @DisplayName("QualityGate skill selection")
    class GateSelection {

        @Test
        @DisplayName("scenario6_qualityContractEnabled_selectsXTestContract")
        void scenario6_qualityContractEnabled_selectsXTestContract() {
            var baseConfig = TestConfigBuilder.builder().build();
            var qualityConfig =
                    new QualityConfig(
                            QualityConfig.PerformanceConfig.DEFAULT,
                            QualityConfig.MutationConfig.DEFAULT,
                            new QualityConfig.ContractConfig(true, false, true, true),
                            QualityConfig.RegressionConfig.DEFAULT,
                            QualityConfig.DastConfig.DEFAULT);
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
                                    qualityConfig,
                                    dev.iadev.domain.model.DependencyPolicyConfig.DEFAULT));
            List<String> skills = SkillsSelection.selectQualitySkills(configWithQuality);
            assertThat(skills).contains("x-test-contract");
        }

        @Test
        @DisplayName("scenario7_qualityContractDisabled_doesNotSelectXTestContract")
        void scenario7_qualityContractDisabled_doesNotSelectXTestContract() {
            var config = TestConfigBuilder.builder().build();
            assertThat(config.quality().contract().enabled()).isFalse();
            List<String> skills = SkillsSelection.selectQualitySkills(config);
            assertThat(skills).doesNotContain("x-test-contract");
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
        @DisplayName("scenario9_openapiPlaybook_exists")
        void scenario9_openapiPlaybook_exists() {
            assertThat(KP_ROOT.resolve("contract-openapi.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario10_grpcPlaybook_exists")
        void scenario10_grpcPlaybook_exists() {
            assertThat(KP_ROOT.resolve("contract-grpc.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario11_eventsPlaybook_exists")
        void scenario11_eventsPlaybook_exists() {
            assertThat(KP_ROOT.resolve("contract-events.md").toAbsolutePath()).exists();
        }
    }

    @Nested
    @DisplayName("Template artifacts")
    class Artifacts {

        @Test
        @DisplayName("scenario12_contractPlanTemplate_exists")
        void scenario12_contractPlanTemplate_exists() {
            assertThat(TEMPLATES_ROOT.resolve("_TEMPLATE-CONTRACT-PLAN.md").toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario13_planTemplateDefinitions_includesContractPlan")
        void scenario13_planTemplateDefinitions_includesContractPlan() {
            assertThat(PlanTemplateDefinitions.TEMPLATE_SECTIONS)
                    .as("must register _TEMPLATE-CONTRACT-PLAN.md")
                    .containsKey("_TEMPLATE-CONTRACT-PLAN.md");
        }

        @Test
        @DisplayName("scenario14_contractPlanTemplate_containsMandatorySections")
        void scenario14_contractPlanTemplate_containsMandatorySections() throws Exception {
            String content =
                    Files.readString(
                            TEMPLATES_ROOT.resolve("_TEMPLATE-CONTRACT-PLAN.md").toAbsolutePath());
            assertThat(content).as("must have Header section").contains("## Header");
            assertThat(content).as("must have Summary section").contains("## Summary");
            assertThat(content)
                    .as("must have Changes Detected section")
                    .contains("Changes Detected");
            assertThat(content)
                    .as("must have CHANGELOG Integration section")
                    .contains("CHANGELOG Integration");
            assertThat(content).as("must have Tooling section").contains("## Tooling");
        }
    }
}
