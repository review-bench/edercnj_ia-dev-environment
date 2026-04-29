package dev.iadev.domain.capability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable record reflecting a {@code capabilities/<category>/<id>.yaml} file.
 *
 * <p>Fields map 1:1 to schema v1.0 ({@code governance/schemas/capabilities-1.0.json}). Uses {@link
 * LinkedHashMap} internally to guarantee deterministic field ordering (RULE-004).
 */
public record CapabilityDefinition(
        CapabilityId id,
        CapabilityKind kind,
        String category,
        Optional<String> version,
        String status,
        String description,
        Map<String, ParameterSpec> parameters,
        List<CapabilityId> requires,
        List<CapabilityId> provides,
        List<CapabilityId> excludes,
        List<CapabilityId> expandsTo,
        List<String> tags) {

    public CapabilityDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(category, "category must not be null");
        version = version == null ? Optional.empty() : version;
        status = status == null ? "stable" : status;
        description = description == null ? "" : description;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        requires = requires == null ? List.of() : List.copyOf(requires);
        provides = provides == null ? List.of() : List.copyOf(provides);
        excludes = excludes == null ? List.of() : List.copyOf(excludes);
        expandsTo = expandsTo == null ? List.of() : List.copyOf(expandsTo);
        tags = tags == null ? List.of() : List.copyOf(tags);

        if (kind == CapabilityKind.PROFILE && expandsTo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Profile capability '" + id + "' must declare expands-to");
        }
    }

    public static CapabilityDefinition atomic(
            CapabilityId id, String category, String description) {
        return new CapabilityDefinition(
                id,
                CapabilityKind.ATOMIC,
                category,
                Optional.empty(),
                "stable",
                description,
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    public static CapabilityDefinition profile(
            CapabilityId id, String category, List<CapabilityId> expandsTo) {
        return new CapabilityDefinition(
                id,
                CapabilityKind.PROFILE,
                category,
                Optional.empty(),
                "stable",
                "",
                new LinkedHashMap<>(),
                List.of(),
                List.of(),
                List.of(),
                expandsTo,
                List.of());
    }

    public boolean isDeprecated() {
        return "deprecated".equals(status);
    }
}
