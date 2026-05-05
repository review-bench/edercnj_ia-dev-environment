package dev.iadev.domain.capability;

import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRootValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RNFNoRelaxValidator {

    private static final Set<RNFCategory> HARD_BLOCKED = Set.of(RNFCategory.SECURITY, RNFCategory.COMPLIANCE);

    public RNFRootValidationResult validate(List<RNFOverride> overrides) {
        List<String> errors = new ArrayList<>();
        for (RNFOverride override : overrides) {
            if (override.isRelaxed()) {
                validateRelaxedOverride(override, errors);
            }
        }
        if (errors.isEmpty()) {
            return RNFRootValidationResult.success();
        }
        return RNFRootValidationResult.failure(errors);
    }

    private void validateRelaxedOverride(RNFOverride override, List<String> errors) {
        if (HARD_BLOCKED.contains(override.category())) {
            errors.add("RNF category " + override.category() + " is mandatory and cannot be relaxed");
            return;
        }
        if (override.justification() == null || override.justification().isBlank()) {
            errors.add("RNF category " + override.category() + " override requires justification");
        }
        if (!override.hasFormalApproval()) {
            errors.add("RNF override detected without approval: " + override.category());
        }
    }
}
