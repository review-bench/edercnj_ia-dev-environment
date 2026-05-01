package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.ScriptsAssembler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0072AuditMutationScoreSmokeIT — audit-mutation-score.sh structural invariants")
class Epic0072AuditMutationScoreSmokeIT {

    private static final Path SCRIPTS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "scripts");

    @Nested
    @DisplayName("audit script — file structure")
    class ScriptStructure {

        @Test
        @DisplayName("scenario1_auditScript_exists")
        void scenario1_auditScript_exists() {
            assertThat(SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario2_auditScript_containsSelfCheck")
        void scenario2_auditScript_containsSelfCheck() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content).as("must implement --self-check flag").contains("--self-check");
            assertThat(content)
                    .as("must check jq availability in self-check")
                    .contains("command -v jq");
        }

        @Test
        @DisplayName("scenario3_auditScript_containsExitCodes")
        void scenario3_auditScript_containsExitCodes() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content)
                    .as("must document MUTATION_SCORE_VIOLATION")
                    .contains("MUTATION_SCORE_VIOLATION");
            assertThat(content)
                    .as("must document MUTATION_REGRESSION")
                    .contains("MUTATION_REGRESSION");
            assertThat(content)
                    .as("must document OPERATIONAL_ERROR")
                    .contains("OPERATIONAL_ERROR");
            assertThat(content)
                    .as("must document BASELINE_CORRUPT")
                    .contains("BASELINE_CORRUPT");
        }

        @Test
        @DisplayName("scenario4_auditScript_containsPathTraversalGuard")
        void scenario4_auditScript_containsPathTraversalGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content)
                    .as("must reject path traversal")
                    .contains("path traversal rejected");
            assertThat(content)
                    .as("must use realpath for path normalization")
                    .contains("realpath");
        }

        @Test
        @DisplayName("scenario5_auditScript_containsSymlinkGuard")
        void scenario5_auditScript_containsSymlinkGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content).as("must reject symlinks").contains("symlink rejected");
        }

        @Test
        @DisplayName("scenario6_auditScript_containsStagePolicyWarnToFail")
        void scenario6_auditScript_containsStagePolicyWarnToFail() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content)
                    .as("must implement WARN-then-FAIL stage policy via release_count")
                    .contains("release_count");
            assertThat(content)
                    .as("must emit WARN on first release (release_count=0)")
                    .contains("first release");
        }

        @Test
        @DisplayName("scenario7_auditScript_containsRegressionCheck")
        void scenario7_auditScript_containsRegressionCheck() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content)
                    .as("must check regression vs baseline")
                    .contains("MUTATION_REGRESSION");
            assertThat(content)
                    .as("must read regression tolerance")
                    .contains("REGRESSION_TOLERANCE");
        }

        @Test
        @DisplayName("scenario8_auditScript_containsMalformedReportGuard")
        void scenario8_auditScript_containsMalformedReportGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-mutation-score.sh").toAbsolutePath());
            assertThat(content)
                    .as("must validate required JSON fields")
                    .contains("malformed mutation report");
            assertThat(content)
                    .as("must check for 'score' field")
                    .contains("\"score\"");
            assertThat(content)
                    .as("must check for 'total_mutations' field")
                    .contains("\"total_mutations\"");
        }
    }

    @Nested
    @DisplayName("ScriptsAssembler wiring")
    class AssemblerWiring {

        @Test
        @DisplayName("scenario9_auditScript_registeredInScriptsAssembler")
        void scenario9_auditScript_registeredInScriptsAssembler() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS)
                    .as("audit-mutation-score.sh must be in AUDIT_SCRIPTS")
                    .contains("audit-mutation-score.sh");
        }

        @Test
        @DisplayName("scenario10_auditScripts_has12Scripts")
        void scenario10_auditScripts_has12Scripts() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS).hasSize(12);
        }
    }

    @Nested
    @DisplayName("Rule 05 extension")
    class Rule05Extension {

        private static final Path RULES_ROOT =
                Path.of("src", "main", "resources", "targets", "claude", "rules");

        @Test
        @DisplayName("scenario11_rule05_containsMutationScoreSection")
        void scenario11_rule05_containsMutationScoreSection() throws Exception {
            Path rule05 = RULES_ROOT.resolve("05-quality-gates.md").toAbsolutePath();
            assertThat(rule05).exists();
            String content = Files.readString(rule05);
            assertThat(content)
                    .as("Rule 05 must contain §Mutation Score Threshold")
                    .contains("Mutation Score Threshold");
            assertThat(content)
                    .as("Rule 05 must reference MUTATION_SCORE_VIOLATION")
                    .contains("MUTATION_SCORE_VIOLATION");
        }

        @Test
        @DisplayName("scenario12_rule05_containsPerformanceBudgetSection")
        void scenario12_rule05_containsPerformanceBudgetSection() throws Exception {
            Path rule05 = RULES_ROOT.resolve("05-quality-gates.md").toAbsolutePath();
            String content = Files.readString(rule05);
            assertThat(content)
                    .as("Rule 05 must contain §Performance Budget")
                    .contains("Performance Budget");
            assertThat(content)
                    .as("Rule 05 must reference perf-baseline-updates.log")
                    .contains("perf-baseline-updates.log");
        }

        @Test
        @DisplayName("scenario13_rule05_containsStagePolicyDoc")
        void scenario13_rule05_containsStagePolicyDoc() throws Exception {
            Path rule05 = RULES_ROOT.resolve("05-quality-gates.md").toAbsolutePath();
            String content = Files.readString(rule05);
            assertThat(content)
                    .as("Rule 05 must document stage policy WARN→FAIL")
                    .contains("Stage Policy");
            assertThat(content)
                    .as("Rule 05 must mention release_count")
                    .contains("release_count");
        }
    }

    @Nested
    @DisplayName("Catalog entry")
    class CatalogEntry {

        @Test
        @DisplayName("scenario14_catalogEntry_exists")
        void scenario14_catalogEntry_exists() throws Exception {
            Path catalog = Path.of("docs", "audit-gates-catalog.md").toAbsolutePath();
            assertThat(catalog).exists();
            String content = Files.readString(catalog);
            assertThat(content)
                    .as("audit-gates-catalog.md must contain audit-mutation-score.sh entry")
                    .contains("audit-mutation-score.sh");
            assertThat(content)
                    .as("catalog entry must reference MUTATION_SCORE_VIOLATION")
                    .contains("MUTATION_SCORE_VIOLATION");
        }
    }
}
