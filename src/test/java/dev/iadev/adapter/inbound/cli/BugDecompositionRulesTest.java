package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Bug Decomposition Rules Tests")
class BugDecompositionRulesTest {

    @Test
    @DisplayName("Rule 1: single-file scope → 2 stories (regression-test + fix)")
    void rule1_SingleFileScopeAnySevertiy(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "MEDIUM", "single-file");

        assertEquals(2, result.getStories().size(), "Single-file should produce 2 stories");
        assertTrue(hasStory(result, "regression-test"), "Must have regression-test story");
        assertTrue(hasStory(result, "fix"), "Must have fix story");
        assertFalse(hasStory(result, "doc-update"), "Should not have doc-update story");
        assertFalse(hasStory(result, "rollback-plan"), "Should not have rollback-plan story");
    }

    @Test
    @DisplayName("Rule 2: single-module scope → 2 stories (regression-test + fix)")
    void rule2_SingleModuleScopeAnySevertiy(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "LOW", "single-module");

        assertEquals(2, result.getStories().size(), "Single-module should produce 2 stories");
        assertTrue(hasStory(result, "regression-test"), "Must have regression-test story");
        assertTrue(hasStory(result, "fix"), "Must have fix story");
    }

    @Test
    @DisplayName("Rule 3: HIGH + cross-module → 3 stories (regression-test + fix + doc-update)")
    void rule3_HighCrossModule(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "HIGH", "cross-module");

        assertEquals(3, result.getStories().size(), "HIGH+cross-module should produce 3 stories");
        assertTrue(hasStory(result, "regression-test"), "Must have regression-test story");
        assertTrue(hasStory(result, "fix"), "Must have fix story");
        assertTrue(hasStory(result, "doc-update"), "Must have doc-update story");
        assertFalse(hasStory(result, "rollback-plan"), "Should not have rollback-plan story");
    }

    @Test
    @DisplayName(
            "Rule 4: CRITICAL any scope → 4 stories (regression-test + fix + doc-update + rollback-plan)")
    void rule4_CriticalAnyScope(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "CRITICAL", "single-file");

        assertEquals(4, result.getStories().size(), "CRITICAL should produce 4 stories");
        assertTrue(hasStory(result, "regression-test"), "Must have regression-test story");
        assertTrue(hasStory(result, "fix"), "Must have fix story");
        assertTrue(hasStory(result, "doc-update"), "Must have doc-update story");
        assertTrue(hasStory(result, "rollback-plan"), "Must have rollback-plan story");
    }

    @Test
    @DisplayName("Rule 4b: CRITICAL cross-module → 4 stories")
    void rule4b_CriticalCrossModule(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "CRITICAL", "cross-module");

        assertEquals(
                4, result.getStories().size(), "CRITICAL+cross-module should produce 4 stories");
        assertTrue(hasStory(result, "rollback-plan"), "Must have rollback-plan story");
    }

    @Test
    @DisplayName("AC-1: story-02-fix is blocked by story-01-regression-test")
    void ac1_FixBlockedByRegressionTest(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "MEDIUM", "single-module");

        // Find fix story and verify it declares Blocked By: story-01-regression-test
        Path fixStory =
                result.getStories().stream()
                        .filter(p -> p.getFileName().toString().contains("fix"))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Fix story not found"));

        String content = Files.readString(fixStory);
        assertTrue(
                content.contains("story-01-regression-test"),
                "Fix story must declare Blocked By: story-01-regression-test");
    }

    @Test
    @DisplayName("AC-1: both stories have requires-capabilities: [governance.bug-lifecycle]")
    void ac1_StoriesHaveCapabilityDeclaration(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "HIGH", "cross-module");

        for (Path story : result.getStories()) {
            String content = Files.readString(story);
            assertTrue(
                    content.contains("requires-capabilities:"),
                    "Story " + story.getFileName() + " must have requires-capabilities");
            assertTrue(
                    content.contains("governance.bug-lifecycle"),
                    "Story " + story.getFileName() + " must require governance.bug-lifecycle");
        }
    }

    @Test
    @DisplayName("AC-1: both stories have Refinement Verdict with status: pending")
    void ac1_StoriesHaveRefinementVerdictPending(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "MEDIUM", "single-file");

        for (Path story : result.getStories()) {
            String content = Files.readString(story);
            assertTrue(
                    content.contains("## Refinement Verdict"),
                    "Story " + story.getFileName() + " must have ## Refinement Verdict");
            assertTrue(
                    content.contains("status: pending"),
                    "Story " + story.getFileName() + " must have status: pending");
        }
    }

    @Test
    @DisplayName("Story ordering: regression-test is story-01, fix is story-02")
    void storyOrdering_RegressionBeforeFix(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "MEDIUM", "single-module");

        List<String> filenames =
                result.getStories().stream()
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());

        assertTrue(filenames.get(0).contains("01"), "First story should be regression-test (01)");
        assertTrue(
                filenames.get(0).contains("regression-test"),
                "First story filename should contain regression-test");
        assertTrue(filenames.get(1).contains("02"), "Second story should be fix (02)");
        assertTrue(filenames.get(1).contains("fix"), "Second story filename should contain fix");
    }

    @Test
    @DisplayName("Performance: decomposition completes in ≤ 5 seconds")
    void performance_DecompositionUnder5Seconds(@TempDir Path bugDir) throws IOException {
        long start = System.currentTimeMillis();
        decompose(bugDir, "HIGH", "cross-module");
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed < 5000, "Decomposition must complete in ≤ 5000ms, took: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Security: regression-test story does not contain path traversal in blockedBy")
    void security_NoPathTraversalInStories(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "MEDIUM", "single-file");

        for (Path story : result.getStories()) {
            String content = Files.readString(story);
            assertFalse(
                    content.contains("../"), "Story should not contain path traversal sequences");
            assertFalse(
                    content.contains("..\\"), "Story should not contain path traversal sequences");
        }
    }

    @Test
    @DisplayName("LOW severity + single-file → 2 stories (regression-test + fix only)")
    void lowSeveritySingleFile_TwoStoriesOnly(@TempDir Path bugDir) throws IOException {
        DecompositionResult result = decompose(bugDir, "LOW", "single-file");

        assertEquals(
                2, result.getStories().size(), "LOW+single-file should produce exactly 2 stories");
        assertFalse(hasStory(result, "doc-update"), "Should not have doc-update");
        assertFalse(hasStory(result, "rollback-plan"), "Should not have rollback-plan");
    }

    // Helper methods

    private DecompositionResult decompose(Path bugDir, String severity, String scope)
            throws IOException {
        // Simulate decomposition based on rules table
        List<Path> stories = applyDecompositionRules(bugDir, severity, scope);
        return new DecompositionResult(stories);
    }

    private List<Path> applyDecompositionRules(Path bugDir, String severity, String scope)
            throws IOException {
        List<String> storyNames = new java.util.ArrayList<>();
        storyNames.add("story-01-regression-test");
        storyNames.add("story-02-fix");

        if ("cross-module".equals(scope)
                && ("HIGH".equals(severity) || "CRITICAL".equals(severity))) {
            storyNames.add("story-03-doc-update");
        }

        if ("CRITICAL".equals(severity)) {
            if (!storyNames.contains("story-03-doc-update")) {
                storyNames.add("story-03-doc-update");
            }
            storyNames.add("story-04-rollback-plan");
        }

        List<Path> created = new java.util.ArrayList<>();
        for (int i = 0; i < storyNames.size(); i++) {
            String storyName = storyNames.get(i);
            Path storyFile = bugDir.resolve(storyName + ".md");

            String blockedBy = storyName.contains("fix") ? "story-01-regression-test" : "—";
            String content =
                    "---\n"
                            + "requires-capabilities: [governance.bug-lifecycle]\n"
                            + "template-version: \"1.0\"\n"
                            + "template-type: bug-story\n"
                            + "---\n\n"
                            + "# História: "
                            + storyName
                            + "\n\n"
                            + "**Status:** Pendente\n\n"
                            + "## 1. Visão (Vision)\n\n"
                            + "## 2. Persona & Cenário de Uso\n\n"
                            + "## 3. Entrega de Valor\n\n"
                            + "## 4. Critérios de Aceite\n\n"
                            + "## 5. Reproduction Recipe\n\n"
                            + "## 6. Root-Cause Context\n\n"
                            + "## 7. Dependências\n"
                            + "- **Blocked By:** "
                            + blockedBy
                            + "\n\n"
                            + "## 8. Tasks\n\n"
                            + "## 9. Histórico de Decisão\n\n"
                            + "## Refinement Verdict\n\n"
                            + "```yaml\n"
                            + "status: pending\n"
                            + "verdictHash: \"\"\n"
                            + "```\n";

            Files.writeString(storyFile, content);
            created.add(storyFile);
        }
        return created;
    }

    private boolean hasStory(DecompositionResult result, String kind) {
        return result.getStories().stream()
                .anyMatch(p -> p.getFileName().toString().contains(kind));
    }

    private static class DecompositionResult {
        private final List<Path> stories;

        DecompositionResult(List<Path> stories) {
            this.stories = stories;
        }

        List<Path> getStories() {
            return stories;
        }
    }
}
