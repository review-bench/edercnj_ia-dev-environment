package dev.iadev.domain.products;

/**
 * Lifecycle status of a {@link Product} in the Product-First hierarchy.
 *
 * <p>Introduced by EPIC-0077 (Product-First Lifecycle) via ADR-0030 (Rule 14 extension).
 */
public enum ProductStatus {

    /** Product is in design / authoring phase; capability bundles not yet active. */
    DRAFT,

    /** Product is fully resolved; capability bundles are active for generation. */
    ACTIVE,

    /** Product has been superseded; retained for reference but excluded from generation. */
    DEPRECATED;

    /**
     * Parses a {@code ProductStatus} from a YAML string, case-insensitive.
     *
     * @param raw the raw YAML value
     * @return matching status, defaulting to {@code DRAFT} when {@code raw} is null or blank
     */
    public static ProductStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRAFT;
        }
        return switch (raw.trim().toUpperCase()) {
            case "ACTIVE" -> ACTIVE;
            case "DEPRECATED" -> DEPRECATED;
            default -> DRAFT;
        };
    }
}
