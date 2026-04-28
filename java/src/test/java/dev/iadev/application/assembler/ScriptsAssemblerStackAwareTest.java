package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ScriptsAssembler stack-aware")
class ScriptsAssemblerStackAwareTest {

    @TempDir
    Path outputDir;

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TemplateEngine();
    }

    @Nested
    @DisplayName("constructor injection")
    class ConstructorInjection {

        @Test
        void defaultConstructor_preservesLegacyBehavior() {
            ScriptsAssembler assembler = new ScriptsAssembler();
            ProjectConfig config = ConfigProfiles.getStack("java-spring");

            List<String> generated = assembler.assemble(config, engine, outputDir);

            assertThat(generated).hasSize(ScriptsAssembler.AUDIT_SCRIPTS.size());
        }

        @Test
        void resolverConstructor_assemblesForJavaMaven() {
            ScriptsAssembler assembler = new ScriptsAssembler(new StackResolver());
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("picocli", "4.7")
                    .buildTool("maven")
                    .build();

            List<String> generated = assembler.assemble(config, engine, outputDir);

            assertThat(generated).isNotEmpty();
        }

        @Test
        void resolverConstructor_assemblesForUnknownStack_fallsBackToDefault() {
            ScriptsAssembler assembler = new ScriptsAssembler(new StackResolver());
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("rust", "1.75")
                    .framework("actix", "4.0")
                    .buildTool("cargo")
                    .build();

            List<String> generated = assembler.assemble(config, engine, outputDir);

            assertThat(generated).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("placeholder resolution")
    class PlaceholderResolution {

        @Test
        void resolvePlaceholders_javaMaven_replacesBuildTool() {
            Map<String, String> vars = ScriptsAssembler.buildPlaceholders("java", "maven");

            assertThat(vars).containsEntry("BUILD_TOOL", "mvn");
        }

        @Test
        void resolvePlaceholders_javaGradle_replacesBuildTool() {
            Map<String, String> vars = ScriptsAssembler.buildPlaceholders("java", "gradle");

            assertThat(vars).containsEntry("BUILD_TOOL", "gradle");
        }

        @Test
        void resolvePlaceholders_node_setCoveragePath() {
            Map<String, String> vars = ScriptsAssembler.buildPlaceholders("node", "npm");

            assertThat(vars).containsEntry("COVERAGE_REPORT_PATH", "coverage/lcov.info");
        }

        @Test
        void resolvePlaceholders_python_setsLockFile() {
            Map<String, String> vars = ScriptsAssembler.buildPlaceholders("python", "pip");

            assertThat(vars).containsEntry("LOCK_FILE", "requirements.txt");
        }

        @Test
        void resolvePlaceholders_go_setsTestCommand() {
            Map<String, String> vars = ScriptsAssembler.buildPlaceholders("go", "go-mod");

            assertThat(vars).containsEntry("TEST_COMMAND", "go test ./...");
        }

        @Test
        void applyPlaceholders_resolvesKnownPlaceholder() {
            String template = "# build: {{BUILD_TOOL}} test: {{TEST_COMMAND}}";
            Map<String, String> vars = Map.of("BUILD_TOOL", "mvn", "TEST_COMMAND", "mvn verify");

            String result = ScriptsAssembler.applyPlaceholders(template, vars);

            assertThat(result).isEqualTo("# build: mvn test: mvn verify");
        }

        @Test
        void applyPlaceholders_leavesUnknownPlaceholderIntact() {
            String template = "# {{UNKNOWN_PLACEHOLDER}}";
            Map<String, String> vars = Map.of("BUILD_TOOL", "mvn");

            String result = ScriptsAssembler.applyPlaceholders(template, vars);

            assertThat(result).contains("{{UNKNOWN_PLACEHOLDER}}");
        }
    }
}
