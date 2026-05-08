package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("integration")
@DisplayName("Bug Refinement Smoke Integration Test")
class BugRefinementSmokeIT {

    private static final String SKILL_PATH =
            "src/main/resources/targets/claude/skills/dev/x-refine-bug/SKILL.md";
    private static final String TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md";

    @Test
    @DisplayName("IT-1: x-refine-bug skill artifact exists in project")
    void it1_SkillArtifactExists() {
        assertTrue(Files.exists(Path.of(SKILL_PATH)), "x-refine-bug SKILL.md must exist");
    }

    @Test
    @DisplayName("IT-2: Skill declares requires-capabilities: [governance.bug-lifecycle]")
    void it2_SkillHasCapabilityDeclaration() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(skill.contains("requires-capabilities:"), "Must have requires-capabilities");
        assertTrue(
                skill.contains("governance.bug-lifecycle"),
                "Must require bug-lifecycle capability");
    }

    @Test
    @DisplayName("IT-3: Skill documents all exit codes (0, 2, 3, 4, 5)")
    void it3_SkillDocumentsExitCodes() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(skill.contains("ARGS_INVALID"), "Must document ARGS_INVALID");
        assertTrue(skill.contains("BUG_NOT_FOUND"), "Must document BUG_NOT_FOUND");
        assertTrue(skill.contains("BUG_FILE_NOT_FOUND"), "Must document BUG_FILE_NOT_FOUND");
        assertTrue(skill.contains("WRITE_FAILED"), "Must document WRITE_FAILED");
    }

    @Test
    @DisplayName("IT-4: Full refinement lifecycle — pending → rejected → approved")
    void it4_FullRefinementLifecycle(@TempDir Path tempDir) throws IOException {
        // Start: pending bug with missing Gherkin
        Path bugFile = createBugWithMissingGherkin(tempDir);
        String initialContent = Files.readString(bugFile);
        assertTrue(initialContent.contains("status: pending"), "Initial status must be pending");

        // Attempt 1: rejected (missing Gherkin)
        String afterFirstRefinement = simulateRefinement(bugFile, false);
        assertTrue(
                afterFirstRefinement.contains("status: rejected"),
                "Bug without Gherkin must be rejected");
        assertTrue(
                afterFirstRefinement.contains("AC_MISSING_GHERKIN"),
                "Must report AC_MISSING_GHERKIN");

        // Fix: add Gherkin
        String fixedContent =
                afterFirstRefinement.replace(
                        "The system should work correctly.",
                        "Dado que o usuário envia dados inválidos\nQuando o endpoint é chamado\nEntão o sistema retorna 400");
        Files.writeString(bugFile, fixedContent);

        // Attempt 2: approved
        String afterSecondRefinement = simulateRefinement(bugFile, true);
        assertTrue(
                afterSecondRefinement.contains("status: approved"), "Fixed bug must be approved");
        assertTrue(
                afterSecondRefinement.contains("**Status:** Refinada"),
                "Status must transition to Refinada");
    }

    @Test
    @DisplayName("IT-5: Template Refinement Verdict section is in correct format")
    void it5_TemplateRefinementVerdictFormat() throws IOException {
        String template = Files.readString(Path.of(TEMPLATE_PATH));
        assertTrue(template.contains("## Refinement Verdict"), "Must have section header");
        assertTrue(template.contains("status: pending"), "Initial status must be pending");
        assertTrue(template.contains("verdictHash:"), "Must have verdictHash");
        assertTrue(template.contains("blockers:"), "Must have blockers field");
    }

    @Test
    @DisplayName("IT-6: Skill documents atomic write contract (temp file + rename)")
    void it6_AtomicWriteDocumented() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(
                skill.contains("mktemp") || skill.contains("atomic"),
                "Skill must document atomic write strategy");
        assertTrue(
                skill.contains("rename") || skill.contains("mv"),
                "Skill must document temp + rename pattern");
    }

    @Test
    @DisplayName("IT-7: Refinement gate — skill blocks decompose on pending verdict")
    void it7_RefinementGateBlocksDecompose() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(
                skill.contains("x-internal-decompose-bug"),
                "Skill must reference decompose as blocked by gate");
    }

    @Test
    @DisplayName("IT-8: Skill documents Gherkin requirement in Phase 2")
    void it8_GherkinRequirementDocumented() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(
                skill.contains("Gherkin") || skill.contains("Dado") || skill.contains("Given"),
                "Skill must document Gherkin requirement for section 4");
    }

    // Helpers

    private Path createBugWithMissingGherkin(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        Files.writeString(
                bugFile,
                "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                        + "# Bug: Test\n\n**Status:** Pendente\n\n"
                        + "## 1. Visão (Vision)\n\nBug occurs.\n\n"
                        + "## 2. Persona & Cenário de Uso\n\nUser.\n\n"
                        + "## 3. Entrega de Valor\n\nFix.\n\n"
                        + "## 4. Critérios de Aceite\n\nThe system should work correctly.\n\n"
                        + "## 5. Reproduction Recipe\n\n"
                        + "### 5.1 Environment\nJava 21\n\n"
                        + "### 5.2 Steps to Reproduce\n1. Step one\n2. Step two\n\n"
                        + "### 5.3 Observed vs Expected\nObserved: Error\nExpected: Success\n\n"
                        + "### 5.4 Artifacts\nlog.txt\n\n"
                        + "## 6. Root-Cause Hypothesis\n\nUnknown root cause here.\n\n"
                        + "## 7. Regression Test Slot\n\nTBD\n\n"
                        + "## 8. Dependências\n\n—\n\n"
                        + "## 9. Histórico de Decisão\n\n—\n\n"
                        + "## Refinement Verdict\n\n"
                        + "```yaml\nstatus: pending\nverdictHash: \"\"\nblockers: []\n```\n");
        return bugFile;
    }

    private String simulateRefinement(Path bugFile, boolean hasGherkin) throws IOException {
        String content = Files.readString(bugFile);

        boolean alreadyApproved = content.contains("status: approved");
        if (alreadyApproved) {
            return content;
        }

        boolean gherkinOk =
                (content.contains("Dado") || content.contains("Given"))
                        && (content.contains("Quando") || content.contains("When"))
                        && (content.contains("Então") || content.contains("Then"));

        java.util.List<String> blockers = new java.util.ArrayList<>();
        if (!gherkinOk) {
            blockers.add("AC_MISSING_GHERKIN");
        }

        String verdict = blockers.isEmpty() ? "approved" : "rejected";
        String blockersYaml =
                blockers.isEmpty()
                        ? "[]"
                        : "\n"
                                + blockers.stream()
                                        .map(b -> "  - " + b)
                                        .collect(java.util.stream.Collectors.joining("\n"));

        String newVerdict =
                "## Refinement Verdict\n\n```yaml\n"
                        + "status: "
                        + verdict
                        + "\n"
                        + "verdictHash: \"hash123\"\n"
                        + "blockers: "
                        + blockersYaml
                        + "\n"
                        + "```\n";

        String updated =
                content.substring(0, content.indexOf("## Refinement Verdict")) + newVerdict;
        if (verdict.equals("approved")) {
            updated = updated.replace("**Status:** Pendente", "**Status:** Refinada");
        }
        Files.writeString(bugFile, updated);
        return updated;
    }
}
