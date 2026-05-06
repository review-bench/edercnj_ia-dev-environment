package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.ScriptsAssembler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0072PerfBaselineSmokeIT — audit-perf-baseline.sh structural invariants")
class Epic0072PerfBaselineSmokeIT {

    private static final Path SCRIPTS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "scripts");

    private static final Path BASELINES_ROOT = Path.of("governance", "baselines");

    @Nested
    @DisplayName("audit script — file structure")
    class ScriptStructure {

        @Test
        @DisplayName("scenario1_auditScript_exists")
        void scenario1_auditScript_exists() {
            assertThat(SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath()).exists();
        }

        @Test
        @DisplayName("scenario2_auditScript_containsSelfCheck")
        void scenario2_auditScript_containsSelfCheck() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath());
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
                            SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath());
            assertThat(content)
                    .as("must document PERF_BASELINE_VIOLATION")
                    .contains("PERF_BASELINE_VIOLATION");
            assertThat(content).as("must document OPERATIONAL_ERROR").contains("OPERATIONAL_ERROR");
            assertThat(content).as("must document BASELINE_CORRUPT").contains("BASELINE_CORRUPT");
        }

        @Test
        @DisplayName("scenario4_auditScript_containsPathTraversalGuard")
        void scenario4_auditScript_containsPathTraversalGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath());
            assertThat(content)
                    .as("must contain path traversal rejection")
                    .contains("path traversal");
            assertThat(content).as("must use realpath for path normalization").contains("realpath");
        }

        @Test
        @DisplayName("scenario5_auditScript_containsSymlinkGuard")
        void scenario5_auditScript_containsSymlinkGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath());
            assertThat(content).as("must reject symlinks").contains("symlink rejected");
            assertThat(content).as("must check -L flag").contains("-L \"${BASELINE_PATH}\"");
        }

        @Test
        @DisplayName("scenario6_auditScript_containsSilentOverwriteDetection")
        void scenario6_auditScript_containsSilentOverwriteDetection() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-perf-baseline.sh").toAbsolutePath());
            assertThat(content)
                    .as("must detect silent overwrite via git diff")
                    .contains("baseline updated without justification");
            assertThat(content)
                    .as("must reference perf-baseline-updates.log")
                    .contains("perf-baseline-updates.log");
        }
    }

    @Nested
    @DisplayName("ScriptsAssembler wiring")
    class AssemblerWiring {

        @Test
        @DisplayName("scenario7_auditScript_registeredInScriptsAssembler")
        void scenario7_auditScript_registeredInScriptsAssembler() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS)
                    .as("audit-perf-baseline.sh must be in AUDIT_SCRIPTS")
                    .contains("audit-perf-baseline.sh");
        }

        @Test
        @DisplayName("scenario8_auditScripts_has12Scripts")
        void scenario8_auditScripts_has12Scripts() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS).hasSize(13);
        }
    }

    @Nested
    @DisplayName("Governance artifacts")
    class GovernanceArtifacts {

        @Test
        @DisplayName("scenario9_perfBaselineUpdatesLog_exists")
        void scenario9_perfBaselineUpdatesLog_exists() {
            assertThat(BASELINES_ROOT.resolve("perf-baseline-updates.log").toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario10_catalogEntry_exists")
        void scenario10_catalogEntry_exists() throws Exception {
            Path catalog = Path.of("docs", "audit-gates-catalog.md").toAbsolutePath();
            assertThat(catalog).exists();
            String content = Files.readString(catalog);
            assertThat(content)
                    .as("audit-gates-catalog.md must contain audit-perf-baseline.sh entry")
                    .contains("audit-perf-baseline.sh");
            assertThat(content)
                    .as("catalog entry must reference PERF_BASELINE_VIOLATION")
                    .contains("PERF_BASELINE_VIOLATION");
        }
    }
}
