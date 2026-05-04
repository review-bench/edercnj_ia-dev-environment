package dev.iadev.application.product;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityStubFactory;
import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidator;
import dev.iadev.domain.product.AutoDecomposeHeuristic;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductToCapabilityTransformer;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CreateCapabilitiesOrchestrationUseCase")
class CreateCapabilitiesOrchestrationUseCaseTest {

    @TempDir
    Path outputDir;

    private final CreateCapabilitiesOrchestrationUseCase useCase =
            new CreateCapabilitiesOrchestrationUseCase(
                    new CapabilityDecompositionUseCase(
                            new AutoDecomposeHeuristic(),
                            new ProductToCapabilityTransformer()));

    private Product sampleProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "sub-second latency", "load test", true),
                new RNFRoot(RNFCategory.RELIABILITY, "99.99% uptime", "chaos test", true),
                new RNFRoot(RNFCategory.SECURITY, "AES-256 at rest", "audit", true)));
    }

    @Test
    void execute_autoDecompose_writesCapabilityAndInheritanceArtifacts() throws IOException {
        CreateCapabilitiesResult result =
                useCase.execute("product-0001", sampleProduct(), List.of(), outputDir);
        assertThat(result.capabilityCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.rnfInheritanceWritten()).isTrue();
    }

    @Test
    void execute_explicitNames_writesExactCapabilities() throws IOException {
        List<String> names = List.of("ingest", "query", "storage");
        CreateCapabilitiesResult result =
                useCase.execute("product-0001", sampleProduct(), names, outputDir);
        assertThat(result.capabilityCount()).isEqualTo(3);
        assertThat(outputDir.resolve("product-0001-capability-c1.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c2.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c3.json")).exists();
    }

    @Test
    void execute_rerun_allSkipped() throws IOException {
        List<String> names = List.of("ingest", "query", "storage");
        useCase.execute("product-0001", sampleProduct(), names, outputDir);
        CreateCapabilitiesResult second =
                useCase.execute("product-0001", sampleProduct(), names, outputDir);
        assertThat(second.skippedCount()).isEqualTo(3);
    }
}
