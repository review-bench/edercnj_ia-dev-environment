package dev.iadev.domain.products;

import java.util.Objects;

/**
 * Value object identifying a Product in the Product-First hierarchy.
 *
 * <p>Canonical format: lowercase, hyphen-separated (e.g., {@code my-product}).
 * Validated at construction time — no null or blank values allowed.
 *
 * <p>Introduced by EPIC-0077 (Product-First Lifecycle) via ADR-0030 (Rule 14 extension).
 */
public final class ProductId {

    private final String value;

    private ProductId(String value) {
        this.value = value;
    }

    /**
     * Creates a {@code ProductId} from the given string.
     *
     * @param value non-null, non-blank product identifier
     * @return validated {@code ProductId}
     * @throws IllegalArgumentException if {@code value} is null or blank
     */
    public static ProductId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId value must not be null or blank");
        }
        return new ProductId(value.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductId other)) return false;
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
