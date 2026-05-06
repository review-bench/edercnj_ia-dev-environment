package dev.iadev.domain.product;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CapabilityNumbering {

    private CapabilityNumbering() {}

    public static Map<String, String> assignIds(List<String> capabilityNames) {
        if (capabilityNames == null || capabilityNames.isEmpty()) {
            throw new IllegalArgumentException("capabilityNames must not be null or empty");
        }
        Map<String, String> result = new LinkedHashMap<>();
        int counter = 1;
        for (String name : capabilityNames) {
            result.put(name, "capability-c" + counter);
            counter++;
        }
        return Collections.unmodifiableMap(result);
    }
}
