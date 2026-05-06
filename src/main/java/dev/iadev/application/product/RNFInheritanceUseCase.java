package dev.iadev.application.product;

import dev.iadev.domain.product.RNFInheritanceContext;
import dev.iadev.domain.product.RNFRoot;
import java.util.List;

public final class RNFInheritanceUseCase {

    public List<RNFRoot> computeEffective(RNFInheritanceContext context) {
        return context.product().rnfRoots().stream()
                .map(rnf -> enforceNoRelax(rnf, context))
                .toList();
    }

    private RNFRoot enforceNoRelax(RNFRoot rnf, RNFInheritanceContext context) {
        if (context.isNoRelax(rnf.category()) && !rnf.mandatory()) {
            return new RNFRoot(rnf.category(), rnf.description(), rnf.verificationMethod(), true);
        }
        return rnf;
    }
}
