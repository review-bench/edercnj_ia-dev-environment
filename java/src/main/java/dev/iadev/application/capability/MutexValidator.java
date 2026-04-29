package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Validates mutex (excludes) invariants — RULE-001: symmetry is part of the contract.
 *
 * <p>Two validation modes with distinct messages:
 * <ul>
 *   <li>{@link #validateCatalog} — detects asymmetric excludes (catalog authoring defect)
 *   <li>{@link #validateProfile} — detects simultaneous activation of mutually exclusive capabilities
 * </ul>
 */
public final class MutexValidator {

    public void validateCatalog(List<CapabilityDefinition> catalog) {
        Map<String, Set<String>> excludeIndex = buildExcludeIndex(catalog);
        Map<String, CapabilityDefinition> byId = catalog.stream()
                .collect(Collectors.toMap(d -> d.id().value(), d -> d));

        new TreeMap<>(excludeIndex).forEach((id, excludes) -> {
            for (String excluded : excludes) {
                Set<String> reverseExcludes = excludeIndex.getOrDefault(excluded, Set.of());
                if (!reverseExcludes.contains(id)) {
                    throw new CapabilityError.MutexConflict(
                            "asymmetric mutex: " + id + " excludes " + excluded
                            + " but " + excluded + " does not exclude " + id);
                }
            }
        });
    }

    public void validateProfile(Profile profile, List<CapabilityDefinition> catalog) {
        Map<String, Set<String>> excludeIndex = buildExcludeIndex(catalog);
        Set<String> activeIds = profile.expand().stream()
                .map(CapabilityId::value)
                .collect(Collectors.toSet());

        List<String> conflicts = new ArrayList<>();
        for (String activeId : activeIds) {
            Set<String> excluded = excludeIndex.getOrDefault(activeId, Set.of());
            for (String ex : excluded) {
                if (activeIds.contains(ex) && activeId.compareTo(ex) < 0) {
                    conflicts.add(activeId + " ✕ " + ex);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new CapabilityError.MutexConflict(
                    "profile '" + profile.name() + "' activates mutually exclusive capabilities: "
                    + String.join(", ", conflicts));
        }
    }

    private Map<String, Set<String>> buildExcludeIndex(List<CapabilityDefinition> catalog) {
        return catalog.stream().collect(Collectors.toMap(
                d -> d.id().value(),
                d -> d.excludes().stream().map(CapabilityId::value).collect(Collectors.toSet())));
    }
}
