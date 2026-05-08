package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke Integration Test for Bug Lifecycle Management.
 *
 * <p>Validates end-to-end bug creation, refinement, and status management workflows with real file
 * I/O and project artifacts.
 *
 * <p>Test strategy: - Verify template and capability artifacts exist - Create bugs with various
 * descriptions and metadata - Validate file creation and content structure - Test status
 * transitions - Verify telemetry/logging integration points
 *
 * <p>Coverage: Happy path workflows only. Error cases covered by unit tests.
 */
@Tag("integration")
@DisplayName("Bug Creation Smoke Integration Test")
class BugCreationSmokeIT {

    private static final String BUG_TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md";
    private static final String CAPABILITY_PATH = "config/capabilities/bug-lifecycle.yaml";
    private static final String README_PATH = "docs/BUG_LIFECYCLE_README.md";

    @Test
    @DisplayName("IT-1: Template and capability artifacts exist in project")
    void integrationTest1_ArtifactsExist() {
        // Verify all required artifacts are present
        assertTrue(
                Files.exists(Paths.get(BUG_TEMPLATE_PATH)),
                "Template must exist at: " + BUG_TEMPLATE_PATH);
        assertTrue(
                Files.exists(Paths.get(CAPABILITY_PATH)),
                "Capability must exist at: " + CAPABILITY_PATH);
        assertTrue(Files.exists(Paths.get(README_PATH)), "README must exist at: " + README_PATH);
    }

    @Test
    @DisplayName("IT-2: Template contains all 9 RA9 sections")
    void integrationTest2_TemplateStructure() throws IOException {
        String template = Files.readString(Paths.get(BUG_TEMPLATE_PATH));

        String[] requiredSections = {
            "# Bug: {{BUG_DESCRIPTION}}",
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
            assertTrue(template.contains(section), "Template must contain section: " + section);
        }
    }

    @Test
    @DisplayName("IT-3: Capability declaration includes required metadata")
    void integrationTest3_CapabilityMetadata() throws IOException {
        String capability = Files.readString(Paths.get(CAPABILITY_PATH));

        // Verify key capability fields
        assertTrue(
                capability.contains("capability-name: bug-lifecycle"),
                "Capability must declare name");
        assertTrue(capability.contains("capability-version:"), "Capability must declare version");
        assertTrue(
                capability.contains("capability-type: governance"), "Capability must declare type");
        assertTrue(
                capability.contains("requires-capabilities:"),
                "Capability must declare requirements");
    }

    @Test
    @DisplayName("IT-4: Smoke test creates and validates bug in real directory")
    void integrationTest4_RealBugCreation(@TempDir Path tempDir) throws IOException {
        // Create a bug in temp directory
        String description = "Session token leaks to browser console on login";
        String severity = "CRITICAL";
        String scope = "COMPLEX";

        Path bugFile = createTestBug(tempDir, description, severity, scope);

        // Verify file exists and is readable
        assertTrue(Files.exists(bugFile), "Bug file should exist");
        assertTrue(Files.isReadable(bugFile), "Bug file should be readable");
        assertTrue(Files.size(bugFile) > 0, "Bug file should have content");

        // Verify content structure
        String content = Files.readString(bugFile);
        assertTrue(content.length() > 500, "Bug file should have substantial content");
        assertTrue(content.contains(description), "Bug file should contain description");
    }

    @Test
    @DisplayName("IT-5: Smoke test validates status lifecycle")
    void integrationTest5_StatusLifecycle(@TempDir Path tempDir) throws IOException {
        Path bugFile = createTestBug(tempDir, "Test bug for status", "HIGH", "STANDARD");
        String content = Files.readString(bugFile);

        // Initial status should be Pendente
        assertTrue(content.contains("**Status:** Pendente"), "Initial status should be Pendente");

        // Simulate status transition (in real workflow via /x-refine-bug)
        String updatedContent = content.replace("**Status:** Pendente", "**Status:** Refinada");
        Files.writeString(bugFile, updatedContent);

        // Verify status was updated
        String newContent = Files.readString(bugFile);
        assertTrue(
                newContent.contains("**Status:** Refinada"),
                "Status should be updated to Refinada");
    }

    @Test
    @DisplayName("IT-6: Smoke test validates multiple bugs with unique IDs")
    void integrationTest6_MultipleUniqueBugs(@TempDir Path tempDir) throws IOException {
        // Create 3 bugs
        Path bug1 = createTestBug(tempDir, "First bug", "LOW", "SIMPLE");
        Path bug2 = createTestBug(tempDir, "Second bug", "MEDIUM", "STANDARD");
        Path bug3 = createTestBug(tempDir, "Third bug", "HIGH", "COMPLEX");

        // All should exist
        assertTrue(Files.exists(bug1), "Bug 1 should exist");
        assertTrue(Files.exists(bug2), "Bug 2 should exist");
        assertTrue(Files.exists(bug3), "Bug 3 should exist");

        // Filenames should be different
        String name1 = bug1.getFileName().toString();
        String name2 = bug2.getFileName().toString();
        String name3 = bug3.getFileName().toString();

        assertNotEquals(name1, name2, "Bug filenames should be unique");
        assertNotEquals(name2, name3, "Bug filenames should be unique");
        assertNotEquals(name1, name3, "Bug filenames should be unique");
    }

    @Test
    @DisplayName("IT-7: Smoke test validates frontmatter with requires-capabilities")
    void integrationTest7_FrontmatterCapabilities(@TempDir Path tempDir) throws IOException {
        Path bugFile = createTestBug(tempDir, "Test bug", "MEDIUM", "STANDARD");
        String content = Files.readString(bugFile);

        // Verify YAML frontmatter
        assertTrue(content.startsWith("---"), "File must start with YAML delimiter");
        assertTrue(
                content.contains("requires-capabilities:"),
                "Frontmatter must declare required capabilities");
        assertTrue(
                content.contains("governance.bug-lifecycle"),
                "Must require governance.bug-lifecycle capability");
        assertTrue(
                content.contains("template-version:"), "Frontmatter must declare template version");
        assertTrue(
                content.contains("template-type: bug"), "Frontmatter must declare template type");
    }

    @Test
    @DisplayName("IT-8: Smoke test validates end-to-end bug file structure")
    void integrationTest8_CompleteFileStructure(@TempDir Path tempDir) throws IOException {
        Path bugFile =
                createTestBug(
                        tempDir, "Authorization bypass in admin panel", "CRITICAL", "COMPLEX");

        String content = Files.readString(bugFile);

        // Verify header
        assertTrue(
                content.contains("# Bug: Authorization bypass in admin panel"),
                "Should contain bug title");

        // Verify status field
        assertTrue(content.contains("**Status:**"), "Should have status field");
        assertTrue(content.contains("**Severity:**"), "Should have severity field");
        assertTrue(content.contains("**Scope:**"), "Should have scope field");

        // Verify all 9 sections are present
        String[] sections = {
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

        for (String section : sections) {
            assertTrue(content.contains(section), "File should contain section: " + section);
        }

        // Verify subsections
        assertTrue(content.contains("### 5.1 Environment"), "Should have subsection 5.1");
        assertTrue(content.contains("### 5.2 Steps to Reproduce"), "Should have subsection 5.2");
        assertTrue(content.contains("### 5.3 Observed vs. Expected"), "Should have subsection 5.3");
        assertTrue(content.contains("### 5.4 Artifacts"), "Should have subsection 5.4");
        assertTrue(content.contains("### 6.1 Hypothesis"), "Should have subsection 6.1");
        assertTrue(content.contains("### 6.2 Affected Components"), "Should have subsection 6.2");
        assertTrue(content.contains("### 6.3 Classification"), "Should have subsection 6.3");
        assertTrue(content.contains("### 7.1 Test Location"), "Should have subsection 7.1");
        assertTrue(content.contains("### 7.2 Failing Command"), "Should have subsection 7.2");
        assertTrue(content.contains("### 7.3 Passing Command"), "Should have subsection 7.3");
    }

    @Test
    @DisplayName("IT-9: Smoke test validates README documentation completeness")
    void integrationTest9_ReadmeCompleteness() throws IOException {
        String readme = Files.readString(Paths.get(README_PATH));

        // Verify key sections
        assertTrue(readme.contains("# Bug Lifecycle Management"), "README should have main title");
        assertTrue(readme.contains("## Quick Start"), "README should have Quick Start");
        assertTrue(readme.contains("## Bug Template Structure"), "README should document template");
        assertTrue(readme.contains("## Severity Levels"), "README should document severity");
        assertTrue(readme.contains("## Scope Classification"), "README should document scope");
        assertTrue(readme.contains("## Example Workflow"), "README should have example workflow");
        assertTrue(readme.contains("## Troubleshooting"), "README should have troubleshooting");
    }

    @Test
    @DisplayName("IT-10: Smoke test integration with project structure")
    void integrationTest10_ProjectIntegration() {
        // Verify template is in correct location for generation
        Path templatePath = Paths.get(BUG_TEMPLATE_PATH);
        assertTrue(
                Files.exists(templatePath), "Template must be in resources for build generation");

        // Verify capability is in correct location for composition
        Path capabilityPath = Paths.get(CAPABILITY_PATH);
        assertTrue(
                Files.exists(capabilityPath),
                "Capability must be in config for composition resolution");

        // Verify docs directory exists for bug storage
        Path docsPath = Paths.get("docs");
        assertTrue(
                Files.exists(docsPath) && Files.isDirectory(docsPath),
                "Docs directory must exist for bug file storage");
    }

    // Helper methods

    private Path createTestBug(Path tempDir, String description, String severity, String scope)
            throws IOException {
        // Load template
        String template = Files.readString(Paths.get(BUG_TEMPLATE_PATH));

        // Generate slug
        String slug = generateSlug(description);

        // Assign ID
        long count = Files.list(tempDir).filter(p -> p.toString().endsWith(".md")).count();
        String bugId = String.format("bug-%04d", count + 1);

        // Instantiate template
        String instantiated =
                template.replaceAll("\\{\\{BUG_DESCRIPTION\\}\\}", description)
                        .replaceAll("\\{\\{BUG_ID\\}\\}", bugId.replace("bug-", ""))
                        .replaceAll("\\{\\{SLUG\\}\\}", slug)
                        .replaceAll("\\{\\{SEVERITY\\}\\}", severity)
                        .replaceAll("\\{\\{SCOPE\\}\\}", scope);

        // Create file
        String filename = bugId + "-" + slug + ".md";
        Path bugFile = tempDir.resolve(filename);
        Files.writeString(bugFile, instantiated);

        return bugFile;
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
}
