package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.product.CapabilityDecompositionUseCase;
import dev.iadev.application.product.CreateCapabilitiesOrchestrationUseCase;
import dev.iadev.application.product.CreateCapabilitiesResult;
import dev.iadev.domain.product.AutoDecomposeHeuristic;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductToCapabilityTransformer;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("XCreateCapability E2E Smoke")
class XCreateCapabilityE2ETest {

    @TempDir
    Path outputDir;

    private final CreateCapabilitiesOrchestrationUseCase useCase =
            new CreateCapabilitiesOrchestrationUseCase(
                    new CapabilityDecompositionUseCase(
                            new AutoDecomposeHeuristic(),
                            new ProductToCapabilityTransformer()));

    private static Product analyticsProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "sub-second latency", "load test", true),
                new RNFRoot(RNFCategory.RELIABILITY, "99.99% uptime", "chaos test", true),
                new RNFRoot(RNFCategory.SECURITY, "AES-256 at rest", "audit", true),
                new RNFRoot(RNFCategory.COMPLIANCE, "GDPR compliant", "legal review", true)));
    }

    @Test
    @DisplayName("autoDecompose_writesCapabilityAndRnfArtifacts")
    void autoDecompose_writesCapabilityAndRnfArtifacts() throws IOException {
        CreateCapabilitiesResult result =
                useCase.execute("product-0001", analyticsProduct(), List.of(), outputDir);

        assertThat(result.capabilityCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.rnfInheritanceWritten()).isTrue();

        long capabilityFiles = Files.list(outputDir)
                .filter(p -> p.getFileName().toString().contains("capability-c"))
                .count();
        assertThat(capabilityFiles).isGreaterThanOrEqualTo(3);
        assertThat(outputDir.resolve("product-0001-rnf-inheritance.json")).exists();
    }

    @Test
    @DisplayName("explicitCapabilities_writesExactThreeArtifacts")
    void explicitCapabilities_writesExactThreeArtifacts() throws IOException {
        CreateCapabilitiesResult result = useCase.execute(
                "product-0001", analyticsProduct(),
                List.of("ingest", "query", "storage"), outputDir);

        assertThat(result.capabilityCount()).isEqualTo(3);
        assertThat(outputDir.resolve("product-0001-capability-c1.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c2.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c3.json")).exists();
    }

    @Test
    @DisplayName("capabilityArtifact_containsCapabilityNameAndProductId")
    void capabilityArtifact_containsCapabilityNameAndProductId() throws IOException {
        useCase.execute("product-0001", analyticsProduct(),
                List.of("ingest", "query", "storage"), outputDir);

        String content = Files.readString(outputDir.resolve("product-0001-capability-c1.json"));
        assertThat(content).contains("ingest").contains("product-0001");
    }

    @Test
    @DisplayName("rerun_isIdempotent_allSkipped")
    void rerun_isIdempotent_allSkipped() throws IOException {
        List<String> names = List.of("ingest", "query", "storage");
        useCase.execute("product-0001", analyticsProduct(), names, outputDir);

        CreateCapabilitiesResult second =
                useCase.execute("product-0001", analyticsProduct(), names, outputDir);

        assertThat(second.capabilityCount()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("rnfInheritanceArtifact_containsAllCategories")
    void rnfInheritanceArtifact_containsAllCategories() throws IOException {
        useCase.execute("product-0001", analyticsProduct(),
                List.of("ingest", "query", "storage"), outputDir);

        String content = Files.readString(outputDir.resolve("product-0001-rnf-inheritance.json"));
        assertThat(content).contains("PERFORMANCE", "RELIABILITY", "SECURITY", "COMPLIANCE");
    }
}
