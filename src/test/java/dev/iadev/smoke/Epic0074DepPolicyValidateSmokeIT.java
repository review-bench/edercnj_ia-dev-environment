package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.PlanTemplateDefinitions;
import dev.iadev.domain.model.BlockAction;
import dev.iadev.domain.model.DependencyPolicyConfig;
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
                    "x-validate-dependency-policy");

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
                    "x-audit-dependencies",
                    "SKILL.md");

    private static final Path TEMPLATES_ROOT =
            Path.of("src", "main", "resources", "shared", "templates");

    // EPIC-0078 rules-consolidation-essentials: Rule 32 merged into 00-essentials.md +
    // knowledge/governance/rules/dependency-policy.md KP.
    private static final Path RULE_32 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "rules",
                    "dependency-policy.md");

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
            assertThat(content).as("must reference allowed-licenses").contains("allowed-licenses");
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
    @DisplayName("x-audit-dependencies --policy flag integration")
    class DepAuditPolicyFlag {

        @Test
        @DisplayName("scenario6_depAuditSkill_containsPolicyFlag")
        void scenario6_depAuditSkill_containsPolicyFlag() throws Exception {
            String content = Files.readString(DEP_AUDIT_SKILL.toAbsolutePath());
            assertThat(content)
                    .as("must document --policy flag in argument-hint")
                    .contains("--policy");
            assertThat(content)
                    .as("must reference x-validate-dependency-policy delegation")
                    .contains("x-validate-dependency-policy");
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
    @DisplayName("CI audit script — audit-dep-policy.sh")
    class AuditScript {

        private static final Path AUDIT_SCRIPT =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "scripts",
                        "audit-dep-policy.sh");

        private static final Path BASELINE =
                Path.of("governance", "baselines", "dep-policy-baseline.txt");

        @Test
        @DisplayName("scenario13_auditScript_exists")
        void scenario13_auditScript_exists() {
            assertThat(AUDIT_SCRIPT.toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario14_auditScript_implementsSelfCheck")
        void scenario14_auditScript_implementsSelfCheck() throws Exception {
            String content = Files.readString(AUDIT_SCRIPT.toAbsolutePath());
            assertThat(content).as("must implement --self-check flag").contains("--self-check");
            assertThat(content)
                    .as("must emit OPERATIONAL_ERROR on missing prerequisite")
                    .contains("OPERATIONAL_ERROR");
        }

        @Test
        @DisplayName("scenario15_auditScript_conformsToRule26ExitCodes")
        void scenario15_auditScript_conformsToRule26ExitCodes() throws Exception {
            String content = Files.readString(AUDIT_SCRIPT.toAbsolutePath());
            assertThat(content).as("must document exit 0 = OK").contains("0=OK");
            assertThat(content)
                    .as("must document exit 1 = DEPENDENCY_POLICY_VIOLATION")
                    .contains("DEPENDENCY_POLICY_VIOLATION");
            assertThat(content)
                    .as("must document exit 2 = OPERATIONAL_ERROR")
                    .contains("2=OPERATIONAL_ERROR");
            assertThat(content)
                    .as("must document exit 3 = BASELINE_CORRUPT")
                    .contains("3=BASELINE_CORRUPT");
        }

        @Test
        @DisplayName("scenario16_baselineFile_exists")
        void scenario16_baselineFile_exists() {
            assertThat(BASELINE.toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario17_auditScript_referencesRule32")
        void scenario17_auditScript_referencesRule32() throws Exception {
            String content = Files.readString(AUDIT_SCRIPT.toAbsolutePath());
            assertThat(content).as("must reference Rule 32").contains("Rule 32");
        }
    }

    @Nested
    @DisplayName("x-implement-story Phase 3 integration (story-0074-0005)")
    class StoryImplementIntegration {

        private static final Path STORY_IMPLEMENT =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "skills",
                        "core",
                        "dev",
                        "x-implement-story",
                        "SKILL.md");

        // EPIC-0078 rules-consolidation-essentials: Rules 24/27 merged into 00-essentials.md +
        // lifecycle KPs. Access content via KP paths.
        private static final Path RULE_24 =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "knowledge",
                        "governance",
                        "rules",
                        "lifecycle-contract.md");

        private static final Path RULE_27 =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "knowledge",
                        "lifecycle",
                        "zero-bypass.md");

        // EPIC-0078 story-0078-0013 slimmed Rules 24/27; full evidence table + 13 surfaces
        // moved to lifecycle KPs execution-integrity.md and zero-bypass.md.
        private static final Path EXECUTION_INTEGRITY_KP =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "knowledge",
                        "lifecycle",
                        "execution-integrity.md");

        private static final Path ZERO_BYPASS_KP =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "knowledge",
                        "lifecycle",
                        "zero-bypass.md");

        @Test
        @DisplayName("scenario18_storyImplement_containsDepPolicyValidateSkillCall")
        void scenario18_storyImplement_containsDepPolicyValidateSkillCall() throws Exception {
            String content = Files.readString(STORY_IMPLEMENT.toAbsolutePath());
            assertThat(content)
                    .as("must invoke x-validate-dependency-policy in Phase 3")
                    .contains("x-validate-dependency-policy");
            assertThat(content)
                    .as("must mark invocation as conditional on dep_policy_enabled")
                    .contains("flag.dep_policy_enabled");
            assertThat(content)
                    .as("must produce dep-policy-validation-report evidence artifact")
                    .contains("dep-policy-validation-report-STORY-ID.md");
        }

        @Test
        @DisplayName("scenario19_storyImplement_documentsDepPolicyBlockExitCode")
        void scenario19_storyImplement_documentsDepPolicyBlockExitCode() throws Exception {
            String content = Files.readString(STORY_IMPLEMENT.toAbsolutePath());
            assertThat(content)
                    .as("must document DEP_POLICY_BLOCK exit code")
                    .contains("DEP_POLICY_BLOCK");
        }

        @Test
        @DisplayName("scenario20_rule24_includesDepPolicyEvidenceArtifact")
        void scenario20_rule24_includesDepPolicyEvidenceArtifact() throws Exception {
            // EPIC-0078 story-0078-0013 slimmed Rule 24: evidence table moved to lifecycle KP.
            // Rule 24 retains a pointer to the KP; the KP holds the full artifact table.
            String rule24Content = Files.readString(RULE_24.toAbsolutePath());
            assertThat(rule24Content)
                    .as("Rule 24 must reference execution-integrity KP (source of evidence table)")
                    .contains("execution-integrity.md");

            String kpContent = Files.readString(EXECUTION_INTEGRITY_KP.toAbsolutePath());
            assertThat(kpContent)
                    .as(
                            "execution-integrity KP must register x-validate-dependency-policy as mandatory evidence artifact")
                    .contains("x-validate-dependency-policy");
            assertThat(kpContent)
                    .as(
                            "execution-integrity KP must reference dep-policy-validation-report artifact path")
                    .contains("dep-policy-validation-report-STORY-ID.md");
        }

        @Test
        @DisplayName("scenario21_rule27_includesSurface13")
        void scenario21_rule27_includesSurface13() throws Exception {
            // EPIC-0078 story-0078-0013 slimmed Rule 27: 13-surfaces table moved to lifecycle KP.
            // Rule 27 retains a pointer "13 surfaces" in its KP reference line.
            String rule27Content = Files.readString(RULE_27.toAbsolutePath());
            assertThat(rule27Content)
                    .as("Rule 27 must enumerate 13 surfaces (pointer to KP)")
                    .contains("13 surfaces");

            String kpContent = Files.readString(ZERO_BYPASS_KP.toAbsolutePath());
            assertThat(kpContent)
                    .as("zero-bypass KP must list dep-policy-validation-report as Surface 13")
                    .contains("dep-policy-validation-report-STORY-ID.md");
        }
    }

    @Nested
    @DisplayName("End-to-end delivery (story-0074-0006)")
    class EndToEnd {

        private static final Path CAPABILITIES_INDEX = Path.of("capabilities", "_index.yaml");

        private static final Path ADR_0027 =
                Path.of("docs", "adr", "ADR-0027-dependency-policy-gate.md");

        private static final Path DEP_POLICY_DECLARATION_TEMPLATE =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "shared",
                        "templates",
                        "_TEMPLATE-DEP-POLICY-DECLARATION.md");

        @Test
        @DisplayName("scenario22_blockAction_hasAllThreeValues")
        void scenario22_blockAction_hasAllThreeValues() {
            assertThat(BlockAction.values())
                    .as("BlockAction must have BLOCK, WARN_ONLY, IGNORE")
                    .containsExactlyInAnyOrder(
                            BlockAction.BLOCK, BlockAction.WARN_ONLY, BlockAction.IGNORE);
        }

        @Test
        @DisplayName("scenario23_blockAction_fromYaml_parsesAnyViolationAsBlock")
        void scenario23_blockAction_fromYaml_parsesAnyViolationAsBlock() {
            assertThat(BlockAction.fromYaml("any-violation"))
                    .as("'any-violation' must map to BLOCK (Rule 32 §block-on)")
                    .isEqualTo(BlockAction.BLOCK);
            assertThat(BlockAction.fromYaml("warn-only"))
                    .as("'warn-only' must map to WARN_ONLY")
                    .isEqualTo(BlockAction.WARN_ONLY);
            assertThat(BlockAction.fromYaml("ignore"))
                    .as("'ignore' must map to IGNORE")
                    .isEqualTo(BlockAction.IGNORE);
        }

        @Test
        @DisplayName("scenario24_capabilitiesIndex_includesDependencyPolicy")
        void scenario24_capabilitiesIndex_includesDependencyPolicy() throws Exception {
            String content = Files.readString(CAPABILITIES_INDEX.toAbsolutePath());
            assertThat(content)
                    .as("capabilities/_index.yaml must list governance.dependency-policy")
                    .contains("governance.dependency-policy");
        }

        @Test
        @DisplayName("scenario25_adr0027_exists")
        void scenario25_adr0027_exists() {
            assertThat(ADR_0027.toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario26_declarationTemplate_hasMandatorySections")
        void scenario26_declarationTemplate_hasMandatorySections() throws Exception {
            String content = Files.readString(DEP_POLICY_DECLARATION_TEMPLATE.toAbsolutePath());
            assertThat(content)
                    .as("declaration template must have Policy Status section")
                    .contains("## Policy Status");
            assertThat(content)
                    .as("declaration template must have Enforcement Matrix (D-R10)")
                    .contains("D-R10");
            assertThat(content)
                    .as("declaration template must have Scope Policy (D-R11)")
                    .contains("D-R11");
            assertThat(content)
                    .as("declaration template must document RULE-074-01 hard-block")
                    .contains("RULE-074-01");
        }

        @Test
        @DisplayName("scenario27_goldenProfiles_includeDependencyPolicyKp")
        void scenario27_goldenProfiles_includeRule32() throws Exception {
            // EPIC-0078 rules-consolidation-essentials: Rule 32 moved from .claude/rules/ to
            // .claude/knowledge/governance/rules/dependency-policy.md KP.
            Path goldenRoot = Path.of("src", "test", "resources", "golden");
            long profilesWithDepPolicy =
                    Files.walk(goldenRoot, 3)
                            .filter(
                                    p ->
                                            p.getFileName().toString().equals("README.md")
                                                    && p.toString().contains(".claude"))
                            .map(p -> p.getParent().resolve("knowledge/governance/rules"))
                            .filter(
                                    kpDir ->
                                            kpDir.resolve("dependency-policy.md").toFile().exists())
                            .count();
            assertThat(profilesWithDepPolicy)
                    .as(
                            "all golden profiles must include dependency-policy.md in .claude/knowledge/governance/rules/")
                    .isGreaterThanOrEqualTo(9L);
        }
    }

    @Nested
    @DisplayName("Template artifacts")
    class Artifacts {

        @Test
        @DisplayName("scenario9_depPolicyReportTemplate_exists")
        void scenario9_depPolicyReportTemplate_exists() {
            assertThat(TEMPLATES_ROOT.resolve("_TEMPLATE-DEP-POLICY-REPORT.md").toAbsolutePath())
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
            assertThat(content).as("must have Policy Snapshot section").contains("Policy Snapshot");
            assertThat(content).as("must have Tooling section").contains("## Tooling");
        }

        @Test
        @DisplayName("scenario12_rule32_exists")
        void scenario12_rule32_exists() {
            assertThat(RULE_32.toAbsolutePath()).exists();
        }
    }
}
