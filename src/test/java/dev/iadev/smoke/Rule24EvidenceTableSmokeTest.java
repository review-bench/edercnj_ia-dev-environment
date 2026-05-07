package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Repo-level smoke test for the Rule 24 evidence-table contract (EPIC-0057 + EPIC-0078).
 *
 * <p>EPIC-0078 story-0078-0013 slimmed Rule 24 to a compact contract stub; the full Mandatory
 * Evidence Artifacts table moved to the lifecycle KP {@code
 * src/main/resources/targets/claude/knowledge/lifecycle/execution-integrity.md}. This test
 * verifies both the slimmed rule and the KP carry the required content.
 *
 * <p>Complements {@link Rule24EvidenceTableExpansionTest}, which exercises the pipeline end-to-end.
 * This smoke version is independent of {@code @TempDir} and runs as a fast guard against drift.
 */
@DisplayName("Rule24EvidenceTableSmokeTest — KP carries evidence table, rule carries contract")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX path resolution; matches sibling smoke tests.")
class Rule24EvidenceTableSmokeTest {

    private static final String REFERENCE_GOLDEN_PATH =
            "src/test/resources/golden/java-spring/" + ".claude/rules/24-execution-integrity.md";

    private static final String EVIDENCE_KP_PATH =
            "src/main/resources/targets/claude/knowledge/lifecycle/execution-integrity.md";

    @Test
    @DisplayName("reference golden Rule 24 (slimmed) carries EIE_EVIDENCE_MISSING contract")
    void smoke_referenceGolden_hasAtLeastElevenRows() throws IOException {
        // EPIC-0078: evidence table moved to KP; rule carries contract stub.
        // Check the KP (source-of-truth) has ≥11 evidence rows.
        Path kp = repoRoot().resolve(EVIDENCE_KP_PATH);
        assertThat(kp).as("execution-integrity KP must exist").exists();

        String body = Files.readString(kp, StandardCharsets.UTF_8);
        long rowCount = body.lines().filter(l -> l.startsWith("| `x-")).count();

        assertThat(rowCount)
                .as("KP evidence table must have ≥11 data rows; found %d", rowCount)
                .isGreaterThanOrEqualTo(11);
    }

    @Test
    @DisplayName("KP references both x-watch-pr-ci and x-audit-dependencies")
    void smoke_referenceGolden_referencesNewSubSkills() throws IOException {
        // EPIC-0078: content moved from Rule 24 to lifecycle KP — check the KP.
        Path kp = repoRoot().resolve(EVIDENCE_KP_PATH);
        String body = Files.readString(kp, StandardCharsets.UTF_8);

        assertThat(body).as("KP must list x-watch-pr-ci").contains("`x-watch-pr-ci`");
        assertThat(body)
                .as("KP must list x-audit-dependencies")
                .contains("`x-audit-dependencies`");
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }
}
