package dev.iadev.adapter.outbound.product;

import dev.iadev.domain.product.CapabilityNumbering;
import dev.iadev.domain.product.IdempotencyHash;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CapabilityArtifactWriter {

    private CapabilityArtifactWriter() {}

    public static WriteResult write(ProductCapabilityDecomposition decomposition, Path outputDir)
            throws IOException {
        if (decomposition == null) {
            throw new IllegalArgumentException("decomposition must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
        Files.createDirectories(outputDir);
        Map<String, String> ids = CapabilityNumbering.assignIds(decomposition.capabilityNames());
        int written = 0;
        int skipped = 0;
        for (Map.Entry<String, String> entry : ids.entrySet()) {
            String name = entry.getKey();
            String capabilityId = entry.getValue();
            String productId = decomposition.productId();
            Path target = outputDir.resolve(productId + "-" + capabilityId + ".json");
            String content = serialize(capabilityId, name, productId);
            String hash = IdempotencyHash.compute(productId + "|" + capabilityId, content);
            if (Files.exists(target)) {
                String existing = Files.readString(target, StandardCharsets.UTF_8);
                if (existing.contains(hash)) {
                    skipped++;
                    continue;
                }
            }
            String withHash =
                    content.replace(
                            "\"idempotencyHash\": \"\"", "\"idempotencyHash\": \"" + hash + "\"");
            Files.writeString(target, withHash, StandardCharsets.UTF_8);
            written++;
        }
        return new WriteResult(written, skipped);
    }

    private static String serialize(String capabilityId, String capabilityName, String productId) {
        return "{\n"
                + "  \"capabilityId\": \""
                + capabilityId
                + "\",\n"
                + "  \"capabilityName\": \""
                + capabilityName
                + "\",\n"
                + "  \"productId\": \""
                + productId
                + "\",\n"
                + "  \"idempotencyHash\": \"\"\n"
                + "}\n";
    }

    public record WriteResult(int writtenCount, int skippedCount) {}
}
