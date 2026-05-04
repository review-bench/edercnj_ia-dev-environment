package dev.iadev.domain.ideation;

import java.util.List;

public record IdeationValidationResult(boolean passed, List<String> errors) {

    public static IdeationValidationResult success() {
        return new IdeationValidationResult(true, List.of());
    }

    public static IdeationValidationResult failure(List<String> errors) {
        return new IdeationValidationResult(false, List.copyOf(errors));
    }
}
