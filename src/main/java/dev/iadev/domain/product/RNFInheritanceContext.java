package dev.iadev.domain.product;

import java.util.Set;

public record RNFInheritanceContext(Product product, Set<RNFCategory> noRelaxCategories) {

    public RNFInheritanceContext {
        noRelaxCategories = Set.copyOf(noRelaxCategories);
    }

    public boolean isNoRelax(RNFCategory category) {
        return noRelaxCategories.contains(category);
    }
}
