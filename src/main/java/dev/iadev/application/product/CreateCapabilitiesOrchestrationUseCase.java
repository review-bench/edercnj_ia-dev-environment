package dev.iadev.application.product;

import dev.iadev.adapter.outbound.product.CapabilityArtifactWriter;
import dev.iadev.adapter.outbound.product.RNFInheritanceWriter;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class CreateCapabilitiesOrchestrationUseCase {

    private final CapabilityDecompositionUseCase decompositionUseCase;

    public CreateCapabilitiesOrchestrationUseCase(
            CapabilityDecompositionUseCase decompositionUseCase) {
        this.decompositionUseCase = decompositionUseCase;
    }

    public CreateCapabilitiesResult execute(
            String productId, Product product, List<String> explicitNames, Path outputDir)
            throws IOException {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
        ProductCapabilityDecomposition decomposition =
                decompositionUseCase.execute(productId, product, explicitNames);
        CapabilityArtifactWriter.WriteResult capResult =
                CapabilityArtifactWriter.write(decomposition, outputDir);
        boolean rnfWritten = RNFInheritanceWriter.write(product, decomposition, outputDir);
        return new CreateCapabilitiesResult(
                capResult.writtenCount(), capResult.skippedCount(), rnfWritten);
    }
}
