package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0013 (Slim Rules 24/27/29/45 to stubs pointing to lifecycle
 * KPs).
 *
 * <p>EPIC-0078 rules-consolidation-essentials: Rules 24/27/29/45 were fully merged into {@code
 * 00-essentials.md} + lifecycle KPs under {@code knowledge/lifecycle/}. This test verifies the
 * lifecycle KPs carry the required content that was previously in those rules.
 */
@DisplayName("Epic0078Story0013SmokeIT — lifecycle KPs carry Rules 24/27/29/45 contract")
class Epic0078Story0013SmokeIT {

    private static final Path LIFECYCLE_DIR =
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle");

    private static final Path EXECUTION_INTEGRITY_KP =
            LIFECYCLE_DIR.resolve("execution-integrity.md");
    private static final Path ZERO_BYPASS_KP = LIFECYCLE_DIR.resolve("zero-bypass.md");
    private static final Path REFINEMENT_GATE_KP = LIFECYCLE_DIR.resolve("refinement-gate.md");
    private static final Path CI_WATCH_KP = LIFECYCLE_DIR.resolve("ci-watch-integrity.md");

    @Test
    @DisplayName("scenario1_allFourLifecycleKpsExist")
    void scenario1_allFourRulesExistAndAreStubbed() throws IOException {
        // EPIC-0078: Rules 24/27/29/45 merged into 00-essentials.md + lifecycle KPs.
        for (Path kp :
                new Path[] {
                    EXECUTION_INTEGRITY_KP, ZERO_BYPASS_KP, REFINEMENT_GATE_KP, CI_WATCH_KP
                }) {
            assertThat(kp).as("Lifecycle KP %s must exist", kp.getFileName()).exists();
        }
    }

    @Test
    @DisplayName("scenario2_executionIntegrityKpPointsToEvidenceTable")
    void scenario2_rule24PointsToExecutionIntegrityKp() throws IOException {
        String content = Files.readString(EXECUTION_INTEGRITY_KP, StandardCharsets.UTF_8);
        assertThat(content)
                .as("execution-integrity KP must have Mandatory Evidence Artifacts table")
                .contains("Mandatory Evidence Artifacts");
        assertThat(content)
                .as("execution-integrity KP must reference EIE_EVIDENCE_MISSING")
                .contains("EIE_EVIDENCE_MISSING");
    }

    @Test
    @DisplayName("scenario3_zeroBypassKpHas13Surfaces")
    void scenario3_rule27PointsToZeroBypassKp() throws IOException {
        String content = Files.readString(ZERO_BYPASS_KP, StandardCharsets.UTF_8);
        assertThat(content).as("zero-bypass KP must reference 13 surfaces").contains("13 surface");
    }

    @Test
    @DisplayName("scenario4_refinementGateKpHasRefinementRequired")
    void scenario4_rule29PointsToRefinementGateKp() throws IOException {
        String content = Files.readString(REFINEMENT_GATE_KP, StandardCharsets.UTF_8);
        assertThat(content)
                .as("refinement-gate KP must reference REFINEMENT_REQUIRED exit code")
                .contains("REFINEMENT_REQUIRED");
    }

    @Test
    @DisplayName("scenario5_ciWatchKpHasExitCodes")
    void scenario5_rule45PointsToCiWatchIntegrityKp() throws IOException {
        String content = Files.readString(CI_WATCH_KP, StandardCharsets.UTF_8);
        assertThat(content)
                .as("ci-watch-integrity KP must list PR_ALREADY_MERGED exit code")
                .contains("PR_ALREADY_MERGED");
    }
}
