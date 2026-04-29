package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityGraph;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Public facade for capability resolution.
 *
 * <p>Orchestration order (RULE-001 — each step is part of the contract):
 * <ol>
 *   <li>Mutex validation (catalog-level symmetry check)
 *   <li>Cycle detection (Tarjan SCC on capability graph)
 *   <li>Profile expansion with prerequisite resolution
 *   <li>Profile mutex validation (activation-level check)
 * </ol>
 *
 * <p>Deterministic: same inputs always produce the same {@link ResolvedCapabilitySet} (RULE-004).
 */
public final class CapabilityResolver {

    private final MutexValidator mutexValidator;
    private final CycleDetector cycleDetector;
    private final ProfileExpander profileExpander;

    public CapabilityResolver() {
        this.mutexValidator = new MutexValidator();
        this.cycleDetector = new CycleDetector();
        this.profileExpander = new ProfileExpander();
    }

    public ResolvedCapabilitySet resolve(Profile profile, List<CapabilityDefinition> catalog) {
        return resolve(profile, catalog, Map.of());
    }

    public ResolvedCapabilitySet resolve(
            Profile profile,
            List<CapabilityDefinition> catalog,
            Map<String, String> parameterOverrides) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");

        mutexValidator.validateCatalog(catalog);
        CapabilityGraph graph = CapabilityGraph.of(catalog);
        cycleDetector.detect(graph).ifPresent(cycle -> {
            throw new dev.iadev.domain.capability.CapabilityError.CyclicDependency(
                    "cyclic dependency detected: " + cycle.cyclePath());
        });

        ResolvedCapabilitySet resolved = profileExpander.expand(profile, catalog, parameterOverrides);
        mutexValidator.validateProfile(profile, catalog);

        return resolved;
    }
}
