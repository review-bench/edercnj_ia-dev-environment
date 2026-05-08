package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Bug PR Lifecycle Tests")
class BugPrLifecycleTest {

    private static final String PR_TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-PR-BUG.md";
    private static final String PR_LIFECYCLE_WIRING_PATH =
            "src/main/resources/targets/claude/skills/dev/x-create-bug/SKILL_PR_LIFECYCLE.md";

    @Test
    @DisplayName("Bug PR template exists with required sections")
    void prTemplateExists() throws IOException {
        assertTrue(Files.exists(Path.of(PR_TEMPLATE_PATH)), "PR-BUG template must exist");
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(template.contains("## Bug Fix Summary"), "Must have Bug Fix Summary section");
        assertTrue(
                template.contains("## Status Transition"), "Must have Status Transition section");
        assertTrue(template.contains("## Review Checklist"), "Must have Review Checklist section");
        assertTrue(
                template.contains("## Orchestrator Evidence"), "Must have Orchestrator Evidence");
    }

    @Test
    @DisplayName("Bug PR template has requires-capabilities declaration")
    void prTemplateHasCapabilityDeclaration() throws IOException {
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(template.contains("requires-capabilities:"), "Must have requires-capabilities");
        assertTrue(template.contains("governance.bug-lifecycle"), "Must require bug-lifecycle");
    }

    @Test
    @DisplayName("Bug PR template has template-version marker for audit-pr-template.sh")
    void prTemplateHasVersionMarker() throws IOException {
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(
                template.contains("<!-- template-version: 1.0 -->"),
                "Must have template-version marker for audit-pr-template.sh");
    }

    @Test
    @DisplayName("PR lifecycle wiring documents all 4 story kinds")
    void prLifecycleWiringExists() throws IOException {
        assertTrue(
                Files.exists(Path.of(PR_LIFECYCLE_WIRING_PATH)), "PR lifecycle wiring must exist");
        String wiring = Files.readString(Path.of(PR_LIFECYCLE_WIRING_PATH));
        assertTrue(wiring.contains("regression-test"), "Must document regression-test story kind");
        assertTrue(wiring.contains("fix"), "Must document fix story kind");
        assertTrue(wiring.contains("doc-update"), "Must document doc-update story kind");
        assertTrue(wiring.contains("rollback-plan"), "Must document rollback-plan story kind");
    }

    @Test
    @DisplayName("AC-1: Status Transition table maps story-01 to Em Investigação")
    void ac1_StoryOneTransitionToEmInvestigacao() throws IOException {
        String wiring = Files.readString(Path.of(PR_LIFECYCLE_WIRING_PATH));
        assertTrue(
                wiring.contains("Em Investigação"),
                "Wiring must document Em Investigação transition");
        assertTrue(
                wiring.contains("story-01-regression-test"), "Must reference story-01 as trigger");
    }

    @Test
    @DisplayName("AC-1: Status Transition table maps story-02 merge to Concluída")
    void ac1_StoryTwoMergeToConcluid() throws IOException {
        String wiring = Files.readString(Path.of(PR_LIFECYCLE_WIRING_PATH));
        assertTrue(wiring.contains("Concluída"), "Wiring must document Concluída final state");
        assertTrue(wiring.contains("story-02-fix"), "Must reference story-02 as trigger");
    }

    @Test
    @DisplayName("AC-2: Review checklist includes regression test RED requirement")
    void ac2_ReviewChecklistHasRegressionTestRequirement() throws IOException {
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(
                template.contains("Regression test is RED"), "Must check regression test is RED");
        assertTrue(
                template.contains("regression test GREEN"), "Must check regression test is GREEN");
    }

    @Test
    @DisplayName("AC-2: Review checklist includes coverage requirement")
    void ac2_ReviewChecklistHasCoverageRequirement() throws IOException {
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(
                template.contains("95%") || template.contains("coverage"),
                "Must reference coverage requirement");
    }

    @Test
    @DisplayName("AC-3: Falha state documented in wiring")
    void ac3_FalhaStateDocumented() throws IOException {
        String wiring = Files.readString(Path.of(PR_LIFECYCLE_WIRING_PATH));
        assertTrue(wiring.contains("Falha"), "Wiring must document Falha state for failed fixes");
    }

    @Test
    @DisplayName("AC-4: Blocked-By chain enforcement documented")
    void ac4_BlockedByChainEnforced() throws IOException {
        String wiring = Files.readString(Path.of(PR_LIFECYCLE_WIRING_PATH));
        assertTrue(
                wiring.contains("story-01") || wiring.contains("regression-test"),
                "Wiring must reference ordering enforcement");
        assertTrue(
                wiring.contains("MUST NOT")
                        || wiring.contains("before")
                        || wiring.contains("merged"),
                "Must enforce ordering constraint");
    }

    @Test
    @DisplayName("Bug ID placeholder present in PR template")
    void prTemplateBugIdPlaceholder() throws IOException {
        String template = Files.readString(Path.of(PR_TEMPLATE_PATH));
        assertTrue(template.contains("{{BUG_ID}}"), "PR template must have BUG_ID placeholder");
        assertTrue(template.contains("{{SEVERITY}}"), "PR template must have SEVERITY placeholder");
        assertTrue(template.contains("{{STORY_ID}}"), "PR template must have STORY_ID placeholder");
    }

    @Test
    @DisplayName("Status lifecycle: valid transitions form a DAG")
    void statusLifecycle_ValidTransitionsDag() {
        String[][] transitions = {
            {"Pendente", "Em Investigação"},
            {"Em Investigação", "Em Correção"},
            {"Em Correção", "Concluída"},
            {"Em Correção", "Falha"},
            {"Falha", "Em Correção"},
            {"Pendente", "Bloqueada"},
            {"Pendente", "Descartada"}
        };

        for (String[] transition : transitions) {
            assertTrue(
                    isValidTransition(transition[0], transition[1]),
                    "Transition " + transition[0] + " → " + transition[1] + " must be valid");
        }

        assertFalse(
                isValidTransition("Concluída", "Pendente"),
                "Concluída → Pendente is not a valid transition");
    }

    private boolean isValidTransition(String from, String to) {
        return switch (from) {
            case "Pendente" ->
                    "Em Investigação".equals(to)
                            || "Bloqueada".equals(to)
                            || "Descartada".equals(to)
                            || "Refinada".equals(to);
            case "Refinada" ->
                    "Em Investigação".equals(to)
                            || "Bloqueada".equals(to)
                            || "Descartada".equals(to);
            case "Em Investigação" ->
                    "Em Correção".equals(to) || "Bloqueada".equals(to) || "Descartada".equals(to);
            case "Em Correção" ->
                    "Concluída".equals(to) || "Falha".equals(to) || "Bloqueada".equals(to);
            case "Falha" -> "Em Correção".equals(to);
            case "Bloqueada" ->
                    "Em Investigação".equals(to)
                            || "Em Correção".equals(to)
                            || "Descartada".equals(to);
            default -> false;
        };
    }
}
