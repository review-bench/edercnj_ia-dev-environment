package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityGraph;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolutionWarning;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Cycle detector using iterative Tarjan SCC (O(V+E), RULE-004 deterministic).
 *
 * <p>Reports the first SCC (size ≥ 2) sorted alphabetically by minimum capability ID. Additional
 * SCCs surface as {@link ResolutionWarning}.
 */
public final class CycleDetector {

    public record CycleDetected(
            List<String> cyclePath, List<ResolutionWarning> additionalWarnings) {
        public CycleDetected {
            Objects.requireNonNull(cyclePath);
            cyclePath = List.copyOf(cyclePath);
            additionalWarnings =
                    additionalWarnings == null ? List.of() : List.copyOf(additionalWarnings);
        }
    }

    public Optional<CycleDetected> detect(CapabilityGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");
        Map<String, CapabilityDefinition> nodes = graph.nodes();
        if (nodes.isEmpty()) return Optional.empty();

        List<List<String>> sccs = findAllSccs(nodes, graph);
        List<List<String>> cyclicSccs =
                sccs.stream()
                        .filter(scc -> scc.size() >= 2)
                        .sorted((a, b) -> minId(a).compareTo(minId(b)))
                        .toList();

        if (cyclicSccs.isEmpty()) return Optional.empty();

        List<String> primary = cyclicSccs.get(0);
        List<ResolutionWarning> warnings = new ArrayList<>();
        for (int i = 1; i < cyclicSccs.size(); i++) {
            List<String> extra = cyclicSccs.get(i);
            warnings.add(
                    ResolutionWarning.of(
                            ResolutionWarning.Kind.CYCLIC_DEPENDENCY_SUSPECT,
                            "additional cycle detected: " + extra));
        }

        List<String> cyclePath = buildCyclePath(primary, graph);
        return Optional.of(new CycleDetected(cyclePath, warnings));
    }

    private List<List<String>> findAllSccs(
            Map<String, CapabilityDefinition> nodes, CapabilityGraph graph) {
        List<String> sortedIds = new ArrayList<>(nodes.keySet());
        sortedIds.sort(String::compareTo);

        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowLink = new HashMap<>();
        Set<String> onStack = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        List<List<String>> result = new ArrayList<>();
        int[] counter = {0};

        for (String id : sortedIds) {
            if (!index.containsKey(id)) {
                tarjan(id, graph, index, lowLink, onStack, stack, result, counter);
            }
        }
        return result;
    }

    private void tarjan(
            String startId,
            CapabilityGraph graph,
            Map<String, Integer> index,
            Map<String, Integer> lowLink,
            Set<String> onStack,
            Deque<String> stack,
            List<List<String>> result,
            int[] counter) {

        Deque<Frame> callStack = new ArrayDeque<>();
        callStack.push(new Frame(startId, new ArrayList<>(), false));

        while (!callStack.isEmpty()) {
            Frame frame = callStack.peek();
            if (!frame.initialized) {
                frame.initialized = true;
                index.put(frame.id, counter[0]);
                lowLink.put(frame.id, counter[0]++);
                stack.push(frame.id);
                onStack.add(frame.id);
                CapabilityDefinition def = graph.nodes().get(frame.id);
                if (def != null) {
                    for (CapabilityId req : def.requires()) {
                        frame.neighbors.add(req.value());
                    }
                }
            }

            boolean pushed = false;
            while (!frame.neighbors.isEmpty()) {
                String neighbor = frame.neighbors.remove(0);
                if (!index.containsKey(neighbor)) {
                    callStack.push(new Frame(neighbor, new ArrayList<>(), false));
                    pushed = true;
                    break;
                } else if (onStack.contains(neighbor)) {
                    lowLink.merge(frame.id, lowLink.get(neighbor), Math::min);
                }
            }

            if (!pushed) {
                callStack.pop();
                if (!callStack.isEmpty()) {
                    Frame parent = callStack.peek();
                    lowLink.merge(parent.id, lowLink.get(frame.id), Math::min);
                }
                if (lowLink.get(frame.id).equals(index.get(frame.id))) {
                    List<String> scc = new ArrayList<>();
                    String w;
                    do {
                        w = stack.pop();
                        onStack.remove(w);
                        scc.add(w);
                    } while (!w.equals(frame.id));
                    result.add(scc);
                }
            }
        }
    }

    private List<String> buildCyclePath(List<String> scc, CapabilityGraph graph) {
        List<String> sorted = new ArrayList<>(scc);
        sorted.sort(String::compareTo);
        String start = sorted.get(0);
        List<String> path = new ArrayList<>();
        path.add(start);
        Set<String> visited = new HashSet<>();
        visited.add(start);
        String current = start;
        Set<String> sccSet = new HashSet<>(scc);
        outer:
        while (true) {
            CapabilityDefinition def = graph.nodes().get(current);
            if (def == null) break;
            for (CapabilityId req : def.requires()) {
                String next = req.value();
                if (sccSet.contains(next) && !visited.contains(next)) {
                    path.add(next);
                    visited.add(next);
                    current = next;
                    continue outer;
                }
            }
            break;
        }
        path.add(start);
        return path;
    }

    private String minId(List<String> scc) {
        return scc.stream().min(String::compareTo).orElse("");
    }

    private static class Frame {
        final String id;
        final List<String> neighbors;
        boolean initialized;

        Frame(String id, List<String> neighbors, boolean initialized) {
            this.id = id;
            this.neighbors = neighbors;
            this.initialized = initialized;
        }
    }
}
