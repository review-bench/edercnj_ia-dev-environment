package dev.iadev.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ProductArtifactWriter")
class ProductArtifactWriterTest {

    @TempDir
    Path tempDir;

    private static Product testProduct() {
        return new Product("Analytics Platform", List.of(
                new RNFRoot(RNFCategory.PERFORMANCE, "p99 < 100ms", "Load test", true),
                new RNFRoot(RNFCategory.RELIABILITY, "99.9% uptime", "SLO monitoring", true)));
    }

    @Test
    @DisplayName("write_newFile_createsFileWithContent")
    void write_newFile_createsFileWithContent() throws IOException {
        var result = ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        assertThat(result.path()).exists();
        assertThat(result.skipped()).isFalse();
    }

    @Test
    @DisplayName("write_newFile_containsProductId")
    void write_newFile_containsProductId() throws IOException {
        ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        Path file = tempDir.resolve("product-0001-product.json");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"productId\": \"product-0001\"");
    }

    @Test
    @DisplayName("write_newFile_containsIdempotencyHash")
    void write_newFile_containsIdempotencyHash() throws IOException {
        ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        Path file = tempDir.resolve("product-0001-product.json");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(content).contains("\"idempotencyHash\":");
        assertThat(content).doesNotContain("\"idempotencyHash\": \"\"");
    }

    @Test
    @DisplayName("write_sameInputTwice_secondCallSkipped")
    void write_sameInputTwice_secondCallSkipped() throws IOException {
        ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        var result2 = ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        assertThat(result2.skipped()).isTrue();
    }

    @Test
    @DisplayName("write_differentProduct_overwrites")
    void write_differentProduct_overwrites() throws IOException {
        ProductArtifactWriter.write(testProduct(), "product-0001", tempDir);
        Product updated = new Product("Updated Platform", List.of(
                new RNFRoot(RNFCategory.SECURITY, "TLS 1.3", "Audit", true)));
        var result2 = ProductArtifactWriter.write(updated, "product-0001", tempDir);
        assertThat(result2.skipped()).isFalse();
    }

    @Test
    @DisplayName("write_nullProduct_throwsIllegalArgument")
    void write_nullProduct_throwsIllegalArgument() {
        assertThatThrownBy(() -> ProductArtifactWriter.write(null, "product-0001", tempDir))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("write_nullProductId_throwsIllegalArgument")
    void write_nullProductId_throwsIllegalArgument() {
        assertThatThrownBy(() -> ProductArtifactWriter.write(testProduct(), null, tempDir))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
