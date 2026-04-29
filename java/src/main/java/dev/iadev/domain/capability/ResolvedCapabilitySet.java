package dev.iadev.domain.capability;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of capability resolution — the effective set of capabilities for a profile.
 *
 * <p>Contains all transitive prerequisites in topological order, effective parameter values,
 * and any resolution warnings emitted during expansion.
 */
public record ResolvedCapabilitySet(
        String profileName,
        List<CapabilityId> capabilities,
        Map<String, String> effectiveParameters,
        List<ResolutionWarning> warnings) {

    public ResolvedCapabilitySet {
        Objects.requireNonNull(profileName, "profileName must not be null");
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        effectiveParameters = effectiveParameters == null ? Map.of() : Map.copyOf(effectiveParameters);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public Optional<String> parameter(String key) {
        return Optional.ofNullable(effectiveParameters.get(key));
    }

    public boolean contains(CapabilityId id) {
        return capabilities.contains(id);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
