package dev.iadev.adapter.inbound.cli;

import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class RNFOverrideArtifactParser {

    private static final String RNF_SECTION = "## 2. RNFs Herdadas";

    List<RNFOverride> parse(Path artifactPath) throws IOException {
        if (artifactPath == null) {
            throw new IllegalArgumentException("artifactPath must not be null");
        }
        if (!Files.exists(artifactPath)) {
            throw new IllegalArgumentException("artifact not found: " + artifactPath);
        }
        return parse(Files.readAllLines(artifactPath));
    }

    List<RNFOverride> parse(List<String> lines) {
        List<RNFOverride> overrides = new ArrayList<>();
        boolean inSection = false;
        for (String line : lines) {
            if (!inSection && line.startsWith(RNF_SECTION)) {
                inSection = true;
                continue;
            }
            if (!inSection) {
                continue;
            }
            if (line.startsWith("## ") && !line.startsWith(RNF_SECTION)) {
                break;
            }
            if (!line.startsWith("|")) {
                continue;
            }
            if (line.contains("Categoria") || line.contains(":---")) {
                continue;
            }
            overrides.add(parseRow(line));
        }
        return overrides;
    }

    private RNFOverride parseRow(String row) {
        String[] rawColumns = row.split("\\|", -1);
        if (rawColumns.length < 9) {
            throw new IllegalArgumentException("Invalid RNF inheritance row: " + row);
        }
        List<String> columns = new ArrayList<>();
        for (int i = 1; i < rawColumns.length - 1; i++) {
            columns.add(normalize(rawColumns[i]));
        }
        RNFCategory category = RNFCategory.valueOf(columns.get(0).toUpperCase());
        boolean noRelax = Boolean.parseBoolean(columns.get(2));
        if (noRelax) {
            return RNFOverride.noRelax(category, columns.get(1));
        }
        return RNFOverride.withApproval(
                category,
                columns.get(1),
                columns.get(3),
                columns.get(4),
                ApprovalStatus.fromString(columns.get(5)),
                columns.get(6));
    }

    private String normalize(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank() || "—".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}
