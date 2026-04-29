package dev.iadev.domain.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Immutable read-only DAG of capability definitions.
 *
 * <p>Provides topological traversal with deterministic alphabetical tie-breaking (RULE-004).
 * Nodes are keyed by {@link CapabilityId#value()} in a {@link LinkedHashMap} for stable order.
 */
public final class CapabilityGraph {

    private final Map<String, CapabilityDefinition> nodes;
    private final Map<String, Set<String>> dependents;

    private CapabilityGraph(Map<String, CapabilityDefinition> nodes) {
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        this.dependents = buildDependents(nodes);
    }

    public static CapabilityGraph of(List<CapabilityDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions must not be null");
        Map<String, CapabilityDefinition> map = new LinkedHashMap<>();
        for (CapabilityDefinition def : definitions) {
            map.put(def.id().value(), def);
        }
        return new CapabilityGraph(map);
    }

    public Map<String, CapabilityDefinition> nodes() {
        return nodes;
    }

    public Set<String> dependentsOf(CapabilityId id) {
        return dependents.getOrDefault(id.value(), Set.of());
    }

    public List<CapabilityId> prerequisitesOf(CapabilityId id) {
        CapabilityDefinition def = nodes.get(id.value());
        if (def == null) return List.of();
        return def.requires();
    }

    public List<CapabilityId> topologicalSort() {
        // indegree = number of prerequisites; nodes with 0 prerequisites come first
        Map<String, Integer> indegree = new HashMap<>();
        for (String key : nodes.keySet()) indegree.put(key, 0);
        for (CapabilityDefinition def : nodes.values()) {
            for (CapabilityId req : def.requires()) {
                indegree.merge(def.id().value(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<CapabilityId> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            List<String> batch = new ArrayList<>(queue);
            Collections.sort(batch);
            queue.clear();
            for (String key : batch) {
                result.add(nodes.get(key).id());
                for (String dep : dependents.getOrDefault(key, Set.of())) {
                    indegree.merge(dep, -1, Integer::sum);
                    if (indegree.get(dep) == 0) queue.add(dep);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<String, Set<String>> buildDependents(Map<String, CapabilityDefinition> nodes) {
        Map<String, Set<String>> deps = new HashMap<>();
        for (CapabilityDefinition def : nodes.values()) {
            for (CapabilityId req : def.requires()) {
                deps.computeIfAbsent(req.value(), k -> new HashSet<>()).add(def.id().value());
            }
        }
        return Collections.unmodifiableMap(deps);
    }
}
