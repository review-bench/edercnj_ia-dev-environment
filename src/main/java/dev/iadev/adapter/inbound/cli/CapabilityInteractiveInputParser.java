package dev.iadev.adapter.inbound.cli;

import java.util.ArrayList;
import java.util.List;

public final class CapabilityInteractiveInputParser {

    private static final int MIN_CAPABILITIES = 3;
    private static final int MAX_CAPABILITIES = 7;

    private CapabilityInteractiveInputParser() {}

    public static List<String> parseCapabilities(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("capabilities JSON must not be null or blank");
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException(
                    "capabilities must be a JSON array, e.g. [\"ingest\",\"query\"]");
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            throw new IllegalArgumentException(
                    "capabilities array must contain between "
                            + MIN_CAPABILITIES
                            + " and "
                            + MAX_CAPABILITIES
                            + " entries");
        }
        List<String> names = new ArrayList<>();
        for (String token : inner.split(",")) {
            String name = token.trim();
            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1).trim();
            }
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        if (names.size() < MIN_CAPABILITIES || names.size() > MAX_CAPABILITIES) {
            throw new IllegalArgumentException(
                    "capabilities must contain between "
                            + MIN_CAPABILITIES
                            + " and "
                            + MAX_CAPABILITIES
                            + " entries, got: "
                            + names.size());
        }
        return List.copyOf(names);
    }
}
