package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.AuditScript;
import dev.iadev.domain.model.AuditScript.ExitCodeEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("DocsAssembler catalog rendering")
class DocsAssemblerCatalogTest {

    private final DocsAssembler assembler = new DocsAssembler();

    private static AuditScript modelSelectionAudit() {
        return new AuditScript(
                "audit-model-selection.sh",
                "Template",
                "frontmatter of SKILL.md declares model:",
                "token cost stays within budget",
                "Rule 23",
                List.of(
                        new ExitCodeEntry(0, "OK", "all checks pass"),
                        new ExitCodeEntry(
                                1, "MODEL_SELECTION_VIOLATION", "missing model: declaration"),
                        new ExitCodeEntry(2, "OPERATIONAL_ERROR", "skills root not found")));
    }

    private static AuditScript actuatorAudit() {
        return new AuditScript(
                "audit-actuator-exposure.sh",
                "Stack-Specific",
                "Spring Boot actuator endpoints not exposed in production",
                "no /actuator/env or /actuator/heapdump in prod profiles",
                "CIS Spring Boot",
                List.of(
                        new ExitCodeEntry(0, "OK", "no exposure"),
                        new ExitCodeEntry(
                                1, "ACTUATOR_EXPOSURE_VIOLATION", "sensitive endpoint exposed"),
                        new ExitCodeEntry(2, "OPERATIONAL_ERROR", "config file unreadable")));
    }

    @Nested
    @DisplayName("renderCatalog")
    class RenderCatalog {

        @Test
        void springBootInventory_containsActuatorSection() {
            List<AuditScript> inventory = List.of(modelSelectionAudit(), actuatorAudit());

            String output = assembler.renderCatalog("spring-boot", inventory);

            assertThat(output).contains("audit-actuator-exposure.sh");
            assertThat(output).contains("Stack: spring-boot");
        }

        @Test
        void springBootInventory_showsCorrectTotalCount() {
            List<AuditScript> inventory = List.of(modelSelectionAudit(), actuatorAudit());

            String output = assembler.renderCatalog("spring-boot", inventory);

            assertThat(output).contains("Total Audits: 2");
        }

        @Test
        void defaultStack_containsNoteAboutRuntimeAudits() {
            List<AuditScript> inventory = List.of(modelSelectionAudit());

            String output = assembler.renderCatalog("_default", inventory);

            assertThat(output).contains("Total Audits: 1");
            assertThat(output).contains("Runtime audits require stack-specific knowledge");
        }

        @Test
        void auditSection_containsExitCodeTable() {
            List<AuditScript> inventory = List.of(modelSelectionAudit());

            String output = assembler.renderCatalog("spring-boot", inventory);

            assertThat(output).contains("MODEL_SELECTION_VIOLATION");
            assertThat(output).contains("Rule 23");
        }

        @Test
        void threeAuditInventory_containsThreeSubsections() {
            AuditScript second =
                    new AuditScript(
                            "audit-skill-visibility.sh",
                            "Template",
                            "internal skills have visibility: internal",
                            "rule 22 enforced",
                            "Rule 22",
                            List.of(new ExitCodeEntry(0, "OK", "pass")));
            AuditScript third =
                    new AuditScript(
                            "audit-bypass-flags.sh",
                            "Template",
                            "--no-ci-watch outside recovery blocks",
                            "no bypass violations",
                            "Rule 45",
                            List.of(new ExitCodeEntry(0, "OK", "pass")));
            List<AuditScript> inventory = List.of(modelSelectionAudit(), second, third);

            String output = assembler.renderCatalog("java-maven", inventory);

            long subsections = output.lines().filter(line -> line.startsWith("### ")).count();
            assertThat(subsections).isEqualTo(3);
        }

        @Test
        void emptyInventory_returnsHeaderOnly() {
            String output = assembler.renderCatalog("java-maven", List.of());

            assertThat(output).contains("Stack: java-maven");
            assertThat(output).contains("Total Audits: 0");
        }

        @Test
        void noTemplateFile_usesFallbackTemplate(@TempDir Path tempDir) {
            DocsAssembler noTemplateAssembler = new DocsAssembler(tempDir);
            List<AuditScript> inventory = List.of(modelSelectionAudit());

            String output = noTemplateAssembler.renderCatalog("spring-boot", inventory);

            assertThat(output).contains("audit-model-selection.sh");
            assertThat(output).contains("Stack: spring-boot");
        }

        @Test
        void templateWithoutLoopMarkers_returnsRenderedPlainTemplate(@TempDir Path tempDir)
                throws IOException {
            Path templatesDir = Files.createDirectories(tempDir.resolve("shared/templates"));
            Files.writeString(
                    templatesDir.resolve("_TEMPLATE-AUDIT-GATES-CATALOG.md"),
                    "Stack: {{STACK}}\nTotal Audits: {{TOTAL_AUDITS}}\n");
            DocsAssembler plainAssembler = new DocsAssembler(tempDir);

            String output =
                    plainAssembler.renderCatalog("spring-boot", List.of(modelSelectionAudit()));

            assertThat(output).contains("Stack: spring-boot");
            assertThat(output).contains("Total Audits: 1");
        }
    }
}
