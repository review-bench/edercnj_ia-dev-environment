package dev.iadev.domain.feature;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeatureNumbering {

    private FeatureNumbering() {
    }

    public static Map<String, String> assignIds(List<String> featureNames) {
        if (featureNames == null || featureNames.isEmpty()) {
            throw new IllegalArgumentException("featureNames must not be null or empty");
        }
        Map<String, String> result = new LinkedHashMap<>();
        int counter = 1;
        for (String name : featureNames) {
            result.put(name, "feature-" + String.format("%04d", counter));
            counter++;
        }
        return Collections.unmodifiableMap(result);
    }
}
