package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.ScriptsAssembler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName(
        "Epic0072AuditContractBreakingSmokeIT — audit-contract-breaking.sh structural invariants")
class Epic0072AuditContractBreakingSmokeIT {

    private static final Path SCRIPTS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "scripts");

    @Nested
    @DisplayName("audit script — file structure")
    class ScriptStructure {

        @Test
        @DisplayName("scenario1_auditScript_exists")
        void scenario1_auditScript_exists() {
            assertThat(SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario2_auditScript_containsSelfCheck")
        void scenario2_auditScript_containsSelfCheck() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content).as("must implement --self-check flag").contains("--self-check");
            assertThat(content)
                    .as("must check jq availability in self-check")
                    .contains("command -v jq");
            assertThat(content)
                    .as("must check git availability in self-check")
                    .contains("command -v git");
        }

        @Test
        @DisplayName("scenario3_auditScript_containsExitCodes")
        void scenario3_auditScript_containsExitCodes() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must document CONTRACT_BREAKING_VIOLATION")
                    .contains("CONTRACT_BREAKING_VIOLATION");
            assertThat(content).as("must document OPERATIONAL_ERROR").contains("OPERATIONAL_ERROR");
            assertThat(content).as("must document BASELINE_CORRUPT").contains("BASELINE_CORRUPT");
        }

        @Test
        @DisplayName("scenario4_auditScript_containsPathTraversalGuard")
        void scenario4_auditScript_containsPathTraversalGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must reject path traversal in artifact paths")
                    .contains("traversal detected");
        }

        @Test
        @DisplayName("scenario5_auditScript_containsCommandInjectionGuard")
        void scenario5_auditScript_containsCommandInjectionGuard() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must have filename sanitizer against command injection")
                    .contains("rejected by sanitizer");
        }

        @Test
        @DisplayName("scenario6_auditScript_containsChangelogBreakingDetection")
        void scenario6_auditScript_containsChangelogBreakingDetection() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must detect ## Breaking entry in CHANGELOG.md diff")
                    .contains("CHANGELOG.md");
            assertThat(content).as("must match Breaking header regex").contains("Breaking");
        }

        @Test
        @DisplayName("scenario7_auditScript_containsConventionalCommitBreakingSignal")
        void scenario7_auditScript_containsConventionalCommitBreakingSignal() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must accept BREAKING CHANGE: footer as secondary signal")
                    .contains("BREAKING CHANGE:");
        }

        @Test
        @DisplayName("scenario8_auditScript_containsShallowCloneHandling")
        void scenario8_auditScript_containsShallowCloneHandling() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must detect and handle shallow clone")
                    .contains("is-shallow-repository");
        }

        @Test
        @DisplayName("scenario9_auditScript_containsAuditHistoryLog")
        void scenario9_auditScript_containsAuditHistoryLog() throws Exception {
            String content =
                    Files.readString(
                            SCRIPTS_ROOT.resolve("audit-contract-breaking.sh").toAbsolutePath());
            assertThat(content)
                    .as("must append to contract-breaking-history.log for documented breakers")
                    .contains("contract-breaking-history.log");
        }
    }

    @Nested
    @DisplayName("ScriptsAssembler wiring")
    class AssemblerWiring {

        @Test
        @DisplayName("scenario10_auditScript_registeredInScriptsAssembler")
        void scenario10_auditScript_registeredInScriptsAssembler() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS)
                    .as("audit-contract-breaking.sh must be in AUDIT_SCRIPTS")
                    .contains("audit-contract-breaking.sh");
        }

        @Test
        @DisplayName("scenario11_auditScripts_has13Scripts")
        void scenario11_auditScripts_has13Scripts() {
            assertThat(ScriptsAssembler.AUDIT_SCRIPTS).hasSize(13);
        }
    }

    @Nested
    @DisplayName("Governance artifacts")
    class GovernanceArtifacts {

        @Test
        @DisplayName("scenario12_contractBreakingHistoryLog_exists")
        void scenario12_contractBreakingHistoryLog_exists() {
            assertThat(
                            Path.of("governance", "audits", "contract-breaking-history.log")
                                    .toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario13_contractBreakingBaseline_exists")
        void scenario13_contractBreakingBaseline_exists() {
            assertThat(
                            Path.of("governance", "baselines", "contract-breaking-baseline.txt")
                                    .toAbsolutePath())
                    .exists();
        }

        @Test
        @DisplayName("scenario14_catalogEntry_exists")
        void scenario14_catalogEntry_exists() throws Exception {
            Path catalog = Path.of("docs", "audit-gates-catalog.md").toAbsolutePath();
            assertThat(catalog).exists();
            String content = Files.readString(catalog);
            assertThat(content)
                    .as("audit-gates-catalog.md must contain audit-contract-breaking.sh entry")
                    .contains("audit-contract-breaking.sh");
            assertThat(content)
                    .as("catalog entry must reference CONTRACT_BREAKING_VIOLATION")
                    .contains("CONTRACT_BREAKING_VIOLATION");
        }
    }
}
