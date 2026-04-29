package dev.iadev.application.capability;

import dev.iadev.domain.capability.CapabilityDefinition;
import dev.iadev.domain.capability.CapabilityError;
import dev.iadev.domain.capability.CapabilityId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

/**
 * Transitively expands a capability's prerequisites using Kahn's algorithm (RULE-004: deterministic).
 *
 * <p>Returns all prerequisites in topological order (leaves first), ending with the requested capability.
 * Glob requires are expanded against the catalog before sorting.
 */
public final class PrerequisiteResolver {

    public List<CapabilityId> expand(String capabilityId, Map<String, CapabilityDefinition> catalog) {
        CapabilityDefinition root = catalog.get(capabilityId);
        if (root == null) {
            throw new CapabilityError.MissingPrerequisite(
                    "capability '" + capabilityId + "' not found in catalog",
                    capabilityId, "");
        }

        Set<String> visited = new HashSet<>();
        Map<String, Set<String>> deps = new TreeMap<>();
        collectTransitive(capabilityId, catalog, visited, deps);

        return kahnSort(deps, catalog);
    }

    private void collectTransitive(
            String id, Map<String, CapabilityDefinition> catalog,
            Set<String> visited, Map<String, Set<String>> deps) {
        if (visited.contains(id)) return;
        visited.add(id);

        CapabilityDefinition def = catalog.get(id);
        if (def == null) return;

        Set<String> directDeps = new HashSet<>();
        for (CapabilityId req : def.requires()) {
            List<String> expanded = expandGlob(req, catalog, id);
            for (String expId : expanded) {
                directDeps.add(expId);
                collectTransitive(expId, catalog, visited, deps);
            }
        }
        deps.put(id, directDeps);
    }

    private List<String> expandGlob(
            CapabilityId req, Map<String, CapabilityDefinition> catalog, String referencedBy) {
        if (!req.isGlob()) {
            if (!catalog.containsKey(req.value())) {
                throw new CapabilityError.MissingPrerequisite(
                        req.value() + " referenced by " + referencedBy + " not found in catalog",
                        req.value(), referencedBy);
            }
            return List.of(req.value());
        }
        List<String> matches = new ArrayList<>();
        for (String candidateId : catalog.keySet()) {
            if (req.matches(CapabilityId.of(candidateId))) {
                matches.add(candidateId);
            }
        }
        matches.sort(String::compareTo);
        return matches;
    }

    private List<CapabilityId> kahnSort(
            Map<String, Set<String>> deps, Map<String, CapabilityDefinition> catalog) {
        // indegree = number of prerequisites (things that must come before)
        Map<String, Integer> indegree = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : deps.entrySet()) {
            indegree.put(e.getKey(), e.getValue().size());
        }

        // reverseDeps: for each node X, who depends on X (i.e., X ∈ deps[Y])
        Map<String, List<String>> reverseDeps = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : deps.entrySet()) {
            String dependent = e.getKey();
            for (String prereq : e.getValue()) {
                reverseDeps.computeIfAbsent(prereq, k -> new ArrayList<>()).add(dependent);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : new TreeMap<>(indegree).entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<CapabilityId> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            List<String> batch = new ArrayList<>(queue);
            batch.sort(String::compareTo);
            queue.clear();
            for (String id : batch) {
                result.add(CapabilityId.of(id));
                for (String dependent : reverseDeps.getOrDefault(id, List.of())) {
                    indegree.merge(dependent, -1, Integer::sum);
                    if (indegree.get(dependent) == 0) queue.add(dependent);
                }
            }
        }
        return List.copyOf(result);
    }
}
