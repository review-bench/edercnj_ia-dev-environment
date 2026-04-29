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
 * Validates x-pr-create/SKILL.md Phase 3 integration with x-internal-pr-body-render
 * (story-0066-0005) via static inspection.
 *
 * <p>Checks: MANDATORY TOOL CALL invocation in Phase 3, deduplication logic in Phase 3.5,
 * Recovery section with fallback body, WARN message includes exit code, dedup invariant.
 *
 * <p>TPP order: degenerate (file exists) → constant (Skill invocation present) →
 * collection (4 sections — Phase 3, 3.5, Recovery, dedup) → conditional (fallback warn) →
 * error (legacy body preserved).
 */
@DisplayName("XPrCreateRenderIntegrationTest")
class XPrCreateRenderIntegrationTest {

    private static final Path SKILL_MD =
            Path.of("src/main/resources/targets/claude/skills/core/pr/x-pr-create/SKILL.md");

    private static final Path REFERENCES_RECOVERY =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/pr/x-pr-create"
                            + "/references/recovery.md");

    private static String content;
    private static String combined;

    @BeforeAll
    static void loadContent() throws IOException {
        assertThat(SKILL_MD).as("x-pr-create/SKILL.md must exist").exists();
        content = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
        // ADR-0007 carve-out: recovery detail may live in references/recovery.md.
        // Combined view is used by tests that allow either location.
        StringBuilder sb = new StringBuilder(content);
        if (Files.exists(REFERENCES_RECOVERY)) {
            sb.append("\n").append(Files.readString(REFERENCES_RECOVERY, StandardCharsets.UTF_8));
        }
        combined = sb.toString();
    }

    @Nested
    @DisplayName("happy path — Phase 3 invokes render skill via Pattern 1")
    class RenderInvocation {

        @Test
        @DisplayName("Phase 3 contains Skill(skill: \"x-internal-pr-body-render\", ...) invocation")
        void phase3_invokesRenderSkill() {
            assertThat(content)
                    .as("Phase 3 must invoke x-internal-pr-body-render via Skill tool")
                    .contains("Skill(skill: \"x-internal-pr-body-render\"");
        }

        @Test
        @DisplayName("invocation passes --kind=implementation")
        void renderInvocation_passesKindImplementation() {
            assertThat(content).contains("--kind=implementation");
        }

        @Test
        @DisplayName("invocation declared as MANDATORY TOOL CALL (Rule 24 Camada-1)")
        void renderInvocation_isMandatoryToolCall() {
            assertThat(content).contains("MANDATORY TOOL CALL");
            assertThat(content).contains("Rule 24");
        }

        @Test
        @DisplayName("invocation uses model: haiku (Rule 23 utility tier)")
        void renderInvocation_usesHaikuModel() {
            assertThat(content)
                    .as("Render skill must be invoked with model: haiku per Rule 23")
                    .contains("model: \"haiku\"");
        }

        @Test
        @DisplayName("--out path uses mktemp (Rule 06 secure temp file)")
        void renderInvocation_usesMktemp() {
            assertThat(content).contains("mktemp");
        }
    }

    @Nested
    @DisplayName("dedup — Phase 3.5 detects existing Orchestrator Evidence section")
    class DedupLogic {

        @Test
        @DisplayName("Phase 3.5 contains grep check for Orchestrator Evidence")
        void phase35_hasGrepCheckForEvidence() {
            assertThat(content)
                    .as("Phase 3.5 must check for existing Orchestrator Evidence with grep")
                    .contains("grep -q \"^## Orchestrator Evidence\"");
        }

        @Test
        @DisplayName("Phase 3.5 logs INFO when skipping injection")
        void phase35_logsInfoOnSkip() {
            assertThat(content).contains("already present");
            assertThat(content).contains("skipping injection");
        }

        @Test
        @DisplayName("dedup invariant documented: exactly one Orchestrator Evidence")
        void dedup_invariantDocumented() {
            assertThat(content)
                    .as("Invariant about exactly one ## Orchestrator Evidence must be documented")
                    .containsAnyOf("exactly one", "exactly 1");
        }
    }

    @Nested
    @DisplayName("fallback — ## Recovery section with legacy inline body")
    class RecoverySection {

        @Test
        @DisplayName("## Recovery section is present")
        void recoverySection_isPresent() {
            assertThat(content).contains("## Recovery");
        }

        @Test
        @DisplayName("Recovery preserves legacy inline body sections (in SKILL.md or references/)")
        void recoverySection_preservesLegacyBody() {
            // ADR-0007: detail may be carved out to references/recovery.md to keep SKILL.md ≤ 500 lines.
            assertThat(combined)
                    .as("Recovery context must contain Summary heading")
                    .contains("## Summary");
            assertThat(combined)
                    .as("Recovery context must contain Task Details heading")
                    .contains("## Task Details");
            assertThat(combined)
                    .as("Recovery context must contain Changes heading")
                    .contains("## Changes");
            assertThat(combined)
                    .as("Recovery context must contain Review Checklist heading")
                    .contains("## Review Checklist");
        }

        @Test
        @DisplayName("Recovery documents WARN message with exit code")
        void recoverySection_documentsWarnWithExitCode() {
            assertThat(content)
                    .as("WARN must include render-fallback marker and exit code reference")
                    .contains("[render-fallback]");
            assertThat(content)
                    .as("WARN message must mention RENDER_EXIT or exit code")
                    .containsAnyOf("RENDER_EXIT", "exit code");
        }

        @Test
        @DisplayName("Recovery documents fail-open per RULE-004")
        void recoverySection_documentsFailOpen() {
            assertThat(content)
                    .as("Recovery section must reference RULE-004 fail-open contract")
                    .contains("RULE-004");
        }
    }

    @Nested
    @DisplayName("audit — audit-pr-template.sh awareness documented")
    class AuditAwareness {

        @Test
        @DisplayName("Phase 3 mentions audit-pr-template.sh requirement")
        void phase3_mentionsAuditPrTemplate() {
            assertThat(content)
                    .as("Phase 3 must reference audit-pr-template.sh marker requirement")
                    .contains("audit-pr-template.sh");
        }

        @Test
        @DisplayName("Recovery warns that fallback body lacks template-version marker")
        void recovery_warnsAboutMissingMarker() {
            assertThat(content)
                    .as("Recovery section must warn that fallback lacks template-version marker")
                    .contains("template-version");
        }
    }
}
