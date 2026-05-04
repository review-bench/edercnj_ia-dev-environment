package dev.iadev.domain.product;

import java.util.List;

public final class ProductToCapabilityTransformer {

    private static final int MIN_CAPABILITIES = 3;
    private static final int MAX_CAPABILITIES = 7;

    public ProductCapabilityDecomposition transform(
            String productId, Product product, List<String> capabilityNames) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (capabilityNames == null) {
            throw new IllegalArgumentException("capabilityNames must not be null");
        }
        if (capabilityNames.size() < MIN_CAPABILITIES || capabilityNames.size() > MAX_CAPABILITIES) {
            throw new IllegalArgumentException(
                    "capabilityNames must contain between " + MIN_CAPABILITIES
                            + " and " + MAX_CAPABILITIES + " entries, got: " + capabilityNames.size());
        }
        return new ProductCapabilityDecomposition(productId, capabilityNames);
    }
}
