package dev.iadev.domain.feature;

import java.util.ArrayList;
import java.util.List;

public class FeatureValidator {

    private static final int MIN_USE_CASES = 3;
    private static final int MAX_USE_CASES = 8;
    private static final int MIN_ACCEPTANCE_CRITERIA = 1;

    public FeatureValidationResult validate(Feature feature) {
        List<String> errors = new ArrayList<>();
        validateUseCases(feature.useCases(), errors);
        validateAcceptanceCriteria(feature.acceptanceCriteria(), errors);
        return errors.isEmpty() ? FeatureValidationResult.success() : FeatureValidationResult.failure(errors);
    }

    private void validateUseCases(List<UseCase> useCases, List<String> errors) {
        if (useCases.isEmpty()) {
            errors.add("Feature must have at least " + MIN_USE_CASES + " use cases");
            return;
        }
        if (useCases.size() < MIN_USE_CASES) {
            errors.add("Feature must have at least " + MIN_USE_CASES + " use cases, found " + useCases.size());
        }
        if (useCases.size() > MAX_USE_CASES) {
            errors.add("Feature must have at most " + MAX_USE_CASES + " use cases, found " + useCases.size());
        }
    }

    private void validateAcceptanceCriteria(List<AcceptanceCriterion> acs, List<String> errors) {
        if (acs.isEmpty()) {
            errors.add("Feature must have at least " + MIN_ACCEPTANCE_CRITERIA + " acceptance criteria");
        }
    }
}
