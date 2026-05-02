package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Allowed SPDX license identifiers for the project's dependency policy.
 *
 * <p>When the list is empty, the license gate is effectively disabled for whitelist enforcement
 * (but {@link BlockOnPolicy#license()} may still report {@code WARN_ONLY}). An empty whitelist
 * combined with {@code license: any-violation} block action triggers a
 * {@link ConfigValidationException} — it would block everything including allowed deps.
 *
 * <p>Introduced by EPIC-0074 — story-0074-0001.
 *
 * @param allowed the list of SPDX identifiers explicitly permitted (e.g. {@code Apache-2.0},
 *     {@code MIT})
 */
public record LicenseWhitelist(List<String> allowed) {

    /** Empty whitelist — license gate disabled. */
    public static final LicenseWhitelist EMPTY = new LicenseWhitelist(List.of());

    /** Compact constructor enforcing immutability. */
    public LicenseWhitelist {
        allowed = allowed == null ? List.of() : List.copyOf(allowed);
    }

    /** {@code true} when no allowed licenses are declared (gate effectively disabled). */
    public boolean isEmpty() {
        return allowed.isEmpty();
    }

    /**
     * Parses from the YAML {@code allowed-licenses} list.
     *
     * @param list the raw list object from YAML (may be null for absent field)
     * @return a {@link LicenseWhitelist}, never null
     */
    @SuppressWarnings("unchecked")
    static LicenseWhitelist fromList(Object list) {
        if (list == null) {
            return EMPTY;
        }
        if (!(list instanceof List<?> rawList)) {
            throw new ConfigValidationException(
                    "allowed-licenses must be a YAML list, got: "
                            + list.getClass().getSimpleName());
        }
        List<String> entries =
                rawList.stream()
                        .map(
                                e -> {
                                    if (e instanceof String s) {
                                        if (s.isBlank()) {
                                            throw new ConfigValidationException(
                                                    "allowed-licenses: empty string is not a"
                                                            + " valid SPDX identifier");
                                        }
                                        return s;
                                    }
                                    throw new ConfigValidationException(
                                            "allowed-licenses entries must be strings, got: "
                                                    + e.getClass().getSimpleName());
                                })
                        .toList();
        return new LicenseWhitelist(entries);
    }

    static LicenseWhitelist fromMap(Map<String, Object> policyMap) {
        return fromList(policyMap.get("allowed-licenses"));
    }
}
