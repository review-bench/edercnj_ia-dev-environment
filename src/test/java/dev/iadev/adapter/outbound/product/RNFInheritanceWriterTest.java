package dev.iadev.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RNFInheritanceWriter")
class RNFInheritanceWriterTest {

    @TempDir
    Path outputDir;

    private Product sampleProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "sub-second latency", "load test", true),
                new RNFRoot(RNFCategory.SECURITY, "AES-256 at rest", "audit", true)));
    }

    private ProductCapabilityDecomposition decomposition() {
        return new ProductCapabilityDecomposition(
                "product-0001", List.of("ingest", "query", "storage"));
    }

    @Test
    void write_validInputs_createsInheritanceArtifact() throws IOException {
        RNFInheritanceWriter.write(sampleProduct(), decomposition(), outputDir);
        assertThat(outputDir.resolve("product-0001-rnf-inheritance.json")).exists();
    }

    @Test
    void write_artifact_containsAllCapabilityNames() throws IOException {
        RNFInheritanceWriter.write(sampleProduct(), decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("product-0001-rnf-inheritance.json"));
        assertThat(content).contains("ingest").contains("query").contains("storage");
    }

    @Test
    void write_artifact_containsRnfCategories() throws IOException {
        RNFInheritanceWriter.write(sampleProduct(), decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("product-0001-rnf-inheritance.json"));
        assertThat(content).contains("PERFORMANCE").contains("SECURITY");
    }

    @Test
    void write_secondRun_skipsExisting() throws IOException {
        boolean first = RNFInheritanceWriter.write(sampleProduct(), decomposition(), outputDir);
        boolean second = RNFInheritanceWriter.write(sampleProduct(), decomposition(), outputDir);
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
