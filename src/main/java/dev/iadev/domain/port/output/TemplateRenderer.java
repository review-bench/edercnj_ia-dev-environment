package dev.iadev.domain.port.output;

import java.util.Map;

/**
 * Output port for rendering templates with contextual data.
 *
 * <p>Abstracts the template engine used for code generation. The domain depends on this interface;
 * concrete implementations (e.g., Pebble-based rendering) reside in the infrastructure adapter
 * layer.
 *
 * <h2>Contract</h2>
 *
 * <ul>
 *   <li>{@link #render(String, Map)} MUST return the fully rendered content as a non-null string.
 *   <li>{@link #templateExists(String)} MUST NOT throw exceptions for missing templates — it
 *       returns false instead.
 *   <li>Context map values may be null; the renderer should handle null values gracefully.
 * </ul>
 *
 * <h2>Pre-conditions</h2>
 *
 * <ul>
 *   <li>{@code templatePath} must not be null or blank.
 *   <li>{@code context} must not be null (empty map is acceptable).
 * </ul>
 *
 * <h2>Post-conditions</h2>
 *
 * <ul>
 *   <li>Rendered output preserves template line endings.
 * </ul>
 *
 * <h2>Exceptions</h2>
 *
 * <ul>
 *   <li>{@link IllegalArgumentException} if templatePath is null or blank, or if context is null.
 *   <li>Implementation-specific unchecked exceptions for template parsing or rendering failures.
 * </ul>
 */
public interface TemplateRenderer {

    /**
     * Renders a template using the provided context variables.
     *
     * @param templatePath path to the template resource
     * @param context key-value pairs for template interpolation
     * @return the rendered content as a string
     * @throws IllegalArgumentException if templatePath is null/blank or context is null
     */
    String render(String templatePath, Map<String, Object> context);

    /**
     * Checks whether a template exists at the given path.
     *
     * @param templatePath path to the template resource
     * @return true if the template exists, false otherwise
     * @throws IllegalArgumentException if templatePath is null or blank
     */
    boolean templateExists(String templatePath);
}
