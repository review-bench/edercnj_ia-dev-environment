package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName(
        "Epic0072Phase3IntegratedSmokeIT — x-implement-story Phase 3 quality gates structural"
                + " invariants")
class Epic0072Phase3IntegratedSmokeIT {

    private static final Path SKILL_MD =
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

    private static final Path RULE_24 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "rules",
                    "24-execution-integrity.md");

    // EPIC-0078 story-0078-0013 slimmed Rule 24; evidence table moved to lifecycle KP.
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

    private static final Path AUDIT_EI =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-execution-integrity.sh");

    @Nested
    @DisplayName("Happy path — 3 quality gates present in Phase 3")
    class HappyPath {

        @Test
        @DisplayName("scenario1_skillMd_containsAllThreeQualityGateInvocations")
        void scenario1_skillMd_containsAllThreeQualityGateInvocations() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content)
                    .as("SKILL.md must invoke x-execute-performance-tests in Phase 3")
                    .contains("x-execute-performance-tests");
            assertThat(content)
                    .as("SKILL.md must invoke x-execute-mutation-tests in Phase 3")
                    .contains("x-execute-mutation-tests");
            assertThat(content)
                    .as("SKILL.md must invoke x-execute-contract-tests in Phase 3")
                    .contains("x-execute-contract-tests");
        }

        @Test
        @DisplayName("scenario2_skillMd_qualityGatesHaveConditionalMarkers")
        void scenario2_skillMd_qualityGatesHaveConditionalMarkers() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content)
                    .as("perf gate must have conditional Rule 28 marker")
                    .contains("flag.quality_performance_enabled");
            assertThat(content)
                    .as("mutation gate must have conditional Rule 28 marker")
                    .contains("flag.quality_mutation_enabled");
            assertThat(content)
                    .as("contract gate must have conditional Rule 28 marker")
                    .contains("flag.quality_contract_enabled");
        }
    }

    @Nested
    @DisplayName("Fast-fail sequence — D-R11 perf → mutation → contract")
    class FastFailSequence {

        @Test
        @DisplayName("scenario3_skillMd_fastFailSequenceDocumented")
        void scenario3_skillMd_fastFailSequenceDocumented() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content).as("SKILL.md must document D-R11 fast-fail").contains("D-R11");
            assertThat(content)
                    .as("SKILL.md must reference PERF_REGRESSION_DETECTED abort code")
                    .contains("PERF_REGRESSION_DETECTED");
            assertThat(content)
                    .as("SKILL.md must reference MUTATION_SCORE_BELOW_THRESHOLD abort code")
                    .contains("MUTATION_SCORE_BELOW_THRESHOLD");
            assertThat(content)
                    .as("SKILL.md must reference CONTRACT_BREAKING_CHANGE abort code")
                    .contains("CONTRACT_BREAKING_CHANGE");
        }

        @Test
        @DisplayName("scenario4_skillMd_abortCodesInErrorEnvelope")
        void scenario4_skillMd_abortCodesInErrorEnvelope() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content)
                    .as("Error Envelope must list exit 14 PERF_REGRESSION_DETECTED")
                    .contains("exit 14");
            assertThat(content)
                    .as("Error Envelope must list exit 17 MUTATION_SCORE_BELOW_THRESHOLD")
                    .contains("exit 17");
            assertThat(content)
                    .as("Error Envelope must list exit 18 CONTRACT_BREAKING_CHANGE")
                    .contains("exit 18");
        }
    }

    @Nested
    @DisplayName("Telemetry markers — Phase-3-Quality sub-phases")
    class TelemetryMarkers {

        @Test
        @DisplayName("scenario5_skillMd_containsTelemetryMarkersForAllThreeQualityPhases")
        void scenario5_skillMd_containsTelemetryMarkersForAllThreeQualityPhases() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content)
                    .as("must emit Phase-3-Quality-Perf telemetry marker")
                    .contains("Phase-3-Quality-Perf");
            assertThat(content)
                    .as("must emit Phase-3-Quality-Mutation telemetry marker")
                    .contains("Phase-3-Quality-Mutation");
            assertThat(content)
                    .as("must emit Phase-3-Quality-Contract telemetry marker")
                    .contains("Phase-3-Quality-Contract");
        }

        @Test
        @DisplayName("scenario6_skillMd_telemetryMarkersAreBalancedStartEnd")
        void scenario6_skillMd_telemetryMarkersAreBalancedStartEnd() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            long perfStarts =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh start")
                                                    && l.contains("Phase-3-Quality-Perf"))
                            .count();
            long perfEnds =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh end")
                                                    && l.contains("Phase-3-Quality-Perf"))
                            .count();
            assertThat(perfStarts)
                    .as("Phase-3-Quality-Perf must have exactly 1 start marker")
                    .isEqualTo(1);
            assertThat(perfEnds)
                    .as("Phase-3-Quality-Perf must have exactly 1 end marker")
                    .isEqualTo(1);

            long mutationStarts =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh start")
                                                    && l.contains("Phase-3-Quality-Mutation"))
                            .count();
            long mutationEnds =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh end")
                                                    && l.contains("Phase-3-Quality-Mutation"))
                            .count();
            assertThat(mutationStarts)
                    .as("Phase-3-Quality-Mutation must have exactly 1 start marker")
                    .isEqualTo(1);
            assertThat(mutationEnds)
                    .as("Phase-3-Quality-Mutation must have exactly 1 end marker")
                    .isEqualTo(1);

            long contractStarts =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh start")
                                                    && l.contains("Phase-3-Quality-Contract"))
                            .count();
            long contractEnds =
                    content.lines()
                            .filter(
                                    l ->
                                            l.contains("telemetry-phase.sh end")
                                                    && l.contains("Phase-3-Quality-Contract"))
                            .count();
            assertThat(contractStarts)
                    .as("Phase-3-Quality-Contract must have exactly 1 start marker")
                    .isEqualTo(1);
            assertThat(contractEnds)
                    .as("Phase-3-Quality-Contract must have exactly 1 end marker")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Rule 24 §Mandatory Evidence Artifacts — 3 conditional entries")
    class Rule24Artifacts {

        @Test
        @DisplayName("scenario7_rule24_containsThreeConditionalQualityArtifacts")
        void scenario7_rule24_containsThreeConditionalQualityArtifacts() throws Exception {
            // EPIC-0078 story-0078-0013 slimmed Rule 24: evidence table moved to lifecycle KP.
            String kpContent = Files.readString(EXECUTION_INTEGRITY_KP.toAbsolutePath());
            assertThat(kpContent)
                    .as(
                            "execution-integrity KP must list perf-report-STORY-ID.md as conditional artifact")
                    .contains("perf-report-STORY-ID.md");
            assertThat(kpContent)
                    .as(
                            "execution-integrity KP must list mutation-report-STORY-ID.md as conditional artifact")
                    .contains("mutation-report-STORY-ID.md");
            assertThat(kpContent)
                    .as(
                            "execution-integrity KP must list contract-report-STORY-ID.md as conditional artifact")
                    .contains("contract-report-STORY-ID.md");
        }

        @Test
        @DisplayName("scenario8_rule24_qualityArtifactsMarkedAsConditional")
        void scenario8_rule24_qualityArtifactsMarkedAsConditional() throws Exception {
            // EPIC-0078 story-0078-0013 slimmed Rule 24: evidence table moved to lifecycle KP.
            String kpContent = Files.readString(EXECUTION_INTEGRITY_KP.toAbsolutePath());
            assertThat(kpContent)
                    .as("perf artifact must be conditional on quality.performance.enabled")
                    .contains("quality.performance.enabled=true");
            assertThat(kpContent)
                    .as("mutation artifact must be conditional on quality.mutation.enabled")
                    .contains("quality.mutation.enabled=true");
            assertThat(kpContent)
                    .as("contract artifact must be conditional on quality.contract.enabled")
                    .contains("quality.contract.enabled=true");
        }
    }

    @Nested
    @DisplayName("audit-execution-integrity.sh — conditional quality artifact checks")
    class AuditScript {

        @Test
        @DisplayName("scenario9_auditScript_containsQualityGateConditionalChecks")
        void scenario9_auditScript_containsQualityGateConditionalChecks() throws Exception {
            String content = Files.readString(AUDIT_EI.toAbsolutePath());
            assertThat(content)
                    .as("audit script must check QUALITY_PERFORMANCE_ENABLED env var")
                    .contains("QUALITY_PERFORMANCE_ENABLED");
            assertThat(content)
                    .as("audit script must check QUALITY_MUTATION_ENABLED env var")
                    .contains("QUALITY_MUTATION_ENABLED");
            assertThat(content)
                    .as("audit script must check QUALITY_CONTRACT_ENABLED env var")
                    .contains("QUALITY_CONTRACT_ENABLED");
        }

        @Test
        @DisplayName("scenario10_auditScript_qualityChecksDefaultToDisabled")
        void scenario10_auditScript_qualityChecksDefaultToDisabled() throws Exception {
            String content = Files.readString(AUDIT_EI.toAbsolutePath());
            assertThat(content)
                    .as("quality checks must default to disabled for backward compatibility")
                    .contains("QUALITY_PERFORMANCE_ENABLED:-false");
            assertThat(content)
                    .as("quality checks must default to disabled for backward compatibility")
                    .contains("QUALITY_MUTATION_ENABLED:-false");
            assertThat(content)
                    .as("quality checks must default to disabled for backward compatibility")
                    .contains("QUALITY_CONTRACT_ENABLED:-false");
        }
    }

    @Nested
    @DisplayName("Recovery section — quality gate abort codes")
    class RecoverySection {

        @Test
        @DisplayName("scenario11_skillMd_recoverySectionContainsSkipQuality")
        void scenario11_skillMd_recoverySectionContainsSkipQuality() throws Exception {
            String content = Files.readString(SKILL_MD.toAbsolutePath());
            assertThat(content)
                    .as("Recovery section must document --skip-quality flag")
                    .contains("--skip-quality");
        }
    }
}
