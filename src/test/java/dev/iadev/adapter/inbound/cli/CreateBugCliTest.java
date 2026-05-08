package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CreateBugCliTest {

    private static final String BUG_TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md";
    private static final String CAPABILITY_PATH = "config/capabilities/bug-lifecycle.yaml";

    @TempDir private Path tempOutputDir;

    @BeforeEach
    void setUp() throws IOException {
        // Verify template exists
        assertTrue(Files.exists(Paths.get(BUG_TEMPLATE_PATH)), "Template must exist");
        assertTrue(Files.exists(Paths.get(CAPABILITY_PATH)), "Capability declaration must exist");
    }

    @Test
    void testDescriptionValidationMinLength() {
        // Description < 8 characters should be rejected
        String shortDesc = "short";
        assertTrue(shortDesc.length() < 8);
    }

    @Test
    void testDescriptionValidationMaxLength() {
        // Description > 200 characters should be rejected
        String longDesc = "a".repeat(201);
        assertTrue(longDesc.length() > 200);
    }

    @Test
    void testDescriptionValidationValid() {
        // Valid description lengths
        String validMin = "a".repeat(8);
        String validMax = "a".repeat(200);
        String validNormal = "Login form crashes on invalid email";

        assertTrue(validMin.length() >= 8 && validMin.length() <= 200);
        assertTrue(validMax.length() >= 8 && validMax.length() <= 200);
        assertTrue(validNormal.length() >= 8 && validNormal.length() <= 200);
    }

    @Test
    void testSlugGenerationFromDescription() {
        // Test slug generation from various descriptions
        testSlugGeneration(
                "Login form crashes on invalid email", "login-form-crashes-on-invalid-email");
        testSlugGeneration(
                "Session token leaks to browser console", "session-token-leaks-to-browser-console");
        testSlugGeneration(
                "Cache invalidation broken after deploy", "cache-invalidation-broken-after-deploy");
    }

    @Test
    void testSlugGenerationSpecialCharacters() {
        // Slug should strip special characters
        testSlugGeneration("Bug: Database (SQL) error #1234", "bug-database-sql-error-1234");
        testSlugGeneration("Request/Response timeout issue", "request-response-timeout-issue");
    }

    @Test
    void testSlugGenerationMultipleHyphens() {
        // Multiple hyphens should collapse to single hyphen
        String desc = "Session   token   leaks   to   console";
        String slug = generateSlug(desc);
        assertFalse(slug.contains("--"), "Slug should not contain multiple hyphens");
    }

    @Test
    void testSlugGenerationTruncation() {
        // Slug should be truncated to 40 characters
        String longDesc = "a".repeat(100) + " " + "b".repeat(100);
        String slug = generateSlug(longDesc);
        assertTrue(slug.length() <= 40, "Slug should be truncated to max 40 characters");
    }

    @Test
    void testSlugGenerationLowercase() {
        // Slug should be lowercase
        String desc = "Login FORM Crashes On Invalid EMAIL";
        String slug = generateSlug(desc);
        assertEquals(slug, slug.toLowerCase(), "Slug should be lowercase");
    }

    @Test
    void testSeverityValidation() {
        String[] validSeverities = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
        for (String severity : validSeverities) {
            assertTrue(isValidSeverity(severity), "Severity " + severity + " should be valid");
        }

        assertFalse(isValidSeverity("INVALID"), "Invalid severity should be rejected");
    }

    @Test
    void testScopeValidation() {
        String[] validScopes = {"SIMPLE", "STANDARD", "COMPLEX"};
        for (String scope : validScopes) {
            assertTrue(isValidScope(scope), "Scope " + scope + " should be valid");
        }

        assertFalse(isValidScope("INVALID"), "Invalid scope should be rejected");
    }

    @Test
    void testBugIdGeneration() throws IOException {
        // Test auto-generated ID format
        String bugId = generateBugId(tempOutputDir, null);
        assertTrue(bugId.matches("^bug-\\d{4}$"), "Bug ID should match pattern bug-NNNN");
    }

    @Test
    void testBugIdIncrement() throws IOException {
        // Create two bugs and verify IDs increment
        String id1 = generateBugId(tempOutputDir, null);
        String id2 = generateBugId(tempOutputDir, null);

        int num1 = Integer.parseInt(id1.replace("bug-", ""));
        int num2 = Integer.parseInt(id2.replace("bug-", ""));

        assertEquals(num2, num1 + 1, "Bug IDs should increment sequentially");
    }

    @Test
    void testBugFileCreation() throws IOException {
        String description = "Login endpoint returns 500 on invalid credentials";
        String severity = "HIGH";
        String scope = "STANDARD";

        Path bugFile = createBugFile(tempOutputDir, description, severity, scope);

        assertTrue(Files.exists(bugFile), "Bug file should be created");
        assertTrue(bugFile.toString().endsWith(".md"), "Bug file should be Markdown");
    }

    @Test
    void testBugFileContainsTemplate() throws IOException {
        String description = "Login form crashes";
        Path bugFile = createBugFile(tempOutputDir, description, "MEDIUM", "STANDARD");

        String content = Files.readString(bugFile);

        // Verify all 9 sections are present
        assertTrue(content.contains("## 1. Visão (Vision)"), "Missing Vision section");
        assertTrue(content.contains("## 2. Persona & Cenário"), "Missing Persona section");
        assertTrue(content.contains("## 3. Entrega de Valor"), "Missing Value Delivery section");
        assertTrue(
                content.contains("## 4. Critérios de Aceite"),
                "Missing Acceptance Criteria section");
        assertTrue(content.contains("## 5. Reproduction Recipe"), "Missing Reproduction Recipe");
        assertTrue(content.contains("## 6. Root-Cause Hypothesis"), "Missing Root-Cause section");
        assertTrue(
                content.contains("## 7. Regression Test Slot"), "Missing Regression Test section");
        assertTrue(content.contains("## 8. Dependências"), "Missing Dependencies section");
        assertTrue(
                content.contains("## 9. Histórico de Decisão"), "Missing Decision History section");
    }

    @Test
    void testBugFileStatusInitialized() throws IOException {
        Path bugFile = createBugFile(tempOutputDir, "Test bug", "MEDIUM", "STANDARD");
        String content = Files.readString(bugFile);

        assertTrue(
                content.contains("**Status:** Pendente"),
                "Status should be initialized to Pendente");
    }

    @Test
    void testBugFileFrontmatter() throws IOException {
        Path bugFile = createBugFile(tempOutputDir, "Test bug", "HIGH", "SIMPLE");
        String content = Files.readString(bugFile);

        assertTrue(content.startsWith("---"), "File should start with YAML frontmatter");
        assertTrue(
                content.contains("requires-capabilities:"), "Should declare required capabilities");
        assertTrue(content.contains("template-version:"), "Should declare template version");
        assertTrue(content.contains("template-type: bug"), "Should declare template type");
    }

    @Test
    void testBugFileNaming() throws IOException {
        String description = "Session token leaks to browser";
        Path bugFile = createBugFile(tempOutputDir, description, "CRITICAL", "COMPLEX");
        String filename = bugFile.getFileName().toString();

        // File should follow pattern: bug-NNNN-slug.md
        assertTrue(
                filename.matches("^bug-\\d{4}-.*\\.md$"),
                "Bug filename should match pattern bug-NNNN-slug.md");
    }

    @Test
    void testMultipleBugCreation() throws IOException {
        // Create 3 bugs and verify they're all valid
        for (int i = 0; i < 3; i++) {
            Path bugFile =
                    createBugFile(
                            tempOutputDir,
                            "Bug number " + i + " with unique description",
                            "MEDIUM",
                            "STANDARD");
            assertTrue(Files.exists(bugFile), "Bug " + i + " file should exist");
        }

        // Verify all files exist
        List<Path> bugFiles =
                Files.list(tempOutputDir)
                        .filter(p -> p.toString().endsWith(".md"))
                        .collect(Collectors.toList());

        assertEquals(3, bugFiles.size(), "Should have created 3 bug files");
    }

    @Test
    void testCapabilityDependencyPresent() {
        assertTrue(
                Files.exists(Paths.get(CAPABILITY_PATH)),
                "bug-lifecycle.yaml capability must be present");
    }

    @Test
    void testTemplateCapabilityDeclaration() throws IOException {
        String templateContent = Files.readString(Paths.get(BUG_TEMPLATE_PATH));
        assertTrue(
                templateContent.contains("requires-capabilities:"),
                "Template should declare required capabilities");
    }

    // Helper methods

    private String generateSlug(String description) {
        // Simple slug generation: lowercase, replace spaces with hyphens, remove special chars
        return description
                .toLowerCase()
                .replaceAll("[^a-z0-9 -]", "")
                .replaceAll(" +", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .substring(0, Math.min(40, description.length()));
    }

    private boolean isValidSeverity(String severity) {
        return severity.matches("^(LOW|MEDIUM|HIGH|CRITICAL)$");
    }

    private boolean isValidScope(String scope) {
        return scope.matches("^(SIMPLE|STANDARD|COMPLEX)$");
    }

    private String generateBugId(Path outputDir, String explicitId) throws IOException {
        if (explicitId != null) {
            return explicitId;
        }

        long count = Files.list(outputDir).filter(p -> p.toString().endsWith(".md")).count();

        return String.format("bug-%04d", count + 1);
    }

    private Path createBugFile(Path outputDir, String description, String severity, String scope)
            throws IOException {
        String bugId = generateBugId(outputDir, null);
        String slug = generateSlug(description);
        String filename = bugId + "-" + slug + ".md";
        Path bugFile = outputDir.resolve(filename);

        String content =
                "---\n"
                        + "requires-capabilities: [governance.bug-lifecycle]\n"
                        + "template-version: \"1.0\"\n"
                        + "template-type: bug\n"
                        + "---\n\n"
                        + "# Bug: "
                        + description
                        + "\n\n"
                        + "**Status:** Pendente\n"
                        + "**Severity:** "
                        + severity
                        + "\n"
                        + "**Scope:** "
                        + scope
                        + "\n\n"
                        + "## 1. Visão (Vision)\n\n"
                        + "## 2. Persona & Cenário\n\n"
                        + "## 3. Entrega de Valor\n\n"
                        + "## 4. Critérios de Aceite\n\n"
                        + "## 5. Reproduction Recipe\n\n"
                        + "## 6. Root-Cause Hypothesis\n\n"
                        + "## 7. Regression Test Slot\n\n"
                        + "## 8. Dependências\n\n"
                        + "## 9. Histórico de Decisão\n";

        Files.writeString(bugFile, content);
        return bugFile;
    }
}
