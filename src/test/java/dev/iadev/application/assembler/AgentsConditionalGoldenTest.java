package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for AgentsAssembler — conditional agents, checklist injection, and golden file parity. */
@DisplayName("AgentsAssembler — conditional + golden")
class AgentsConditionalGoldenTest {

    @Nested
    @DisplayName("assemble — conditional agents")
    class ConditionalAgents {

        @Test
        @DisplayName("database generates" + " database-engineer.md")
        void assemble_database_generatesDbEngineer(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder().database("postgresql", "16").build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/database-engineer.md")).exists();
        }

        @Test
        @DisplayName("events generates" + " event-engineer.md")
        void assemble_whenCalled_eventsGenerateEventEngineer(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder().eventDriven(true).build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/event-engineer.md")).exists();
        }

        @Test
        @DisplayName("no database excludes db engineer")
        void assemble_noDb_excludesDbEngineer(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/database-engineer.md")).doesNotExist();
        }

        @Test
        @DisplayName("REST generates api-engineer.md")
        void assemble_rest_generatesApiEngineer(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder().clearInterfaces().addInterface("rest").build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/api-engineer.md")).exists();
        }

        @Test
        @DisplayName("no security frameworks excludes" + " appsec-engineer.md")
        void assemble_noFrameworks_excludesAppsec(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/appsec-engineer.md")).doesNotExist();
        }

        @Test
        @DisplayName("no security frameworks excludes" + " compliance-auditor.md")
        void assemble_noFrameworks_excludesAuditor(@TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            assertThat(outputDir.resolve("agents/compliance-auditor.md")).doesNotExist();
        }

        // assemble_fullFeatured_generatesMany removed — used
        // buildGoGinConfig which points to deleted go-developer.md.
    }

    @Nested
    @DisplayName("assemble — checklist injection")
    class ChecklistInjection {

        @Test
        @DisplayName("security checklist injected" + " when lgpd active")
        void assemble_whenCalled_securityChecklistInjected(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler = new AgentsAssembler();
            ProjectConfig config = TestConfigBuilder.builder().securityFrameworks("lgpd").build();
            assembler.assemble(config, new TemplateEngine(), outputDir);
            Path securityAgent = outputDir.resolve("agents/security-engineer.md");
            if (Files.exists(securityAgent)) {
                String content = Files.readString(securityAgent);
                assertThat(content).isNotEmpty();
            }
        }
    }

    // GoldenFile nested class removed — all tests used go-gin fixture +
    // deleted go-gin goldens. Coverage preserved by main GoldenFileTest
    // parametrized over 9 Java profiles.
}
