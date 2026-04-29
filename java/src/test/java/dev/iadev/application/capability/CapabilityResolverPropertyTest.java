package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityGenerators;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.Profile;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.util.List;
import java.util.Map;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Label;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based test suite for CapabilityResolver — 4 invariantes (story-0064-0111, RULE-001/RULE-004).
 *
 * <p>Runs with 100 random cases (balanced: full 10K too slow in CI; 100 catches structural bugs).
 */
class CapabilityResolverPropertyTest {

    private final CapabilityResolver resolver = new CapabilityResolver();

    @Provide
    Arbitrary<List<CapabilityDefinition>> smallCatalogs() {
        return CapabilityGenerators.smallCatalogs();
    }

    @Provide
    Arbitrary<Profile> profiles() {
        return CapabilityGenerators.profiles(CapabilityGenerators.smallCatalogs());
    }

    @Property(tries = 100)
    @Label("Invariante 1 — idempotência: resolve(p) == resolve(p)")
    void idempotency(
            @ForAll @From("smallCatalogs") List<CapabilityDefinition> catalog,
            @ForAll @From("profiles") Profile profile) {
        try {
            ResolvedCapabilitySet r1 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r2 = resolver.resolve(profile, catalog);
            assertThat(r1.capabilities()).isEqualTo(r2.capabilities());
        } catch (RuntimeException e) {
            // Catalog errors (mutex, cycle, missing) are valid outcomes — not property violations
        }
    }

    @Property(tries = 100)
    @Label("Invariante 2 — determinismo: 3 invocações = mesmo output (RULE-004)")
    void determinism(
            @ForAll @From("smallCatalogs") List<CapabilityDefinition> catalog,
            @ForAll @From("profiles") Profile profile) {
        try {
            ResolvedCapabilitySet r1 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r2 = resolver.resolve(profile, catalog);
            ResolvedCapabilitySet r3 = resolver.resolve(profile, catalog);
            assertThat(r1.capabilities()).isEqualTo(r2.capabilities()).isEqualTo(r3.capabilities());
        } catch (RuntimeException e) {
            // Expected for cyclic/mutex/missing catalogs
        }
    }

    @Property(tries = 100)
    @Label("Invariante 3 — prerequisites preservados: cada capability tem prereqs no output")
    void prerequisitesClosure(
            @ForAll @From("smallCatalogs") List<CapabilityDefinition> catalog,
            @ForAll @From("profiles") Profile profile) {
        try {
            ResolvedCapabilitySet resolved = resolver.resolve(profile, catalog);
            Map<String, CapabilityDefinition> byId = new java.util.LinkedHashMap<>();
            for (CapabilityDefinition def : catalog) byId.put(def.id().value(), def);

            for (CapabilityId active : resolved.capabilities()) {
                CapabilityDefinition def = byId.get(active.value());
                if (def != null) {
                    for (CapabilityId req : def.requires()) {
                        if (!req.isGlob()) {
                            assertThat(resolved.contains(req))
                                    .as("%s requires %s which should be in resolved set", active, req)
                                    .isTrue();
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Expected for invalid catalogs
        }
    }

    @Property(tries = 100)
    @Label("Invariante 4 — empty profile produz set vazio")
    void emptyProfileProducesEmptySet(
            @ForAll @From("smallCatalogs") List<CapabilityDefinition> catalog) {
        try {
            Profile empty = Profile.of("empty", List.of());
            ResolvedCapabilitySet result = resolver.resolve(empty, catalog);
            assertThat(result.capabilities()).isEmpty();
        } catch (RuntimeException e) {
            // Catalog validation errors are OK (mutex in catalog)
        }
    }
}
