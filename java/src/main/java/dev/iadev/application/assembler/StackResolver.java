package dev.iadev.application.assembler;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Maps a project's (language, framework, buildTool) triple to a script-template directory.
 *
 * <p>Template directories live under {@code targets/claude/scripts/{stack}/}. Stacks with
 * runtime-specific audit scripts are listed in {@link #supportedStacks()}; all other combinations
 * fall back to {@code _default/} (markdown audits only).
 *
 * <p>Security: directory names are validated via {@link #isStackSafe(String)} before use, blocking
 * path-traversal attempts.
 */
public final class StackResolver {

    private static final Logger LOG = Logger.getLogger(StackResolver.class.getName());

    static final String DEFAULT_STACK = "_default";

    private static final Pattern SAFE_STACK_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    private static final List<String> RUNTIME_STACKS =
            List.of("java-maven", "java-gradle", "spring-boot", "node", "python", "go");

    private static final Map<String, String> FRAMEWORK_OVERRIDES =
            Map.of("spring-boot", "spring-boot");

    private static final Map<String, String> LANGUAGE_STACKS =
            Map.of(
                    "java", "java-maven",
                    "go", "go",
                    "python", "python",
                    "node", "node",
                    "javascript", "node",
                    "typescript", "node");

    private static final Map<String, String> BUILD_TOOL_OVERRIDES =
            Map.of("gradle", "java-gradle", "go-mod", "go", "npm", "node", "pip", "python");

    /**
     * Returns the template directory name for the given project coordinates.
     *
     * @param language the project language (e.g. "java", "python")
     * @param framework the framework name (e.g. "spring-boot", "gin")
     * @param buildTool the build tool (e.g. "maven", "gradle", "npm")
     * @return stack directory name (never null; falls back to {@code _default})
     */
    public String resolveTemplateDir(String language, String framework, String buildTool) {
        if (language == null) {
            throw new IllegalArgumentException("language must not be null");
        }

        String normalizedLanguage = language.toLowerCase();
        String normalizedFramework = framework == null ? "" : framework.toLowerCase();
        String normalizedBuildTool = buildTool == null ? "" : buildTool.toLowerCase();

        String stack = resolveStack(normalizedLanguage, normalizedFramework, normalizedBuildTool);

        if (!isStackSafe(stack) && !stack.equals(DEFAULT_STACK)) {
            LOG.warning(() ->
                    "Stack name '" + stack + "' failed safety check — falling back to _default");
            return DEFAULT_STACK;
        }
        return stack;
    }

    /** Returns the list of stacks with runtime audit scripts (excludes {@code _default}). */
    public List<String> supportedStacks() {
        return List.copyOf(RUNTIME_STACKS);
    }

    /**
     * Validates that a stack name is safe to use as a path component.
     *
     * <p>Accepts only lowercase letters, digits, and hyphens starting with a letter.
     * Rejects {@code _default} (uses underscore) and path-traversal patterns.
     *
     * @param stack the stack name to validate
     * @return {@code true} if the name is safe
     */
    public static boolean isStackSafe(String stack) {
        return stack != null && SAFE_STACK_PATTERN.matcher(stack).matches();
    }

    private String resolveStack(String language, String framework, String buildTool) {
        if (FRAMEWORK_OVERRIDES.containsKey(framework)) {
            return FRAMEWORK_OVERRIDES.get(framework);
        }
        if ("java".equals(language) && BUILD_TOOL_OVERRIDES.containsKey(buildTool)) {
            return BUILD_TOOL_OVERRIDES.get(buildTool);
        }
        if (LANGUAGE_STACKS.containsKey(language)) {
            return LANGUAGE_STACKS.get(language);
        }
        LOG.warning(() -> "Stack '" + language + "/" + framework
                + "' not supported — falling back to _default");
        return DEFAULT_STACK;
    }
}
