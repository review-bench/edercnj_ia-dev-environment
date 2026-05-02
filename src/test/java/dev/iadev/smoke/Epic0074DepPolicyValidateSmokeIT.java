package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.PlanTemplateDefinitions;
import dev.iadev.domain.model.DependencyPolicyConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0074DepPolicyValidateSmokeIT — x-dep-policy-validate structural invariants")
class Epic0074DepPolicyValidateSmokeIT {

    private static final Path SKILL_ROOT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "security",
                    "x-dep-policy-validate");

    private static final Path DEP_AUDIT_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "security",
                    "x-dependency-audit",
                    "SKILL.md");

    private static final Path TEMPLATES_ROOT =
            Path.of("src", "main", "resources", "shared", "templates");

    private static final Path RULE_32 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "rules",
                    "32-dependency-policy-gate.md");

    @Nested
    @DisplayName("SKILL.md — file structure")
    class SkillStructure {

        @Test
        @DisplayName("scenario1_skillFile_exists")
        void scenario1_skillFile_exists() {
            assertThat(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario2_skillFile_containsExitCodes")
        void scenario2_skillFile_containsExitCodes() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must document DEP_POLICY_BLOCK").contains("DEP_POLICY_BLOCK");
            assertThat(content).as("must document DEP_POLICY_WARN").contains("DEP_POLICY_WARN");
            assertThat(content)
                    .as("must document DEP_POLICY_DISABLED")
                    .contains("DEP_POLICY_DISABLED");
        }

        @Test
        @DisplayName("scenario3_skillFile_containsEnforcementDimensions")
        void scenario3_skillFile_containsEnforcementDimensions() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content).as("must reference denied-cves").contains("denied-cves");
            assertThat(content)
                    .as("must reference allowed-licenses")
                    .contains("allowed-licenses");
            assertThat(content).as("must reference min-versions").contains("min-versions");
            assertThat(content).as("must reference max-versions").contains("max-versions");
            assertThat(content)
                    .as("must reference freshness-window-days")
                    .contains("freshness-window-days");
        }

        @Test
        @DisplayName("scenario4_skillFile_containsRULE074_01")
        void scenario4_skillFile_containsRULE074_01() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content)
                    .as("must document RULE-074-01 denied-cve hard-block")
                    .contains("RULE-074-01");
            assertThat(content)
                    .as("must state denied CVEs bypass scope-policy")
                    .contains("Hard-block");
        }

        @Test
        @DisplayName("scenario5_skillFile_requiresCapabilityFrontmatter")
        void scenario5_skillFile_requiresCapabilityFrontmatter() throws Exception {
            String content = Files.readString(SKILL_ROOT.resolve("SKILL.md").toAbsolutePath());
            assertThat(content)
                    .as("must declare requires-capabilities")
                    .contains("requires-capabilities:");
            assertThat(content)
                    .as("must require governance.dependency-policy capability")
                    .contains("governance.dependency-policy");
        }
    }

    @Nested
    @DisplayName("x-dependency-audit --policy flag integration")
    class DepAuditPolicyFlag {

        @Test
        @DisplayName("scenario6_depAuditSkill_containsPolicyFlag")
        void scenario6_depAuditSkill_containsPolicyFlag() throws Exception {
            String content = Files.readString(DEP_AUDIT_SKILL.toAbsolutePath());
            assertThat(content)
                    .as("must document --policy flag in argument-hint")
                    .contains("--policy");
            assertThat(content)
                    .as("must reference x-dep-policy-validate delegation")
                    .contains("x-dep-policy-validate");
        }
    }

    @Nested
    @DisplayName("Domain model — DependencyPolicyConfig defaults")
    class DomainModel {

        @Test
        @DisplayName("scenario7_defaultConfig_policyDisabled")
        void scenario7_defaultConfig_policyDisabled() {
            var config = TestConfigBuilder.builder().build();
            assertThat(config.dependencyPolicy().enabled()).isFalse();
        }

        @Test
        @DisplayName("scenario8_projectConfig_exposesDependencyPolicy")
        void scenario8_projectConfig_exposesDependencyPolicy() {
            var config = TestConfigBuilder.builder().build();
            assertThat(config.dependencyPolicy()).isEqualTo(DependencyPolicyConfig.DEFAULT);
        }
    }

    @Nested
    @DisplayName("Template artifacts")
    class Artifacts {

        @Test
        @DisplayName("scenario9_depPolicyReportTemplate_exists")
        void scenario9_depPolicyReportTemplate_exists() {
            assertThat(
                            TEMPLATES_ROOT
                                    .resolve("_TEMPLATE-DEP-POLICY-REPORT.md")
                                    .toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario10_planTemplateDefinitions_includesDepPolicyReport")
        void scenario10_planTemplateDefinitions_includesDepPolicyReport() {
            assertThat(PlanTemplateDefinitions.TEMPLATE_SECTIONS)
                    .as("must register _TEMPLATE-DEP-POLICY-REPORT.md")
                    .containsKey("_TEMPLATE-DEP-POLICY-REPORT.md");
        }

        @Test
        @DisplayName("scenario11_depPolicyReportTemplate_containsMandatorySections")
        void scenario11_depPolicyReportTemplate_containsMandatorySections() throws Exception {
            String content =
                    Files.readString(
                            TEMPLATES_ROOT
                                    .resolve("_TEMPLATE-DEP-POLICY-REPORT.md")
                                    .toAbsolutePath());
            assertThat(content).as("must have Header section").contains("## Header");
            assertThat(content).as("must have Summary section").contains("## Summary");
            assertThat(content)
                    .as("must have Blocking Violations section")
                    .contains("Blocking Violations");
            assertThat(content)
                    .as("must have Warning Violations section")
                    .contains("Warning Violations");
            assertThat(content)
                    .as("must have Policy Snapshot section")
                    .contains("Policy Snapshot");
            assertThat(content).as("must have Tooling section").contains("## Tooling");
        }

        @Test
        @DisplayName("scenario12_rule32_exists")
        void scenario12_rule32_exists() {
            assertThat(RULE_32.toAbsolutePath()).exists();
        }
    }
}
