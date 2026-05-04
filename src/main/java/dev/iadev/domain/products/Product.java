package dev.iadev.domain.products;

import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root of the Product-First hierarchy.
 *
 * <p>A {@code Product} groups a named set of capability IDs under a unique {@link ProductId}.
 * During generation, the {@code CapabilityResolver} expands the {@code capabilityIds} set into
 * the full {@code ResolvedCapabilitySet} that drives output composition.
 *
 * <p>Introduced by EPIC-0077 (Product-First Lifecycle) via ADR-0030 (Rule 14 extension).
 *
 * @param id unique identifier for this product
 * @param name human-readable display name
 * @param status lifecycle status controlling generation eligibility
 * @param capabilityIds the set of capability IDs bundled by this product
 */
public record Product(ProductId id, String name, ProductStatus status, Set<String> capabilityIds) {

    /**
     * Compact constructor — validates mandatory fields.
     */
    public Product {
        Objects.requireNonNull(id, "Product id must not be null");
        Objects.requireNonNull(name, "Product name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        Objects.requireNonNull(status, "Product status must not be null");
        capabilityIds = capabilityIds == null ? Set.of() : Set.copyOf(capabilityIds);
    }

    /**
     * Returns {@code true} when this product is eligible for capability resolution.
     *
     * <p>Only {@link ProductStatus#ACTIVE} products contribute capability bundles to
     * the output composition pipeline.
     */
    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }
}
