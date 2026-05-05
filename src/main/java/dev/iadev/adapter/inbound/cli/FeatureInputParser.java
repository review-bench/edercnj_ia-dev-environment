package dev.iadev.adapter.inbound.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeatureInputParser {

    static final int MIN_FEATURES = 4;
    static final int MAX_FEATURES = 8;

    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private FeatureInputParser() {}

    public static List<String> parseFeatures(String json) {
        if (json == null) {
            throw new IllegalArgumentException("features JSON must not be null");
        }
        String trimmed = json.strip();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException(
                    "features JSON must be a JSON array: [{\"name\":\"...\"},...] ");
        }
        Matcher matcher = NAME_PATTERN.matcher(trimmed);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException(
                    "features JSON must contain at least "
                            + MIN_FEATURES
                            + " objects with \"name\" field");
        }
        if (names.size() < MIN_FEATURES) {
            throw new IllegalArgumentException(
                    "features requires at least " + MIN_FEATURES + " entries; got " + names.size());
        }
        if (names.size() > MAX_FEATURES) {
            throw new IllegalArgumentException(
                    "features allows at most " + MAX_FEATURES + " entries; got " + names.size());
        }
        return List.copyOf(names);
    }
}
