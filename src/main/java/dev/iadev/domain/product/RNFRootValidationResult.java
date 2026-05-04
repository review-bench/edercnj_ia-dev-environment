package dev.iadev.domain.product;

import java.util.List;

public record RNFRootValidationResult(boolean passed, List<String> errors) {

    public static RNFRootValidationResult success() {
        return new RNFRootValidationResult(true, List.of());
    }

    public static RNFRootValidationResult failure(List<String> errors) {
        return new RNFRootValidationResult(false, List.copyOf(errors));
    }
}
