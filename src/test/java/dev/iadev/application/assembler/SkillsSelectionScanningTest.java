package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for SkillsSelection — security scanning skill selection based on ScanningConfig,
 * QualityGateConfig, and pentest flags.
 */
@DisplayName("SkillsSelection — scanning")
class SkillsSelectionScanningTest {

    @Nested
    @DisplayName("selectSecurityScanningSkills")
    class SelectSecurityScanningSkills {

        @Test
        @DisplayName("all flags false returns empty list")
        void select_allFlagsFalse_returnsEmpty() {
            ProjectConfig config = TestConfigBuilder.builder().build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("sast enabled returns x-run-sast")
        void select_sastEnabled_returnsSastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(true, false, false, false, false)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-run-sast");
        }

        @Test
        @DisplayName("dast enabled returns x-run-dast")
        void select_dastEnabled_returnsDastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(false, true, false, false, false)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-run-dast");
        }

        @Test
        @DisplayName("secretScan enabled returns" + " x-scan-secrets")
        void select_secretScanEnabled_returnsSecretScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(false, false, true, false, false)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-scan-secrets");
        }

        @Test
        @DisplayName("containerScan enabled returns" + " x-scan-container-security")
        void select_containerScanEnabled_returnsContScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(false, false, false, true, false)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-scan-container-security");
        }

        @Test
        @DisplayName("infraScan enabled returns" + " x-assess-infrastructure-security")
        void select_infraScanEnabled_returnsInfraScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(false, false, false, false, true)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-assess-infrastructure-security");
        }

        @Test
        @DisplayName(
                "pentest enabled does not add"
                        + " x-run-pentest to scanning skills"
                        + " (delegated to selectPentestSkills)")
        void select_pentestEnabled_excludesPentest() {
            ProjectConfig config = TestConfigBuilder.builder().pentest(true).build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).doesNotContain("x-run-pentest");
        }

        @Test
        @DisplayName("qualityGate sonarqube returns" + " x-run-sonar-security")
        void select_sonarqubeProvider_returnsSonarGate() {
            ProjectConfig config =
                    TestConfigBuilder.builder().qualityGateProvider("sonarqube").build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-run-sonar-security");
        }

        @Test
        @DisplayName("qualityGate sonarcloud returns" + " x-run-sonar-security")
        void select_sonarcloudProvider_returnsSonarGate() {
            ProjectConfig config =
                    TestConfigBuilder.builder().qualityGateProvider("sonarcloud").build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactly("x-run-sonar-security");
        }

        @Test
        @DisplayName("qualityGate none returns empty")
        void select_noneProvider_excludesSonarGate() {
            ProjectConfig config = TestConfigBuilder.builder().qualityGateProvider("none").build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("all flags enabled returns all skills")
        void select_allEnabled_returnsAllSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(true, true, true, true, true)
                            .pentest(true)
                            .qualityGateProvider("sonarqube")
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills)
                    .containsExactlyInAnyOrder(
                            "x-run-sast",
                            "x-run-dast",
                            "x-scan-secrets",
                            "x-scan-container-security",
                            "x-assess-infrastructure-security",
                            "x-run-sonar-security");
        }

        @Test
        @DisplayName("mixed flags returns matching skills")
        void select_mixedFlags_returnsMatchingSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(true, false, true, false, false)
                            .pentest(true)
                            .build();

            List<String> skills = SkillsSelection.selectSecurityScanningSkills(config);

            assertThat(skills).containsExactlyInAnyOrder("x-run-sast", "x-scan-secrets");
        }
    }

    @Nested
    @DisplayName("selectConditionalSkills includes scanning")
    class ConditionalIncludesScanning {

        @Test
        @DisplayName("aggregation includes scanning skills" + " when flags enabled")
        void conditional_scanningEnabled_includesSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .scanningFlags(true, false, false, false, false)
                            .build();

            List<String> skills = SkillsSelection.selectConditionalSkills(config);

            assertThat(skills).contains("x-run-sast");
        }

        @Test
        @DisplayName("aggregation excludes scanning skills" + " when all flags false")
        void conditional_scanningDisabled_excludesSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder().clearInterfaces().addInterface("rest").build();

            List<String> skills = SkillsSelection.selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain(
                            "x-run-sast",
                            "x-run-dast",
                            "x-scan-secrets",
                            "x-scan-container-security",
                            "x-assess-infrastructure-security",
                            "x-run-pentest",
                            "x-run-sonar-security");
        }
    }
}
