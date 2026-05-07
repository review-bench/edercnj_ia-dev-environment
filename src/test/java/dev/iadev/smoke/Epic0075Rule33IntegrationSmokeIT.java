package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0075Rule33IntegrationSmokeIT — Rule 33 + x-implement-epic Phase 5 integration")
class Epic0075Rule33IntegrationSmokeIT {

    // EPIC-0078: Rule 33 content migrated to governance KP ai-memory-production.md.
    private static final Path RULE_FILE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "rules",
                    "ai-memory-production.md");

    private static final Path X_EPIC_IMPLEMENT_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "dev",
                    "x-implement-epic",
                    "SKILL.md");

    private static final Path CAPABILITY_FILE =
            Path.of("capabilities", "governance", "ai-memory.yaml");

    private String read(Path p) throws IOException {
        assertThat(p).as(p + " must exist").exists();
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    // ── Rule 33 structural checks ────────────────────────────────────────────

    @Test
    @DisplayName("scenario1_rule33_exists")
    void scenario1_rule33_exists() {
        assertThat(RULE_FILE).as("Rule 33 file must exist").exists();
    }

    @Test
    @DisplayName("scenario2_rule33_mandatoryInvocationSection")
    void scenario2_rule33_mandatoryInvocationSection() throws IOException {
        String rule = read(RULE_FILE);
        assertThat(rule).as("must declare mandatory invocation").contains("MANDATORY TOOL CALL");
        assertThat(rule)
                .as("must reference x-internal-summarize-epic")
                .contains("x-internal-summarize-epic");
        assertThat(rule).as("must state Rule 24 link").contains("Rule 24");
    }

    @Test
    @DisplayName("scenario3_rule33_backwardCompatibilitySection")
    void scenario3_rule33_backwardCompatibilitySection() throws IOException {
        String rule = read(RULE_FILE);
        assertThat(rule).as("must declare backward compatibility with Rule 19").contains("Rule 19");
        assertThat(rule).as("must state safe default (disabled)").contains("disabled");
    }

    @Test
    @DisplayName("scenario4_rule33_auditSelfCheckDeclared")
    void scenario4_rule33_auditSelfCheckDeclared() throws IOException {
        String rule = read(RULE_FILE);
        assertThat(rule)
                .as("must reference audit-memory-coverage.sh")
                .contains("audit-memory-coverage.sh");
        assertThat(rule).as("must declare --self-check").contains("--self-check");
        assertThat(rule)
                .as("must declare RULE_33_ENFORCEMENT_BROKEN")
                .contains("RULE_33_ENFORCEMENT_BROKEN");
    }

    // ── x-implement-epic Phase 5 integration ─────────────────────────────────

    @Test
    @DisplayName("scenario5_xEpicImplement_phase5HasMemorySummaryCall")
    void scenario5_xEpicImplement_phase5HasMemorySummaryCall() throws IOException {
        String skill = read(X_EPIC_IMPLEMENT_SKILL);
        int phase5Start = skill.indexOf("## Phase 5");
        assertThat(phase5Start).as("Phase 5 must exist in x-implement-epic").isGreaterThan(0);

        String phase5Body = skill.substring(phase5Start);
        assertThat(phase5Body)
                .as("Phase 5 must invoke x-internal-summarize-epic")
                .contains("x-internal-summarize-epic");
        assertThat(phase5Body)
                .as("Phase 5 must declare MANDATORY TOOL CALL")
                .contains("MANDATORY TOOL CALL");
        assertThat(phase5Body).as("Phase 5 must reference Rule 33").contains("Rule 33");
    }

    @Test
    @DisplayName("scenario6_xEpicImplement_phase5MemoryCallIsConditional")
    void scenario6_xEpicImplement_phase5MemoryCallIsConditional() throws IOException {
        String skill = read(X_EPIC_IMPLEMENT_SKILL);
        int phase5Start = skill.indexOf("## Phase 5");
        String phase5Body = skill.substring(phase5Start);

        assertThat(phase5Body)
                .as("memory call must be conditional on ai_memory_enabled")
                .containsAnyOf("ai_memory_enabled", "governance.ai-memory", "[conditional:");
    }

    @Test
    @DisplayName("scenario7_xEpicImplement_phase5ExitCodeHandling")
    void scenario7_xEpicImplement_phase5ExitCodeHandling() throws IOException {
        String skill = read(X_EPIC_IMPLEMENT_SKILL);
        int phase5Start = skill.indexOf("## Phase 5");
        String phase5Body = skill.substring(phase5Start);

        assertThat(phase5Body)
                .as("must handle MANUAL_REFINEMENT_PRESENT exit code (7)")
                .contains("MANUAL_REFINEMENT_PRESENT");
    }

    // ── capability file ──────────────────────────────────────────────────────

    @Test
    @DisplayName("scenario8_aiMemoryCapabilityFile_exists")
    void scenario8_aiMemoryCapabilityFile_exists() {
        assertThat(CAPABILITY_FILE)
                .as("capabilities/governance/ai-memory.yaml must exist")
                .exists();
    }
}
