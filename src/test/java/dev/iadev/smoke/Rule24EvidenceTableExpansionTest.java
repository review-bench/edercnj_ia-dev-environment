package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies the Rule 24 "Mandatory Evidence Artifacts" table is present in the lifecycle KP.
 *
 * <p>EPIC-0078 story-0078-0013 slimmed Rule 24 to a compact contract stub; the full evidence table
 * (previously 11+ entries) moved to the lifecycle KP {@code
 * .claude/knowledge/lifecycle/execution-integrity.md}. This test guards the KP contract.
 *
 * <p>Acceptance criteria: every regenerated profile golden carries the KP with all 11 canonical
 * sub-skill entries; build fails if the table regresses.
 */
@DisplayName("Rule24EvidenceTableExpansionTest — lifecycle KP carries 11+ evidence entries")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason =
                "Pipeline regen relies on POSIX execute bit;"
                        + " mirrors Epic0055FoundationSmokeTest gating.")
class Rule24EvidenceTableExpansionTest extends SmokeTestBase {

    private static final List<String> EXPECTED_SUBSKILLS =
            List.of(
                    "x-internal-verify-story",
                    "x-review-codebase",
                    "x-review-pr",
                    "x-internal-write-story-report",
                    "x-plan-architecture",
                    "x-watch-pr-ci",
                    "x-create-pr",
                    "x-drive-tdd",
                    "x-commit-changes",
                    "x-audit-dependencies",
                    "x-model-threats");

    private static final String TABLE_HEADER = "| Sub-skill | Artifact path | Enforced by |";

    // EPIC-0078 story-0078-0013: evidence table moved from Rule 24 to lifecycle KP.
    private static final String EVIDENCE_KP_PATH =
            ".claude/knowledge/lifecycle/execution-integrity.md";

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("lifecycle KP evidence table contains all 11 expected sub-skills")
    void rule24_evidenceTable_containsExpectedSubSkills(String profile) throws IOException {
        runPipeline(profile);
        // rules-consolidation-essentials: Rule 24 stub removed from .claude/rules/;
        // content fully lives in the lifecycle execution-integrity KP.
        Path kp = getOutputDir(profile).resolve(EVIDENCE_KP_PATH);

        assertThat(kp).as("profile %s: execution-integrity KP must exist", profile).exists();

        String body = Files.readString(kp, StandardCharsets.UTF_8);

        assertThat(body)
                .as("profile %s: KP table header must be present", profile)
                .contains(TABLE_HEADER);

        for (String subskill : EXPECTED_SUBSKILLS) {
            assertThat(body)
                    .as("profile %s: KP table must reference '%s'", profile, subskill)
                    .contains("`" + subskill);
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("lifecycle KP evidence table has at least 11 data rows")
    void rule24_evidenceTable_hasAtLeastElevenRows(String profile) throws IOException {
        runPipeline(profile);
        Path kp = getOutputDir(profile).resolve(EVIDENCE_KP_PATH);

        assertThat(kp).as("profile %s: execution-integrity KP must exist", profile).exists();
        String body = Files.readString(kp, StandardCharsets.UTF_8);
        long rowCount = countTableRows(body);

        assertThat(rowCount)
                .as("profile %s: KP table must have ≥11 data rows; found %d", profile, rowCount)
                .isGreaterThanOrEqualTo(11);
    }

    private long countTableRows(String body) {
        int tableStart = body.indexOf(TABLE_HEADER);
        if (tableStart < 0) {
            return 0;
        }
        int separatorEnd = body.indexOf('\n', body.indexOf("| :---", tableStart)) + 1;
        int tableEnd = body.indexOf("\n\n", separatorEnd);
        if (tableEnd < 0) {
            tableEnd = body.length();
        }
        String tableBody = body.substring(separatorEnd, tableEnd);
        return tableBody.lines().filter(line -> line.startsWith("| `")).count();
    }
}
