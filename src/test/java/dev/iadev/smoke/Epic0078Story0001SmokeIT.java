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
 * Smoke test — validates structural invariants for EPIC-0078 story-0078-0001
 * (Context Budget Baseline Tooling).
 *
 * <p>Verifies that: scripts are present, baseline JSON schema is valid, and
 * ScriptsAssembler.AUDIT_SCRIPTS includes both new scripts.
 */
@DisplayName("Epic0078Story0001SmokeIT — Context Budget Baseline Tooling")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class Epic0078Story0001SmokeIT {

    private static final Path SCRIPTS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "scripts");

    private static final Path BASELINE_PATH =
            Path.of("governance", "baselines", "context-budget.json");

    @Test
    @DisplayName("scenario1_measureScript_existsWithSelfCheck")
    void scenario1_measureScript_existsWithSelfCheck() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("measure-context-budget.sh");
        assertThat(script).as("measure-context-budget.sh must exist").exists();
        String content = Files.readString(script, StandardCharsets.UTF_8);
        assertThat(content).as("must implement --self-check").contains("--self-check");
        assertThat(content).as("must use bytes/4 heuristic comment").contains("bytes/4");
    }

    @Test
    @DisplayName("scenario2_auditScript_existsWithAdvisoryMode")
    void scenario2_auditScript_existsWithAdvisoryMode() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("audit-context-budget.sh");
        assertThat(script).as("audit-context-budget.sh must exist").exists();
        String content = Files.readString(script, StandardCharsets.UTF_8);
        assertThat(content).as("must implement --advisory flag").contains("--advisory");
        assertThat(content).as("must implement --self-check").contains("--self-check");
    }

    @Test
    @DisplayName("scenario3_baseline_schemaIsValid")
    void scenario3_baseline_schemaIsValid() throws IOException {
        assertThat(BASELINE_PATH).as("context-budget.json must exist").exists();
        String json = Files.readString(BASELINE_PATH, StandardCharsets.UTF_8);
        assertThat(json).as("must contain alwaysLoaded").contains("\"alwaysLoaded\"");
        assertThat(json).as("must contain measuredAt").contains("\"measuredAt\"");
        assertThat(json).as("must contain ref (40-char sha)").contains("\"ref\"");
        assertThat(json).as("must contain tolerancePct").contains("\"tolerancePct\"");
    }

    @Test
    @DisplayName("scenario4_auditScript_exitCodesDocumented")
    void scenario4_auditScript_exitCodesDocumented() throws IOException {
        Path script = SCRIPTS_ROOT.resolve("audit-context-budget.sh");
        String content = Files.readString(script, StandardCharsets.UTF_8);
        assertThat(content).as("must document exit code 0=OK").contains("0=OK");
        assertThat(content).as("must document exit code 1=CONTEXT_BUDGET_VIOLATION")
                .contains("CONTEXT_BUDGET_VIOLATION");
        assertThat(content).as("must document exit code 2=OPERATIONAL_ERROR")
                .contains("OPERATIONAL_ERROR");
        assertThat(content).as("must document exit code 3=BASELINE_CORRUPT")
                .contains("BASELINE_CORRUPT");
    }

    @Test
    @DisplayName("scenario5_assemblerList_containsBothNewScripts")
    void scenario5_assemblerList_containsBothNewScripts() {
        assertThat(dev.iadev.application.assembler.ScriptsAssembler.AUDIT_SCRIPTS)
                .as("AUDIT_SCRIPTS must include audit-context-budget.sh")
                .contains("audit-context-budget.sh")
                .as("AUDIT_SCRIPTS must include measure-context-budget.sh")
                .contains("measure-context-budget.sh")
                .as("AUDIT_SCRIPTS must have 15 entries")
                .hasSize(15);
    }
}
