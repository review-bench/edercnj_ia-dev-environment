package dev.iadev.domain.capability;

import dev.iadev.domain.product.RNFCategory;

public record RNFOverride(
        RNFCategory category,
        String originalValue,
        String overrideValue,
        boolean noRelaxed,
        String justification
) {
    public static RNFOverride noRelax(RNFCategory category, String originalValue) {
        return new RNFOverride(category, originalValue, null, true, null);
    }

    public static RNFOverride withOverride(RNFCategory category, String originalValue,
                                           String overrideValue, String justification) {
        return new RNFOverride(category, originalValue, overrideValue, false, justification);
    }

    public boolean isRelaxed() {
        return !noRelaxed;
    }
}
