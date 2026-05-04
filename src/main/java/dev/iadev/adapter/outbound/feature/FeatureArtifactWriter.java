package dev.iadev.adapter.outbound.feature;

import dev.iadev.domain.feature.CapabilityFeatureDecomposition;
import dev.iadev.domain.feature.FeatureNumbering;
import dev.iadev.domain.feature.GherkinACGenerator;
import dev.iadev.domain.product.IdempotencyHash;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class FeatureArtifactWriter {

    private FeatureArtifactWriter() {
    }

    public static WriteResult write(
            CapabilityFeatureDecomposition decomposition,
            Path outputDir) throws IOException {
        return write(decomposition, new GherkinACGenerator(), outputDir);
    }

    public static WriteResult write(
            CapabilityFeatureDecomposition decomposition,
            GherkinACGenerator gherkinGenerator,
            Path outputDir) throws IOException {
        Map<String, String> ids = FeatureNumbering.assignIds(decomposition.featureNames());
        int written = 0;
        int skipped = 0;
        for (String featureName : decomposition.featureNames()) {
            String featureId = ids.get(featureName);
            String fileName = decomposition.capabilityId() + "-" + featureId + ".json";
            Path target = outputDir.resolve(fileName);
            List<String> scenarios = gherkinGenerator.generate(featureName);
            String content = buildJson(featureId, featureName, decomposition.capabilityId(), scenarios);
            if (Files.exists(target) && content.equals(Files.readString(target))) {
                skipped++;
                continue;
            }
            Files.writeString(target, content);
            written++;
        }
        return new WriteResult(written, skipped);
    }

    static String buildJson(String featureId, String featureName, String capabilityId,
            List<String> scenarios) {
        String hash = IdempotencyHash.compute(featureId + "|" + featureName,
                featureId + featureName + capabilityId);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"featureId\": \"").append(featureId).append("\",\n");
        sb.append("  \"featureName\": \"").append(featureName).append("\",\n");
        sb.append("  \"capabilityId\": \"").append(capabilityId).append("\",\n");
        sb.append("  \"acceptanceCriteriaCount\": ").append(scenarios.size()).append(",\n");
        sb.append("  \"gherkinScenarios\": [\n");
        for (int i = 0; i < scenarios.size(); i++) {
            String escaped = scenarios.get(i).replace("\"", "\\\"").replace("\n", "\\n");
            sb.append("    \"").append(escaped).append("\"");
            if (i < scenarios.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"idempotencyHash\": \"").append(hash).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    public record WriteResult(int writtenCount, int skippedCount) {
    }
}
