package dev.iadev.domain.product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RNFRootValidator {

    private static final int MIN_MANDATORY_CATEGORIES = 6;
    private static final int MIN_TOTAL_CATEGORIES = 10;
    static final String MIN_TOTAL_CATEGORIES_ERROR =
            "Minimum 10 RNF categories required (6 mandatory + 4 optional minimum)";

    public RNFRootValidationResult validate(Product product) {
        List<String> errors = new ArrayList<>();
        validateDescriptions(product, errors);
        validateMandatoryCategories(product, errors);
        validateTotalCount(product, errors);
        if (errors.isEmpty()) {
            return RNFRootValidationResult.success();
        }
        return RNFRootValidationResult.failure(errors);
    }

    private void validateDescriptions(Product product, List<String> errors) {
        for (RNFRoot rnf : product.rnfRoots()) {
            if (rnf.description() == null || rnf.description().isBlank()) {
                errors.add("RNF " + rnf.category() + " description must not be blank");
            }
        }
    }

    private void validateMandatoryCategories(Product product, List<String> errors) {
        Set<RNFCategory> presentCategories =
                product.rnfRoots().stream().map(RNFRoot::category).collect(Collectors.toSet());

        List<RNFCategory> mandatoryMissing =
                Arrays.stream(RNFCategory.values())
                        .filter(RNFCategory::isMandatory)
                        .filter(c -> !presentCategories.contains(c))
                        .toList();

        for (RNFCategory missing : mandatoryMissing) {
            errors.add("Mandatory RNF category missing: " + missing);
        }

        long mandatoryPresent = presentCategories.stream().filter(RNFCategory::isMandatory).count();
        if (mandatoryPresent < MIN_MANDATORY_CATEGORIES) {
            errors.add(
                    "Product must have minimum 6 mandatory RNF categories, found: "
                            + mandatoryPresent);
        }
    }

    private void validateTotalCount(Product product, List<String> errors) {
        if (product.rnfRoots().size() < MIN_TOTAL_CATEGORIES) {
            errors.add(MIN_TOTAL_CATEGORIES_ERROR);
        }
    }
}
