package dev.iadev.adapter.outbound.product;

import dev.iadev.domain.product.IdempotencyHash;
import dev.iadev.domain.product.Product;
import dev.iadev.domain.product.ProductCapabilityDecomposition;
import dev.iadev.domain.product.RNFRoot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RNFInheritanceWriter {

    private RNFInheritanceWriter() {}

    public static boolean write(
            Product product, ProductCapabilityDecomposition decomposition, Path outputDir)
            throws IOException {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
        if (decomposition == null) {
            throw new IllegalArgumentException("decomposition must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
        Files.createDirectories(outputDir);
        String productId = decomposition.productId();
        Path target = outputDir.resolve(productId + "-rnf-inheritance.json");
        String content = serialize(product, decomposition);
        String hash = IdempotencyHash.compute(productId + "|rnf-inheritance", content);
        if (Files.exists(target)) {
            String existing = Files.readString(target, StandardCharsets.UTF_8);
            if (existing.contains(hash)) {
                return false;
            }
        }
        String withHash =
                content.replace(
                        "\"idempotencyHash\": \"\"", "\"idempotencyHash\": \"" + hash + "\"");
        Files.writeString(target, withHash, StandardCharsets.UTF_8);
        return true;
    }

    private static String serialize(Product product, ProductCapabilityDecomposition decomposition) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"productId\": \"").append(decomposition.productId()).append("\",\n");
        sb.append("  \"idempotencyHash\": \"\",\n");
        sb.append("  \"capabilities\": [\n");
        var names = decomposition.capabilityNames();
        for (int i = 0; i < names.size(); i++) {
            sb.append("    \"").append(names.get(i)).append("\"");
            if (i < names.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"rnfRoots\": [\n");
        var roots = product.rnfRoots();
        for (int i = 0; i < roots.size(); i++) {
            RNFRoot r = roots.get(i);
            sb.append("    {\"category\": \"")
                    .append(r.category().name())
                    .append("\", \"mandatory\": ")
                    .append(r.mandatory())
                    .append("}");
            if (i < roots.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
}
