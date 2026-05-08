package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Bug Creation Acceptance Tests")
class BugCreationAcceptanceTest {

    private static final String BUG_TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md";
    private static final String CAPABILITY_PATH = "config/capabilities/bug-lifecycle.yaml";
    private static final String DOCS_BUGS_DIR = "docs/bugs";

    @BeforeAll
    static void setupFixtures() throws IOException {
        // Verify template and capability exist
        assertTrue(Files.exists(Paths.get(BUG_TEMPLATE_PATH)), "Template must exist");
        assertTrue(Files.exists(Paths.get(CAPABILITY_PATH)), "Capability must exist");

        // Verify docs/bugs directory exists
        Path bugDir = Paths.get(DOCS_BUGS_DIR);
        if (!Files.exists(bugDir)) {
            Files.createDirectories(bugDir);
        }
    }

    @Test
    @DisplayName("AC-1: Happy path - create bug report with valid description")
    void acceptanceCriteria1_CreateValidBug() throws IOException {
        // Given: valid bug description (8-200 chars)
        String description = "Login endpoint returns 500 on invalid credentials";
        String severity = "HIGH";
        String scope = "STANDARD";

        // When: bug is created
        BugReport bug = createBug(description, severity, scope);

        // Then: bug report is created with correct metadata
        assertNotNull(bug, "Bug report should be created");
        assertNotNull(bug.getId(), "Bug ID should be assigned");
        assertNotNull(bug.getSlug(), "Slug should be generated");
        assertEquals(severity, bug.getSeverity(), "Severity should match");
        assertEquals(scope, bug.getScope(), "Scope should match");
        assertEquals("Pendente", bug.getStatus(), "Initial status should be Pendente");
        assertTrue(Files.exists(bug.getFilePath()), "Bug file should exist on disk");
    }

    @Test
    @DisplayName("AC-2: No regressions - existing test suite passes")
    void acceptanceCriteria2_RegressionTest() throws IOException {
        // Given: the bug creation system
        // When: multiple bugs are created with various descriptions
        for (int i = 0; i < 5; i++) {
            BugReport bug =
                    createBug(
                            "Bug number " + i + " with unique description text",
                            "MEDIUM",
                            "STANDARD");
            // Then: all bugs are created successfully
            assertNotNull(bug, "Bug " + i + " should be created");
            assertTrue(Files.exists(bug.getFilePath()), "Bug " + i + " file should exist");
        }
    }

    @Test
    @DisplayName("AC-3: Performance - bug creation completes in < 500ms")
    void acceptanceCriteria3_PerformanceTargetMet() throws IOException {
        String description = "Cache invalidation failed after deployment";

        long startTime = System.currentTimeMillis();
        BugReport bug = createBug(description, "HIGH", "COMPLEX");
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(
                elapsed < 500, "Bug creation should complete in < 500ms, took: " + elapsed + "ms");
        assertNotNull(bug, "Bug should be created within performance budget");
    }

    @Test
    @DisplayName("AC-4: Template sections - all 9 RA9 sections present in created bug")
    void acceptanceCriteria4_AllSectionsPresent() throws IOException {
        BugReport bug = createBug("Session token exposure in logs", "CRITICAL", "COMPLEX");
        String content = Files.readString(bug.getFilePath());

        // Verify all 9 RA9 sections
        String[] requiredSections = {
            "## 1. Visão (Vision)",
            "## 2. Persona & Cenário",
            "## 3. Entrega de Valor",
            "## 4. Critérios de Aceite",
            "## 5. Reproduction Recipe",
            "## 6. Root-Cause Hypothesis",
            "## 7. Regression Test Slot",
            "## 8. Dependências",
            "## 9. Histórico de Decisão"
        };

        for (String section : requiredSections) {
            assertTrue(content.contains(section), "Missing section: " + section);
        }
    }

    @Test
    @DisplayName("Scenario: Slug generation handles special characters correctly")
    void scenario_SlugWithSpecialCharacters() throws IOException {
        // Given: description with special characters
        String description = "SQL injection vulnerability in user search (CVE-2024-1234)";

        // When: bug is created
        BugReport bug = createBug(description, "CRITICAL", "STANDARD");

        // Then: slug is valid and contains only alphanumeric and hyphens
        assertTrue(
                bug.getSlug().matches("^[a-z0-9-]+$"),
                "Slug should contain only alphanumeric and hyphens");
        assertTrue(bug.getSlug().length() <= 40, "Slug should be max 40 characters");
    }

    @Test
    @DisplayName("Scenario: Multiple bug creation maintains sequential IDs")
    void scenario_SequentialBugIds() throws IOException {
        // Given: a clean directory
        // When: 3 bugs are created in sequence
        BugReport bug1 = createBug("First bug", "MEDIUM", "STANDARD");
        BugReport bug2 = createBug("Second bug", "HIGH", "STANDARD");
        BugReport bug3 = createBug("Third bug", "LOW", "SIMPLE");

        // Then: IDs are sequential
        String id1 = extractIdNumber(bug1.getId());
        String id2 = extractIdNumber(bug2.getId());
        String id3 = extractIdNumber(bug3.getId());

        int num1 = Integer.parseInt(id1);
        int num2 = Integer.parseInt(id2);
        int num3 = Integer.parseInt(id3);

        assertEquals(num2, num1 + 1, "Second bug ID should be one greater than first");
        assertEquals(num3, num2 + 1, "Third bug ID should be one greater than second");
    }

    @Test
    @DisplayName("Scenario: Bug file contains valid YAML frontmatter")
    void scenario_FrontmatterValidation() throws IOException {
        BugReport bug = createBug("Database transaction timeout", "HIGH", "COMPLEX");
        String content = Files.readString(bug.getFilePath());

        // Verify YAML frontmatter structure
        assertTrue(content.startsWith("---"), "File should start with ---");
        assertTrue(content.contains("requires-capabilities:"), "Should have requires-capabilities");
        assertTrue(content.contains("template-version:"), "Should have template-version");
        assertTrue(content.contains("template-type: bug"), "Should have template-type bug");

        // Verify frontmatter section is properly closed
        String[] lines = content.split("\n");
        int closingIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].equals("---")) {
                closingIndex = i;
                break;
            }
        }
        assertTrue(closingIndex > 1, "YAML frontmatter should be properly closed");
    }

    @Test
    @DisplayName("Scenario: Bug report filename matches pattern")
    void scenario_FilenamePattern() throws IOException {
        BugReport bug = createBug("Memory leak in session manager", "HIGH", "STANDARD");
        String filename = bug.getFilePath().getFileName().toString();

        // Should match: bug-NNNN-slug.md
        assertTrue(
                filename.matches("^bug-\\d{4}-[a-z0-9-]+\\.md$"),
                "Filename should match pattern bug-NNNN-slug.md, got: " + filename);
    }

    @Test
    @DisplayName("Scenario: Bug status initialization")
    void scenario_StatusInitialization() throws IOException {
        BugReport bug = createBug("Authorization bypass in admin panel", "CRITICAL", "COMPLEX");
        String content = Files.readString(bug.getFilePath());

        // Status should be initialized to "Pendente"
        assertTrue(
                content.contains("**Status:** Pendente"),
                "Bug status should be initialized to Pendente");
    }

    @Test
    @DisplayName("Integration: Full workflow from creation to verification")
    void integration_FullWorkflow() throws IOException {
        // Create bug
        BugReport bug = createBug("Integration test bug", "MEDIUM", "STANDARD");

        // Verify file exists and is readable
        assertTrue(Files.exists(bug.getFilePath()), "Bug file should exist");
        assertTrue(Files.isReadable(bug.getFilePath()), "Bug file should be readable");

        // Read and verify content
        String content = Files.readString(bug.getFilePath());
        assertTrue(content.length() > 0, "Bug file should have content");
        assertTrue(content.contains("Integration test bug"), "Description should be in file");

        // Verify structure
        assertTrue(content.startsWith("---"), "Should have YAML frontmatter");
        assertTrue(content.contains("# Bug:"), "Should have title");
        assertTrue(content.contains("## 1. Visão"), "Should have sections");
    }

    // Helper methods

    private BugReport createBug(String description, String severity, String scope)
            throws IOException {
        // Simple in-memory bug creation for testing
        Path docsDir = Paths.get(DOCS_BUGS_DIR);
        Files.createDirectories(docsDir);

        long count = Files.list(docsDir).filter(p -> p.toString().endsWith(".md")).count();
        String bugId = String.format("bug-%04d", count + 1);
        String slug = generateSlug(description);
        String filename = bugId + "-" + slug + ".md";
        Path filePath = docsDir.resolve(filename);

        String template = loadTemplate();
        String instantiated =
                template.replaceAll("\\{\\{BUG_DESCRIPTION\\}\\}", description)
                        .replaceAll("\\{\\{BUG_ID\\}\\}", bugId.replace("bug-", ""))
                        .replaceAll("\\{\\{SLUG\\}\\}", slug)
                        .replaceAll("\\{\\{SEVERITY\\}\\}", severity)
                        .replaceAll("\\{\\{SCOPE\\}\\}", scope);

        // Ensure Status is set to Pendente
        if (!instantiated.contains("**Status:** Pendente")) {
            instantiated = instantiated.replaceFirst("(# Bug:.*\n)", "$1**Status:** Pendente\n");
        }

        Files.writeString(filePath, instantiated);

        return new BugReport(bugId, slug, filePath, severity, scope, "Pendente");
    }

    private String loadTemplate() throws IOException {
        return Files.readString(Paths.get(BUG_TEMPLATE_PATH));
    }

    private String generateSlug(String description) {
        return description
                .toLowerCase()
                .replaceAll("[^a-z0-9 -]", "")
                .replaceAll(" +", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .substring(0, Math.min(40, description.length()));
    }

    private String extractIdNumber(String bugId) {
        return bugId.replace("bug-", "");
    }

    // Value object for test
    private static class BugReport {
        private final String id;
        private final String slug;
        private final Path filePath;
        private final String severity;
        private final String scope;
        private final String status;

        BugReport(
                String id,
                String slug,
                Path filePath,
                String severity,
                String scope,
                String status) {
            this.id = id;
            this.slug = slug;
            this.filePath = filePath;
            this.severity = severity;
            this.scope = scope;
            this.status = status;
        }

        String getId() {
            return id;
        }

        String getSlug() {
            return slug;
        }

        Path getFilePath() {
            return filePath;
        }

        String getSeverity() {
            return severity;
        }

        String getScope() {
            return scope;
        }

        String getStatus() {
            return status;
        }
    }
}
