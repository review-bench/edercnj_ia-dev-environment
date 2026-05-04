package dev.iadev.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutoDecomposeHeuristic")
class AutoDecomposeHeuristicTest {

    private final AutoDecomposeHeuristic heuristic = new AutoDecomposeHeuristic();

    private Product sampleProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "sub-second latency", "load test", true),
                new RNFRoot(RNFCategory.RELIABILITY, "99.99% uptime", "chaos test", true),
                new RNFRoot(RNFCategory.SECURITY, "AES-256 at rest", "audit", true),
                new RNFRoot(RNFCategory.COMPLIANCE, "GDPR compliant", "legal review", true)));
    }

    @Test
    void decompose_nullProduct_throwsIllegalArgument() {
        assertThatThrownBy(() -> heuristic.decompose(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decompose_validProduct_returnsAtLeastThreeCapabilities() {
        List<String> result = heuristic.decompose(sampleProduct());
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void decompose_validProduct_returnsAtMostSevenCapabilities() {
        List<String> result = heuristic.decompose(sampleProduct());
        assertThat(result).hasSizeLessThanOrEqualTo(7);
    }

    @Test
    void decompose_validProduct_noDuplicates() {
        List<String> result = heuristic.decompose(sampleProduct());
        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    void decompose_validProduct_allNamesNonBlank() {
        List<String> result = heuristic.decompose(sampleProduct());
        assertThat(result).allSatisfy(name -> assertThat(name).isNotBlank());
    }

    @Test
    void decompose_productWithNoRnfRoots_returnsDefaultCapabilities() {
        Product empty = new Product("EmptyProduct", List.of());
        List<String> result = heuristic.decompose(empty);
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    }
}
