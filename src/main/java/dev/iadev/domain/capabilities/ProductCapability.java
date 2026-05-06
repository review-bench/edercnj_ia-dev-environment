package dev.iadev.domain.capabilities;

import java.util.Objects;

/**
 * A capability declared in the Product-First capability catalog.
 *
 * <p>Maps 1-to-1 with a capability YAML file under {@code capabilities/product-first/}. During
 * generation, active {@link dev.iadev.domain.products.Product} records resolve their {@code
 * capabilityIds} set into {@code ProductCapability} instances via the catalog, which are then
 * forwarded to the {@code CapabilityAwareComposer}.
 *
 * <p>Introduced by EPIC-0077 (Product-First Lifecycle) via ADR-0031.
 *
 * @param id unique identifier for this capability
 * @param name human-readable display name
 * @param description one-line description used in generated documentation
 * @param universal when {@code true} this capability is always active (no project YAML opt-in
 *     needed)
 */
public record ProductCapability(
        ProductCapabilityId id, String name, String description, boolean universal) {

    /** Compact constructor — validates mandatory fields. */
    public ProductCapability {
        Objects.requireNonNull(id, "ProductCapability id must not be null");
        Objects.requireNonNull(name, "ProductCapability name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("ProductCapability name must not be blank");
        }
        Objects.requireNonNull(description, "ProductCapability description must not be null");
    }
}
