package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validates x-feature-create/SKILL.md backlog render integration (story-0066-0006) via static
 * inspection.
 *
 * <p>Checks: Phase P5.5 BACKLOG-RENDER MANDATORY TOOL CALL, --kind=backlog argument, Phase P6 uses
 * gh pr create --body-file, ## Recovery section, fail-open contract.
 */
@DisplayName("XFeatureCreateRenderIntegrationTest")
class XFeatureCreateRenderIntegrationTest {

    private static final Path SKILL_MD =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/plan/"
                            + "x-feature-create/SKILL.md");

    private static String content;

    @BeforeAll
    static void loadContent() throws IOException {
        assertThat(SKILL_MD).as("x-feature-create/SKILL.md must exist").exists();
        content = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("happy path — Phase P5.5 BACKLOG-RENDER added")
    class BacklogRenderPhase {

        @Test
        @DisplayName("Phase P5.5 heading is present")
        void phaseP55_isPresent() {
            assertThat(content).contains("Phase P5.5");
            assertThat(content).contains("BACKLOG-RENDER");
        }

        @Test
        @DisplayName("Phase P5.5 invokes x-internal-pr-body-render with --kind=backlog")
        void phaseP55_invokesRenderSkillWithBacklogKind() {
            assertThat(content)
                    .as("Phase P5.5 must invoke render skill via Skill tool")
                    .contains("Skill(skill: \"x-internal-pr-body-render\"");
            assertThat(content)
                    .as("invocation must use --kind=backlog (not implementation)")
                    .contains("--kind=backlog");
        }

        @Test
        @DisplayName("Phase P5.5 declared as MANDATORY TOOL CALL (Rule 24)")
        void phaseP55_isMandatoryToolCall() {
            assertThat(content).contains("MANDATORY TOOL CALL");
            assertThat(content).contains("Rule 24");
        }

        @Test
        @DisplayName("Phase P5.5 emits telemetry markers for the new phase")
        void phaseP55_hasTelemetryMarkers() {
            assertThat(content).contains("Phase-P55-Render");
        }

        @Test
        @DisplayName("--out path uses mktemp (Rule 06)")
        void phaseP55_usesMktemp() {
            assertThat(content).contains("mktemp");
        }
    }

    @Nested
    @DisplayName("Phase P6 uses --body-file with rendered body")
    class PRCreationUsesBodyFile {

        @Test
        @DisplayName("Phase P6 uses gh pr create with --body-file flag")
        void phaseP6_usesBodyFile() {
            assertThat(content)
                    .as("Phase P6 must use gh pr create --body-file (not Skill x-pr-create)")
                    .contains("--body-file");
        }

        @Test
        @DisplayName("Phase P6 references PR_BODY_FILE variable from Phase P5.5")
        void phaseP6_referencesPrBodyFileVariable() {
            assertThat(content).contains("PR_BODY_FILE");
        }

        @Test
        @DisplayName("Phase P6 still labels PR with docs and epic-XXXX")
        void phaseP6_preservesLabels() {
            assertThat(content).contains("--label docs");
            assertThat(content).contains("epic-${EPIC_ID}");
        }

        @Test
        @DisplayName("Phase P6 enables auto-merge for docs PRs (Rule 21 exception)")
        void phaseP6_enablesAutoMerge() {
            assertThat(content).contains("gh pr merge");
            assertThat(content).contains("--auto");
        }
    }

    @Nested
    @DisplayName("fallback — ## Recovery documented")
    class RecoverySection {

        @Test
        @DisplayName("## Recovery section is present")
        void recoverySection_isPresent() {
            assertThat(content).contains("## Recovery");
        }

        @Test
        @DisplayName("Recovery documents WARN with RENDER_EXIT exit code")
        void recovery_documentsWarnWithExitCode() {
            assertThat(content).contains("[render-fallback]");
            assertThat(content).contains("RENDER_EXIT");
        }

        @Test
        @DisplayName("Recovery references RULE-004 fail-open contract")
        void recovery_documentsRule004() {
            assertThat(content).contains("RULE-004");
        }

        @Test
        @DisplayName("Recovery warns about missing template-version marker")
        void recovery_warnsAboutMissingMarker() {
            assertThat(content).contains("template-version");
            assertThat(content)
                    .as("Recovery must warn audit will block merge")
                    .containsAnyOf("audit-pr-template", "PR_TEMPLATE_VIOLATION");
        }
    }

    @Nested
    @DisplayName("audit awareness — story-0066-0007 readiness")
    class AuditAwareness {

        @Test
        @DisplayName("Phase P5.5 mentions audit-pr-template.sh")
        void phaseP55_mentionsAuditPrTemplate() {
            assertThat(content).contains("audit-pr-template.sh");
        }
    }
}
