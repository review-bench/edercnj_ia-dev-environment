package dev.iadev.domain.product;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RNFRootValidatorTest {

    private final RNFRootValidator validator = new RNFRootValidator();

    @Test
    void completeProduct_allMandatoryCategories_passes() {
        Product product = productWithAllMandatory();

        var result = validator.validate(product);

        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void missingSecurity_reportsSpecificError() {
        Product product = productWithout(RNFCategory.SECURITY);

        var result = validator.validate(product);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("SECURITY"));
    }

    @Test
    void missingPerformance_reportsSpecificError() {
        Product product = productWithout(RNFCategory.PERFORMANCE);

        var result = validator.validate(product);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("PERFORMANCE"));
    }

    @Test
    void onlyTwoMandatoryCategories_reportsCountError() {
        List<RNFRoot> rnfs = List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "P99 < 200ms", "load test", true),
                new RNFRoot(RNFCategory.SECURITY, "OAuth 2.0", "pen test", true)
        );
        Product product = new Product("Test Product", rnfs);

        var result = validator.validate(product);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("minimum 6 mandatory"));
    }

    @Test
    void allSixMandatoryCategories_noOptionals_passes() {
        List<RNFRoot> rnfs = List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "P99 < 200ms", "k6", true),
                new RNFRoot(RNFCategory.SCALABILITY, "10x peak", "chaos test", true),
                new RNFRoot(RNFCategory.RELIABILITY, "99.9% SLA", "uptime", true),
                new RNFRoot(RNFCategory.SECURITY, "OAuth 2.0", "pen test", true),
                new RNFRoot(RNFCategory.COMPLIANCE, "LGPD", "audit", true),
                new RNFRoot(RNFCategory.OBSERVABILITY, "trace_id", "Grafana", true)
        );
        Product product = new Product("Minimal Compliant Product", rnfs);

        var result = validator.validate(product);

        assertThat(result.passed()).isTrue();
    }

    @Test
    void blankRNFDescription_reportsContentError() {
        List<RNFRoot> rnfs = mandatoryRnfs();
        rnfs.set(0, new RNFRoot(RNFCategory.PERFORMANCE, "", "k6", true));
        Product product = new Product("Test Product", rnfs);

        var result = validator.validate(product);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("PERFORMANCE") && e.contains("description"));
    }

    @Test
    void multipleViolations_reportsAll() {
        Product product = new Product("Empty Product", List.of());

        var result = validator.validate(product);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).hasSizeGreaterThanOrEqualTo(2);
    }

    private Product productWithAllMandatory() {
        return new Product("Full Product", mandatoryRnfs());
    }

    private Product productWithout(RNFCategory excluded) {
        List<RNFRoot> rnfs = mandatoryRnfs();
        rnfs.removeIf(r -> r.category() == excluded);
        return new Product("Product without " + excluded, rnfs);
    }

    private java.util.ArrayList<RNFRoot> mandatoryRnfs() {
        java.util.ArrayList<RNFRoot> list = new java.util.ArrayList<>();
        list.add(new RNFRoot(RNFCategory.PERFORMANCE, "P99 < 200ms", "k6", true));
        list.add(new RNFRoot(RNFCategory.SCALABILITY, "10x peak", "chaos test", true));
        list.add(new RNFRoot(RNFCategory.RELIABILITY, "99.9% SLA", "uptime", true));
        list.add(new RNFRoot(RNFCategory.SECURITY, "OAuth 2.0 + MFA", "pen test", true));
        list.add(new RNFRoot(RNFCategory.COMPLIANCE, "LGPD DPA", "audit", true));
        list.add(new RNFRoot(RNFCategory.OBSERVABILITY, "trace_id propagated", "Grafana", true));
        return list;
    }
}
