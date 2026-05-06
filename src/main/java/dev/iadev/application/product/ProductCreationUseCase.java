package dev.iadev.application.product;

import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.RNFRootValidationResult;
import dev.iadev.domain.product.RNFRootValidator;

public final class ProductCreationUseCase {

    private final RNFRootValidator validator;

    public ProductCreationUseCase(RNFRootValidator validator) {
        this.validator = validator;
    }

    public RNFRootValidationResult create(Product product) {
        return validator.validate(product);
    }
}
