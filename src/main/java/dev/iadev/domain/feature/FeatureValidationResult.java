package dev.iadev.domain.feature;

import java.util.List;

public record FeatureValidationResult(boolean isValid, List<String> errors) {

    public static FeatureValidationResult success() {
        return new FeatureValidationResult(true, List.of());
    }

    public static FeatureValidationResult failure(List<String> errors) {
        return new FeatureValidationResult(false, List.copyOf(errors));
    }
}
