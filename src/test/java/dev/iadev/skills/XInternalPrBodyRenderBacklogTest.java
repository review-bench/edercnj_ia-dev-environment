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
 * Validates x-internal-pr-body-render/SKILL.md backlog extension (story-0066-0004) via static
 * inspection.
 *
 * <p>Checks: Phase 1.5 BACKLOG-GATHER presence + fail-open placeholders for backlog sources,
 * --epic-id validation in Phase 0, Phase 3 template dispatch for backlog, RULE-007 (Orchestrator
 * Evidence without Story IDs), phase-no-gate comment for Phase 1.5.
 *
 * <p>TPP order: degenerate (Phase 1.5 exists) → constant (epic-id validation documented) →
 * collection (backlog sources) → conditional (fail-open placeholders) → error (INVALID_SCOPE doc).
 */
@DisplayName("XInternalPrBodyRenderBacklogTest")
class XInternalPrBodyRenderBacklogTest {

    private static final Path SKILL_MD =
            Path.of(
                    "src/main/resources/targets/claude/skills/core/internal/pr"
                            + "/x-internal-pr-body-render/SKILL.md");

    private static String content;

    @BeforeAll
    static void loadContent() throws IOException {
        assertThat(SKILL_MD).as("SKILL.md must exist").exists();
        content = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("degenerate — Phase 1.5 BACKLOG-GATHER is documented")
    class Phase15Exists {

        @Test
        @DisplayName("Phase 1.5 heading exists in SKILL.md")
        void skillMd_hasPhase15BacklogGather() {
            assertThat(content).contains("Phase 1.5");
            assertThat(content).contains("BACKLOG-GATHER");
        }

        @Test
        @DisplayName("Phase 1.5 has phase-no-gate comment (non-gated sub-phase)")
        void phase15_hasNoGateComment() {
            assertThat(content).contains("phase-no-gate");
        }
    }

    @Nested
    @DisplayName("constant — --epic-id validation documented in Phase 0")
    class EpicIdValidation {

        @Test
        @DisplayName("INVALID_SCOPE for --kind=backlog without --epic-id is documented")
        void epicIdValidation_invalidScopeBacklog() {
            assertThat(content).contains("INVALID_SCOPE: --epic-id required for --kind=backlog");
        }

        @Test
        @DisplayName("epic-id format validation documented")
        void epicIdValidation_formatRegex() {
            assertThat(content)
                    .as("epic-id format must be documented")
                    .satisfiesAnyOf(
                        c -> assertThat(c).contains("^epic-[0-9]{4}$"),
                        c -> assertThat(c).contains("epic-XXXX"));
        }

        @Test
        @DisplayName("epic-id normalization documented (EPIC-XXXX → epic-XXXX)")
        void epicIdValidation_normalization() {
            assertThat(content)
                    .as("EPIC-XXXX normalization should be documented")
                    .satisfiesAnyOf(
                        c -> assertThat(c).contains("Normaliz"),
                        c -> assertThat(c).contains("EPIC-"),
                        c -> assertThat(c).contains("normaliz"));
        }
    }

    @Nested
    @DisplayName("collection — backlog gather sources documented")
    class BacklogSources {

        @Test
        @DisplayName("epic.md parsing documented")
        void backlogSources_epicMarkdown() {
            assertThat(content).contains("epic-XXXX.md");
            assertThat(content).contains("successMetrics");
        }

        @Test
        @DisplayName("IMPLEMENTATION-MAP.md parsing documented")
        void backlogSources_implementationMap() {
            assertThat(content).contains("IMPLEMENTATION-MAP.md");
            assertThat(content).contains("criticalPath");
        }

        @Test
        @DisplayName("spec localization documented with security (no glob injection)")
        void backlogSources_specSearch() {
            assertThat(content).contains("spec-*.md");
            assertThat(content)
                    .as("spec search must be security-bounded to REPO_ROOT")
                    .satisfiesAnyOf(
                        c -> assertThat(c).contains("REPO_ROOT"),
                        c -> assertThat(c).contains("${REPO_ROOT}"));
        }

        @Test
        @DisplayName("in-progress epic coordinations scanning documented")
        void backlogSources_coordination() {
            assertThat(content)
                    .as("coordination with in-progress epics must be documented")
                    .satisfiesAnyOf(
                        c -> assertThat(c).contains("execution-state.json"),
                        c -> assertThat(c).contains("Em Andamento"),
                        c -> assertThat(c).contains("in_progress"));
        }
    }

    @Nested
    @DisplayName("conditional — fail-open placeholders for backlog (RULE-004)")
    class BacklogFailOpen {

        @Test
        @DisplayName("(map missing) placeholder documented for absent IMPLEMENTATION-MAP.md")
        void failOpen_mapMissing() {
            assertThat(content).contains("(map missing)");
        }

        @Test
        @DisplayName("(no metrics declared) placeholder documented")
        void failOpen_noMetrics() {
            assertThat(content).contains("(no metrics declared)");
        }

        @Test
        @DisplayName("(spec not found) placeholder documented")
        void failOpen_specNotFound() {
            assertThat(content).contains("(spec not found)");
        }
    }

    @Nested
    @DisplayName("error — RULE-007 backlog orchestrator evidence")
    class OrchestratorEvidence {

        @Test
        @DisplayName("Phase 3 template dispatch selects _TEMPLATE-PR-BACKLOG.md for backlog")
        void phase3_selectsBacklogTemplate() {
            assertThat(content).contains("_TEMPLATE-PR-BACKLOG.md");
        }

        @Test
        @DisplayName("RULE-007 documented: backlog PR evidence without Story IDs")
        void rule007_backlogEvidenceWithoutStoryIds() {
            assertThat(content)
                    .as("RULE-007 or backlog Orchestrator Evidence must reference reduced form")
                    .satisfiesAnyOf(
                        c -> assertThat(c).contains("RULE-007"),
                        c -> assertThat(c).contains("without Story IDs"),
                        c -> assertThat(c).contains("Backlog-only PR"));
        }
    }
}
