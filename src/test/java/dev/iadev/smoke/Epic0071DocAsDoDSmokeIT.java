package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    private String read(Path root, String relative) throws IOException {
        Path p = root.resolve(relative).toAbsolutePath();
        assertThat(p).as(relative + " must exist").exists();
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("scenario1_auditDocFreshness_detectsReadmeStaleness")
    void scenario1_auditDocFreshness_detectsReadmeStaleness() throws IOException {
        String s = read(SCRIPTS_ROOT, "audit-doc-freshness.sh");
        assertThat(s).as("must emit DOC_FRESHNESS_VIOLATION").contains("DOC_FRESHNESS_VIOLATION");
        assertThat(s)
                .as("must detect README staleness")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("README"),
                        c -> assertThat(c).contains("readme"));
        assertThat(s).as("must implement --self-check (Rule 26)").contains("--self-check");
    }

    @Test
    @DisplayName("scenario2_auditDocFreshness_detectsOpenApiStaleness")
    void scenario2_auditDocFreshness_detectsOpenApiStaleness() throws IOException {
        String s = read(SCRIPTS_ROOT, "audit-doc-freshness.sh");
        assertThat(s)
                .as("must detect REST changes without OpenAPI update")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("OpenAPI"),
                        c -> assertThat(c).contains("openapi"),
                        c -> assertThat(c).contains("RestController"),
                        c -> assertThat(c).contains("GetMapping"));
        assertThat(s)
                .as("must reference openapi.yaml or openapi.json")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("openapi.yaml"),
                        c -> assertThat(c).contains("openapi.json"));
    }

    @Test
    @DisplayName("scenario3_xDocValidate_skillPresent_withPassContract")
    void scenario3_xDocValidate_skillPresent_withPassContract() throws IOException {
        String s = read(SKILLS_ROOT, "ops/x-validate-docs/SKILL.md");
        assertThat(s)
                .as("must document exit 0 = PASS contract")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("exit 0"),
                        c -> assertThat(c).contains("Exits `0`"),
                        c -> assertThat(c).contains("0 | `OK`"),
                        c -> assertThat(c).contains("0=OK"));
        assertThat(s).as("must document DOC_VALIDATION_FAILED").contains("DOC_VALIDATION_FAILED");
        assertThat(s)
                .as("must be stack-aware")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("stack-aware"),
                        c -> assertThat(c).contains("documentation.targets"),
                        c -> assertThat(c).contains("auto-detect"));
        assertThat(s)
                .as("must produce a report artifact")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("--report-path"),
                        c -> assertThat(c).contains("doc-validate-report"));
    }

    @Test
    @DisplayName("scenario4_xStoryImplement_phase3DocGate_blocksOnDocValidateFail")
    void scenario4_xStoryImplement_phase3DocGate_blocksOnDocValidateFail() throws IOException {
        String s = read(SKILLS_ROOT, "dev/x-implement-story/SKILL.md");
        assertThat(s)
                .as("Phase 3 MUST invoke x-generate-docs (Rule 24 + Rule 31)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("x-generate-docs"),
                        c -> assertThat(c).contains("Skill(skill: \"x-generate-docs\""));
        assertThat(s)
                .as("Phase 3 MUST invoke x-validate-docs (Rule 24 + Rule 31)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("x-validate-docs"),
                        c -> assertThat(c).contains("Skill(skill: \"x-validate-docs\""));
        assertThat(s).as("MUST abort with DOC_VALIDATION_FAILED").contains("DOC_VALIDATION_FAILED");
        assertThat(s)
                .as("doc gate MUST run BEFORE verify gate (step 3.0)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("3.0"),
                        c -> assertThat(c).contains("step 3.0"),
                        c -> assertThat(c).contains("### 3.0"));
    }

    @Test
    @DisplayName("scenario5_skipDoc_confinedToRecoveryBlock")
    void scenario5_skipDoc_confinedToRecoveryBlock() throws IOException {
        String s = read(SKILLS_ROOT, "dev/x-implement-story/SKILL.md");
        assertThat(s).as("MUST document --skip-doc flag").contains("--skip-doc");
        assertThat(s)
                .as("--skip-doc MUST be confined to Recovery / hotfix/* (Rule 27)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("Recovery").contains("--skip-doc"),
                        c -> assertThat(c).contains("Recovery-only").contains("skip-doc"));
        assertSkipDocNotInPrimaryParams(s);
    }

    private void assertSkipDocNotInPrimaryParams(String s) {
        int paramIdx = s.indexOf("## Parameters");
        int skipDocIdx = s.indexOf("--skip-doc");
        if (paramIdx >= 0 && s.contains("## Recovery") && skipDocIdx >= 0) {
            assertThat(skipDocIdx)
                    .as("--skip-doc first occurrence must be AFTER ## Parameters section")
                    .isGreaterThan(paramIdx + 200);
        }
    }

    @Test
    @DisplayName("scenario6_auditExempt_withoutReason_rejected")
    void scenario6_auditExempt_withoutReason_rejected() throws IOException {
        String s = read(SCRIPTS_ROOT, "audit-doc-freshness.sh");
        assertThat(s)
                .as("MUST reject audit-exempt without reason (exit 3 INVALID_EXEMPTION)")
                .contains("INVALID_EXEMPTION");
        assertThat(s)
                .as("MUST support audit-exempt detection")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("--pr-body-file"),
                        c -> assertThat(c).contains("audit-exempt"));
    }

    @Test
    @DisplayName("scenario7_rule31Governance_filesPresent")
    void scenario7_rule31Governance_filesPresent() throws IOException {
        // EPIC-0078: Rule 31 content migrated to governance KP doc-freshness-gate.md.
        Path docFreshnessKp =
                Path.of(
                        "src",
                        "main",
                        "resources",
                        "targets",
                        "claude",
                        "knowledge",
                        "governance",
                        "rules",
                        "doc-freshness-gate.md");
        assertThat(docFreshnessKp.toAbsolutePath())
                .as("31-documentation-freshness-gate.md must exist at KP destination")
                .exists();
        String rule31 = Files.readString(docFreshnessKp.toAbsolutePath(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(rule31)
                .as("Rule 31 MUST reference audit-doc-freshness.sh")
                .contains("audit-doc-freshness.sh");
        assertThat(rule31).as("Rule 31 MUST reference x-validate-docs").contains("x-validate-docs");
        assertBaselineHasZeroEntries();
    }

    private void assertBaselineHasZeroEntries() throws IOException {
        Path baseline = BASELINES_ROOT.resolve("doc-freshness-baseline.txt").toAbsolutePath();
        assertThat(baseline).as("doc-freshness-baseline.txt must exist").exists();
        long nonCommentLines =
                Files.readString(baseline, StandardCharsets.UTF_8)
                        .lines()
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .count();
        assertThat(nonCommentLines).as("baseline MUST have zero active entries").isZero();
    }

    @Test
    @DisplayName("scenario8_verifyStoryCompletion_checksDocValidateArtifact")
    void scenario8_verifyStoryCompletion_checksDocValidateArtifact() throws IOException {
        String s = read(HOOKS_ROOT, "verify-story-completion.sh");
        assertThat(s)
                .as("MUST check for doc-validate-report artifact (story-0071-0006)")
                .satisfiesAnyOf(
                        c -> assertThat(c).contains("doc-validate-report"),
                        c -> assertThat(c).contains("x-doc-validate"));
    }
}
