package dev.iadev.domain.capability;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Profile record — a named macro that expands to a stable, ordered set of atomic capability IDs.
 *
 * <p>{@link #expand()} is deterministic: same inputs always produce the same {@link Set} instance
 * (RULE-004). Internally uses {@link LinkedHashSet} to preserve insertion order.
 */
public record Profile(String name, List<CapabilityId> capabilities) {

    public Profile {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("profile name must not be blank");
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }

    public static Profile of(String name, List<CapabilityId> capabilities) {
        return new Profile(name, capabilities);
    }

    public Set<CapabilityId> expand() {
        Set<CapabilityId> result = new LinkedHashSet<>();
        for (CapabilityId id : capabilities) {
            if (!id.isGlob()) result.add(id);
        }
        return result;
    }
}
