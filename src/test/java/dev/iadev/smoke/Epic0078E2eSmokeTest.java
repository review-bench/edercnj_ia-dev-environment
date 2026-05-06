package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.composition.CapabilityAwareComposer;
import dev.iadev.application.composition.CompositionPlan;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Epic-level acceptance test — EPIC-0078 (Context Budget Optimization).
 *
 * <p>Verifies the end-to-end contract across story-0078-0016 deliverables:
 * ADR existence, rule template existence, hard-fail default, and HARD-prune exclusion.
 */
@DisplayName("Epic0078E2eSmokeTest — context budget optimization end-to-end")
class Epic0078E2eSmokeTest {

    private static final Path ADR_PATH =
            Path.of("docs", "adr", "ADR-0033-rules-as-short-contracts.md");

    private static final Path RULE_TEMPLATE_PATH =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "_TEMPLATE-RULE.md");

    private static final Path CONTEXT_BUDGET_BASELINE =
            Path.of("governance", "baselines", "context-budget.json");

    private static final Path AUDIT_SCRIPT =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "scripts",
                    "audit-context-budget.sh");

    private static ResolvedCapabilitySet capSet(List<String> ids) {
        return new ResolvedCapabilitySet(
                "e2e", ids.stream().map(CapabilityId::of).toList(), Map.of(), List.of());
    }

    @Test
    @DisplayName("scenario1_adr0033Exists")
    void scenario1_adr0033Exists() {
        assertThat(ADR_PATH).as("ADR-0033 must exist").exists();
        String content;
        try {
            content = Files.readString(ADR_PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Cannot read ADR-0033: " + e.getMessage(), e);
        }
        assertThat(content).as("ADR must reference 25000-token limit").contains("25,000");
        assertThat(content).as("ADR must reference story-0078-0016").contains("story-0078-0016");
    }

    @Test
    @DisplayName("scenario2_ruleTemplateExists")
    void scenario2_ruleTemplateExists() {
        assertThat(RULE_TEMPLATE_PATH).as("_TEMPLATE-RULE.md must exist").exists();
        String content;
        try {
            content = Files.readString(RULE_TEMPLATE_PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Cannot read rule template: " + e.getMessage(), e);
        }
        assertThat(content).as("template must contain Purpose block").contains("## Purpose");
        assertThat(content).as("template must contain Invariants block").contains("## Invariants");
        assertThat(content).as("template must contain Enforcement block").contains("## Enforcement");
        assertThat(content).as("template must contain Reference block").contains("## Reference");
    }

    @Test
    @DisplayName("scenario3_contextBudgetBaselineHas25kLimit")
    void scenario3_contextBudgetBaselineHas25kLimit() throws IOException {
        assertThat(CONTEXT_BUDGET_BASELINE).as("context-budget.json must exist").exists();
        String content = Files.readString(CONTEXT_BUDGET_BASELINE, StandardCharsets.UTF_8);
        assertThat(content).as("baseline must contain limit field").contains("\"limit\"");
        assertThat(content).as("baseline must set limit to 25000").contains("25000");
    }

    @Test
    @DisplayName("scenario4_auditScriptDefaultIsHardFail")
    void scenario4_auditScriptDefaultIsHardFail() throws IOException {
        assertThat(AUDIT_SCRIPT).as("audit-context-budget.sh must exist").exists();
        String content = Files.readString(AUDIT_SCRIPT, StandardCharsets.UTF_8);
        assertThat(content)
                .as("default ADVISORY must be false (hard-fail)")
                .contains("ADVISORY:-false");
    }

    @Test
    @DisplayName("scenario5_composerDefaultIsHardPrune")
    void scenario5_composerDefaultIsHardPrune() throws IOException {
        CapabilityAwareComposer composer = new CapabilityAwareComposer();
        assertThat(composer.mode())
                .as("default pruning mode must be HARD since story-0078-0016")
                .isEqualTo(CapabilityAwareComposer.PruningMode.HARD);

        Path tempDir = Files.createTempDirectory("epic0078-e2e");
        try {
            Path rule = tempDir.resolve("12-security-anti-patterns.md");
            Files.writeString(rule, "---\nrequires-capabilities: [lang.java.*]\n---\n# Rule\n");

            ResolvedCapabilitySet empty = capSet(List.of());
            CompositionPlan plan = composer.plan(empty, tempDir);
            assertThat(plan.included()).as("non-Java rule must be excluded in HARD mode").isEmpty();
            assertThat(plan.excluded()).hasSize(1);
            assertThat(plan.warnings()).isEmpty();
        } finally {
            Files.deleteIfExists(tempDir.resolve("12-security-anti-patterns.md"));
            Files.deleteIfExists(tempDir);
        }
    }
}
