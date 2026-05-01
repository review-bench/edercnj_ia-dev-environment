package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E2E smoke tests for EPIC-0071 (Documentation as DoD).
 *
 * <p>Validates the 5 canonical scenarios introduced by stories 0071-0001 through 0071-0007:
 *
 * <ol>
 *   <li>PR without README update: audit-doc-freshness.sh detects DOC_FRESHNESS_VIOLATION
 *   <li>PR with stale OpenAPI: audit-doc-freshness.sh detects DOC_FRESHNESS_VIOLATION
 *   <li>Correctly documented PR: x-doc-validate reports PASS (exit 0)
 *   <li>x-story-implement Phase 3 blocked if x-doc-validate fails (DOC_VALIDATION_FAILED)
 *   <li>Legitimate opt-out via Recovery block is honoured (--skip-doc in Recovery only)
 * </ol>
 *
 * <p>Plus security scenario: audit-exempt without reason → exit 3 INVALID_EXEMPTION.
 */
@DisplayName("Epic0071DocAsDoDSmokeIT — Documentation as DoD structural invariants")
class Epic0071DocAsDoDSmokeIT {

    private static final Path SKILLS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "skills", "core");

    private static final Path SCRIPTS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "scripts");

    private static final Path RULES_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "rules");

    private static final Path BASELINES_ROOT = Path.of("governance", "baselines");

    private static final Path HOOKS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "hooks");

    // ─── Scenario 1: PR without README update → DOC_FRESHNESS_VIOLATION ────────

    @Test
    @DisplayName("scenario1_auditDocFreshness_detectsReadmeStaleness")
    void scenario1_auditDocFreshness_detectsReadmeStaleness() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("audit-doc-freshness.sh").toAbsolutePath();

        assertThat(script)
                .as("audit-doc-freshness.sh must exist (story-0071-0005)")
                .exists();

        String content = Files.readString(script, StandardCharsets.UTF_8);

        assertThat(content)
                .as("audit-doc-freshness.sh MUST emit DOC_FRESHNESS_VIOLATION exit code 1")
                .contains("DOC_FRESHNESS_VIOLATION");

        assertThat(content)
                .as("audit-doc-freshness.sh MUST detect SKILL.md changes requiring README update")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("README"),
                        c -> assertThat(c).contains("readme"));

        assertThat(content)
                .as("audit-doc-freshness.sh MUST implement --self-check (Rule 26)")
                .contains("--self-check");
    }

    // ─── Scenario 2: PR with stale OpenAPI → DOC_FRESHNESS_VIOLATION ────────────

    @Test
    @DisplayName("scenario2_auditDocFreshness_detectsOpenApiStaleness")
    void scenario2_auditDocFreshness_detectsOpenApiStaleness() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("audit-doc-freshness.sh").toAbsolutePath();

        assertThat(script).exists();
        String content = Files.readString(script, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "audit-doc-freshness.sh MUST detect REST annotation changes without OpenAPI"
                                + " update")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("OpenAPI"),
                        c -> assertThat(c).contains("openapi"),
                        c -> assertThat(c).contains("RestController"),
                        c -> assertThat(c).contains("GetMapping"));

        assertThat(content)
                .as(
                        "audit-doc-freshness.sh MUST reference openapi.yaml or openapi.json as"
                                + " expected artifact")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("openapi.yaml"),
                        c -> assertThat(c).contains("openapi.json"));
    }

    // ─── Scenario 3: x-doc-validate skill present with PASS contract ────────────

    @Test
    @DisplayName("scenario3_xDocValidate_skillPresent_withPassContract")
    void scenario3_xDocValidate_skillPresent_withPassContract() throws IOException {
        Path skillFile =
                SKILLS_ROOT.resolve("ops/x-doc-validate/SKILL.md").toAbsolutePath();

        assertThat(skillFile)
                .as("x-doc-validate SKILL.md must exist (story-0071-0002)")
                .exists();

        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-doc-validate MUST document exit 0 = PASS contract")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("exit 0"),
                        c -> assertThat(c).contains("Exits `0`"),
                        c -> assertThat(c).contains("0 | `OK`"),
                        c -> assertThat(c).contains("0=OK"));

        assertThat(content)
                .as("x-doc-validate MUST document DOC_VALIDATION_FAILED exit code")
                .contains("DOC_VALIDATION_FAILED");

        assertThat(content)
                .as("x-doc-validate MUST be stack-aware (reads documentation.targets)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("stack-aware"),
                        c -> assertThat(c).contains("documentation.targets"),
                        c -> assertThat(c).contains("auto-detect"));

        assertThat(content)
                .as("x-doc-validate MUST produce a report artifact")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("--report-path"),
                        c -> assertThat(c).contains("doc-validate-report"));
    }

    // ─── Scenario 4: x-story-implement Phase 3 blocked if x-doc-validate fails ──

    @Test
    @DisplayName("scenario4_xStoryImplement_phase3DocGate_blocksOnDocValidateFail")
    void scenario4_xStoryImplement_phase3DocGate_blocksOnDocValidateFail() throws IOException {
        Path skillFile =
                SKILLS_ROOT.resolve("dev/x-story-implement/SKILL.md").toAbsolutePath();

        assertThat(skillFile).exists();
        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "x-story-implement Phase 3 MUST invoke x-doc-generate as MANDATORY TOOL"
                                + " CALL (Rule 24 + Rule 31)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("x-doc-generate"),
                        c -> assertThat(c).contains("Skill(skill: \"x-doc-generate\""));

        assertThat(content)
                .as(
                        "x-story-implement Phase 3 MUST invoke x-doc-validate as MANDATORY TOOL"
                                + " CALL (Rule 24 + Rule 31)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("x-doc-validate"),
                        c -> assertThat(c).contains("Skill(skill: \"x-doc-validate\""));

        assertThat(content)
                .as(
                        "x-story-implement MUST abort Phase 3 with DOC_VALIDATION_FAILED when"
                                + " x-doc-validate exits 1")
                .contains("DOC_VALIDATION_FAILED");

        assertThat(content)
                .as("x-story-implement MUST document that doc gate runs BEFORE verify gate (step 3.0)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("3.0"),
                        c -> assertThat(c).contains("step 3.0"),
                        c -> assertThat(c).contains("### 3.0"));
    }

    // ─── Scenario 5: --skip-doc confined to Recovery block ───────────────────────

    @Test
    @DisplayName("scenario5_skipDoc_confinedToRecoveryBlock")
    void scenario5_skipDoc_confinedToRecoveryBlock() throws IOException {
        Path skillFile =
                SKILLS_ROOT.resolve("dev/x-story-implement/SKILL.md").toAbsolutePath();

        assertThat(skillFile).exists();
        String content = Files.readString(skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("x-story-implement MUST document --skip-doc flag")
                .contains("--skip-doc");

        assertThat(content)
                .as(
                        "x-story-implement MUST confine --skip-doc to Recovery or hotfix/* context"
                                + " (Rule 27)")
                .satisfiesAnyOf(
                        c ->
                                assertThat(c)
                                        .contains("Recovery")
                                        .contains("--skip-doc"),
                        c ->
                                assertThat(c)
                                        .contains("Recovery-only")
                                        .contains("skip-doc"));

        assertThat(content)
                .as(
                        "x-story-implement MUST NOT list --skip-doc in the primary Parameters table"
                                + " — it is recovery-only")
                .satisfies(
                        c -> {
                            int paramTableIdx = c.indexOf("## Parameters");
                            int recoveryIdx = c.indexOf("## Recovery");
                            int skipDocIdx = c.indexOf("--skip-doc");
                            if (paramTableIdx >= 0 && recoveryIdx >= 0 && skipDocIdx >= 0) {
                                assertThat(skipDocIdx)
                                        .as(
                                                "--skip-doc first occurrence must be AFTER ##"
                                                        + " Parameters section (not in parameter"
                                                        + " table)")
                                        .isGreaterThan(paramTableIdx + 200);
                            }
                        });
    }

    // ─── Security scenario: audit-exempt without reason → INVALID_EXEMPTION ─────

    @Test
    @DisplayName("scenario6_auditExempt_withoutReason_rejected")
    void scenario6_auditExempt_withoutReason_rejected() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("audit-doc-freshness.sh").toAbsolutePath();

        assertThat(script).exists();
        String content = Files.readString(script, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "audit-doc-freshness.sh MUST reject audit-exempt markers without a reason"
                                + " (exit 3 INVALID_EXEMPTION)")
                .contains("INVALID_EXEMPTION");

        assertThat(content)
                .as(
                        "audit-doc-freshness.sh MUST support --pr-body-file for exemption"
                                + " detection")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("--pr-body-file"),
                        c -> assertThat(c).contains("audit-exempt"));
    }

    // ─── Governance: Rule 31 file + capability + baseline present ────────────────

    @Test
    @DisplayName("scenario7_rule31Governance_filesPresent")
    void scenario7_rule31Governance_filesPresent() throws IOException {
        Path rule31 = RULES_ROOT.resolve("31-documentation-freshness-gate.md").toAbsolutePath();

        assertThat(rule31)
                .as("Rule 31 documentation-freshness-gate.md must exist (story-0071-0001)")
                .exists();

        String rule31Content = Files.readString(rule31, StandardCharsets.UTF_8);

        assertThat(rule31Content)
                .as("Rule 31 MUST reference audit-doc-freshness.sh (Camada 2)")
                .contains("audit-doc-freshness.sh");

        assertThat(rule31Content)
                .as("Rule 31 MUST reference x-doc-validate (Camada 0)")
                .contains("x-doc-validate");

        Path baseline = BASELINES_ROOT.resolve("doc-freshness-baseline.txt").toAbsolutePath();

        assertThat(baseline)
                .as("doc-freshness-baseline.txt must exist (story-0071-0005)")
                .exists();

        String baselineContent = Files.readString(baseline, StandardCharsets.UTF_8);
        long nonCommentLines =
                baselineContent.lines()
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .count();
        assertThat(nonCommentLines)
                .as(
                        "doc-freshness-baseline.txt MUST have zero active entries"
                                + " (no pre-EPIC-0071 exceptions grandfathered)")
                .isZero();
    }

    // ─── verify-story-completion.sh extended with doc-validate check ─────────────

    @Test
    @DisplayName("scenario8_verifyStoryCompletion_checksDocValidateArtifact")
    void scenario8_verifyStoryCompletion_checksDocValidateArtifact() throws IOException {
        Path hook = HOOKS_ROOT.resolve("verify-story-completion.sh").toAbsolutePath();

        assertThat(hook)
                .as("verify-story-completion.sh must exist")
                .exists();

        String content = Files.readString(hook, StandardCharsets.UTF_8);

        assertThat(content)
                .as(
                        "verify-story-completion.sh MUST check for doc-validate-report artifact"
                                + " (story-0071-0006)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("doc-validate-report"),
                        c -> assertThat(c).contains("x-doc-validate"));
    }
}
