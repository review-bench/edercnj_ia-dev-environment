package dev.iadev.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductToCapabilityTransformer")
class ProductToCapabilityTransformerTest {

    private final ProductToCapabilityTransformer transformer = new ProductToCapabilityTransformer();

    private Product sampleProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "sub-second latency", "load test", true)));
    }

    @Test
    void transform_validProductAndNames_returnsDecomposition() {
        List<String> names = List.of("ingest", "query", "storage");
        ProductCapabilityDecomposition result = transformer.transform("product-0001", sampleProduct(), names);
        assertThat(result.productId()).isEqualTo("product-0001");
        assertThat(result.capabilityNames()).containsExactly("ingest", "query", "storage");
    }

    @Test
    void transform_nullProductId_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                transformer.transform(null, sampleProduct(), List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_nullProduct_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                transformer.transform("product-0001", null, List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_nullNames_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                transformer.transform("product-0001", sampleProduct(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_tooFewNames_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                transformer.transform("product-0001", sampleProduct(), List.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_tooManyNames_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                transformer.transform("product-0001", sampleProduct(),
                        List.of("a", "b", "c", "d", "e", "f", "g", "h")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_sevenNames_returnsDecomposition() {
        List<String> names = List.of("a", "b", "c", "d", "e", "f", "g");
        ProductCapabilityDecomposition result = transformer.transform("product-0001", sampleProduct(), names);
        assertThat(result.capabilityNames()).hasSize(7);
    }
}
