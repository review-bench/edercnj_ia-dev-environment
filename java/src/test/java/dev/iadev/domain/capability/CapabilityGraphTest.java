package dev.iadev.domain.capability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityGraph")
class CapabilityGraphTest {

    private static CapabilityDefinition atomic(String id) {
        return CapabilityDefinition.atomic(CapabilityId.of(id), id.split("\\.")[0], "");
    }

    private static CapabilityDefinition atomicWithReqs(String id, List<CapabilityId> reqs) {
        return new CapabilityDefinition(
                CapabilityId.of(id), CapabilityKind.ATOMIC, id.split("\\.")[0],
                java.util.Optional.empty(), "stable", "",
                java.util.Map.of(), reqs, List.of(), List.of(), List.of(), List.of());
    }

    @Nested
    @DisplayName("topologicalSort()")
    class TopologicalSort {

        @Test
        @DisplayName("single node sorts trivially")
        void singleNode() {
            CapabilityGraph g = CapabilityGraph.of(List.of(atomic("data.database.postgres")));
            List<CapabilityId> sorted = g.topologicalSort();
            assertThat(sorted).hasSize(1);
            assertThat(sorted.get(0).value()).isEqualTo("data.database.postgres");
        }

        @Test
        @DisplayName("nodes without requirements sort alphabetically")
        void alphabeticalTieBreak() {
            CapabilityGraph g = CapabilityGraph.of(List.of(
                    atomic("data.database.postgres"),
                    atomic("data.cache.redis"),
                    atomic("data.database.mysql")
            ));
            List<CapabilityId> sorted = g.topologicalSort();
            assertThat(sorted).hasSize(3);
            assertThat(sorted.get(0).value()).isEqualTo("data.cache.redis");
            assertThat(sorted.get(1).value()).isEqualTo("data.database.mysql");
            assertThat(sorted.get(2).value()).isEqualTo("data.database.postgres");
        }

        @Test
        @DisplayName("topological order respects dependencies")
        void respectsDependencies() {
            CapabilityId base = CapabilityId.of("data.database.postgres");
            CapabilityId dep = CapabilityId.of("framework.spring-boot.data-jpa");
            CapabilityGraph g = CapabilityGraph.of(List.of(
                    atomic("data.database.postgres"),
                    atomicWithReqs("framework.spring-boot.data-jpa", List.of(base))
            ));
            List<CapabilityId> sorted = g.topologicalSort();
            int baseIdx = sorted.indexOf(base);
            int depIdx = sorted.indexOf(dep);
            assertThat(baseIdx).isLessThan(depIdx);
        }

        @Test
        @DisplayName("sort is deterministic across 3 invocations")
        void deterministic() {
            CapabilityGraph g = CapabilityGraph.of(List.of(
                    atomic("data.database.postgres"),
                    atomic("data.cache.redis"),
                    atomic("framework.spring-boot.mvc")
            ));
            List<CapabilityId> first = g.topologicalSort();
            List<CapabilityId> second = g.topologicalSort();
            List<CapabilityId> third = g.topologicalSort();
            assertThat(first).isEqualTo(second).isEqualTo(third);
        }
    }

    @Nested
    @DisplayName("dependentsOf()")
    class Dependents {

        @Test
        @DisplayName("returns dependents of a required node")
        void returnsDependents() {
            CapabilityId base = CapabilityId.of("data.database.postgres");
            CapabilityGraph g = CapabilityGraph.of(List.of(
                    atomic("data.database.postgres"),
                    atomicWithReqs("framework.spring-boot.data-jpa", List.of(base))
            ));
            Set<String> deps = g.dependentsOf(base);
            assertThat(deps).containsExactly("framework.spring-boot.data-jpa");
        }

        @Test
        @DisplayName("returns empty set for node with no dependents")
        void emptyForLeaf() {
            CapabilityGraph g = CapabilityGraph.of(List.of(atomic("data.database.postgres")));
            assertThat(g.dependentsOf(CapabilityId.of("data.database.postgres"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("prerequisitesOf()")
    class Prerequisites {

        @Test
        @DisplayName("returns empty list for node without requirements")
        void emptyForRoot() {
            CapabilityGraph g = CapabilityGraph.of(List.of(atomic("data.database.postgres")));
            assertThat(g.prerequisitesOf(CapabilityId.of("data.database.postgres"))).isEmpty();
        }

        @Test
        @DisplayName("returns requirements list for node with deps")
        void returnsRequirements() {
            CapabilityId base = CapabilityId.of("data.database.postgres");
            CapabilityGraph g = CapabilityGraph.of(List.of(
                    atomic("data.database.postgres"),
                    atomicWithReqs("framework.spring-boot.data-jpa", List.of(base))
            ));
            assertThat(g.prerequisitesOf(CapabilityId.of("framework.spring-boot.data-jpa")))
                    .containsExactly(base);
        }
    }
}
