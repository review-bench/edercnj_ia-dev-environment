package dev.iadev.adapter.outbound.product;

import dev.iadev.domain.product.IdempotencyHash;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProductArtifactWriter {

    private ProductArtifactWriter() {}

    public static WriteResult write(Product product, String productId, Path outputDir)
            throws IOException {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }

        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(productId + "-product.json");

        String content = serialize(product, productId);
        String hash = IdempotencyHash.compute(productId, content);

        if (Files.exists(target)) {
            String existing = Files.readString(target, StandardCharsets.UTF_8);
            String existingHash = extractHash(existing);
            if (hash.equals(existingHash)) {
                return WriteResult.skipped(target, hash);
            }
        }

        String withHash =
                content.replace(
                        "\"idempotencyHash\": \"\"", "\"idempotencyHash\": \"" + hash + "\"");
        Files.writeString(target, withHash, StandardCharsets.UTF_8);
        return WriteResult.written(target, hash);
    }

    private static String serialize(Product product, String productId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"productId\": \"").append(productId).append("\",\n");
        sb.append("  \"name\": \"").append(product.name()).append("\",\n");
        sb.append("  \"idempotencyHash\": \"\",\n");
        sb.append("  \"rnfRoots\": [\n");
        var roots = product.rnfRoots();
        for (int i = 0; i < roots.size(); i++) {
            RNFRoot r = roots.get(i);
            sb.append("    {\n");
            sb.append("      \"category\": \"").append(r.category().name()).append("\",\n");
            sb.append("      \"description\": \"").append(escape(r.description())).append("\",\n");
            sb.append("      \"verificationMethod\": \"")
                    .append(escape(r.verificationMethod()))
                    .append("\",\n");
            sb.append("      \"mandatory\": ").append(r.mandatory()).append("\n");
            sb.append("    }");
            if (i < roots.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractHash(String content) {
        int idx = content.indexOf("\"idempotencyHash\": \"");
        if (idx < 0) return "";
        int start = idx + "\"idempotencyHash\": \"".length();
        int end = content.indexOf("\"", start);
        return end > start ? content.substring(start, end) : "";
    }

    public record WriteResult(Path path, String hash, boolean skipped) {
        static WriteResult written(Path path, String hash) {
            return new WriteResult(path, hash, false);
        }

        static WriteResult skipped(Path path, String hash) {
            return new WriteResult(path, hash, true);
        }
    }
}
