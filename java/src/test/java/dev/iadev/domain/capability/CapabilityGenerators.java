package dev.iadev.domain.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

/**
 * jqwik generators for EPIC-0064 capability domain types (story-0064-0110, RULE-003/RULE-004).
 *
 * <p>All generators produce canonical IDs (category.subcategory.atomic format, no uppercase).
 * Deterministic with reproducible seed.
 */
public final class CapabilityGenerators {

    private static final List<String> CATEGORIES =
            List.of("lang", "runtime", "data", "framework", "cli", "build", "testing");
    private static final List<String> SUBCATEGORIES =
            List.of("java", "jvm", "database", "cache", "spring", "picocli", "maven", "junit");
    private static final List<String> ATOMICS =
            List.of(
                    "openjdk",
                    "postgres",
                    "mysql",
                    "redis",
                    "mvc",
                    "framework",
                    "standard",
                    "jupiter");

    private CapabilityGenerators() {}

    public static Arbitrary<CapabilityId> capabilityIds() {
        return Combinators.combine(
                        Arbitraries.of(CATEGORIES),
                        Arbitraries.of(SUBCATEGORIES),
                        Arbitraries.of(ATOMICS))
                .as((cat, sub, atom) -> CapabilityId.of(cat + "." + sub + "." + atom));
    }

    public static Arbitrary<CapabilityDefinition> atomicDefinitions(Arbitrary<CapabilityId> ids) {
        return ids.map(id -> CapabilityDefinition.atomic(id, id.category(), "generated"));
    }

    public static Arbitrary<List<CapabilityDefinition>> smallCatalogs() {
        return capabilityIds()
                .list()
                .ofMinSize(3)
                .ofMaxSize(10)
                .map(
                        ids -> {
                            Map<String, CapabilityDefinition> seen = new LinkedHashMap<>();
                            for (CapabilityId id : ids) {
                                seen.putIfAbsent(
                                        id.value(),
                                        CapabilityDefinition.atomic(
                                                id, id.category(), "generated"));
                            }
                            return new ArrayList<>(seen.values());
                        });
    }

    public static Arbitrary<Profile> profiles(Arbitrary<List<CapabilityDefinition>> catalogs) {
        return catalogs.map(
                defs -> {
                    List<CapabilityId> ids =
                            defs.stream().map(CapabilityDefinition::id).limit(3).toList();
                    return Profile.of("generated", ids);
                });
    }
}
