package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0013 (Slim Rules 24/27/29/45 to stubs pointing to lifecycle KPs).
 */
@DisplayName("Epic0078Story0013SmokeIT — Rules 24/27/29/45 slimmed to stubs")
class Epic0078Story0013SmokeIT {

    private static final Path RULE_24 = Path.of(
            "src", "main", "resources", "targets", "claude", "rules", "24-execution-integrity.md");
    private static final Path RULE_27 = Path.of(
            "src", "main", "resources", "targets", "claude", "rules", "27-zero-bypass-lifecycle.md");
    private static final Path RULE_29 = Path.of(
            "src", "main", "resources", "targets", "claude", "rules", "29-refinement-gate.md");
    private static final Path RULE_45 = Path.of(
            "src", "main", "resources", "targets", "claude", "rules", "45-ci-watch-integrity.md");

    @Test
    @DisplayName("scenario1_allFourRulesExistAndAreStubbed")
    void scenario1_allFourRulesExistAndAreStubbed() throws IOException {
        for (Path rule : new Path[]{RULE_24, RULE_27, RULE_29, RULE_45}) {
            assertThat(rule).as("Rule %s must exist", rule.getFileName()).exists();
            long lineCount = Files.lines(rule, StandardCharsets.UTF_8).count();
            assertThat(lineCount)
                    .as("Rule %s must be ≤25 lines (stub) but was %d", rule.getFileName(), lineCount)
                    .isLessThanOrEqualTo(25);
        }
    }

    @Test
    @DisplayName("scenario2_rule24PointsToExecutionIntegrityKp")
    void scenario2_rule24PointsToExecutionIntegrityKp() throws IOException {
        String content = Files.readString(RULE_24, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 24 must reference execution-integrity KP")
                .contains("lifecycle/execution-integrity.md");
        assertThat(content).as("Rule 24 must reference EIE_EVIDENCE_MISSING")
                .contains("EIE_EVIDENCE_MISSING");
    }

    @Test
    @DisplayName("scenario3_rule27PointsToZeroBypassKp")
    void scenario3_rule27PointsToZeroBypassKp() throws IOException {
        String content = Files.readString(RULE_27, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 27 must reference zero-bypass KP")
                .contains("lifecycle/zero-bypass.md");
        assertThat(content).as("Rule 27 must mention 13 surfaces")
                .contains("13 surface");
    }

    @Test
    @DisplayName("scenario4_rule29PointsToRefinementGateKp")
    void scenario4_rule29PointsToRefinementGateKp() throws IOException {
        String content = Files.readString(RULE_29, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 29 must reference refinement-gate KP")
                .contains("lifecycle/refinement-gate.md");
        assertThat(content).as("Rule 29 must reference REFINEMENT_REQUIRED exit code")
                .contains("REFINEMENT_REQUIRED");
    }

    @Test
    @DisplayName("scenario5_rule45PointsToCiWatchIntegrityKp")
    void scenario5_rule45PointsToCiWatchIntegrityKp() throws IOException {
        String content = Files.readString(RULE_45, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 45 must reference ci-watch-integrity KP")
                .contains("lifecycle/ci-watch-integrity.md");
        assertThat(content).as("Rule 45 must enumerate 8 exit codes")
                .contains("PR_ALREADY_MERGED");
    }
}
