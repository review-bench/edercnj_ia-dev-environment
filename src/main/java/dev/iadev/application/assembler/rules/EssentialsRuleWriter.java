package dev.iadev.application.assembler.rules;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes the consolidated {@code 00-essentials.md} rule file from the {@code
 * _TEMPLATE-ESSENTIALS-RULE.md} template.
 *
 * <p>The essentials file is the single always-loaded rule file produced by {@code ia-dev-env
 * generate}. It consolidates the minimal normative contract (project identity, hard limits,
 * architecture golden rule, forbidden items, lifecycle contract, skill invocation protocol, and
 * knowledge pack index) from the ~29 individual rule files that existed before EPIC-0078.
 *
 * <p>Generated to {@code <output>/.claude/rules/00-essentials.md}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EssentialsRuleWriter writer = new EssentialsRuleWriter(resourcesDir);
 * String path = writer.write(config, engine, rulesDir, context);
 * }</pre>
 *
 * @see dev.iadev.application.assembler.RulesAssembler
 * @see RulesIdentity
 */
public final class EssentialsRuleWriter {

    private static final String TEMPLATE_PATH = "shared/templates/_TEMPLATE-ESSENTIALS-RULE.md";
    private static final String IDENTITY_MARKER = "{PROJECT_IDENTITY_SECTION}";
    private static final String OUTPUT_FILENAME = "00-essentials.md";

    private final Path resourcesDir;

    /**
     * Creates an EssentialsRuleWriter with the given resources directory.
     *
     * @param resourcesDir the base resources directory (parent of shared/ and targets/)
     */
    public EssentialsRuleWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Writes {@code 00-essentials.md} by:
     *
     * <ol>
     *   <li>Reading {@code _TEMPLATE-ESSENTIALS-RULE.md} from the resources directory
     *   <li>Running standard placeholder replacement ({@code {key}} single-brace, lowercase)
     *   <li>Replacing the {@code {PROJECT_IDENTITY_SECTION}} marker with the programmatically
     *       generated project identity block
     * </ol>
     *
     * @param config the project configuration
     * @param engine the template engine for placeholder replacement
     * @param rulesDir the output rules directory
     * @param context the template context map
     * @return the absolute path string of the written file
     */
    public String write(
            ProjectConfig config,
            TemplateEngine engine,
            Path rulesDir,
            Map<String, Object> context) {

        String templateContent = readTemplate();
        String withPlaceholders = engine.replacePlaceholders(templateContent, context);
        String identitySection = buildIdentitySection(config);
        String finalContent = withPlaceholders.replace(IDENTITY_MARKER, identitySection);

        Path dest = rulesDir.resolve(OUTPUT_FILENAME);
        try {
            Files.writeString(dest, finalContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write 00-essentials.md", e);
        }
        return dest.toString();
    }

    private String readTemplate() {
        Path templatePath = resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templatePath)) {
            throw new IllegalStateException("Essentials rule template not found: " + templatePath);
        }
        try {
            return Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read essentials template", e);
        }
    }

    private static String buildIdentitySection(ProjectConfig config) {
        String ifaces =
                config.interfaces().stream().map(i -> i.type()).collect(Collectors.joining(", "));
        if (ifaces.isEmpty()) {
            ifaces = "none";
        }
        String version = config.framework().version();
        String fwVer = (version == null || version.isEmpty()) ? "" : " " + version;

        StringBuilder sb = new StringBuilder();
        sb.append("## §1. Project Identity\n\n");
        sb.append("- **Name:** ").append(config.project().name()).append("\n");
        sb.append("- **Purpose:** ").append(config.project().purpose()).append("\n");
        sb.append("- **Architecture:** ").append(config.architecture().style()).append("\n");
        sb.append("- **Language:** ")
                .append(config.language().name())
                .append(" ")
                .append(config.language().version())
                .append("\n");
        sb.append("- **Framework:** ").append(config.framework().name()).append(fwVer).append("\n");
        sb.append("- **Interfaces:** ").append(ifaces).append("\n");
        sb.append("\n");
        sb.append("### Technology Stack\n\n");
        sb.append("| Layer | Technology |\n");
        sb.append("|-------|-----------|\n");
        sb.append("| Architecture | ").append(config.architecture().style()).append(" |\n");
        sb.append("| Language | ")
                .append(config.language().name())
                .append(" ")
                .append(config.language().version())
                .append(" |\n");
        sb.append("| Framework | ").append(config.framework().name()).append(fwVer).append(" |\n");
        sb.append("| Build Tool | ").append(config.framework().buildTool()).append(" |\n");
        sb.append("| Database | ").append(config.databaseName()).append(" |\n");
        sb.append("| Migration | ").append(config.migrationName()).append(" |\n");
        sb.append("| Cache | ").append(config.cacheName()).append(" |\n");
        sb.append("| Container | ").append(config.infrastructure().container()).append(" |\n");
        sb.append("| Orchestrator | ")
                .append(config.infrastructure().orchestrator())
                .append(" |\n");
        sb.append("| Resilience | Mandatory (always enabled) |\n");
        sb.append("\n");
        sb.append("### Constraints\n\n");
        sb.append("- Cloud-Agnostic: ZERO dependencies on cloud-specific services\n");
        sb.append("- Horizontal scalability: Application must be stateless\n");
        sb.append(
                "- Externalized configuration: All configuration via environment variables"
                        + " or ConfigMaps\n");

        return sb.toString();
    }
}
