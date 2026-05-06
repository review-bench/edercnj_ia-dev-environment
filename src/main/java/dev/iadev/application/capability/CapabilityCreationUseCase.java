package dev.iadev.application.capability;

import dev.iadev.domain.capability.Capability;
import dev.iadev.domain.capability.RNFNoRelaxValidator;
import dev.iadev.domain.product.RNFRootValidationResult;

public final class CapabilityCreationUseCase {

    private final RNFNoRelaxValidator validator;

    public CapabilityCreationUseCase(RNFNoRelaxValidator validator) {
        this.validator = validator;
    }

    public RNFRootValidationResult create(Capability capability) {
        return validator.validate(capability.rnfOverrides());
    }
}
