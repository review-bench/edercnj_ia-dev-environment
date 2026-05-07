package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for RulesAssembler — generates .claude/rules/00-essentials.md (single consolidated rule
 * file) instead of ~29 numbered rule files (EPIC-0078 Rules Consolidation).
 */
@DisplayName("RulesAssembler")
class RulesAssemblerTest {

    @Nested
    @DisplayName("assemble — essentials rule generation")
    class EssentialsRule {

        @Test
        @DisplayName("generates 00-essentials.md for minimal config")
        void assemble_whenCalled_generatesEssentialsRule(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            assertThat(rulesDir.resolve("00-essentials.md")).exists();
        }

        @Test
        @DisplayName("old numbered rule files are not generated")
        void assemble_whenCalled_oldRuleFilesAbsent(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            assertThat(rulesDir.resolve("01-project-identity.md")).doesNotExist();
            assertThat(rulesDir.resolve("02-domain.md")).doesNotExist();
            assertThat(rulesDir.resolve("03-coding-standards.md")).doesNotExist();
            assertThat(rulesDir.resolve("04-architecture-summary.md")).doesNotExist();
            assertThat(rulesDir.resolve("05-quality-gates.md")).doesNotExist();
            assertThat(rulesDir.resolve("06-security-baseline.md")).doesNotExist();
            assertThat(rulesDir.resolve("07-operations-baseline.md")).doesNotExist();
            assertThat(rulesDir.resolve("08-release-process.md")).doesNotExist();
            assertThat(rulesDir.resolve("09-branching-model.md")).doesNotExist();
        }

        @Test
        @DisplayName("00-essentials.md contains project identity data")
        void assemble_essentials_containsProjectData(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-api")
                            .language("java", "21")
                            .framework("spring-boot", "3.4")
                            .archStyle("microservice")
                            .build();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            String content =
                    Files.readString(
                            outputDir.resolve("rules/00-essentials.md"),
                            StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("my-api")
                    .contains("java 21")
                    .contains("spring-boot 3.4")
                    .contains("microservice");
        }

        @Test
        @DisplayName("00-essentials.md contains hard limits section")
        void assemble_essentials_containsHardLimits(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            String content =
                    Files.readString(
                            outputDir.resolve("rules/00-essentials.md"),
                            StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("§2. Hard Limits")
                    .contains("≤ 25 lines")
                    .contains("≥ 95%")
                    .contains("≥ 90%");
        }

        @Test
        @DisplayName("00-essentials.md contains lifecycle contract section")
        void assemble_essentials_containsLifecycleContract(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            String content =
                    Files.readString(
                            outputDir.resolve("rules/00-essentials.md"),
                            StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("§5. Lifecycle Integrity Contract")
                    .contains("flowVersion")
                    .contains("x-implement-story");
        }

        @Test
        @DisplayName("00-essentials.md contains KP index section")
        void assemble_essentials_containsKpIndex(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            String content =
                    Files.readString(
                            outputDir.resolve("rules/00-essentials.md"),
                            StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("§7. Knowledge Pack Index")
                    .contains("governance/rules/");
        }
    }

    @Nested
    @DisplayName("assemble — conditional rules still generated")
    class ConditionalRules {

        @Test
        @DisplayName("09-data-management generated when database configured")
        void assemble_withDb_generatesDataManagement(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder().database("postgresql", "17").build();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            assertThat(rulesDir.resolve("09-data-management.md")).exists();
        }

        @Test
        @DisplayName("09-data-management not generated when no database")
        void assemble_noDatabase_noDataManagement(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            assembler.assemble(config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve("rules/09-data-management.md")).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — returns generated file paths")
    class ReturnsPaths {

        @Test
        @DisplayName("returned list is not empty")
        void assemble_whenCalled_returnedListNotEmpty(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("returned paths include essentials rule")
        void assemble_whenCalled_includesEssentialsRule(@TempDir Path tempDir) throws IOException {
            Path resourceDir = createMinimalResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler = new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(f -> f.contains("00-essentials.md"));
        }
    }

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void assemble_whenCalled_isAssemblerInstance() {
            RulesAssembler assembler = new RulesAssembler();

            assertThat(assembler).isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("context via ContextBuilder")
    class BuildContext {

        @Test
        @DisplayName("contains all expected keys")
        void context_whenCalled_containsAllKeys() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("ctx-test")
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .archStyle("microservice")
                            .buildTool("maven")
                            .build();

            Map<String, Object> context = ContextBuilder.buildContext(config);

            assertThat(context)
                    .containsEntry("project_name", "ctx-test")
                    .containsEntry("language_name", "java")
                    .containsEntry("language_version", "21")
                    .containsEntry("framework_name", "quarkus")
                    .containsEntry("framework_version", "3.17")
                    .containsEntry("build_tool", "maven")
                    .containsEntry("architecture_style", "microservice");
        }
    }

    @Nested
    @DisplayName("golden file — 00-essentials.md present in golden")
    class GoldenFile {

        @Test
        @DisplayName("00-essentials.md present in java-quarkus golden files")
        void golden_essentials_presentInGoldenFiles() {
            var url =
                    getClass()
                            .getClassLoader()
                            .getResource("golden/java-quarkus/.claude/rules/00-essentials.md");

            assertThat(url)
                    .as("Golden file 00-essentials.md must exist in java-quarkus")
                    .isNotNull();
        }

        @Test
        @DisplayName("09-data-management template exists in conditional resources")
        void golden_dataManagement_templateExists() {
            var url =
                    getClass()
                            .getClassLoader()
                            .getResource(
                                    "targets/claude/rules/"
                                            + "conditional/"
                                            + "09-data-management.md");

            assertThat(url).as("Conditional template must exist in resources").isNotNull();
        }
    }

    private static Path createMinimalResources(Path tempDir) throws IOException {
        Path resourceDir = tempDir.resolve("res");
        createEssentialsTemplate(resourceDir);
        createConditionalRules(resourceDir);
        return resourceDir;
    }

    private static void createEssentialsTemplate(Path resourceDir) throws IOException {
        Path templates = resourceDir.resolve("shared/templates");
        Files.createDirectories(templates);
        Files.writeString(
                templates.resolve("_TEMPLATE-ESSENTIALS-RULE.md"),
                "---\nrequires-capabilities: []\n---\n"
                        + "{PROJECT_IDENTITY_SECTION}\n"
                        + "## §2. Hard Limits\n\n"
                        + "≤ 25 lines per method, ≥ 95% line coverage, ≥ 90% branch.\n\n"
                        + "## §5. Lifecycle Integrity Contract\n\n"
                        + "flowVersion, x-implement-story\n\n"
                        + "## §7. Knowledge Pack Index\n\n"
                        + "governance/rules/coding-standards-rule.md\n",
                StandardCharsets.UTF_8);
    }

    private static void createConditionalRules(Path resourceDir) throws IOException {
        Path condRules = resourceDir.resolve("targets/claude/rules/conditional");
        Files.createDirectories(condRules);
        Files.writeString(
                condRules.resolve("09-data-management.md"),
                "# Rule 09 — Data Management\n" + "DB: {DATABASE_NAME}\n" + "## Migration\n",
                StandardCharsets.UTF_8);
    }
}
