package dev.iadev.application.product;

import dev.iadev.domain.capability.Capability;
import dev.iadev.domain.capability.CapabilityStubFactory;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.RNFRootValidationResult;
import dev.iadev.domain.product.RNFRootValidator;

public final class CreateProductOrchestrationUseCase {

    private final IdeationValidator validator;
    private final IdeationToProductTransformer transformer;
    private final CapabilityStubFactory capabilityStubFactory;
    private final RNFRootValidator rnfValidator;

    public CreateProductOrchestrationUseCase(
            IdeationValidator validator,
            IdeationToProductTransformer transformer,
            CapabilityStubFactory capabilityStubFactory,
            RNFRootValidator rnfValidator) {
        this.validator = validator;
        this.transformer = transformer;
        this.capabilityStubFactory = capabilityStubFactory;
        this.rnfValidator = rnfValidator;
    }

    public CreateProductResult execute(String productId, IdeationTemplate ideation) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (ideation == null) {
            throw new IllegalArgumentException("ideation must not be null");
        }

        long start = System.currentTimeMillis();

        IdeationValidationResult validation = validator.validate(ideation);
        if (!validation.passed()) {
            return CreateProductResult.failure(validation.errors());
        }

        Product product = transformer.transform(ideation);

        RNFRootValidationResult rnfResult = rnfValidator.validate(product);
        if (!rnfResult.passed()) {
            return CreateProductResult.failure(rnfResult.errors());
        }

        Capability c1Stub = capabilityStubFactory.createC1Stub(productId);
        long elapsed = System.currentTimeMillis() - start;
        return CreateProductResult.success(product, c1Stub, elapsed);
    }
}
