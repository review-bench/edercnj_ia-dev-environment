package dev.iadev.application.product;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.product.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RNFInheritanceUseCaseIT {

    private final RNFInheritanceUseCase useCase = new RNFInheritanceUseCase();

    @Test
    void capabilityInheritsAllProductRNFs() {
        Product product = productWithMandatoryRnfs();
        RNFInheritanceContext context = new RNFInheritanceContext(product, Set.of());

        List<RNFRoot> effective = useCase.computeEffective(context);

        assertThat(effective).hasSize(product.rnfRoots().size());
        assertThat(effective).containsExactlyElementsOf(product.rnfRoots());
    }

    @Test
    void capabilityNoRelaxSecurity_securityIsPreserved() {
        Product product = productWithMandatoryRnfs();
        RNFInheritanceContext context =
                new RNFInheritanceContext(product, Set.of(RNFCategory.SECURITY));

        List<RNFRoot> effective = useCase.computeEffective(context);

        assertThat(effective).anyMatch(r -> r.category() == RNFCategory.SECURITY && r.mandatory());
    }

    @Test
    void cascadeInheritance_allLevelsRetainMandatoryRNFs() {
        Product product = productWithMandatoryRnfs();
        RNFInheritanceContext capabilityContext =
                new RNFInheritanceContext(product, Set.of(RNFCategory.PERFORMANCE));
        List<RNFRoot> capabilityEffective = useCase.computeEffective(capabilityContext);

        Product capabilityAsProduct = new Product("CapabilityLevel", capabilityEffective);
        RNFInheritanceContext featureContext =
                new RNFInheritanceContext(capabilityAsProduct, Set.of());
        List<RNFRoot> featureEffective = useCase.computeEffective(featureContext);

        assertThat(featureEffective)
                .anyMatch(r -> r.category() == RNFCategory.PERFORMANCE && r.mandatory());
        assertThat(featureEffective)
                .anyMatch(r -> r.category() == RNFCategory.SECURITY && r.mandatory());
    }

    private Product productWithMandatoryRnfs() {
        return new Product(
                "Test Product",
                List.of(
                        new RNFRoot(RNFCategory.PERFORMANCE, "P99 < 200ms", "k6", true),
                        new RNFRoot(RNFCategory.SCALABILITY, "10x peak", "chaos", true),
                        new RNFRoot(RNFCategory.RELIABILITY, "99.9% SLA", "uptime", true),
                        new RNFRoot(RNFCategory.SECURITY, "OAuth 2.0 + MFA", "pen test", true),
                        new RNFRoot(RNFCategory.COMPLIANCE, "LGPD", "audit", true),
                        new RNFRoot(RNFCategory.OBSERVABILITY, "trace_id", "Grafana", true)));
    }
}
