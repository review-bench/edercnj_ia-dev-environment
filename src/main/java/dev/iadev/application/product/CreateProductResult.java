package dev.iadev.application.product;

import dev.iadev.domain.capability.Capability;
import dev.iadev.domain.product.Product;
import java.util.List;

public record CreateProductResult(
        boolean successful,
        Product product,
        Capability c1Stub,
        List<String> validationErrors,
        long executionTimeMs) {

    public static CreateProductResult success(Product product, Capability c1Stub, long executionTimeMs) {
        return new CreateProductResult(true, product, c1Stub, List.of(), executionTimeMs);
    }

    public static CreateProductResult failure(List<String> validationErrors) {
        return new CreateProductResult(false, null, null, List.copyOf(validationErrors), 0L);
    }
}
