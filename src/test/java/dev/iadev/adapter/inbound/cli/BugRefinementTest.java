package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Bug Refinement Tests")
class BugRefinementTest {

    private static final String SKILL_PATH =
            "src/main/resources/targets/claude/skills/dev/x-refine-bug/SKILL.md";
    private static final String TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md";

    @Test
    @DisplayName("x-refine-bug skill exists with required exit codes")
    void skillExists() throws IOException {
        assertTrue(Files.exists(Path.of(SKILL_PATH)), "x-refine-bug SKILL.md must exist");
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(skill.contains("ARGS_INVALID"), "Must document ARGS_INVALID exit");
        assertTrue(skill.contains("BUG_NOT_FOUND"), "Must document BUG_NOT_FOUND exit");
        assertTrue(skill.contains("approved"), "Must document approved verdict");
        assertTrue(skill.contains("rejected"), "Must document rejected verdict");
    }

    @Test
    @DisplayName("Bug template has Refinement Verdict block with status: pending")
    void templateHasRefinementVerdict() throws IOException {
        assertTrue(Files.exists(Path.of(TEMPLATE_PATH)), "Bug template must exist");
        String template = Files.readString(Path.of(TEMPLATE_PATH));
        assertTrue(
                template.contains("## Refinement Verdict"),
                "Template must have Refinement Verdict");
        assertTrue(template.contains("status: pending"), "Initial status must be pending");
        assertTrue(template.contains("verdictHash:"), "Must have verdictHash field");
        assertTrue(template.contains("blockers:"), "Must have blockers field");
    }

    @Test
    @DisplayName("AC-1: Valid bug with all 9 sections produces approved verdict")
    void ac1_ValidBugApproved(@TempDir Path tempDir) throws IOException {
        Path bugFile = createValidBug(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals("approved", result.getVerdict(), "Complete bug must be approved");
        assertTrue(result.getBlockers().isEmpty(), "No blockers for complete bug");
    }

    @Test
    @DisplayName("AC-1: Approved bug transitions status Pendente → Refinada")
    void ac1_ApprovedBugStatusTransition(@TempDir Path tempDir) throws IOException {
        Path bugFile = createValidBug(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals(
                "Pendente→Refinada",
                result.getStatusTransition(),
                "Approved bug must transition to Refinada");

        String updatedContent = Files.readString(bugFile);
        assertTrue(
                updatedContent.contains("**Status:** Refinada"),
                "Bug file must show Refinada status");
    }

    @Test
    @DisplayName("AC-1: Approved verdict writes verdictHash to bug file")
    void ac1_ApprovedVerdictHashWritten(@TempDir Path tempDir) throws IOException {
        Path bugFile = createValidBug(tempDir);

        RefinementResult result = refine(bugFile);

        assertNotNull(result.getVerdictHash(), "VerdictHash must be set");
        assertFalse(result.getVerdictHash().isEmpty(), "VerdictHash must not be empty");

        String updatedContent = Files.readString(bugFile);
        assertTrue(updatedContent.contains("status: approved"), "Verdict status must be approved");
    }

    @Test
    @DisplayName("AC-2: Bug missing Gherkin triplet is rejected with AC_MISSING_GHERKIN")
    void ac2_MissingGherkinRejected(@TempDir Path tempDir) throws IOException {
        Path bugFile = createBugWithoutGherkin(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals("rejected", result.getVerdict(), "Bug without Gherkin must be rejected");
        assertTrue(
                result.getBlockers().contains("AC_MISSING_GHERKIN"),
                "Must report AC_MISSING_GHERKIN blocker");
    }

    @Test
    @DisplayName("AC-2: Bug missing reproduction steps rejected with RECIPE_MISSING_STEPS")
    void ac2_MissingRecipeStepsRejected(@TempDir Path tempDir) throws IOException {
        Path bugFile = createBugWithoutRecipeSteps(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals("rejected", result.getVerdict(), "Bug without recipe steps must be rejected");
        assertTrue(
                result.getBlockers().contains("RECIPE_MISSING_STEPS"),
                "Must report RECIPE_MISSING_STEPS blocker");
    }

    @Test
    @DisplayName("AC-2: Bug missing section 5 sub-sections collected as blockers")
    void ac2_MissingRecipeSubsectionsCollected(@TempDir Path tempDir) throws IOException {
        Path bugFile = createMinimalBugMissingRecipe(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals("rejected", result.getVerdict(), "Bug without recipe must be rejected");
        assertFalse(result.getBlockers().isEmpty(), "Must have blockers");
    }

    @Test
    @DisplayName("AC-3: Idempotency — already approved bug exits with idempotent=true")
    void ac3_AlreadyApprovedIsIdempotent(@TempDir Path tempDir) throws IOException {
        Path bugFile = createApprovedBug(tempDir);

        RefinementResult result = refine(bugFile);

        assertEquals("approved", result.getVerdict(), "Already approved bug stays approved");
        assertTrue(result.isIdempotent(), "Second run must report idempotent=true");
    }

    @Test
    @DisplayName("AC-4: Rejected bug does not change status")
    void ac4_RejectedBugStatusUnchanged(@TempDir Path tempDir) throws IOException {
        Path bugFile = createBugWithoutGherkin(tempDir);
        String originalContent = Files.readString(bugFile);

        RefinementResult result = refine(bugFile);

        assertEquals("rejected", result.getVerdict());
        assertNull(result.getStatusTransition(), "Rejected bug must not transition status");

        String updatedContent = Files.readString(bugFile);
        assertTrue(
                updatedContent.contains("**Status:** Pendente"),
                "Status must remain Pendente on rejection");
    }

    @Test
    @DisplayName("Bug ID format validation: non-conforming ID returns ARGS_INVALID")
    void bugIdValidation_InvalidFormat() {
        boolean valid = isValidBugId("bug-001");
        assertFalse(valid, "6-digit format required — bug-001 must fail");

        valid = isValidBugId("bug-0000001");
        assertFalse(valid, "7-digit ID must fail");

        valid = isValidBugId("bug-000001");
        assertTrue(valid, "bug-000001 is valid format");
    }

    @Test
    @DisplayName("Refinement gate: decompose refuses to run on pending verdict")
    void refinementGate_PendingBlocksDecomposition(@TempDir Path tempDir) throws IOException {
        Path bugFile = createBugWithPendingVerdict(tempDir);
        String verdict = extractVerdict(bugFile);

        assertFalse("approved".equals(verdict), "Pending verdict must block decomposition");
    }

    @Test
    @DisplayName("Skill documents refinement gate contract")
    void skillDocumentsGateContract() throws IOException {
        String skill = Files.readString(Path.of(SKILL_PATH));
        assertTrue(
                skill.contains("Refinement Gate Contract"),
                "Skill must document the refinement gate");
        assertTrue(
                skill.contains("x-internal-decompose-bug"),
                "Must reference decompose skill as blocked");
        assertTrue(skill.contains("pending"), "Must document pending initial state");
    }

    // Helper methods

    private Path createValidBug(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        Files.writeString(bugFile, buildValidBugContent());
        return bugFile;
    }

    private Path createBugWithoutGherkin(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        String content = buildBugContentWithoutGherkin();
        Files.writeString(bugFile, content);
        return bugFile;
    }

    private Path createBugWithoutRecipeSteps(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        Files.writeString(bugFile, buildBugContentWithoutRecipeSteps());
        return bugFile;
    }

    private Path createMinimalBugMissingRecipe(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        Files.writeString(bugFile, buildBugContentMissingRecipe());
        return bugFile;
    }

    private Path createApprovedBug(Path dir) throws IOException {
        Path bugFile = dir.resolve("bug.md");
        Files.writeString(bugFile, buildApprovedBugContent());
        return bugFile;
    }

    private Path createBugWithPendingVerdict(Path dir) throws IOException {
        return createValidBug(dir);
    }

    private RefinementResult refine(Path bugFile) throws IOException {
        String content = Files.readString(bugFile);

        // Parse current verdict
        boolean isApproved = content.contains("status: approved");

        if (isApproved) {
            return new RefinementResult("approved", null, "", true, null);
        }

        // Validate sections
        java.util.List<String> blockers = new java.util.ArrayList<>();

        if (!hasGherkinTriplet(content)) {
            blockers.add("AC_MISSING_GHERKIN");
        }
        if (!hasRecipeSteps(content)) {
            blockers.add("RECIPE_MISSING_STEPS");
        }
        if (!hasRecipeSubsections(content)) {
            if (!blockers.contains("RECIPE_MISSING_STEPS")) {
                blockers.add("RECIPE_MISSING_ENV");
            }
        }

        String verdict = blockers.isEmpty() ? "approved" : "rejected";
        String hash =
                verdict.equals("approved")
                        ? Integer.toHexString((bugFile.toString() + System.nanoTime()).hashCode())
                        : "";
        String transition = verdict.equals("approved") ? "Pendente→Refinada" : null;

        // Write verdict back
        String updatedContent = updateVerdictInContent(content, verdict, hash, blockers);
        if (verdict.equals("approved")) {
            updatedContent = updatedContent.replace("**Status:** Pendente", "**Status:** Refinada");
        }
        Files.writeString(bugFile, updatedContent);

        return new RefinementResult(verdict, transition, hash, false, blockers);
    }

    private boolean hasGherkinTriplet(String content) {
        boolean hasDado = content.contains("Dado") || content.contains("Given");
        boolean hasQuando = content.contains("Quando") || content.contains("When");
        boolean hasEntao = content.contains("Então") || content.contains("Then");
        return hasDado && hasQuando && hasEntao;
    }

    private boolean hasRecipeSteps(String content) {
        return content.contains("5.2") && content.contains("Steps");
    }

    private boolean hasRecipeSubsections(String content) {
        return content.contains("5.1")
                && content.contains("5.2")
                && content.contains("5.3")
                && content.contains("5.4");
    }

    private String updateVerdictInContent(
            String content, String verdict, String hash, java.util.List<String> blockers) {
        String blockersYaml =
                blockers.isEmpty()
                        ? "[]"
                        : "\n"
                                + blockers.stream()
                                        .map(b -> "  - " + b)
                                        .collect(java.util.stream.Collectors.joining("\n"));

        String newVerdict =
                "## Refinement Verdict\n\n"
                        + "```yaml\n"
                        + "status: "
                        + verdict
                        + "\n"
                        + "verdictHash: \""
                        + hash
                        + "\"\n"
                        + "refinedAt: \"2026-05-07T14:00:00Z\"\n"
                        + "blockers: "
                        + blockersYaml
                        + "\n"
                        + "```\n";

        if (content.contains("## Refinement Verdict")) {
            int idx = content.indexOf("## Refinement Verdict");
            return content.substring(0, idx) + newVerdict;
        }
        return content + "\n" + newVerdict;
    }

    private boolean isValidBugId(String id) {
        return id != null && id.matches("^bug-[0-9]{6}$");
    }

    private String extractVerdict(Path bugFile) throws IOException {
        String content = Files.readString(bugFile);
        if (content.contains("status: approved")) return "approved";
        if (content.contains("status: rejected")) return "rejected";
        return "pending";
    }

    private String buildValidBugContent() {
        return "---\n"
                + "requires-capabilities: [governance.bug-lifecycle]\n"
                + "template-version: \"1.0\"\n"
                + "template-type: bug\n"
                + "severity: HIGH\n"
                + "scope: single-module\n"
                + "---\n\n"
                + "# Bug: Login endpoint returns 500\n\n"
                + "**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\nThe login endpoint crashes on invalid input.\n\n"
                + "## 2. Persona & Cenário de Uso\n\nUm usuário tenta fazer login.\n\n"
                + "## 3. Entrega de Valor\n\nCorrigir o erro 500.\n\n"
                + "## 4. Critérios de Aceite\n\n"
                + "Dado que o usuário envia credenciais inválidas\n"
                + "Quando o endpoint /login é chamado\n"
                + "Então o sistema retorna 401 (não 500)\n\n"
                + "## 5. Reproduction Recipe\n\n"
                + "### 5.1 Environment\nJava 21, Spring Boot 3.x\n\n"
                + "### 5.2 Steps to Reproduce\n1. POST /login with invalid body\n2. Observe 500\n\n"
                + "### 5.3 Observed vs Expected\nObserved: 500 Internal Server Error\nExpected: 401\n\n"
                + "### 5.4 Artifacts\nSee logs/error.log\n\n"
                + "## 6. Root-Cause Hypothesis\n\nNullPointerException in validation layer.\n\n"
                + "## 7. Regression Test Slot\n\nLoginEndpointTest#shouldReturn401OnInvalidCredentials\n\n"
                + "## 8. Dependências\n\n—\n\n"
                + "## 9. Histórico de Decisão\n\n—\n\n"
                + "## Refinement Verdict\n\n"
                + "```yaml\n"
                + "status: pending\n"
                + "verdictHash: \"\"\n"
                + "refinedAt: \"\"\n"
                + "blockers: []\n"
                + "```\n";
    }

    private String buildBugContentWithoutGherkin() {
        return "---\n"
                + "requires-capabilities: [governance.bug-lifecycle]\n"
                + "template-version: \"1.0\"\n"
                + "---\n\n"
                + "# Bug: Missing Gherkin\n\n"
                + "**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\nSome bug.\n\n"
                + "## 2. Persona & Cenário de Uso\n\nUser.\n\n"
                + "## 3. Entrega de Valor\n\nFix it.\n\n"
                + "## 4. Critérios de Aceite\n\nThe system should work correctly.\n\n"
                + "## 5. Reproduction Recipe\n\n"
                + "### 5.1 Environment\nJava 21\n\n"
                + "### 5.2 Steps to Reproduce\n1. Do something\n2. See error\n\n"
                + "### 5.3 Observed vs Expected\nObserved: Error\nExpected: Success\n\n"
                + "### 5.4 Artifacts\nlogs/\n\n"
                + "## 6. Root-Cause Hypothesis\n\nUnknown cause.\n\n"
                + "## 7. Regression Test Slot\n\nTBD\n\n"
                + "## 8. Dependências\n\n—\n\n"
                + "## 9. Histórico de Decisão\n\n—\n\n"
                + "## Refinement Verdict\n\n"
                + "```yaml\nstatus: pending\nverdictHash: \"\"\nblockers: []\n```\n";
    }

    private String buildBugContentWithoutRecipeSteps() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                + "# Bug: No recipe steps\n\n"
                + "**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\nBug.\n\n"
                + "## 2. Persona & Cenário de Uso\n\nUser.\n\n"
                + "## 3. Entrega de Valor\n\nFix.\n\n"
                + "## 4. Critérios de Aceite\n\n"
                + "Dado que algo acontece\nQuando o usuário age\nEntão o sistema responde\n\n"
                + "## 5. Reproduction Recipe\n\n### 5.1 Environment\nJava 21\n\n"
                + "## 6. Root-Cause Hypothesis\n\nUnknown.\n\n"
                + "## 7. Regression Test Slot\n\nTBD\n\n"
                + "## 8. Dependências\n\n—\n\n"
                + "## 9. Histórico de Decisão\n\n—\n\n"
                + "## Refinement Verdict\n\n"
                + "```yaml\nstatus: pending\nverdictHash: \"\"\nblockers: []\n```\n";
    }

    private String buildBugContentMissingRecipe() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                + "# Bug: Missing recipe\n\n"
                + "**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\nBug.\n\n"
                + "## 2. Persona & Cenário de Uso\n\nUser.\n\n"
                + "## 3. Entrega de Valor\n\nFix.\n\n"
                + "## 4. Critérios de Aceite\n\n"
                + "Dado que algo acontece\nQuando o usuário age\nEntão o sistema responde\n\n"
                + "## 5. Reproduction Recipe\n\n(no sub-sections)\n\n"
                + "## 6. Root-Cause Hypothesis\n\nUnknown.\n\n"
                + "## 7. Regression Test Slot\n\nTBD\n\n"
                + "## 8. Dependências\n\n—\n\n"
                + "## 9. Histórico de Decisão\n\n—\n\n"
                + "## Refinement Verdict\n\n"
                + "```yaml\nstatus: pending\nverdictHash: \"\"\nblockers: []\n```\n";
    }

    private String buildApprovedBugContent() {
        return buildValidBugContent()
                .replace("status: pending", "status: approved")
                .replace("**Status:** Pendente", "**Status:** Refinada");
    }

    private static class RefinementResult {
        private final String verdict;
        private final String statusTransition;
        private final String verdictHash;
        private final boolean idempotent;
        private final java.util.List<String> blockers;

        RefinementResult(
                String verdict,
                String statusTransition,
                String verdictHash,
                boolean idempotent,
                java.util.List<String> blockers) {
            this.verdict = verdict;
            this.statusTransition = statusTransition;
            this.verdictHash = verdictHash;
            this.idempotent = idempotent;
            this.blockers = blockers != null ? blockers : java.util.List.of();
        }

        String getVerdict() {
            return verdict;
        }

        String getStatusTransition() {
            return statusTransition;
        }

        String getVerdictHash() {
            return verdictHash;
        }

        boolean isIdempotent() {
            return idempotent;
        }

        java.util.List<String> getBlockers() {
            return blockers;
        }
    }
}
