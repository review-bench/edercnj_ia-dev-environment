package dev.iadev.application.product;

import dev.iadev.domain.product.AutoDecomposeHeuristic;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import dev.iadev.domain.product.ProductToCapabilityTransformer;
import java.util.List;

public final class CapabilityDecompositionUseCase {

    private final AutoDecomposeHeuristic heuristic;
    private final ProductToCapabilityTransformer transformer;

    public CapabilityDecompositionUseCase(
            AutoDecomposeHeuristic heuristic, ProductToCapabilityTransformer transformer) {
        this.heuristic = heuristic;
        this.transformer = transformer;
    }

    public ProductCapabilityDecomposition execute(
            String productId, Product product, List<String> explicitNames) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        List<String> names =
                (explicitNames == null || explicitNames.isEmpty())
                        ? heuristic.decompose(product)
                        : explicitNames;
        return transformer.transform(productId, product, names);
    }
}
