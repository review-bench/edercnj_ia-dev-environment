package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.AuditScript;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles documentation artifacts for generated projects.
 *
 * <p>Provides three capabilities:
 *
 * <ol>
 *   <li>{@link #assemble} — renders {@code steering/service-architecture.md} from the service
 *       architecture template (EPIC-0014 original).
 *   <li>{@link #renderCatalog} — produces a markdown catalog of audit gates for a given stack from
 *       {@code _TEMPLATE-AUDIT-GATES-CATALOG.md} (EPIC-0061 story-0061-0003).
 *   <li>{@link #assembleSystemArchitecture} — renders {@code docs/architecture/system.md} from the
 *       system architecture template (EPIC-0070 story-0070-0004). Idempotent: skips if the output
 *       file already exists.
 * </ol>
 *
 * <p>Graceful no-op: if the source template does not exist in the resources directory, returns an
 * empty list (backward compatibility).
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class DocsAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md";
    static final String CATALOG_TEMPLATE_PATH = "shared/templates/_TEMPLATE-AUDIT-GATES-CATALOG.md";
    static final String SYSTEM_ARCH_TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-ARCHITECTURE-SYSTEM.md";
    private static final String OUTPUT_SUBDIR = "steering";
    private static final String OUTPUT_FILENAME = "service-architecture.md";
    private static final String SYSTEM_ARCH_OUTPUT = "docs/architecture/system.md";
    private static final String FRONTMATTER_DELIMITER = "---";
    private static final String DEFAULT_STACK = "_default";

    private final Path resourcesDir;

    /** Creates a DocsAssembler using classpath resources. */
    public DocsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a DocsAssembler with an explicit resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public DocsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> assemble(ProjectConfig config, TemplateEngine engine, Path outputDir) {
        Path templateFile = resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        Map<String, Object> context = ContextBuilder.buildContext(config);
        String rendered = engine.render(TEMPLATE_PATH, context);
        Path destDir = outputDir.resolve(OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);
        Path destFile = destDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    /**
     * Renders and writes {@code docs/architecture/system.md} for the generated project.
     *
     * <p>Idempotent: if the output file already exists it is left unchanged and an empty list is
     * returned. Frontmatter in the template (capability declaration) is stripped before rendering
     * so the output file contains only the document body.
     *
     * @param config the project configuration
     * @param engine the template rendering engine
     * @param outputDir the root output directory for the generated project
     * @return a list containing the path of the written file, or an empty list when skipped
     */
    public List<String> assembleSystemArchitecture(
            ProjectConfig config, TemplateEngine engine, Path outputDir) {
        Path templateFile = resourcesDir.resolve(SYSTEM_ARCH_TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        Path destFile = outputDir.resolve(SYSTEM_ARCH_OUTPUT);
        if (Files.exists(destFile)) {
            return List.of();
        }
        Map<String, Object> context = ContextBuilder.buildContext(config);
        String rendered = engine.render(SYSTEM_ARCH_TEMPLATE_PATH, context);
        rendered = stripFrontmatter(rendered);
        CopyHelpers.ensureDirectory(destFile.getParent());
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    /**
     * Strips YAML frontmatter ({@code ---...---}) from a rendered template output.
     *
     * <p>Frontmatter is present in the template source for capability-driven composition (Rule 28)
     * but must not appear in the generated output file.
     *
     * @param content the rendered template content, possibly starting with frontmatter
     * @return content with the leading frontmatter block removed, or the original content unchanged
     */
    static String stripFrontmatter(String content) {
        if (!content.startsWith(FRONTMATTER_DELIMITER)) {
            return content;
        }
        int secondDelimiter = content.indexOf(FRONTMATTER_DELIMITER, FRONTMATTER_DELIMITER.length());
        if (secondDelimiter < 0) {
            return content;
        }
        String afterFrontmatter =
                content.substring(secondDelimiter + FRONTMATTER_DELIMITER.length());
        return afterFrontmatter.replaceFirst("^\r?\n", "");
    }

    /**
     * Renders the audit gates catalog for a given stack and audit inventory.
     *
     * <p>Substitutes {@code {{STACK}}}, {@code {{TOTAL_AUDITS}}}, iterates {@code {{#each audit}}}
     * loops, and renders per-audit exit-code tables. Falls back gracefully to a minimal catalog
     * when the template is not found on classpath.
     *
     * @param stack the stack name (e.g., {@code "spring-boot"}, {@code "_default"})
     * @param inventory the list of audit scripts delivered for this stack
     * @return the rendered markdown catalog string
     */
    public String renderCatalog(String stack, List<AuditScript> inventory) {
        Path catalogTemplate = resourcesDir.resolve(CATALOG_TEMPLATE_PATH);
        String template =
                Files.exists(catalogTemplate)
                        ? readFile(catalogTemplate)
                        : buildFallbackCatalogTemplate();

        String result =
                template.replace("{{STACK}}", stack)
                        .replace("{{TOTAL_AUDITS}}", String.valueOf(inventory.size()));

        result = renderAuditLoop(result, inventory);
        result = renderConditionals(result, stack);
        return result;
    }

    private String renderAuditLoop(String template, List<AuditScript> inventory) {
        int eachStart = template.indexOf("{{#each audit}}");
        int eachEnd = template.lastIndexOf("{{/each}}");
        if (eachStart < 0 || eachEnd < 0) {
            return template;
        }
        String before = template.substring(0, eachStart);
        String block = template.substring(eachStart + "{{#each audit}}".length(), eachEnd);
        String after = template.substring(eachEnd + "{{/each}}".length());

        StringBuilder sb = new StringBuilder(before);
        for (AuditScript audit : inventory) {
            sb.append(renderAuditBlock(block, audit));
        }
        sb.append(after);
        return sb.toString();
    }

    private String renderAuditBlock(String block, AuditScript audit) {
        String rendered =
                block.replace("{{audit.name}}", audit.name())
                        .replace("{{audit.category}}", audit.category())
                        .replace("{{audit.validates}}", audit.validates())
                        .replace("{{audit.guarantees}}", audit.guarantees())
                        .replace("{{audit.ruleAnchor}}", audit.ruleAnchor());
        return renderExitCodeLoop(rendered, audit);
    }

    private String renderExitCodeLoop(String block, AuditScript audit) {
        int start = block.indexOf("{{#each audit.exitCodes}}");
        int end = block.indexOf("{{/each}}");
        if (start < 0 || end < 0) {
            return block;
        }
        String before = block.substring(0, start);
        String rowBlock = block.substring(start + "{{#each audit.exitCodes}}".length(), end);
        String after = block.substring(end + "{{/each}}".length());

        StringBuilder sb = new StringBuilder(before);
        for (AuditScript.ExitCodeEntry entry : audit.exitCodes()) {
            sb.append(
                    rowBlock.replace("{{exitCode.code}}", String.valueOf(entry.code()))
                            .replace("{{exitCode.constant}}", entry.constant())
                            .replace("{{exitCode.meaning}}", entry.meaning()));
        }
        sb.append(after);
        return sb.toString();
    }

    private String renderConditionals(String template, String stack) {
        boolean isDefault = DEFAULT_STACK.equals(stack);
        if (isDefault) {
            return template.replace("{{#if isDefaultStack}}", "").replace("{{/if}}", "");
        }
        int ifStart = template.indexOf("{{#if isDefaultStack}}");
        int ifEnd = template.indexOf("{{/if}}");
        if (ifStart < 0 || ifEnd < 0) {
            return template;
        }
        return template.substring(0, ifStart) + template.substring(ifEnd + "{{/if}}".length());
    }

    private static String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to read catalog template: " + file, e);
        }
    }

    private static String buildFallbackCatalogTemplate() {
        return """
                # Audit Gates Catalog — {{STACK}} Stack
                Stack: {{STACK}}
                Total Audits: {{TOTAL_AUDITS}}

                {{#each audit}}
                ### {{audit.name}}
                - **Category:** {{audit.category}}
                - **Validates:** {{audit.validates}}
                - **Rule:** {{audit.ruleAnchor}}
                {{/each}}
                {{#if isDefaultStack}}
                > Runtime audits require stack-specific knowledge — provide custom templates under targets/claude/scripts/{stack}/
                {{/if}}
                """;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver.resolveResourceDir("shared").getParent();
    }

    /** Returns a new list — no mutation of input. */
    private static List<AuditScript> copy(List<AuditScript> list) {
        return new ArrayList<>(list);
    }
}
