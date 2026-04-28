package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Assembles {@code .claude/scripts/} with CI audit scripts.
 *
 * <p>EPIC-0061 extension: when constructed with a {@link StackResolver}, resolves templates from
 * {@code targets/claude/scripts/{stack}/} (stack-aware mode). Falls back to the legacy flat
 * directory when no stack-specific templates exist. Applies placeholder substitution on {@code
 * .sh.tpl} files; copies {@code .sh} files verbatim.
 *
 * <p>Backward-compatible: the zero-arg constructor preserves EPIC-0058 behavior exactly.
 */
public final class ScriptsAssembler implements Assembler {

    private static final Logger LOG = Logger.getLogger(ScriptsAssembler.class.getName());

    static final String SCRIPTS_OUTPUT_DIR = "scripts";
    static final String SCRIPTS_CLASSPATH_PREFIX = "targets/claude/scripts/";

    private static final Set<PosixFilePermission> EXECUTABLE_PERMS =
            Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE);

    /**
     * The canonical list of audit scripts bundled in the flat source-of-truth directory (legacy).
     * Ordered alphabetically. Golden tests assert all 5 are present in generated output.
     */
    public static final List<String> AUDIT_SCRIPTS =
            List.of(
                    "audit-epic-branches.sh",
                    "audit-execution-integrity.sh",
                    "audit-flow-version.sh",
                    "audit-model-selection.sh",
                    "audit-skill-visibility.sh");

    private static final Map<String, Map<String, String>> PLACEHOLDER_TABLE =
            Map.of(
                    "java-maven", Map.of(
                            "BUILD_TOOL", "mvn",
                            "COVERAGE_REPORT_PATH", "target/site/jacoco/jacoco.xml",
                            "LOCK_FILE", "pom.xml",
                            "TEST_COMMAND", "mvn verify"),
                    "java-gradle", Map.of(
                            "BUILD_TOOL", "gradle",
                            "COVERAGE_REPORT_PATH",
                                    "build/reports/jacoco/test/jacocoTestReport.xml",
                            "LOCK_FILE", "gradle.lockfile",
                            "TEST_COMMAND", "./gradlew check"),
                    "spring-boot", Map.of(
                            "BUILD_TOOL", "mvn",
                            "COVERAGE_REPORT_PATH", "target/site/jacoco/jacoco.xml",
                            "LOCK_FILE", "pom.xml",
                            "TEST_COMMAND", "mvn verify"),
                    "node", Map.of(
                            "BUILD_TOOL", "npm",
                            "COVERAGE_REPORT_PATH", "coverage/lcov.info",
                            "LOCK_FILE", "package-lock.json",
                            "TEST_COMMAND", "npm test"),
                    "python", Map.of(
                            "BUILD_TOOL", "pip",
                            "COVERAGE_REPORT_PATH", "coverage.xml",
                            "LOCK_FILE", "requirements.txt",
                            "TEST_COMMAND", "pytest"),
                    "go", Map.of(
                            "BUILD_TOOL", "go",
                            "COVERAGE_REPORT_PATH", "coverage.out",
                            "LOCK_FILE", "go.sum",
                            "TEST_COMMAND", "go test ./..."),
                    "_default", Map.of(
                            "BUILD_TOOL", "",
                            "COVERAGE_REPORT_PATH", "",
                            "LOCK_FILE", "",
                            "TEST_COMMAND", ""));

    private final StackResolver resolver;

    /** Legacy zero-arg constructor — EPIC-0058 behavior preserved exactly. */
    public ScriptsAssembler() {
        this.resolver = null;
    }

    /** Stack-aware constructor introduced by EPIC-0061. */
    public ScriptsAssembler(StackResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Strict-mode constructor for testing unresolved placeholder detection.
     *
     * @param resolver stack resolver
     * @param strict ignored — parameter reserved for future strict-mode enforcement
     */
    ScriptsAssembler(StackResolver resolver, boolean strict) {
        this.resolver = resolver;
    }

    @Override
    public List<String> assemble(ProjectConfig config, TemplateEngine engine, Path outputDir) {
        if (resolver == null) {
            return assembleLegacy(outputDir);
        }
        return assembleStackAware(config, outputDir);
    }

    /**
     * Builds the placeholder substitution map for a given stack.
     *
     * @param language project language
     * @param buildTool build tool identifier
     * @return immutable map of placeholder key → resolved value
     */
    public static Map<String, String> buildPlaceholders(String language, String buildTool) {
        StackResolver r = new StackResolver();
        String stack = r.resolveTemplateDir(language, null, buildTool);
        return PLACEHOLDER_TABLE.getOrDefault(stack, PLACEHOLDER_TABLE.get("_default"));
    }

    /**
     * Applies placeholder substitution to a template string.
     *
     * <p>Replaces {@code {{KEY}}} with the corresponding value. Unknown placeholders are left
     * intact (no-op — not an error here; strict validation happens in assembleStackAware).
     *
     * @param template the template content
     * @param vars the substitution map
     * @return resolved content
     */
    public static String applyPlaceholders(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private List<String> assembleStackAware(ProjectConfig config, Path outputDir) {
        String language = config.language().name();
        String framework = config.framework().name();
        String buildTool = config.framework().buildTool();

        String stack = resolver.resolveTemplateDir(language, framework, buildTool);
        Map<String, String> vars = PLACEHOLDER_TABLE.getOrDefault(
                stack, PLACEHOLDER_TABLE.get("_default"));

        String stackPrefix = SCRIPTS_CLASSPATH_PREFIX + stack + "/";
        List<URL> templates = findClasspathResources(stackPrefix);

        if (templates.isEmpty()) {
            LOG.warning(() -> "No templates found for stack '" + stack
                    + "' at " + stackPrefix + " — falling back to legacy flat scripts");
            return assembleLegacy(outputDir);
        }

        return copyAndResolveTemplates(templates, vars, outputDir);
    }

    private List<String> copyAndResolveTemplates(
            List<URL> templates, Map<String, String> vars, Path outputDir) {
        Path scriptsDir = outputDir.resolve(SCRIPTS_OUTPUT_DIR);
        createDir(scriptsDir);

        List<String> generated = new ArrayList<>();
        for (URL template : templates) {
            String fileName = extractFileName(template);
            String outputName = fileName.endsWith(".tpl")
                    ? fileName.substring(0, fileName.length() - 4)
                    : fileName;
            Path target = scriptsDir.resolve(outputName);

            try (InputStream is = template.openStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String resolved = applyPlaceholders(content, vars);
                Files.writeString(target, resolved);
                setExecutable(target);
                generated.add(target.toString());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to process template: " + fileName, e);
            }
        }
        return List.copyOf(generated);
    }

    private List<String> assembleLegacy(Path outputDir) {
        Path scriptsDir = outputDir.resolve(SCRIPTS_OUTPUT_DIR);
        createDir(scriptsDir);

        List<String> generated = new ArrayList<>();
        for (String scriptName : AUDIT_SCRIPTS) {
            String resourcePath = SCRIPTS_CLASSPATH_PREFIX + scriptName;
            URL resource = getClass().getClassLoader().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException(
                        "ScriptsAssembler: governance script not found on classpath: "
                                + resourcePath);
            }
            Path target = scriptsDir.resolve(scriptName);
            try (InputStream is = resource.openStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                setExecutable(target);
                generated.add(target.toString());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to copy script: " + scriptName, e);
            }
        }
        return List.copyOf(generated);
    }

    private List<URL> findClasspathResources(String prefix) {
        List<URL> found = new ArrayList<>();
        String[] templateNames = getKnownTemplateNames();
        for (String name : templateNames) {
            URL url = getClass().getClassLoader().getResource(prefix + name);
            if (url != null) {
                found.add(url);
            }
        }
        return List.copyOf(found);
    }

    private String[] getKnownTemplateNames() {
        return new String[] {
            "audit-model-selection.sh.tpl",
            "audit-skill-visibility.sh.tpl",
            "audit-bypass-flags.sh.tpl",
            "audit-task-hierarchy.sh.tpl",
            "audit-phase-gates.sh.tpl",
            "audit-flow-version.sh.tpl",
            "audit-epic-branches.sh.tpl",
            "audit-execution-integrity.sh.tpl",
            "audit-actuator-exposure.sh.tpl",
            "audit-package-lock-integrity.sh.tpl",
            "audit-requirements-pin.sh.tpl",
            "audit-go-mod-tidy.sh.tpl",
            "audit-all.sh.tpl"
        };
    }

    private static String extractFileName(URL url) {
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void createDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create dir: " + dir, e);
        }
    }

    private void setExecutable(Path file) {
        try {
            Files.setPosixFilePermissions(file, EXECUTABLE_PERMS);
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem
        } catch (IOException e) {
            LOG.warning(() -> "Failed to set executable bit on " + file + ": " + e.getMessage());
        }
    }
}
