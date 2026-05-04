package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for story-0022-0014: Enhanced Supply Chain Audit (x-audit-supply-chain) skill generation.
 *
 * <p>Validates that the x-audit-supply-chain SKILL.md is generated as a core skill, contains all 6
 * advanced capabilities, risk scoring formula, SARIF output format, and does not duplicate
 * x-audit-dependencies content.
 */
@DisplayName("Supply Chain Audit Skill (x-audit-supply-chain)")
class SupplyChainAuditSkillTest {

    @Nested
    @DisplayName("Core Skill Generation")
    class CoreSkillGeneration {

        @Test
        @DisplayName("x-audit-supply-chain SKILL.md exists" + " after assembly")
        void assemble_minimal_generatesSkillMd(@TempDir Path tempDir) throws IOException {
            generateOutput(tempDir);
            Path skillMd = tempDir.resolve("output/skills/x-audit-supply-chain" + "/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("SKILL.md contains correct frontmatter" + " name")
        void assemble_minimal_hasFrontmatterName(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("name: x-audit-supply-chain");
        }

        @Test
        @DisplayName("SKILL.md contains allowed-tools")
        void assemble_minimal_hasAllowedTools(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("allowed-tools:");
        }

        @Test
        @DisplayName("SKILL.md contains Purpose section")
        void assemble_minimal_hasPurposeSection(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("## Purpose").contains("supply chain security");
        }
    }

    @Nested
    @DisplayName("Six Advanced Capabilities")
    class AdvancedCapabilities {

        @Test
        @DisplayName("contains maintainer risk analysis")
        void assemble_minimal_hasMaintainerRisk(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("Maintainer Risk Analysis").contains("bus factor");
        }

        @Test
        @DisplayName("contains typosquatting detection")
        void assemble_minimal_hasTyposquatting(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("Typosquatting Detection").contains("Levenshtein");
        }

        @Test
        @DisplayName("contains phantom dependency detection")
        void assemble_minimal_hasPhantomDeps(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("Phantom Dependency Detection").contains("AST");
        }

        @Test
        @DisplayName("contains dependency age analysis")
        void assemble_minimal_hasDependencyAge(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("Dependency Age Analysis").contains("last release");
        }

        @Test
        @DisplayName("contains EPSS scoring")
        void assemble_minimal_hasEpssScoring(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("EPSS Scoring").contains("FIRST.org");
        }

        @Test
        @DisplayName("contains SLSA assessment")
        void assemble_minimal_hasSlsaAssessment(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("SLSA Assessment").contains("SLSA 0").contains("SLSA 3");
        }
    }

    @Nested
    @DisplayName("Risk Scoring Formula")
    class RiskScoringFormula {

        @Test
        @DisplayName("contains weighted risk score formula")
        void assemble_minimal_hasRiskFormula(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("cve_severity * 0.40")
                    .contains("depth_score * 0.20")
                    .contains("maintainer_risk * 0.15")
                    .contains("license_risk * 0.15")
                    .contains("popularity_inverse * 0.10");
        }

        @Test
        @DisplayName("contains severity classification" + " thresholds")
        void assemble_minimal_hasSeverityClassification(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("CRITICAL")
                    .contains("HIGH")
                    .contains("MEDIUM")
                    .contains("LOW")
                    .contains("INFO");
        }

        @Test
        @DisplayName("contains grade scale A through F")
        void assemble_minimal_hasGradeScale(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("| A |")
                    .contains("| B |")
                    .contains("| C |")
                    .contains("| D |")
                    .contains("| F |");
        }
    }

    @Nested
    @DisplayName("CLI Parameters")
    class CliParameters {

        @Test
        @DisplayName("contains --depth parameter")
        void assemble_minimal_hasDepthParam(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("--depth").contains("shallow").contains("deep");
        }

        @Test
        @DisplayName("contains --include-dev-deps" + " parameter")
        void assemble_minimal_hasIncludeDevDeps(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("--include-dev-deps");
        }

        @Test
        @DisplayName("contains --risk-threshold parameter")
        void assemble_minimal_hasRiskThreshold(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("--risk-threshold");
        }

        @Test
        @DisplayName("contains --focus parameter with all" + " categories")
        void assemble_minimal_hasFocusParam(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("--focus")
                    .contains("maintainer")
                    .contains("typosquatting")
                    .contains("phantom")
                    .contains("age")
                    .contains("epss")
                    .contains("slsa");
        }
    }

    @Nested
    @DisplayName("SARIF Output Format")
    class SarifOutput {

        @Test
        @DisplayName("references SARIF 2.1.0")
        void assemble_minimal_referencesSarif(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("SARIF 2.1.0");
        }

        @Test
        @DisplayName("contains SARIF rule IDs")
        void assemble_minimal_hasSarifRuleIds(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("SCA-MAINT-001")
                    .contains("SCA-TYPO-001")
                    .contains("SCA-PHANTOM-001")
                    .contains("SCA-AGE-001")
                    .contains("SCA-EPSS-001")
                    .contains("SCA-SLSA-001");
        }

        @Test
        @DisplayName("references sarif-template.md" + " knowledge pack")
        void assemble_minimal_referencesSarifTemplate(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("sarif-template.md");
        }
    }

    @Nested
    @DisplayName("Relationship with x-audit-dependencies")
    class DependencyAuditRelation {

        @Test
        @DisplayName("documents relationship with" + " x-audit-dependencies")
        void assemble_minimal_hasRelationTable(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("x-audit-dependencies").contains("x-audit-supply-chain");
        }

        @Test
        @DisplayName("states it complements not replaces" + " x-audit-dependencies")
        void assemble_minimal_complementsNotReplaces(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("complements").contains("does NOT replace");
        }

        @Test
        @DisplayName("x-audit-dependencies skill remains" + " unchanged")
        void assemble_minimal_depAuditUnchanged(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler = new SkillsAssembler();
            assembler.assemble(TestConfigBuilder.minimal(), new TemplateEngine(), outputDir);
            String depAudit =
                    Files.readString(
                            outputDir.resolve("skills/x-audit-dependencies" + "/SKILL.md"),
                            StandardCharsets.UTF_8);
            assertThat(depAudit)
                    .contains("name: x-audit-dependencies")
                    .contains("## Workflow")
                    .contains("DETECT")
                    .contains("AUDIT")
                    .contains("## Error Handling");
        }
    }

    @Nested
    @DisplayName("Template Variable Preservation")
    class TemplateVariables {

        @Test
        @DisplayName("preserves PROJECT_NAME placeholder" + " for runtime substitution")
        void assemble_minimal_preservesProjectName(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("{{PROJECT_NAME}}");
        }

        @Test
        @DisplayName("preserves BUILD_TOOL placeholder" + " for runtime substitution")
        void assemble_minimal_preservesBuildTool(@TempDir Path tempDir) throws IOException {
            String content = generateContent(tempDir);
            assertThat(content).contains("{{BUILD_TOOL}}");
        }
    }

    private String generateContent(Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(TestConfigBuilder.minimal(), new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve("skills/x-audit-supply-chain" + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private void generateOutput(Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(TestConfigBuilder.minimal(), new TemplateEngine(), outputDir);
    }
}
