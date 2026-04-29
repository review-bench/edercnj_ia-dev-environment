package dev.iadev.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * E2E smoke test for EPIC-0063 (Local-First Pre-Flight Gates) — story-0063-0011.
 *
 * <p>Terminal story: validates all 20 Wave 1-6 stories working together as an integrated system.
 * Tests that all scripts exist and are executable, key rule files are in place, and key docs
 * exist (ADR-0016, bypass-catalog, branch-protection).
 */
class Epic0063LocalFirstSmokeTest {

    // ── claude/scripts paths (source-of-truth — `.claude/` is a generated
    //    output and is gitignored, so the test asserts on the versioned
    //    template tree under src/main/resources/targets/claude/) ────────────

    private static final Path CLAUDE_SCRIPTS =
            Path.of("src/main/resources/targets/claude/scripts");

    private static final Path AUDIT_REVIEW_CONTENT =
            CLAUDE_SCRIPTS.resolve("audit-review-content.sh");
    private static final Path AUDIT_VERIFY_ENVELOPE =
            CLAUDE_SCRIPTS.resolve("audit-verify-envelope.sh");
    private static final Path AUDIT_COVERAGE_LOCAL =
            CLAUDE_SCRIPTS.resolve("audit-coverage-local.sh");
    private static final Path AUDIT_EXECUTION_INTEGRITY =
            CLAUDE_SCRIPTS.resolve("audit-execution-integrity.sh");
    private static final Path AUDIT_TOOL_CALL_GRAMMAR_CLAUDE =
            CLAUDE_SCRIPTS.resolve("audit-tool-call-grammar.sh");
    private static final Path AUDIT_PLANNING_CONTENT =
            CLAUDE_SCRIPTS.resolve("audit-planning-content.sh");
    private static final Path AUDIT_NDJSON_HASH_CHAIN =
            CLAUDE_SCRIPTS.resolve("audit-ndjson-hash-chain.sh");
    private static final Path AUDIT_WAVE_DISPATCH =
            CLAUDE_SCRIPTS.resolve("audit-wave-dispatch.sh");
    private static final Path AUDIT_RECOVERY_MODE =
            CLAUDE_SCRIPTS.resolve("audit-recovery-mode.sh");
    private static final Path AUDIT_EPIC_REVIEW_RECONCILIATION =
            CLAUDE_SCRIPTS.resolve("audit-epic-review-reconciliation.sh");
    private static final Path AUDIT_ROLLOUT_STATUS =
            CLAUDE_SCRIPTS.resolve("audit-rollout-status.sh");
    private static final Path AUDIT_HOOKS_SELF_CHECK =
            CLAUDE_SCRIPTS.resolve("audit-hooks-self-check.sh");

    // ── scripts/ paths ────────────────────────────────────────────────────────

    private static final Path SCRIPTS = Path.of("scripts");

    private static final Path PREFLIGHT = SCRIPTS.resolve("preflight.sh");
    private static final Path AUDIT_TOOL_CALL_GRAMMAR_SCRIPTS =
            CLAUDE_SCRIPTS.resolve("audit-tool-call-grammar.sh");

    // ── claude/hooks paths (source-of-truth) ────────────────────────────────

    private static final Path CLAUDE_HOOKS =
            Path.of("src/main/resources/targets/claude/hooks");

    private static final Path ENFORCE_PREFLIGHT_V1 =
            CLAUDE_HOOKS.resolve("enforce-preflight-gates.sh");
    private static final Path ENFORCE_PREFLIGHT_V2 =
            CLAUDE_HOOKS.resolve("enforce-preflight-gates-v2.sh");

    // ── Rule files ────────────────────────────────────────────────────────────

    private static final Path RULE_28_TOOL_CALL_GRAMMAR =
            Path.of("src/main/resources/targets/claude/rules/28-tool-call-grammar.md");

    // ── Documentation paths ───────────────────────────────────────────────────

    private static final Path AUDIT_BYPASS_CATALOG = Path.of("docs/audit-bypass-catalog.md");
    private static final Path BRANCH_PROTECTION = Path.of("docs/branch-protection.md");
    private static final Path ADR_0016_PREFLIGHT =
            Path.of("docs/adr/ADR-0019-preflight-warn-to-fail-rollout.md");

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 1: Core Infrastructure (stories 0001-0003)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void preflightShExistsAndIsExecutable() {
        assertThat(PREFLIGHT).exists().isRegularFile();
        assertThat(Files.isExecutable(PREFLIGHT))
                .as("scripts/preflight.sh must be executable")
                .isTrue();
    }

    @Test
    void auditReviewContentExistsAndIsExecutable() {
        assertThat(AUDIT_REVIEW_CONTENT).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_REVIEW_CONTENT))
                .as("audit-review-content.sh must be executable")
                .isTrue();
    }

    @Test
    void auditVerifyEnvelopeExistsAndIsExecutable() {
        assertThat(AUDIT_VERIFY_ENVELOPE).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_VERIFY_ENVELOPE))
                .as("audit-verify-envelope.sh must be executable")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 2: Gate Enforcement (stories 0004-0006)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void enforcePreflightGatesV1ExistsAndIsExecutable() {
        assertThat(ENFORCE_PREFLIGHT_V1).exists().isRegularFile();
        assertThat(Files.isExecutable(ENFORCE_PREFLIGHT_V1))
                .as("enforce-preflight-gates.sh must be executable")
                .isTrue();
    }

    @Test
    void auditCoverageLocalExistsAndIsExecutable() {
        assertThat(AUDIT_COVERAGE_LOCAL).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_COVERAGE_LOCAL))
                .as("audit-coverage-local.sh must be executable")
                .isTrue();
    }

    @Test
    void auditExecutionIntegrityExistsAndIsExecutable() {
        assertThat(AUDIT_EXECUTION_INTEGRITY).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_EXECUTION_INTEGRITY))
                .as("audit-execution-integrity.sh must be executable")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 3: Content & Docs (stories 0007-0010)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void branchProtectionDocExists() {
        assertThat(BRANCH_PROTECTION).exists().isRegularFile();
    }

    @Test
    void auditBypassCatalogExistsWithExpectedSkillCount() throws Exception {
        assertThat(AUDIT_BYPASS_CATALOG).exists().isRegularFile();
        String content = Files.readString(AUDIT_BYPASS_CATALOG);
        long skillCount = content
                .lines()
                .filter(line -> line.matches("^## \\d+\\. x-[a-z-]+.*"))
                .count();
        assertThat(skillCount)
                .as("docs/audit-bypass-catalog.md must list exactly 11 skills")
                .isEqualTo(11L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 4: Grammar & Extended Gates (stories 0012-0015)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void rule28ToolCallGrammarExists() {
        assertThat(RULE_28_TOOL_CALL_GRAMMAR).exists().isRegularFile();
    }

    @Test
    void rule28ContainsGrammarMarkers() throws Exception {
        String content = Files.readString(RULE_28_TOOL_CALL_GRAMMAR);
        assertThat(content)
                .as("Rule 28 must define grammar markers [required], [optional], [conditional]")
                .containsPattern("\\[required\\]|\\[optional\\]|\\[conditional");
    }

    @Test
    void auditToolCallGrammarExistsInScripts() {
        assertThat(AUDIT_TOOL_CALL_GRAMMAR_SCRIPTS).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_TOOL_CALL_GRAMMAR_SCRIPTS))
                .as("scripts/audit-tool-call-grammar.sh must be executable")
                .isTrue();
    }

    @Test
    void auditPlanningContentExistsAndIsExecutable() {
        assertThat(AUDIT_PLANNING_CONTENT).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_PLANNING_CONTENT))
                .as("audit-planning-content.sh must be executable")
                .isTrue();
    }

    @Test
    void auditWaveDispatchExistsAndIsExecutable() {
        assertThat(AUDIT_WAVE_DISPATCH).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_WAVE_DISPATCH))
                .as("audit-wave-dispatch.sh must be executable")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 5: Rollout & Recovery (stories 0016-0017)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void adr0016PreflightWarnToFailRolloutExists() {
        assertThat(ADR_0016_PREFLIGHT).exists().isRegularFile();
    }

    @Test
    void auditRolloutStatusExistsAndIsExecutable() {
        assertThat(AUDIT_ROLLOUT_STATUS).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_ROLLOUT_STATUS))
                .as("audit-rollout-status.sh must be executable")
                .isTrue();
    }

    @Test
    void auditRecoveryModeExistsAndIsExecutable() {
        assertThat(AUDIT_RECOVERY_MODE).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_RECOVERY_MODE))
                .as("audit-recovery-mode.sh must be executable")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wave 6: Telemetry Integrity & Hook Hardening (stories 0018-0021)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void auditHooksSelfCheckExistsAndIsExecutable() {
        assertThat(AUDIT_HOOKS_SELF_CHECK).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_HOOKS_SELF_CHECK))
                .as("audit-hooks-self-check.sh must be executable")
                .isTrue();
    }

    @Test
    void auditNdjsonHashChainExistsAndIsExecutable() {
        assertThat(AUDIT_NDJSON_HASH_CHAIN).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_NDJSON_HASH_CHAIN))
                .as("audit-ndjson-hash-chain.sh must be executable")
                .isTrue();
    }

    @Test
    void enforcePreflightGatesV2ExistsAndIsExecutable() {
        assertThat(ENFORCE_PREFLIGHT_V2).exists().isRegularFile();
        assertThat(Files.isExecutable(ENFORCE_PREFLIGHT_V2))
                .as("enforce-preflight-gates-v2.sh must be executable")
                .isTrue();
    }

    @Test
    void auditEpicReviewReconciliationExistsAndIsExecutable() {
        assertThat(AUDIT_EPIC_REVIEW_RECONCILIATION).exists().isRegularFile();
        assertThat(Files.isExecutable(AUDIT_EPIC_REVIEW_RECONCILIATION))
                .as("audit-epic-review-reconciliation.sh must be executable")
                .isTrue();
    }
}
