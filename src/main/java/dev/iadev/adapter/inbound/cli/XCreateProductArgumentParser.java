package dev.iadev.adapter.inbound.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class XCreateProductArgumentParser {

    private static final String DEFAULT_OUTPUT_DIR = "ai/products";
    private static final int MAX_PRODUCT_ID_LENGTH = 15;
    private static final String PRODUCT_ID_PATTERN = "product-\\d{4}";

    private XCreateProductArgumentParser() {}

    public static CreateProductRequest parse(
            String ideationFilePath, String outputDirPath, String productId, boolean dryRun) {

        Path ideationFile = Paths.get(ideationFilePath).normalize();

        Path outputDir =
                outputDirPath != null
                        ? Paths.get(outputDirPath).normalize()
                        : Paths.get(DEFAULT_OUTPUT_DIR);

        Optional<String> resolvedProductId = Optional.ofNullable(productId);

        return new CreateProductRequest(ideationFile, outputDir, resolvedProductId, dryRun);
    }

    static void validateProductId(String productId) {
        if (productId == null) {
            return;
        }
        if (productId.length() > MAX_PRODUCT_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "product-id exceeds maximum length of "
                            + MAX_PRODUCT_ID_LENGTH
                            + ": "
                            + productId);
        }
        if (!productId.matches(PRODUCT_ID_PATTERN)) {
            throw new IllegalArgumentException(
                    "product-id must match pattern 'product-NNNN', got: " + productId);
        }
    }
}
