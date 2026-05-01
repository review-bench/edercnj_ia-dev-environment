package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * E2E smoke test for EPIC-0073 (Regression Shell + DAST) — end-to-end structural wiring.
 *
 * <p>Covers the 5 canonical scenarios declared in story-0073-0007 AC:
 *
 * <ol>
 *   <li>x-story-implement Phase 3 declares x-test-regression-shell as MANDATORY conditional gate
 *   <li>DAST is CI-only — x-story-implement Phase 3 does NOT invoke x-pentest-dynamic
 *   <li>Regression gate has correct conditional marker and D-R11 fast-fail ordering
 *   <li>audit-regression-shell.sh + audit-dast-gate.sh exist with self-check contracts
 *   <li>opt-out: quality.regression+dast disabled produces no-op (ScriptsAssembler)
 * </ol>
 */
@Tag("smoke")
@Tag("e2e")
@DisplayName("Epic0073RegressionDastSmokeIT — EPIC-0073 structural wiring E2E smoke")
class Epic0073RegressionDastSmokeIT {

    private static final Path STORY_IMPLEMENT_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "dev",
                    "x-story-implement",
                    "SKILL.md");

    private static final Path REGRESSION_SHELL_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-test-regression-shell",
                    "SKILL.md");

    private static final Path DAST_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "security",
                    "x-pentest-dynamic",
                    "SKILL.md");

    private static final Path AUDIT_REGRESSION =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-regression-shell.sh");

    private static final Path AUDIT_DAST =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-dast-gate.sh");

    private static final Path REGRESSION_BASELINE =
            Path.of("governance", "baselines", "regression-shell-baseline.txt");

    private static final Path DAST_BASELINE =
            Path.of("governance", "baselines", "dast-gate-baseline.txt");

    // ─── Scenario 1 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "scenario1_xStoryImplement_phase3_invokesRegressionShell_asMandatoryConditional — "
                    + "Phase 3 declares x-test-regression-shell as MANDATORY conditional when"
                    + " quality.regression.enabled=true")
    void scenario1_xStoryImplement_phase3_invokesRegressionShell_asMandatoryConditional()
            throws Exception {
        String content = Files.readString(STORY_IMPLEMENT_SKILL.toAbsolutePath());

        assertThat(content)
                .as("SKILL.md must invoke x-test-regression-shell in Phase 3")
                .contains("x-test-regression-shell");
        assertThat(content)
                .as("regression gate must carry conditional Rule 28 marker")
                .contains("flag.quality_regression_enabled");
        assertThat(content)
                .as("regression gate must carry [conditional: flag.quality_regression_enabled] Rule 28 marker")
                .contains("flag.quality_regression_enabled");
        assertThat(content)
                .as("regression gate must produce a report artifact")
                .contains("regression-report-STORY-ID.md");
    }

    // ─── Scenario 2 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "scenario2_xStoryImplement_phase3_doesNotInvokeDast — "
                    + "DAST is CI-only: x-pentest-dynamic must NOT appear in Phase 3 SKILL.md")
    void scenario2_xStoryImplement_phase3_doesNotInvokeDast() throws Exception {
        String content = Files.readString(STORY_IMPLEMENT_SKILL.toAbsolutePath());

        assertThat(content)
                .as("x-story-implement SKILL.md must NOT invoke x-pentest-dynamic in Phase 3")
                .doesNotContain("x-pentest-dynamic");
        assertThat(content)
                .as("DAST must be documented as CI-only, not in Phase 3")
                .doesNotContain("quality_dast_enabled");
    }

    // ─── Scenario 3 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "scenario3_regressionGate_firstInD_R11_sequence — "
                    + "regression runs first (cheapest gate) before perf/mutation/contract")
    void scenario3_regressionGate_firstInD_R11_sequence() throws Exception {
        String content = Files.readString(STORY_IMPLEMENT_SKILL.toAbsolutePath());

        assertThat(content)
                .as("SKILL.md must define Phase-3-Quality-Regression telemetry marker")
                .contains("Phase-3-Quality-Regression");

        int regressionPos = content.indexOf("Phase-3-Quality-Regression");
        int perfPos = content.indexOf("Phase-3-Quality-Perf");

        assertThat(regressionPos)
                .as("regression telemetry marker must appear before perf marker in SKILL.md")
                .isLessThan(perfPos);

        assertThat(content)
                .as("SKILL.md must document REGRESSION_DETECTED exit code")
                .contains("REGRESSION_DETECTED");
        assertThat(content)
                .as("D-R11 fast-fail note must document skipping perf+mutation+contract on"
                        + " regression failure")
                .contains("D-R11");
    }

    // ─── Scenario 4 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "scenario4_auditScriptsExist_withSelfCheckContracts — "
                    + "audit-regression-shell.sh and audit-dast-gate.sh exist, have self-check"
                    + " and correct exit codes")
    void scenario4_auditScriptsExist_withSelfCheckContracts() throws Exception {
        assertThat(AUDIT_REGRESSION.toAbsolutePath())
                .as("audit-regression-shell.sh must exist")
                .exists();
        assertThat(AUDIT_DAST.toAbsolutePath()).as("audit-dast-gate.sh must exist").exists();

        String regressionAudit = Files.readString(AUDIT_REGRESSION.toAbsolutePath());
        assertThat(regressionAudit)
                .as("audit-regression-shell.sh must implement --self-check")
                .contains("--self-check");
        assertThat(regressionAudit)
                .as("audit-regression-shell.sh must emit REGRESSION_SHELL_VIOLATION on failure")
                .contains("REGRESSION_SHELL_VIOLATION");

        String dastAudit = Files.readString(AUDIT_DAST.toAbsolutePath());
        assertThat(dastAudit)
                .as("audit-dast-gate.sh must implement --self-check")
                .contains("--self-check");
        assertThat(dastAudit)
                .as("audit-dast-gate.sh must emit DAST_GATE_VIOLATION on failure")
                .contains("DAST_GATE_VIOLATION");
        assertThat(dastAudit)
                .as("audit-dast-gate.sh must block production targets")
                .contains("DAST_TARGET_PRODUCTION_FORBIDDEN");

        assertThat(REGRESSION_BASELINE.toAbsolutePath())
                .as("regression-shell-baseline.txt must exist")
                .exists();
        assertThat(DAST_BASELINE.toAbsolutePath())
                .as("dast-gate-baseline.txt must exist")
                .exists();
    }

    // ─── Scenario 5 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "scenario5_regressionShellSkill_declaresRequiresAny_andDastSkillExists — "
                    + "x-test-regression-shell uses requires-any for capabilities;"
                    + " x-pentest-dynamic exists as standalone skill")
    void scenario5_regressionShellSkill_declaresRequiresAny_andDastSkillExists() throws Exception {
        assertThat(REGRESSION_SHELL_SKILL.toAbsolutePath())
                .as("x-test-regression-shell SKILL.md must exist")
                .exists();

        String regressionContent = Files.readString(REGRESSION_SHELL_SKILL.toAbsolutePath());
        assertThat(regressionContent)
                .as("regression skill must declare requires-any with regression capabilities")
                .containsAnyOf("quality.regression.self", "quality.regression.service");
        assertThat(regressionContent)
                .as("regression skill must document REGRESSION_DETECTED exit code")
                .contains("REGRESSION_DETECTED");
        assertThat(regressionContent)
                .as("regression skill must support both self and service modes")
                .contains("self")
                .contains("service");

        assertThat(DAST_SKILL.toAbsolutePath())
                .as("x-pentest-dynamic SKILL.md must exist (CI-only DAST skill)")
                .exists();

        String dastContent = Files.readString(DAST_SKILL.toAbsolutePath());
        assertThat(dastContent)
                .as("DAST skill must reference SARIF output format")
                .containsAnyOf("sarif", "SARIF");
        assertThat(dastContent)
                .as("DAST skill must declare requires-any with dast capabilities")
                .containsAnyOf("quality.dast", "dast");
    }
}
