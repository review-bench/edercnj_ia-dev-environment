package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PrerequisiteResolver")
class PrerequisiteResolverTest {

    private final PrerequisiteResolver resolver = new PrerequisiteResolver();

    private static Map<String, CapabilityDefinition> catalog(CapabilityDefinition... defs) {
        Map<String, CapabilityDefinition> map = new LinkedHashMap<>();
        for (CapabilityDefinition def : defs) map.put(def.id().value(), def);
        return map;
    }

    private static CapabilityDefinition def(String id, List<String> requires) {
        return new CapabilityDefinition(
                CapabilityId.of(id),
                CapabilityKind.ATOMIC,
                id.split("\\.")[0],
                Optional.empty(),
                "stable",
                "",
                Map.of(),
                requires.stream().map(CapabilityId::of).toList(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    @Nested
    @DisplayName("degenerate cases")
    class Degenerate {

        @Test
        @DisplayName("capability without prerequisites returns only itself")
        void noPrereqs() {
            Map<String, CapabilityDefinition> cat = catalog(def("runtime.jvm.openjdk", List.of()));
            List<CapabilityId> result = resolver.expand("runtime.jvm.openjdk", cat);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).value()).isEqualTo("runtime.jvm.openjdk");
        }

        @Test
        @DisplayName("missing capability throws MissingPrerequisite")
        void missingCapability() {
            assertThatThrownBy(() -> resolver.expand("not.in.catalog", Map.of()))
                    .isInstanceOf(CapabilityError.MissingPrerequisite.class);
        }
    }

    @Nested
    @DisplayName("transitive expansion")
    class TransitiveExpansion {

        @Test
        @DisplayName("linear chain A→B→C returns C,B,A in topological order (happy)")
        void linearChain() {
            Map<String, CapabilityDefinition> cat =
                    catalog(
                            def("aaa.bbb.c", List.of()),
                            def("aaa.bbb.b", List.of("aaa.bbb.c")),
                            def("aaa.bbb.a", List.of("aaa.bbb.b")));
            List<CapabilityId> result = resolver.expand("aaa.bbb.a", cat);
            assertThat(result).hasSize(3);
            assertThat(result.get(0).value()).isEqualTo("aaa.bbb.c");
            assertThat(result.get(1).value()).isEqualTo("aaa.bbb.b");
            assertThat(result.get(2).value()).isEqualTo("aaa.bbb.a");
        }

        @Test
        @DisplayName("missing required capability throws MissingPrerequisite with context")
        void missingPrerequisite() {
            Map<String, CapabilityDefinition> cat =
                    catalog(def("aaa.bbb.x", List.of("aaa.bbb.missing")));
            assertThatThrownBy(() -> resolver.expand("aaa.bbb.x", cat))
                    .isInstanceOf(CapabilityError.MissingPrerequisite.class)
                    .hasMessageContaining("aaa.bbb.missing")
                    .hasMessageContaining("aaa.bbb.x");
        }

        @Test
        @DisplayName("deep chain (10 levels) resolved in topological order (boundary)")
        void deepChain() {
            Map<String, CapabilityDefinition> cat = new LinkedHashMap<>();
            for (int i = 10; i >= 1; i--) {
                String id = "aaa.bbb.a" + i;
                List<String> reqs = i < 10 ? List.of("aaa.bbb.a" + (i + 1)) : List.of();
                cat.put(id, def(id, reqs));
            }
            List<CapabilityId> result = resolver.expand("aaa.bbb.a1", cat);
            assertThat(result).hasSize(10);
            assertThat(result.get(0).value()).isEqualTo("aaa.bbb.a10");
            assertThat(result.get(9).value()).isEqualTo("aaa.bbb.a1");
        }
    }

    @Nested
    @DisplayName("glob expansion")
    class GlobExpansion {

        @Test
        @DisplayName("glob requires expands to all matching atomics")
        void globRequiresExpands() {
            Map<String, CapabilityDefinition> cat =
                    catalog(
                            def("data.database.postgres", List.of()),
                            def("data.database.mysql", List.of()),
                            def("data.database.mongo", List.of()),
                            def("aaa.bbb.x", List.of("data.database.*")));
            List<CapabilityId> result = resolver.expand("aaa.bbb.x", cat);
            assertThat(result).hasSize(4);
            List<String> ids = result.stream().map(CapabilityId::value).toList();
            assertThat(ids)
                    .containsSequence(
                            "data.database.mongo", "data.database.mysql", "data.database.postgres");
        }
    }
}
