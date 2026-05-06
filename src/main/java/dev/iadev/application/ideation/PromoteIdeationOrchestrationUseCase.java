package dev.iadev.application.ideation;

import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationValidationResult;
import dev.iadev.domain.ideation.IdeationValidator;
import java.util.List;

public class PromoteIdeationOrchestrationUseCase {

    private final IdeationValidator validator;

    public PromoteIdeationOrchestrationUseCase(IdeationValidator validator) {
        if (validator == null) throw new IllegalArgumentException("validator must not be null");
        this.validator = validator;
    }

    public IdeationValidationResult validate(IdeationTemplate ideation) {
        if (ideation == null) {
            return IdeationValidationResult.failure(List.of("Ideation template must not be null"));
        }
        return validator.validate(ideation);
    }

    public IdeationPromotionResult promote(
            IdeationTemplate ideation, String requestedId, int nextSequence) {
        IdeationValidationResult validation = validate(ideation);
        if (!validation.passed()) {
            return IdeationPromotionResult.failed(String.join("; ", validation.errors()));
        }
        String resolvedId =
                requestedId != null && !requestedId.isBlank()
                        ? requestedId
                        : String.format("ideation-%04d", nextSequence);
        return IdeationPromotionResult.success(resolvedId);
    }
}
