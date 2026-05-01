package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.assembler.ScriptsAssembler;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.QualityConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * E2E smoke test for story-0073-0004 (scenarios.yaml.template + ScriptsAssembler integration).
 *
 * <p>Validates structural invariants for the regression scenario template: snippet files exist on
 * classpath, the main template resolves blocks correctly, and {@link
 * ScriptsAssembler#renderRegressionScenarios} writes {@code tests/regression/scenarios.yaml} only
 * when regression is enabled.
 */
@Tag("smoke")
@Tag("e2e")
@DisplayName("Epic0073ScenariosTemplateSmokeIT — regression scenarios template E2E smoke")
class Epic0073ScenariosTemplateSmokeIT {

    private static final Path REST_SNIPPET =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "regression",
                    "blocks",
                    "rest.yaml.snippet");

    private static final Path GRPC_SNIPPET =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "regression",
                    "blocks",
                    "grpc.yaml.snippet");

    private static final Path SOCKET_SNIPPET =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "regression",
                    "blocks",
                    "socket.yaml.snippet");

    private static final Path CLI_SNIPPET =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "regression",
                    "blocks",
                    "cli.yaml.snippet");

    private static final Path MAIN_TEMPLATE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "shared",
                    "templates",
                    "scenarios.yaml.template");

    private static final Path REGRESSION_SKILL =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "conditional",
                    "test",
                    "x-test-regression-shell",
                    "SKILL.md");

    // ─── Scenario 1 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("allSnippetFilesExist — four block snippets are present on classpath")
    void allSnippetFilesExist() {
        assertThat(REST_SNIPPET.toAbsolutePath()).as("rest.yaml.snippet must exist").exists();
        assertThat(GRPC_SNIPPET.toAbsolutePath()).as("grpc.yaml.snippet must exist").exists();
        assertThat(SOCKET_SNIPPET.toAbsolutePath()).as("socket.yaml.snippet must exist").exists();
        assertThat(CLI_SNIPPET.toAbsolutePath()).as("cli.yaml.snippet must exist").exists();
    }

    // ─── Scenario 2 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("mainTemplateContainsAllPlaceholders — scenarios.yaml.template has 4 block tokens")
    void mainTemplateContainsAllPlaceholders() throws Exception {
        String content = Files.readString(MAIN_TEMPLATE.toAbsolutePath());

        assertThat(content)
                .as("must contain REST block placeholder")
                .contains("{{INTERFACE_REST_BLOCK}}");
        assertThat(content)
                .as("must contain gRPC block placeholder")
                .contains("{{INTERFACE_GRPC_BLOCK}}");
        assertThat(content)
                .as("must contain WebSocket block placeholder")
                .contains("{{INTERFACE_WEBSOCKET_BLOCK}}");
        assertThat(content)
                .as("must contain CLI block placeholder")
                .contains("{{INTERFACE_CLI_BLOCK}}");
        assertThat(content)
                .as("must contain scenarios: root key")
                .contains("scenarios:");
    }

    // ─── Scenario 3 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "renderRegressionScenarios_disabled_returnsNull — "
                    + "no output when quality.regression.enabled=false")
    void renderRegressionScenarios_disabled_returnsNull(@TempDir Path outputDir) {
        ProjectConfig config = buildConfig(false, List.of());
        ScriptsAssembler assembler = new ScriptsAssembler();

        Path result = assembler.renderRegressionScenarios(config, outputDir);

        assertThat(result).as("must return null when regression disabled").isNull();
        assertThat(outputDir.resolve("tests/regression/scenarios.yaml"))
                .as("scenarios.yaml must not be created")
                .doesNotExist();
    }

    // ─── Scenario 4 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "renderRegressionScenarios_restOnly_includesRestBlock — "
                    + "REST interface injects rest block; gRPC/cli/ws blocks absent")
    void renderRegressionScenarios_restOnly_includesRestBlock(@TempDir Path outputDir)
            throws Exception {
        ProjectConfig config =
                buildConfig(true, List.of(new InterfaceConfig("rest", "openapi", null)));
        ScriptsAssembler assembler = new ScriptsAssembler();

        Path result = assembler.renderRegressionScenarios(config, outputDir);

        assertThat(result).as("must return path to generated file").isNotNull();
        assertThat(result).as("file must exist at tests/regression/scenarios.yaml").exists();

        String content = Files.readString(result);
        assertThat(content)
                .as("must contain rest-health-check from REST snippet")
                .contains("rest-health-check");
        assertThat(content)
                .as("must not contain grpc-health-check when gRPC absent")
                .doesNotContain("grpc-health-check");
        assertThat(content)
                .as("must not contain websocket when ws absent")
                .doesNotContain("websocket-connect");
        assertThat(content)
                .as("must not contain cli-help when cli absent")
                .doesNotContain("cli-help");
        assertThat(content)
                .as("must not leave unresolved REST_BLOCK placeholder")
                .doesNotContain("{{INTERFACE_REST_BLOCK}}");
    }

    // ─── Scenario 5 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "renderRegressionScenarios_grpcInterface_includesGrpcBlock — "
                    + "gRPC interface injects grpc block; REST block absent")
    void renderRegressionScenarios_grpcInterface_includesGrpcBlock(@TempDir Path outputDir)
            throws Exception {
        ProjectConfig config =
                buildConfig(true, List.of(new InterfaceConfig("grpc", "proto", null)));
        ScriptsAssembler assembler = new ScriptsAssembler();

        Path result = assembler.renderRegressionScenarios(config, outputDir);

        String content = Files.readString(result);
        assertThat(content)
                .as("must contain grpc-health-check from gRPC snippet")
                .contains("grpc-health-check");
        assertThat(content)
                .as("must not contain rest block when only gRPC declared")
                .doesNotContain("rest-health-check");
    }

    // ─── Scenario 6 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "renderRegressionScenarios_cliInterface_includesCliBlock — "
                    + "CLI interface injects cli block with project name substituted")
    void renderRegressionScenarios_cliInterface_includesCliBlock(@TempDir Path outputDir)
            throws Exception {
        ProjectConfig config =
                buildConfig(true, List.of(new InterfaceConfig("cli", null, null)));
        ScriptsAssembler assembler = new ScriptsAssembler();

        Path result = assembler.renderRegressionScenarios(config, outputDir);

        String content = Files.readString(result);
        assertThat(content).as("must contain cli-help from CLI snippet").contains("cli-help");
        assertThat(content)
                .as("must not leave {{PROJECT_NAME}} placeholder unresolved")
                .doesNotContain("{{PROJECT_NAME}}");
    }

    // ─── Scenario 7 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "renderRegressionScenarios_allInterfaces_includesAllBlocks — "
                    + "all four interface types produce all four blocks")
    void renderRegressionScenarios_allInterfaces_includesAllBlocks(@TempDir Path outputDir)
            throws Exception {
        List<InterfaceConfig> interfaces =
                List.of(
                        new InterfaceConfig("rest", "openapi", null),
                        new InterfaceConfig("grpc", "proto", null),
                        new InterfaceConfig("websocket", null, null),
                        new InterfaceConfig("cli", null, null));
        ProjectConfig config = buildConfig(true, interfaces);
        ScriptsAssembler assembler = new ScriptsAssembler();

        Path result = assembler.renderRegressionScenarios(config, outputDir);

        String content = Files.readString(result);
        assertThat(content).contains("rest-health-check");
        assertThat(content).contains("grpc-health-check");
        assertThat(content).contains("websocket-connect");
        assertThat(content).contains("cli-help");
    }

    // ─── Scenario 8 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "regressionSkillExists_withRequiresAnyCapabilities — "
                    + "x-test-regression-shell SKILL.md exists and declares requires-any capabilities")
    void regressionSkillExists_withRequiresAnyCapabilities() throws Exception {
        assertThat(REGRESSION_SKILL.toAbsolutePath())
                .as("x-test-regression-shell SKILL.md must exist")
                .exists();

        String content = Files.readString(REGRESSION_SKILL.toAbsolutePath());
        assertThat(content)
                .as("must declare requires-any with regression capabilities")
                .containsAnyOf("quality.regression.self", "quality.regression.service");
        assertThat(content)
                .as("must document REGRESSION_DETECTED exit code")
                .contains("REGRESSION_DETECTED");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static ProjectConfig buildConfig(boolean regressionEnabled,
            List<InterfaceConfig> interfaces) {
        Map<String, Object> rawQuality =
                regressionEnabled
                        ? Map.of(
                                "regression",
                                Map.of("enabled", true, "mode", "service"),
                                "performance",
                                Map.of("enabled", false),
                                "mutation",
                                Map.of("enabled", false),
                                "contract",
                                Map.of("enabled", false),
                                "dast",
                                Map.of("enabled", false))
                        : Map.of(
                                "regression",
                                Map.of("enabled", false),
                                "performance",
                                Map.of("enabled", false),
                                "mutation",
                                Map.of("enabled", false),
                                "contract",
                                Map.of("enabled", false),
                                "dast",
                                Map.of("enabled", false));

        QualityConfig quality = QualityConfig.fromMap(rawQuality);

        return ProjectConfig.fromMap(
                Map.of(
                        "project", Map.of("name", "test-project", "purpose", "test smoke"),
                        "architecture", Map.of("style", "library"),
                        "interfaces",
                                interfaces.stream()
                                        .map(
                                                i -> {
                                                    var m = new java.util.HashMap<String, Object>();
                                                    m.put("type", i.type());
                                                    if (i.spec() != null) m.put("spec", i.spec());
                                                    if (i.broker() != null)
                                                        m.put("broker", i.broker());
                                                    return m;
                                                })
                                        .collect(java.util.stream.Collectors.toList()),
                        "language", Map.of("name", "java", "version", "21"),
                        "framework", Map.of("name", "picocli", "version", "4.7", "build-tool", "maven"),
                        "quality", rawQuality));
    }
}
