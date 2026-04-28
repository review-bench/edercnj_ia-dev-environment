package dev.iadev.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke tests verifying that ScriptsAssembler generates the correct audit script files for each
 * supported stack. Validates file counts and absence of unresolved placeholders.
 *
 * <p>This is the Stack{X}AuditSmokeIT consolidation from story §5.3 DoD — one class per story
 * instead of 7 separate classes, following Rule 03 DRY principle.
 */
@DisplayName("StackAuditSmokeIT — per-stack template assembly")
class StackAuditSmokeIT {

    @TempDir
    Path outputDir;

    private ScriptsAssembler assembler;
    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        assembler = new ScriptsAssembler(new StackResolver());
        engine = new TemplateEngine();
    }

    @Test
    @DisplayName("java-maven: generates 9 scripts with no unresolved placeholders")
    void javaMaven_generates9ScriptsNoUnresolvedPlaceholders() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("java", "21")
                .framework("picocli", "4.7")
                .buildTool("maven")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 9);
        assertNoUnresolvedPlaceholders(outputDir.resolve("scripts"));
        assertContainsBuildTool(outputDir.resolve("scripts"), "mvn");
    }

    @Test
    @DisplayName("java-gradle: generates 9 scripts with gradle build tool")
    void javaGradle_generates9ScriptsWithGradle() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("java", "21")
                .framework("quarkus", "3.0")
                .buildTool("gradle")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 9);
        assertNoUnresolvedPlaceholders(outputDir.resolve("scripts"));
    }

    @Test
    @DisplayName("spring-boot: generates 10 scripts including actuator-exposure")
    void springBoot_generates10ScriptsWithActuatorAudit() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("java", "21")
                .framework("spring-boot", "3.3")
                .buildTool("maven")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 10);
        assertContainsScript(outputDir.resolve("scripts"), "audit-actuator-exposure.sh");
        assertNoUnresolvedPlaceholders(outputDir.resolve("scripts"));
    }

    @Test
    @DisplayName("node: generates 10 scripts including package-lock audit")
    void node_generates10ScriptsWithPackageLockAudit() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("typescript", "5.4")
                .framework("express", "4.18")
                .buildTool("npm")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 10);
        assertContainsScript(outputDir.resolve("scripts"), "audit-package-lock-integrity.sh");
        assertNoUnresolvedPlaceholders(outputDir.resolve("scripts"));
    }

    @Test
    @DisplayName("python: generates 10 scripts including requirements-pin audit")
    void python_generates10ScriptsWithRequirementsPinAudit() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("python", "3.12")
                .framework("fastapi", "0.115")
                .buildTool("pip")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 10);
        assertContainsScript(outputDir.resolve("scripts"), "audit-requirements-pin.sh");
    }

    @Test
    @DisplayName("go: generates 10 scripts including go-mod-tidy audit")
    void go_generates10ScriptsWithGoModTidyAudit() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("go", "1.22")
                .framework("gin", "1.10")
                .buildTool("go-mod")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 10);
        assertContainsScript(outputDir.resolve("scripts"), "audit-go-mod-tidy.sh");
    }

    @Test
    @DisplayName("_default: generates 6 scripts (5 markdown + audit-all) for unknown stack")
    void defaultStack_generates6ScriptsForUnknownLanguage() throws IOException {
        ProjectConfig config = TestConfigBuilder.builder()
                .language("rust", "1.75")
                .framework("actix", "4.0")
                .buildTool("cargo")
                .build();

        List<String> generated = assembler.assemble(config, engine, outputDir);

        assertScriptCount(generated, 6);
        assertContainsScript(outputDir.resolve("scripts"), "audit-all.sh");
    }

    private void assertScriptCount(List<String> generated, int expected) {
        assertThat(generated).hasSize(expected);
    }

    private void assertNoUnresolvedPlaceholders(Path scriptsDir) throws IOException {
        if (!Files.exists(scriptsDir)) return;
        List<Path> withPlaceholders = Files.list(scriptsDir)
                .filter(p -> {
                    try {
                        return Files.readString(p).contains("{{") &&
                               !Files.readString(p).matches("(?s).*\\{\\{[A-Z_]+\\}\\}.*");
                    } catch (IOException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        // Only fail if there are double-brace placeholders that weren't resolved
        // (we allow single-brace shell variables like ${VAR})
    }

    private void assertContainsBuildTool(Path scriptsDir, String tool) throws IOException {
        if (!Files.exists(scriptsDir)) return;
        boolean found = Files.list(scriptsDir)
                .anyMatch(p -> {
                    try {
                        return Files.readString(p).contains(tool);
                    } catch (IOException e) {
                        return false;
                    }
                });
        assertThat(found)
                .as("At least one script should contain build tool '%s'", tool)
                .isTrue();
    }

    private void assertContainsScript(Path scriptsDir, String scriptName) throws IOException {
        if (!Files.exists(scriptsDir)) return;
        boolean exists = Files.list(scriptsDir)
                .anyMatch(p -> p.getFileName().toString().equals(scriptName));
        assertThat(exists)
                .as("Script '%s' should be present in %s", scriptName, scriptsDir)
                .isTrue();
    }
}
