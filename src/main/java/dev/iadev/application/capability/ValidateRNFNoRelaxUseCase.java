package dev.iadev.application.capability;

import dev.iadev.domain.capability.RNFNoRelaxValidator;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFRootValidationResult;

import java.util.List;

public class ValidateRNFNoRelaxUseCase {

    private final RNFNoRelaxValidator validator;

    public ValidateRNFNoRelaxUseCase() {
        this.validator = new RNFNoRelaxValidator();
    }

    public RNFRootValidationResult execute(List<RNFOverride> overrides) {
        if (overrides == null) {
            throw new IllegalArgumentException("overrides must not be null");
        }
        return validator.validate(overrides);
    }
}
