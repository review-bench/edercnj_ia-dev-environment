package dev.iadev.application.ideation;

import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;

public final class IdeationValidationUseCase {

    private final IdeationValidator validator;

    public IdeationValidationUseCase(IdeationValidator validator) {
        this.validator = validator;
    }

    public IdeationValidationResult execute(IdeationTemplate ideation) {
        return validator.validate(ideation);
    }
}
