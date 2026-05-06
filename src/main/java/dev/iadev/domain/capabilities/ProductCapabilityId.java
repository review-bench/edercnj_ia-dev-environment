package dev.iadev.domain.capabilities;

import java.util.Objects;

/**
 * Value object for a Product-First capability identifier.
 *
 * <p>Format: {@code category.name} (e.g., {@code product-first.c4-model-mandatory}). Distinct from
 * {@link dev.iadev.domain.capability.CapabilityId} which supports glob patterns for the EPIC-0064
 * composition resolver. This simpler variant is used exclusively for the Product-First hierarchy
 * (EPIC-0077, ADR-0031) where capabilities are referenced by exact ID.
 *
 * <p>Introduced by EPIC-0077 (Product-First Lifecycle) via ADR-0031.
 */
public final class ProductCapabilityId {

    private final String value;

    private ProductCapabilityId(String value) {
        this.value = value;
    }

    /**
     * Creates a {@code ProductCapabilityId} from the given string.
     *
     * @param value non-null, non-blank capability identifier (e.g., {@code
     *     product-first.c4-model-mandatory})
     * @return validated {@code ProductCapabilityId}
     * @throws IllegalArgumentException if {@code value} is null or blank
     */
    public static ProductCapabilityId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "ProductCapabilityId value must not be null or blank");
        }
        return new ProductCapabilityId(value.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCapabilityId other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
