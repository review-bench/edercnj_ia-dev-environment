package dev.iadev.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.product.AutoDecomposeHeuristic;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import dev.iadev.domain.product.ProductToCapabilityTransformer;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityDecompositionUseCase")
class CapabilityDecompositionUseCaseTest {

    private final CapabilityDecompositionUseCase useCase =
            new CapabilityDecompositionUseCase(
                    new AutoDecomposeHeuristic(), new ProductToCapabilityTransformer());

    private Product sampleProduct() {
        return new Product(
                "Analytics Platform",
                List.of(
                        new RNFRoot(
                                RNFCategory.PERFORMANCE, "sub-second latency", "load test", true),
                        new RNFRoot(RNFCategory.RELIABILITY, "99.99% uptime", "chaos test", true)));
    }

    @Test
    void execute_autoDecompose_returnsDecompositionWithAtLeastThreeCapabilities() {
        ProductCapabilityDecomposition result =
                useCase.execute("product-0001", sampleProduct(), List.of());
        assertThat(result.productId()).isEqualTo("product-0001");
        assertThat(result.capabilityNames()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void execute_explicitNames_returnsDecompositionWithThoseNames() {
        List<String> names = List.of("ingest", "query", "storage");
        ProductCapabilityDecomposition result =
                useCase.execute("product-0001", sampleProduct(), names);
        assertThat(result.capabilityNames()).containsExactly("ingest", "query", "storage");
    }

    @Test
    void execute_nullProductId_throwsIllegalArgument() {
        assertThatThrownBy(() -> useCase.execute(null, sampleProduct(), List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_nullProduct_throwsIllegalArgument() {
        assertThatThrownBy(() -> useCase.execute("product-0001", null, List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_tooFewExplicitNames_throwsIllegalArgument() {
        assertThatThrownBy(
                        () -> useCase.execute("product-0001", sampleProduct(), List.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_sevenExplicitNames_returnsDecomposition() {
        List<String> names = List.of("a", "b", "c", "d", "e", "f", "g");
        ProductCapabilityDecomposition result =
                useCase.execute("product-0001", sampleProduct(), names);
        assertThat(result.capabilityNames()).hasSize(7);
    }
}
