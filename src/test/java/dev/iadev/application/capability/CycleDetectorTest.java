package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityGraph;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.CapabilityKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CycleDetector")
class CycleDetectorTest {

    private final CycleDetector detector = new CycleDetector();

    private static CapabilityDefinition def(String id, List<String> requires) {
        List<CapabilityId> reqs = requires.stream().map(CapabilityId::of).toList();
        return new CapabilityDefinition(
                CapabilityId.of(id),
                CapabilityKind.ATOMIC,
                id.split("\\.")[0],
                java.util.Optional.empty(),
                "stable",
                "",
                java.util.Map.of(),
                reqs,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    @Nested
    @DisplayName("acyclic graphs")
    class AcyclicGraphs {

        @Test
        @DisplayName("single node graph returns empty (degenerate)")
        void singleNode() {
            CapabilityGraph g =
                    CapabilityGraph.of(List.of(def("data.database.postgres", List.of())));
            assertThat(detector.detect(g)).isEmpty();
        }

        @Test
        @DisplayName("empty graph returns empty")
        void emptyGraph() {
            assertThat(detector.detect(CapabilityGraph.of(List.of()))).isEmpty();
        }

        @Test
        @DisplayName("linear chain A→B→C is acyclic (happy)")
        void linearChain() {
            CapabilityGraph g =
                    CapabilityGraph.of(
                            List.of(
                                    def("aaa.bbb.a", List.of()),
                                    def("aaa.bbb.b", List.of("aaa.bbb.a")),
                                    def("aaa.bbb.c", List.of("aaa.bbb.b"))));
            assertThat(detector.detect(g)).isEmpty();
        }
    }

    @Nested
    @DisplayName("cyclic graphs")
    class CyclicGraphs {

        @Test
        @DisplayName("direct cycle A→B, B→A detected (error path)")
        void directCycle() {
            CapabilityGraph g =
                    CapabilityGraph.of(
                            List.of(
                                    def("aaa.bbb.alpha", List.of("aaa.bbb.beta")),
                                    def("aaa.bbb.beta", List.of("aaa.bbb.alpha"))));
            Optional<CycleDetector.CycleDetected> result = detector.detect(g);
            assertThat(result).isPresent();
            List<String> path = result.get().cyclePath();
            assertThat(path).hasSizeGreaterThanOrEqualTo(3);
            assertThat(path.get(0)).isEqualTo(path.get(path.size() - 1));
        }

        @Test
        @DisplayName("indirect cycle A→B→C→A detected (error path)")
        void indirectCycle() {
            CapabilityGraph g =
                    CapabilityGraph.of(
                            List.of(
                                    def("aaa.bbb.alpha", List.of("aaa.bbb.beta")),
                                    def("aaa.bbb.beta", List.of("aaa.bbb.gamma")),
                                    def("aaa.bbb.gamma", List.of("aaa.bbb.alpha"))));
            Optional<CycleDetector.CycleDetected> result = detector.detect(g);
            assertThat(result).isPresent();
            List<String> path = result.get().cyclePath();
            assertThat(path).hasSizeGreaterThanOrEqualTo(4);
            assertThat(path.get(0)).isEqualTo(path.get(path.size() - 1));
        }

        @Test
        @DisplayName("multiple disjoint cycles — primary SCC is alphabetically first (boundary)")
        void multipleCyclesDeterministic() {
            CapabilityGraph g =
                    CapabilityGraph.of(
                            List.of(
                                    def("xxx.yyy.alpha", List.of("xxx.yyy.beta")),
                                    def("xxx.yyy.beta", List.of("xxx.yyy.alpha")),
                                    def("zzz.yyy.x", List.of("zzz.yyy.y")),
                                    def("zzz.yyy.y", List.of("zzz.yyy.x"))));
            Optional<CycleDetector.CycleDetected> first = detector.detect(g);
            Optional<CycleDetector.CycleDetected> second = detector.detect(g);
            Optional<CycleDetector.CycleDetected> third = detector.detect(g);

            assertThat(first).isPresent();
            assertThat(first.get().cyclePath()).isEqualTo(second.get().cyclePath());
            assertThat(second.get().cyclePath()).isEqualTo(third.get().cyclePath());
            assertThat(first.get().cyclePath().get(0)).startsWith("xxx");
            assertThat(first.get().additionalWarnings()).hasSize(1);
        }
    }
}
