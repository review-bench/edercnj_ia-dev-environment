package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validates x-internal-render-pr-body/SKILL.md contract via static inspection.
 *
 * <p>Checks: frontmatter (visibility=internal, user-invocable=false, model=haiku), body marker, 5
 * numbered phases with balanced telemetry markers, exit-code contract, fail-open placeholders,
 * Haiku eligibility note in Integration Notes.
 *
 * <p>TPP order: degenerate (file exists) → constant (frontmatter fields) → collection (phases +
 * markers) → conditional (fail-open placeholders) → error (INVALID_KIND doc).
 */
@DisplayName("XInternalPrBodyRenderImplementationTest")
class XInternalPrBodyRenderImplementationTest {

    private static final Path SKILL_MD =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/internal/pr"
                            + "/x-internal-render-pr-body/SKILL.md");

    private static String content;

    @BeforeAll
    static void loadContent() throws IOException {
        assertThat(SKILL_MD).as("SKILL.md must exist").exists();
        content = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("degenerate — SKILL.md is a valid file")
    class FileExists {

        @Test
        @DisplayName("file exists and is non-empty")
        void skillMd_exists_andIsNonEmpty() {
            assertThat(content).isNotBlank();
        }
    }

    @Nested
    @DisplayName("constant — frontmatter contract (Rule 22)")
    class FrontmatterContract {

        @Test
        @DisplayName("visibility is internal")
        void frontmatter_hasVisibilityInternal() {
            assertThat(content).contains("visibility: internal");
        }

        @Test
        @DisplayName("user-invocable is false")
        void frontmatter_hasUserInvocableFalse() {
            assertThat(content).contains("user-invocable: false");
        }

        @Test
        @DisplayName("model is haiku (Rule 23 utility criteria)")
        void frontmatter_hasModelHaiku() {
            assertThat(content).contains("model: haiku");
        }

        @Test
        @DisplayName("allowed-tools includes Skill (required for Rule 13 Pattern 1)")
        void frontmatter_allowedToolsIncludesSkill() {
            assertThat(content).contains("Skill");
        }

        @Test
        @DisplayName("body marker is present in first 30 lines")
        void bodyMarker_presentInFirst30Lines() {
            String first30Lines = content.lines().limit(30).reduce("", (a, b) -> a + "\n" + b);
            assertThat(first30Lines).contains("🔒 **INTERNAL SKILL**");
        }
    }

    @Nested
    @DisplayName("collection — 5 numbered phases with balanced telemetry markers")
    class PhaseMarkersBalance {

        @Test
        @DisplayName("has exactly 5 numbered Phase N headings")
        void hasExactly5NumberedPhases() {
            long count =
                    content.lines().filter(line -> line.matches("^## Phase [0-4] —.*")).count();
            assertThat(count).as("Should have 5 numbered phases (0-4)").isEqualTo(5);
        }

        @Test
        @DisplayName("phase.start markers match phase.end markers in count")
        void phaseMarkers_areBalanced() {
            long starts =
                    countOccurrences(content, "telemetry-phase.sh start x-internal-render-pr-body");
            long ends =
                    countOccurrences(content, "telemetry-phase.sh end x-internal-render-pr-body");
            assertThat(starts).as("phase.start count").isEqualTo(5);
            assertThat(ends).as("phase.end count").isEqualTo(5);
            assertThat(starts).as("starts must equal ends").isEqualTo(ends);
        }

        @Test
        @DisplayName("all phase identifiers use kebab-case (Rule 13 §Forbidden)")
        void phaseIdentifiers_useKebabCase() {
            Pattern identifierPattern =
                    Pattern.compile(
                            "telemetry-phase\\.sh\\s+start\\s+\\S+\\s+(Phase-\\d+-[^\\s`'\"]+)");
            java.util.regex.Matcher m = identifierPattern.matcher(content);
            while (m.find()) {
                String id = m.group(1);
                assertThat(id)
                        .as("Phase identifier '%s' must be kebab-case", id)
                        .matches("Phase-\\d+-[a-zA-Z][a-zA-Z0-9-]+");
                assertThat(id).as("Phase identifier must not contain dots").doesNotContain(".");
                assertThat(id.length())
                        .as("Phase identifier must be ≤ 64 chars")
                        .isLessThanOrEqualTo(64);
            }
        }
    }

    @Nested
    @DisplayName("conditional — fail-open placeholders (RULE-004)")
    class FailOpenPlaceholders {

        @Test
        @DisplayName("skill documents fail-open placeholders (pending, not yet run, no telemetry)")
        void skillDocuments_failOpenPlaceholders() {
            assertThat(content).contains("(pending)");
            assertThat(content).contains("(not yet run)");
            assertThat(content).contains("(no telemetry available)");
        }

        @Test
        @DisplayName("RULE-004 forbids audit-sentinel placeholder values")
        void rule004_forbidsAuditSentinelPlaceholders() {
            assertThat(content)
                    .as("Fail-open contract section must be documented")
                    .contains("Forbidden placeholder values");
        }

        @Test
        @DisplayName("telemetry invoke documents fail-open for exit 1 and exit 2")
        void telemetryPhase_documentsBothFailCases() {
            assertThat(content).contains("NO_TELEMETRY");
            assertThat(content).contains("OPERATIONAL_ERROR");
        }
    }

    @Nested
    @DisplayName("error — exit codes documented")
    class ExitCodes {

        @Test
        @DisplayName("INVALID_KIND documented with exit 1")
        void exitCode1_invalidKindDocumented() {
            assertThat(content).contains("INVALID_KIND");
            assertThat(content).contains("exit 1");
        }

        @Test
        @DisplayName("INVALID_SCOPE documented with exit 3")
        void exitCode3_invalidScopeDocumented() {
            assertThat(content).contains("INVALID_SCOPE");
            assertThat(content).contains("exit 3");
        }

        @Test
        @DisplayName("OPERATIONAL_ERROR documented with exit 2")
        void exitCode2_operationalErrorDocumented() {
            assertThat(content).contains("OPERATIONAL_ERROR");
            assertThat(content).contains("exit 2");
        }

        @Test
        @DisplayName("Haiku eligibility note present in Integration Notes (Rule 23)")
        void haiku_eligibilityNotePresent() {
            assertThat(content)
                    .as("Integration Notes must contain Haiku eligibility rationale")
                    .contains("Haiku eligibility");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long countOccurrences(String text, String pattern) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
