package dev.iadev.domain.product;

import java.util.List;

public record ProductCapabilityDecomposition(String productId, List<String> capabilityNames) {

    private static final int MIN_CAPABILITIES = 3;
    private static final int MAX_CAPABILITIES = 7;

    public ProductCapabilityDecomposition {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (capabilityNames == null || capabilityNames.isEmpty()) {
            throw new IllegalArgumentException("capabilityNames must not be null or empty");
        }
        if (capabilityNames.size() < MIN_CAPABILITIES
                || capabilityNames.size() > MAX_CAPABILITIES) {
            throw new IllegalArgumentException(
                    "capabilityNames must contain between "
                            + MIN_CAPABILITIES
                            + " and "
                            + MAX_CAPABILITIES
                            + " entries, got: "
                            + capabilityNames.size());
        }
        capabilityNames = List.copyOf(capabilityNames);
    }
}
