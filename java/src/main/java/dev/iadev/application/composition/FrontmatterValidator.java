package dev.iadev.application.composition;

import dev.iadev.domain.capability.CapabilityId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Validates frontmatter blocks in markdown files against schema v3.0 (RULE-002, RULE-006).
 *
 * <p>Required fields: {@code name}, {@code requires-capabilities}.
 * Missing {@code requires-capabilities} = frontmatter v2 = hard-fail (RULE-006).
 */
public final class FrontmatterValidator {

    private static final String FRONTMATTER_DELIMITER = "---";
    private static final Set<String> REQUIRED_FIELDS = Set.of("name", "requires-capabilities");

    public record ValidationResult(boolean passed, List<String> errors, List<String> warnings) {

        public static ValidationResult pass() {
            return new ValidationResult(true, List.of(), List.of());
        }

        public static ValidationResult failed(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors), List.of());
        }

        public static ValidationResult withWarnings(List<String> warnings) {
            return new ValidationResult(true, List.of(), List.copyOf(warnings));
        }

        public boolean ok() {
            return passed;
        }
    }

    public ValidationResult validate(Path file) {
        return validate(file, Set.of());
    }

    public ValidationResult validate(Path file, Set<String> knownCapabilityIds) {
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            return ValidationResult.failed(List.of("cannot read file: " + file));
        }

        String frontmatterYaml = extractFrontmatter(content);
        if (frontmatterYaml == null) {
            return ValidationResult.failed(List.of("missing frontmatter block in: " + file));
        }

        Map<String, Object> parsed = parseYaml(frontmatterYaml);
        if (parsed == null) {
            return ValidationResult.failed(List.of("invalid YAML frontmatter in: " + file));
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String required : REQUIRED_FIELDS) {
            if (!parsed.containsKey(required)) {
                errors.add("missing required field: " + required + " in: " + file);
            }
        }

        if (!errors.isEmpty()) return ValidationResult.failed(errors);

        if (!knownCapabilityIds.isEmpty()) {
            Object reqs = parsed.get("requires-capabilities");
            if (reqs instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String capId && !knownCapabilityIds.contains(capId)) {
                        warnings.add("capability " + capId + " not found in catalog (file: " + file + ")");
                    }
                }
            }
        }

        return warnings.isEmpty() ? ValidationResult.pass() : ValidationResult.withWarnings(warnings);
    }

    public List<ValidationResult> validateAll(Path root) {
        List<ValidationResult> results = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".md"))
                    .sorted()
                    .forEach(p -> results.add(validate(p)));
        } catch (IOException e) {
            results.add(ValidationResult.failed(List.of("error scanning root: " + root + ": " + e.getMessage())));
        }
        return results;
    }

    private String extractFrontmatter(String content) {
        String trimmed = content.stripLeading();
        if (!trimmed.startsWith(FRONTMATTER_DELIMITER)) return null;
        int start = trimmed.indexOf('\n') + 1;
        int end = trimmed.indexOf("\n" + FRONTMATTER_DELIMITER, start);
        if (end < 0) return null;
        return trimmed.substring(start, end);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yaml) {
        try {
            LoaderOptions opts = new LoaderOptions();
            Object parsed = new Yaml(new SafeConstructor(opts)).load(yaml);
            if (parsed instanceof Map) return (Map<String, Object>) parsed;
        } catch (Exception ignored) {}
        return null;
    }
}
