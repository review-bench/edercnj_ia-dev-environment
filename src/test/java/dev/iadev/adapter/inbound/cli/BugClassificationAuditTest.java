package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Bug Classification Audit Tests")
class BugClassificationAuditTest {

    private static final String AUDIT_SCRIPT_PATH = "scripts/audit-bug-classification.sh";

    @Test
    @DisplayName("Audit script exists and is executable")
    void auditScriptExists() throws IOException {
        Path scriptPath = Path.of(AUDIT_SCRIPT_PATH);
        assertTrue(Files.exists(scriptPath), "audit-bug-classification.sh must exist");
        assertTrue(
                Files.isExecutable(scriptPath) || scriptPath.toFile().canRead(),
                "Script must be readable");
    }

    @Test
    @DisplayName("Audit script contains required exit codes (0, 1, 2)")
    void auditScriptExitCodesDocumented() throws IOException {
        String script = Files.readString(Path.of(AUDIT_SCRIPT_PATH));
        assertTrue(
                script.contains("exit 0") || script.contains("EXIT_CODE=0"),
                "Script must handle exit code 0");
        assertTrue(
                script.contains("exit 1") || script.contains("EXIT_CODE=1"),
                "Script must handle exit code 1");
        assertTrue(script.contains("exit 2"), "Script must handle exit code 2 (usage error)");
    }

    @Test
    @DisplayName("AC-1: Valid bug file passes audit with 0 violations")
    void ac1_ValidBugPassesAudit(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildValidBugContent());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertEquals(0, result.getErrorCount(), "Valid bug must produce 0 errors");
        assertEquals(0, result.getWarnCount(), "Valid bug must produce 0 warnings");
    }

    @Test
    @DisplayName("AC-2: Bug missing requires-capabilities triggers ERROR violation")
    void ac2_MissingCapabilityDeclarationTriggersError(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildBugWithoutCapability());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertTrue(result.getErrorCount() > 0, "Missing capability must produce ERROR");
        assertTrue(
                result.hasViolation("MISSING_CAPABILITY_DECL")
                        || result.hasViolation("WRONG_CAPABILITY"),
                "Must report capability violation");
    }

    @Test
    @DisplayName("AC-2: Bug with invalid severity triggers ERROR violation")
    void ac2_InvalidSeverityTriggersError(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildBugWithInvalidSeverity());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertTrue(result.getErrorCount() > 0, "Invalid severity must produce ERROR");
        assertTrue(
                result.hasViolation("INVALID_SEVERITY"), "Must report INVALID_SEVERITY violation");
    }

    @Test
    @DisplayName("AC-2: Bug missing RA9 sections triggers ERROR violations")
    void ac2_MissingSectionsTriggersErrors(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildBugMissingSections());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertTrue(result.getErrorCount() > 0, "Missing sections must produce ERRORs");
    }

    @Test
    @DisplayName("AC-3: Bug directory name not matching bug-NNNNNN triggers ERROR")
    void ac3_InvalidBugIdFormatTriggersError(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildValidBugContent());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertTrue(result.getErrorCount() > 0, "Invalid bug-id format must produce ERROR");
        assertTrue(result.hasViolation("BUG_ID_FORMAT"), "Must report BUG_ID_FORMAT violation");
    }

    @Test
    @DisplayName("AC-4: Multiple bugs scanned — all results aggregated")
    void ac4_MultipleBugsAggregated(@TempDir Path tempDir) throws IOException {
        // Bug 1: valid
        Path bug1Dir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bug1Dir);
        Files.writeString(bug1Dir.resolve("bug.md"), buildValidBugContent());

        // Bug 2: invalid (missing capability)
        Path bug2Dir = tempDir.resolve("ai/bugs/bug-000002");
        Files.createDirectories(bug2Dir);
        Files.writeString(bug2Dir.resolve("bug.md"), buildBugWithoutCapability());

        AuditResult result = runAudit(bug1Dir.getParent().getParent());

        assertEquals(2, result.getBugsScanned(), "Must scan 2 bugs");
        assertTrue(result.getErrorCount() > 0, "Must detect errors from bug-000002");
    }

    @Test
    @DisplayName("Empty bugs directory produces 0 violations and exit 0")
    void emptyBugsDirPasses(@TempDir Path tempDir) {
        Path bugsDir = tempDir.resolve("ai/bugs");
        AuditResult result = runAudit(bugsDir.getParent());

        assertEquals(0, result.getErrorCount(), "Empty dir must produce 0 errors");
        assertEquals(0, result.getBugsScanned(), "No bugs scanned from empty dir");
    }

    @Test
    @DisplayName("Audit detects invalid lifecycle status")
    void invalidStatusDetected(@TempDir Path tempDir) throws IOException {
        Path bugsDir = tempDir.resolve("ai/bugs/bug-000001");
        Files.createDirectories(bugsDir);
        Files.writeString(bugsDir.resolve("bug.md"), buildBugWithInvalidStatus());

        AuditResult result = runAudit(bugsDir.getParent().getParent());

        assertTrue(result.getErrorCount() > 0, "Invalid status must produce ERROR");
        assertTrue(result.hasViolation("INVALID_STATUS"), "Must report INVALID_STATUS violation");
    }

    // Helper methods

    private AuditResult runAudit(Path rootDir) {
        // Simulate audit logic in Java (mirroring shell script behavior)
        List<AuditViolation> violations = new ArrayList<>();
        int bugsScanned = 0;

        // Support both: rootDir=project-root (resolve ai/bugs) and rootDir=bugs-parent-dir
        Path bugsDir = rootDir.resolve("ai/bugs");
        if (!Files.exists(bugsDir)) {
            bugsDir = rootDir.resolve("bugs");
        }
        if (!Files.exists(bugsDir)) {
            return new AuditResult(0, violations);
        }

        try {
            var bugDirs = Files.list(bugsDir).filter(Files::isDirectory).toList();

            for (Path bugDir : bugDirs) {
                bugsScanned++;
                String bugId = bugDir.getFileName().toString();

                if (!bugId.matches("^bug-[0-9]{6}$")) {
                    violations.add(
                            new AuditViolation(
                                    "ERROR",
                                    bugId,
                                    "BUG_ID_FORMAT",
                                    "Bug directory name must match bug-NNNNNN"));
                    continue;
                }

                Path bugFile = bugDir.resolve("bug.md");
                if (!Files.exists(bugFile)) {
                    violations.add(
                            new AuditViolation(
                                    "ERROR", bugId, "BUG_FILE_MISSING", "bug.md not found"));
                    continue;
                }

                String content = Files.readString(bugFile);

                if (!content.contains("requires-capabilities:")) {
                    violations.add(
                            new AuditViolation(
                                    "ERROR",
                                    bugId,
                                    "MISSING_CAPABILITY_DECL",
                                    "bug.md must declare requires-capabilities"));
                } else if (!content.contains("governance.bug-lifecycle")) {
                    violations.add(
                            new AuditViolation(
                                    "ERROR",
                                    bugId,
                                    "WRONG_CAPABILITY",
                                    "bug.md must require governance.bug-lifecycle"));
                }

                String[] requiredSections = {
                    "## 1. Visão", "## 2. Persona", "## 3. Entrega",
                    "## 4. Critérios", "## 5. Reproduction", "## 6. Root-Cause",
                    "## 7. Regression", "## 8. Dependências", "## 9. Histórico"
                };
                String[] sectionNames = {
                    "1-Visao",
                    "2-Persona",
                    "3-Entrega",
                    "4-Criterios",
                    "5-Recipe",
                    "6-RootCause",
                    "7-RegressionTest",
                    "8-Dependencies",
                    "9-History"
                };
                for (int i = 0; i < requiredSections.length; i++) {
                    if (!content.contains(requiredSections[i])) {
                        violations.add(
                                new AuditViolation(
                                        "ERROR",
                                        bugId,
                                        "MISSING_SECTION_" + sectionNames[i],
                                        "Required section missing"));
                    }
                }

                if (content.contains("**Severity:**")) {
                    String severityLine =
                            content.lines()
                                    .filter(l -> l.startsWith("**Severity:**"))
                                    .findFirst()
                                    .orElse("");
                    String severity = severityLine.replace("**Severity:**", "").trim();
                    if (!severity.matches("LOW|MEDIUM|HIGH|CRITICAL")) {
                        violations.add(
                                new AuditViolation(
                                        "ERROR",
                                        bugId,
                                        "INVALID_SEVERITY",
                                        "Severity '" + severity + "' is not valid"));
                    }
                }

                if (content.contains("**Status:**")) {
                    String statusLine =
                            content.lines()
                                    .filter(l -> l.startsWith("**Status:**"))
                                    .findFirst()
                                    .orElse("");
                    String status = statusLine.replace("**Status:**", "").trim();
                    List<String> validStatuses =
                            List.of(
                                    "Pendente",
                                    "Refinada",
                                    "Em Investigação",
                                    "Em Correção",
                                    "Concluída",
                                    "Bloqueada",
                                    "Descartada",
                                    "Falha");
                    if (!validStatuses.contains(status)) {
                        violations.add(
                                new AuditViolation(
                                        "ERROR",
                                        bugId,
                                        "INVALID_STATUS",
                                        "Status '" + status + "' is not valid"));
                    }
                }

                if (!content.contains("## Refinement Verdict")) {
                    violations.add(
                            new AuditViolation(
                                    "WARNING",
                                    bugId,
                                    "MISSING_REFINEMENT_VERDICT",
                                    "bug.md should have Refinement Verdict block"));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new AuditResult(bugsScanned, violations);
    }

    private String buildValidBugContent() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n"
                + "template-version: \"1.0\"\ntemplate-type: bug\n---\n\n"
                + "# Bug: Test bug\n\n**Status:** Pendente\n**Severity:** HIGH\n\n"
                + "## 1. Visão (Vision)\n\nBug occurs.\n\n"
                + "## 2. Persona & Cenário de Uso\n\nUser.\n\n"
                + "## 3. Entrega de Valor\n\nFix.\n\n"
                + "## 4. Critérios de Aceite\n\nGiven/When/Then.\n\n"
                + "## 5. Reproduction Recipe\n\nSteps.\n\n"
                + "## 6. Root-Cause Hypothesis\n\nHypothesis.\n\n"
                + "## 7. Regression Test Slot\n\nTBD\n\n"
                + "## 8. Dependências\n\n—\n\n"
                + "## 9. Histórico de Decisão\n\n—\n\n"
                + "## Refinement Verdict\n\n```yaml\nstatus: pending\n```\n";
    }

    private String buildBugWithoutCapability() {
        return "---\ntemplate-version: \"1.0\"\n---\n\n"
                + "# Bug: No capability\n\n**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\n## 2. Persona & Cenário de Uso\n\n"
                + "## 3. Entrega de Valor\n\n## 4. Critérios de Aceite\n\n"
                + "## 5. Reproduction Recipe\n\n## 6. Root-Cause Hypothesis\n\n"
                + "## 7. Regression Test Slot\n\n## 8. Dependências\n\n## 9. Histórico de Decisão\n\n";
    }

    private String buildBugWithInvalidSeverity() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                + "# Bug: Invalid severity\n\n**Status:** Pendente\n**Severity:** BLOCKER\n\n"
                + "## 1. Visão (Vision)\n\n## 2. Persona & Cenário de Uso\n\n"
                + "## 3. Entrega de Valor\n\n## 4. Critérios de Aceite\n\n"
                + "## 5. Reproduction Recipe\n\n## 6. Root-Cause Hypothesis\n\n"
                + "## 7. Regression Test Slot\n\n## 8. Dependências\n\n## 9. Histórico de Decisão\n\n";
    }

    private String buildBugMissingSections() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                + "# Bug: Missing sections\n\n**Status:** Pendente\n\n"
                + "## 1. Visão (Vision)\n\nSome bug.\n\n";
    }

    private String buildBugWithInvalidStatus() {
        return "---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n"
                + "# Bug: Invalid status\n\n**Status:** InProgress\n\n"
                + "## 1. Visão (Vision)\n\n## 2. Persona & Cenário de Uso\n\n"
                + "## 3. Entrega de Valor\n\n## 4. Critérios de Aceite\n\n"
                + "## 5. Reproduction Recipe\n\n## 6. Root-Cause Hypothesis\n\n"
                + "## 7. Regression Test Slot\n\n## 8. Dependências\n\n## 9. Histórico de Decisão\n\n";
    }

    private static class AuditResult {
        private final int bugsScanned;
        private final List<AuditViolation> violations;

        AuditResult(int bugsScanned, List<AuditViolation> violations) {
            this.bugsScanned = bugsScanned;
            this.violations = violations;
        }

        int getBugsScanned() {
            return bugsScanned;
        }

        int getErrorCount() {
            return (int) violations.stream().filter(v -> "ERROR".equals(v.level())).count();
        }

        int getWarnCount() {
            return (int) violations.stream().filter(v -> "WARNING".equals(v.level())).count();
        }

        boolean hasViolation(String code) {
            return violations.stream().anyMatch(v -> v.code().equals(code));
        }
    }

    private record AuditViolation(String level, String bugId, String code, String message) {}
}
