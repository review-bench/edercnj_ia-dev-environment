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

@DisplayName("Epic0072PerformanceSkillSmokeIT — x-test-performance structural invariants")
class Epic0072PerformanceSkillSmokeIT {

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
                    "x-test-performance");

    private static final Path KP_ROOT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "performance-engineering");

    private static final Path TEMPLATES_ROOT =
            Path.of("src", "main", "resources", "shared", "templates");

    private static final Path BASELINES_ROOT = Path.of("governance", "baselines");

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
            assertThat(content).as("must contain Newman for REST").contains("Newman");
            assertThat(content).as("must contain ghz for gRPC").contains("ghz");
            assertThat(content).as("must contain hyperfine for CLI").contains("hyperfine");
            assertThat(content).as("must contain Artillery for GraphQL").contains("Artillery");
        }

        @Test
        @DisplayName("scenario3_skillFile_containsExitCodes")
        void scenario3_skillFile_containsExitCodes() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must document PERF_REGRESSION_DETECTED").contains("PERF_REGRESSION_DETECTED");
            assertThat(content).as("must document TOOL_NOT_FOUND").contains("TOOL_NOT_FOUND");
            assertThat(content).as("must document PERF_DISABLED exit code").contains("PERF_DISABLED");
        }

        @Test
        @DisplayName("scenario4_skillFile_containsBaselineComparison")
        void scenario4_skillFile_containsBaselineComparison() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must reference performance-baseline.json").contains("performance-baseline.json");
            assertThat(content).as("must document baseline-tolerance-pct").contains("baseline-tolerance-pct");
        }

        @Test
        @DisplayName("scenario5_skillFile_requiresCapabilityFrontmatter")
        void scenario5_skillFile_requiresCapabilityFrontmatter() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must declare requires-any").contains("requires-any:");
            assertThat(content).as("must list quality.performance.rest").contains("quality.performance.rest");
            assertThat(content).as("must list quality.performance.grpc").contains("quality.performance.grpc");
        }
    }

    @Nested
    @DisplayName("QualityGate skill selection")
    class GateSelection {

        @Test
        @DisplayName("scenario6_qualityPerformanceEnabled_selectsXTestPerformance")
        void scenario6_qualityPerformanceEnabled_selectsXTestPerformance() {
            // Build a config with quality.performance.enabled=true via Governance override
            var baseConfig = TestConfigBuilder.builder().build();
            var qualityConfig = new QualityConfig(
                    new QualityConfig.PerformanceConfig(true, null, 10, java.util.Map.of()),
                    QualityConfig.MutationConfig.DEFAULT,
                    QualityConfig.ContractConfig.DEFAULT);
            var configWithQuality = new dev.iadev.domain.model.ProjectConfig(
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
            assertThat(skills).contains("x-test-performance");
        }

        @Test
        @DisplayName("scenario7_qualityPerformanceDisabled_doesNotSelectXTestPerformance")
        void scenario7_qualityPerformanceDisabled_doesNotSelectXTestPerformance() {
            var config = TestConfigBuilder.builder().build();
            assertThat(config.quality().performance().enabled()).isFalse();
            List<String> skills = SkillsSelection.selectQualitySkills(config);
            assertThat(skills).doesNotContain("x-test-performance");
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
        @DisplayName("scenario9_restPlaybook_exists")
        void scenario9_restPlaybook_exists() {
            assertThat(KP_ROOT.resolve("performance-rest.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario10_grpcPlaybook_exists")
        void scenario10_grpcPlaybook_exists() {
            assertThat(KP_ROOT.resolve("performance-grpc.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario11_cliPlaybook_exists")
        void scenario11_cliPlaybook_exists() {
            assertThat(KP_ROOT.resolve("performance-cli.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario12_graphqlPlaybook_exists")
        void scenario12_graphqlPlaybook_exists() {
            assertThat(KP_ROOT.resolve("performance-graphql.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario13_socketPlaybook_exists")
        void scenario13_socketPlaybook_exists() {
            assertThat(KP_ROOT.resolve("performance-socket.md").toAbsolutePath()).exists();
        }
    }

    @Nested
    @DisplayName("Template and Baseline artifacts")
    class Artifacts {

        @Test
        @DisplayName("scenario14_performancePlanTemplate_exists")
        void scenario14_performancePlanTemplate_exists() {
            assertThat(TEMPLATES_ROOT.resolve("_TEMPLATE-PERFORMANCE-PLAN.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario15_performanceBaselineJson_exists")
        void scenario15_performanceBaselineJson_exists() {
            assertThat(BASELINES_ROOT.resolve("performance-baseline.json").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario16_planTemplateDefinitions_includesPerformancePlan")
        void scenario16_planTemplateDefinitions_includesPerformancePlan() {
            assertThat(PlanTemplateDefinitions.TEMPLATE_SECTIONS)
                    .as("must register _TEMPLATE-PERFORMANCE-PLAN.md")
                    .containsKey("_TEMPLATE-PERFORMANCE-PLAN.md");
        }

        @Test
        @DisplayName("scenario17_performancePlanTemplate_containsMandatorySections")
        void scenario17_performancePlanTemplate_containsMandatorySections() throws Exception {
            String content =
                    Files.readString(
                            TEMPLATES_ROOT.resolve("_TEMPLATE-PERFORMANCE-PLAN.md").toAbsolutePath());
            assertThat(content).as("must have Header section").contains("## Header");
            assertThat(content).as("must have Summary section").contains("## Summary");
            assertThat(content).as("must have Results per Endpoint section").contains("Results per Endpoint");
            assertThat(content).as("must have Tooling section").contains("## Tooling");
        }
    }
}
