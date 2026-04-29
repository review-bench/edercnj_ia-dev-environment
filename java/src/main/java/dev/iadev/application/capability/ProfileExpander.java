package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolutionWarning;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Expands a {@link Profile} against a catalog to produce a {@link ResolvedCapabilitySet}.
 *
 * <p>Orchestrates: glob expansion → prerequisite resolution → parameter override application. Does
 * NOT run cycle or mutex validation — those belong to {@link CapabilityResolver}.
 */
public final class ProfileExpander {

    private final PrerequisiteResolver prerequisiteResolver;

    public ProfileExpander() {
        this.prerequisiteResolver = new PrerequisiteResolver();
    }

    public ResolvedCapabilitySet expand(
            Profile profile,
            List<CapabilityDefinition> catalog,
            Map<String, String> parameterOverrides) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");

        Map<String, CapabilityDefinition> byId = new LinkedHashMap<>();
        for (CapabilityDefinition def : catalog) byId.put(def.id().value(), def);

        Set<CapabilityId> directCapabilities = profile.expand();
        List<ResolutionWarning> warnings = new ArrayList<>();
        Set<CapabilityId> allCapabilities = new LinkedHashSet<>();

        for (CapabilityId cap : directCapabilities) {
            List<CapabilityId> transitive = prerequisiteResolver.expand(cap.value(), byId);
            allCapabilities.addAll(transitive);
        }

        Map<String, String> effectiveParams = new LinkedHashMap<>();
        for (Map.Entry<String, String> override :
                (parameterOverrides == null ? Map.<String, String>of() : parameterOverrides)
                        .entrySet()) {
            String key = override.getKey();
            String capId = extractCapabilityId(key);
            if (capId != null && !isCapabilityActive(capId, allCapabilities)) {
                warnings.add(
                        ResolutionWarning.of(
                                ResolutionWarning.Kind.UNKNOWN_CAPABILITY,
                                "override target '" + capId + "' not in profile — skipping"));
            } else {
                effectiveParams.put(key, override.getValue());
            }
        }

        return new ResolvedCapabilitySet(
                profile.name(), List.copyOf(allCapabilities), effectiveParams, warnings);
    }

    private String extractCapabilityId(String paramKey) {
        int lastDot = paramKey.lastIndexOf('.');
        if (lastDot < 0) return null;
        String candidate = paramKey.substring(0, lastDot);
        try {
            CapabilityId.of(candidate);
            return candidate;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCapabilityActive(String capId, Set<CapabilityId> active) {
        return active.stream().anyMatch(c -> c.value().equals(capId));
    }
}
