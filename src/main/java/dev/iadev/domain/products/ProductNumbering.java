package dev.iadev.domain.products;

import java.util.Objects;

public final class ProductNumbering {

    private static final int MIN = 1;
    private static final int MAX = 9999;

    private final int sequence;

    private ProductNumbering(int sequence) {
        this.sequence = sequence;
    }

    public static ProductNumbering of(Integer sequence) {
        if (sequence == null) {
            throw new IllegalArgumentException("sequence must not be null");
        }
        if (sequence < MIN || sequence > MAX) {
            throw new IllegalArgumentException(
                    "sequence must be between " + MIN + " and " + MAX + ", got: " + sequence);
        }
        return new ProductNumbering(sequence);
    }

    public int sequence() {
        return sequence;
    }

    public String formatted() {
        return String.format("product-%04d", sequence);
    }

    public ProductNumbering next() {
        if (sequence == MAX) {
            throw new IllegalStateException(
                    "ProductNumbering sequence overflow: max is "
                            + MAX
                            + " (current: "
                            + sequence
                            + ")");
        }
        return new ProductNumbering(sequence + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductNumbering other)) return false;
        return sequence == other.sequence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
