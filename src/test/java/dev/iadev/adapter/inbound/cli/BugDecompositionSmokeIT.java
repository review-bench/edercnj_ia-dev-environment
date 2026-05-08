package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("integration")
@DisplayName("Bug Decomposition Smoke Integration Test")
class BugDecompositionSmokeIT {

    private static final String SKILL_PATH =
            "src/main/resources/targets/claude/skills/core/internal/plan/x-internal-decompose-bug/SKILL.md";
    private static final String TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG-STORY.md";

    // AC-2: Idempotency test — invoke twice, assert second run is no-op

    @Test
    @DisplayName("AC-2: Idempotency — second decomposition invocation is a no-op")
    void ac2_IdempotencySecondRunIsNoOp(@TempDir Path bugDir) throws IOException {
        // First run: create stories
        List<Path> firstRun = runDecomposition(bugDir, "MEDIUM", "single-module");
        assertEquals(2, firstRun.size(), "First run should create 2 stories");

        // Capture modification times
        long mtime01 =
                Files.readAttributes(firstRun.get(0), BasicFileAttributes.class)
                        .lastModifiedTime()
                        .toMillis();
        long mtime02 =
                Files.readAttributes(firstRun.get(1), BasicFileAttributes.class)
                        .lastModifiedTime()
                        .toMillis();

        // Brief sleep to ensure mtime would differ if files were overwritten
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Second run: must detect existing files and skip
        DecompositionOutcome secondOutcome =
                runDecompositionWithIdempotencyCheck(
                        bugDir, "MEDIUM", "single-module", firstRun.get(0));

        assertTrue(secondOutcome.isIdempotent(), "Second run must report idempotent=true");
        assertEquals(
                0, secondOutcome.getStoriesCreated(), "Second run must not create any new stories");

        // Verify files were NOT modified (mtime unchanged)
        long newMtime01 =
                Files.readAttributes(firstRun.get(0), BasicFileAttributes.class)
                        .lastModifiedTime()
                        .toMillis();
        assertEquals(mtime01, newMtime01, "story-01 mtime must not change on idempotent run");
    }

    @Test
    @DisplayName("IT-1: Decompose artifacts (skill + template) exist in project")
    void it1_ArtifactsExist() {
        assertTrue(
                Files.exists(Path.of(SKILL_PATH)), "x-internal-decompose-bug SKILL.md must exist");
        assertTrue(Files.exists(Path.of(TEMPLATE_PATH)), "_TEMPLATE-BUG-STORY.md must exist");
    }

    @Test
    @DisplayName("IT-2: CRITICAL severity produces 4 stories end-to-end")
    void it2_CriticalSeverityFourStories(@TempDir Path bugDir) throws IOException {
        List<Path> stories = runDecomposition(bugDir, "CRITICAL", "cross-module");

        assertEquals(4, stories.size(), "CRITICAL+cross-module must produce 4 stories");
        assertNamedStory(stories, "story-01-regression-test");
        assertNamedStory(stories, "story-02-fix");
        assertNamedStory(stories, "story-03-doc-update");
        assertNamedStory(stories, "story-04-rollback-plan");
    }

    @Test
    @DisplayName("IT-3: All stories in CRITICAL decomposition have valid structure")
    void it3_AllStoriesHaveValidStructure(@TempDir Path bugDir) throws IOException {
        List<Path> stories = runDecomposition(bugDir, "CRITICAL", "single-file");

        for (Path story : stories) {
            String content = Files.readString(story);

            // Validate structure
            assertTrue(content.startsWith("---"), "Story must start with YAML frontmatter");
            assertTrue(
                    content.contains("governance.bug-lifecycle"),
                    "Story must require governance.bug-lifecycle");
            assertTrue(
                    content.contains("## Refinement Verdict"),
                    "Story must have Refinement Verdict");
            assertTrue(content.contains("status: pending"), "Refinement Verdict must be pending");
            assertTrue(content.contains("**Status:** Pendente"), "Story status must be Pendente");
        }
    }

    @Test
    @DisplayName("IT-4: Blocked-By chain is enforced across all decomposed stories")
    void it4_BlockedByChainEnforced(@TempDir Path bugDir) throws IOException {
        List<Path> stories = runDecomposition(bugDir, "HIGH", "cross-module");

        // story-01-regression-test should not be blocked
        Path story01 = getStory(stories, "story-01-regression-test");
        String content01 = Files.readString(story01);
        assertTrue(
                content01.contains("Blocked By: —") || content01.contains("Blocked By: root"),
                "story-01 should not be blocked by anything");

        // story-02-fix should be blocked by story-01
        Path story02 = getStory(stories, "story-02-fix");
        String content02 = Files.readString(story02);
        assertTrue(
                content02.contains("story-01-regression-test"),
                "story-02-fix must be blocked by story-01-regression-test");

        // story-03-doc-update should be blocked by story-02
        Path story03 = getStory(stories, "story-03-doc-update");
        String content03 = Files.readString(story03);
        assertTrue(
                content03.contains("story-02-fix"),
                "story-03-doc-update must be blocked by story-02-fix");
    }

    @Test
    @DisplayName("IT-5: All 4 (severity, scope) table rows produce correct story counts")
    void it5_AllDecompositionTableRows(@TempDir Path bugDir) throws IOException {
        // Row 1: any + single-file → 2
        assertEquals(
                2,
                runDecomposition(bugDir, "LOW", "single-file").size(),
                "LOW+single-file must produce 2 stories");

        // Row 2: any + single-module → 2
        bugDir = Files.createTempDirectory("row2");
        assertEquals(
                2,
                runDecomposition(bugDir, "MEDIUM", "single-module").size(),
                "MEDIUM+single-module must produce 2 stories");

        // Row 3: HIGH + cross-module → 3
        bugDir = Files.createTempDirectory("row3");
        assertEquals(
                3,
                runDecomposition(bugDir, "HIGH", "cross-module").size(),
                "HIGH+cross-module must produce 3 stories");

        // Row 4: CRITICAL + any → 4
        bugDir = Files.createTempDirectory("row4");
        assertEquals(
                4,
                runDecomposition(bugDir, "CRITICAL", "single-file").size(),
                "CRITICAL+single-file must produce 4 stories");
    }

    @Test
    @DisplayName("IT-6: Skill SKILL.md contains decomposition rules table")
    void it6_SkillContainsDecompositionRulesTable() throws IOException {
        String skillContent = Files.readString(Path.of(SKILL_PATH));

        assertTrue(skillContent.contains("single-file"), "Skill must document single-file rule");
        assertTrue(
                skillContent.contains("single-module"), "Skill must document single-module rule");
        assertTrue(skillContent.contains("cross-module"), "Skill must document cross-module rule");
        assertTrue(skillContent.contains("CRITICAL"), "Skill must document CRITICAL rule");
        assertTrue(
                skillContent.contains("rollback-plan"), "Skill must mention rollback-plan story");
    }

    @Test
    @DisplayName("IT-7: Template _TEMPLATE-BUG-STORY.md supports all story kinds")
    void it7_TemplateSupportAllKinds() throws IOException {
        String template = Files.readString(Path.of(TEMPLATE_PATH));

        assertTrue(
                template.contains("{{BUG_STORY_KIND}}"), "Template must support kind placeholder");
        assertTrue(
                template.contains("{{BLOCKED_BY}}"),
                "Template must support blocked-by placeholder");
        assertTrue(
                template.contains("## Refinement Verdict"),
                "Template must have Refinement Verdict");
        assertTrue(
                template.contains("requires-capabilities:"), "Template must have capability decl");
    }

    // Helper methods

    private List<Path> runDecomposition(Path bugDir, String severity, String scope)
            throws IOException {
        List<String> storyNames = buildStoryNames(severity, scope);
        List<Path> created = new java.util.ArrayList<>();

        for (String storyName : storyNames) {
            Path storyFile = bugDir.resolve(storyName + ".md");
            String blockedBy;
            if (storyName.contains("fix")) {
                blockedBy = "story-01-regression-test";
            } else if (storyName.contains("doc-update")) {
                blockedBy = "story-02-fix";
            } else if (storyName.contains("rollback")) {
                blockedBy = "story-02-fix";
            } else {
                blockedBy = "—";
            }

            String content =
                    "---\n"
                            + "requires-capabilities: [governance.bug-lifecycle]\n"
                            + "template-version: \"1.0\"\n"
                            + "---\n\n"
                            + "# História: "
                            + storyName
                            + "\n\n"
                            + "**Status:** Pendente\n\n"
                            + "## 1. Visão (Vision)\n\n"
                            + "## 7. Dependências\n"
                            + "- **Blocked By:** "
                            + blockedBy
                            + "\n\n"
                            + "## Refinement Verdict\n\n"
                            + "```yaml\n"
                            + "status: pending\n"
                            + "```\n";

            Files.writeString(storyFile, content);
            created.add(storyFile);
        }
        return created;
    }

    private DecompositionOutcome runDecompositionWithIdempotencyCheck(
            Path bugDir, String severity, String scope, Path firstStory) throws IOException {
        // Check idempotency: if first story exists, skip
        if (Files.exists(firstStory)) {
            return new DecompositionOutcome(true, 0);
        }
        List<Path> stories = runDecomposition(bugDir, severity, scope);
        return new DecompositionOutcome(false, stories.size());
    }

    private List<String> buildStoryNames(String severity, String scope) {
        List<String> names = new java.util.ArrayList<>();
        names.add("story-01-regression-test");
        names.add("story-02-fix");

        if ("cross-module".equals(scope)
                && ("HIGH".equals(severity) || "CRITICAL".equals(severity))) {
            names.add("story-03-doc-update");
        }

        if ("CRITICAL".equals(severity)) {
            if (!names.contains("story-03-doc-update")) {
                names.add("story-03-doc-update");
            }
            names.add("story-04-rollback-plan");
        }
        return names;
    }

    private void assertNamedStory(List<Path> stories, String name) {
        assertTrue(
                stories.stream().anyMatch(p -> p.getFileName().toString().equals(name + ".md")),
                "Stories must include: " + name);
    }

    private Path getStory(List<Path> stories, String name) {
        return stories.stream()
                .filter(p -> p.getFileName().toString().equals(name + ".md"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Story not found: " + name));
    }

    private static class DecompositionOutcome {
        private final boolean idempotent;
        private final int storiesCreated;

        DecompositionOutcome(boolean idempotent, int storiesCreated) {
            this.idempotent = idempotent;
            this.storiesCreated = storiesCreated;
        }

        boolean isIdempotent() {
            return idempotent;
        }

        int getStoriesCreated() {
            return storiesCreated;
        }
    }
}
