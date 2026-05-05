package dev.iadev.adapter.outbound.product;

import dev.iadev.domain.capability.Capability;
import dev.iadev.domain.product.IdempotencyHash;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CapabilityStubWriter {

    private CapabilityStubWriter() {}

    public static WriteResult write(Capability capability, Path outputDir) throws IOException {
        if (capability == null) {
            throw new IllegalArgumentException("capability must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }

        Files.createDirectories(outputDir);
        Path target =
                outputDir.resolve(
                        capability.productId() + "-" + capability.capabilityId() + "-stub.json");

        String content = serialize(capability);
        String hash = IdempotencyHash.compute(capability.productId(), content);

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

    private static String serialize(Capability capability) {
        return "{\n"
                + "  \"capabilityId\": \""
                + capability.capabilityId()
                + "\",\n"
                + "  \"productId\": \""
                + capability.productId()
                + "\",\n"
                + "  \"idempotencyHash\": \"\",\n"
                + "  \"rnfOverrides\": []\n"
                + "}\n";
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
