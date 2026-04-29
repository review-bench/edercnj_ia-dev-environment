package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pairwise covering test matrix for capability-aware composition (story-0064-0608, RULE-004).
 *
 * <p><b>Rationale:</b> Full cross-product of 9 capability dimensions (lang, web, data, infra,
 * messaging, compliance, test, build, observability) would produce ~10^9 combinations — not
 * feasible. Pairwise covering (All-Pairs) guarantees that every pair of dimension-values appears
 * in at least one test combination, while reducing the matrix to ≤ 50 combinations.
 *
 * <p><b>Algorithm:</b> Greedy pair-coverage. Iterate uncovered pairs; for each uncovered pair,
 * find the combination that covers the most remaining pairs and add it to the set. This is an
 * approximation of the NP-hard optimal set cover, sufficient for our purposes.
 *
 * <p><b>Coverage guarantee:</b> After generation, every pair (dim_i=val_x, dim_j=val_y) where
 * i≠j appears in ≥ 1 combination. The test verifies this invariant explicitly.
 */
@DisplayName("PairwiseCapabilityMatrixTest")
class PairwiseCapabilityMatrixTest {

    @TempDir
    Path tempDir;

    /**
     * Capability dimensions and their representative values (capability IDs). Each dimension
     * represents one axis of the capability space. Value "none" means the capability is absent.
     */
    private static final Map<String, List<String>> DIMENSIONS = new LinkedHashMap<>();

    static {
        DIMENSIONS.put("lang", Arrays.asList("lang.java.21", "none"));
        DIMENSIONS.put("web", Arrays.asList("web.spring.boot", "web.quarkus.framework", "none"));
        DIMENSIONS.put("data", Arrays.asList("data.database.postgres", "none"));
        DIMENSIONS.put("infra", Arrays.asList("infra.docker.standard", "none"));
        DIMENSIONS.put("messaging", Arrays.asList("messaging.kafka.standard", "none"));
        DIMENSIONS.put("compliance", Arrays.asList("compliance.pci.dss", "none"));
        DIMENSIONS.put("testing", Arrays.asList("testing.junit.5", "none"));
        DIMENSIONS.put("build", Arrays.asList("build.maven.standard", "none"));
        DIMENSIONS.put("observability", Arrays.asList("observability.none", "none"));
    }

    /** Generate ≤ 50 pairwise-covering combinations and run composer plan() on each. */
    @TestFactory
    @DisplayName("Pairwise matrix: ≤50 combinations cover all capability pairs")
    List<DynamicTest> pairwiseMatrix() throws IOException {
        List<Map<String, String>> matrix = generatePairwiseCombinations(DIMENSIONS);

        assertThat(matrix)
                .as("Pairwise matrix must produce ≤ 50 combinations")
                .hasSizeLessThanOrEqualTo(50);

        assertPairCoverage(matrix, DIMENSIONS);

        CapabilityAwareComposer composer = new CapabilityAwareComposer();
        List<DynamicTest> tests = new ArrayList<>();

        for (int i = 0; i < matrix.size(); i++) {
            final int idx = i;
            final Map<String, String> combo = matrix.get(i);
            tests.add(DynamicTest.dynamicTest(
                    "combo-" + (idx + 1) + " " + describeCombo(combo),
                    () -> runCombo(composer, combo)));
        }
        return tests;
    }

    private void runCombo(CapabilityAwareComposer composer, Map<String, String> combo)
            throws IOException {
        List<CapabilityId> capIds = new ArrayList<>();
        for (String val : combo.values()) {
            if (!"none".equals(val)) {
                capIds.add(CapabilityId.of(val));
            }
        }
        ResolvedCapabilitySet capSet = new ResolvedCapabilitySet("pairwise", capIds, Map.of(), List.of());
        Path targetsRoot = tempDir.resolve("targets-" + System.nanoTime());
        writeMinimalTargets(targetsRoot);
        CompositionPlan plan = composer.plan(capSet, targetsRoot);
        assertThat(plan).isNotNull();
        assertThat(plan.included().size() + plan.excluded().size()).isGreaterThan(0);
    }

    private static String describeCombo(Map<String, String> combo) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : combo.entrySet()) {
            if (!"none".equals(e.getValue())) {
                parts.add(e.getKey() + "=" + e.getValue().replaceFirst(".*\\.", ""));
            }
        }
        return parts.isEmpty() ? "(empty)" : String.join(",", parts);
    }

    /**
     * Greedy pairwise covering algorithm. Generates combinations such that every (dim_i=val_x,
     * dim_j=val_y) pair appears in ≥ 1 combination.
     */
    static List<Map<String, String>> generatePairwiseCombinations(Map<String, List<String>> dims) {
        List<String> dimNames = new ArrayList<>(dims.keySet());
        Set<String> uncoveredPairs = new LinkedHashSet<>();

        for (int i = 0; i < dimNames.size(); i++) {
            for (int j = i + 1; j < dimNames.size(); j++) {
                String di = dimNames.get(i);
                String dj = dimNames.get(j);
                for (String vi : dims.get(di)) {
                    for (String vj : dims.get(dj)) {
                        uncoveredPairs.add(pairKey(di, vi, dj, vj));
                    }
                }
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        int maxIter = 200;

        while (!uncoveredPairs.isEmpty() && result.size() < 50 && maxIter-- > 0) {
            Map<String, String> best = greedyBestCombo(dimNames, dims, uncoveredPairs);
            result.add(best);
            removeCoveredPairs(best, dimNames, uncoveredPairs);
        }
        return result;
    }

    private static Map<String, String> greedyBestCombo(
            List<String> dimNames,
            Map<String, List<String>> dims,
            Set<String> uncoveredPairs) {
        Map<String, String> best = null;
        int bestScore = -1;

        // Sample candidate combinations from uncovered pairs
        List<Map<String, String>> candidates = new ArrayList<>();
        for (String pair : uncoveredPairs) {
            String[] parts = pair.split("\\|");
            String di = parts[0], vi = parts[1], dj = parts[2], vj = parts[3];
            Map<String, String> candidate = new LinkedHashMap<>();
            for (String dim : dimNames) {
                if (dim.equals(di)) candidate.put(dim, vi);
                else if (dim.equals(dj)) candidate.put(dim, vj);
                else candidate.put(dim, dims.get(dim).get(0));
            }
            candidates.add(candidate);
            if (candidates.size() >= 30) break;
        }

        for (Map<String, String> candidate : candidates) {
            int score = scoreCombo(candidate, dimNames, uncoveredPairs);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) {
            best = new LinkedHashMap<>();
            for (String dim : dimNames) {
                best.put(dim, dims.get(dim).get(0));
            }
        }
        return best;
    }

    private static int scoreCombo(
            Map<String, String> combo, List<String> dimNames, Set<String> uncoveredPairs) {
        int score = 0;
        for (int i = 0; i < dimNames.size(); i++) {
            for (int j = i + 1; j < dimNames.size(); j++) {
                String di = dimNames.get(i);
                String dj = dimNames.get(j);
                if (uncoveredPairs.contains(pairKey(di, combo.get(di), dj, combo.get(dj)))) {
                    score++;
                }
            }
        }
        return score;
    }

    private static void removeCoveredPairs(
            Map<String, String> combo, List<String> dimNames, Set<String> uncoveredPairs) {
        for (int i = 0; i < dimNames.size(); i++) {
            for (int j = i + 1; j < dimNames.size(); j++) {
                String di = dimNames.get(i);
                String dj = dimNames.get(j);
                uncoveredPairs.remove(pairKey(di, combo.get(di), dj, combo.get(dj)));
            }
        }
    }

    private static String pairKey(String di, String vi, String dj, String vj) {
        return di + "|" + vi + "|" + dj + "|" + vj;
    }

    /** Verifies every pair of dimension-values is covered by ≥ 1 combination. */
    static void assertPairCoverage(List<Map<String, String>> matrix, Map<String, List<String>> dims) {
        List<String> dimNames = new ArrayList<>(dims.keySet());
        for (int i = 0; i < dimNames.size(); i++) {
            for (int j = i + 1; j < dimNames.size(); j++) {
                String di = dimNames.get(i);
                String dj = dimNames.get(j);
                for (String vi : dims.get(di)) {
                    for (String vj : dims.get(dj)) {
                        boolean covered = matrix.stream()
                                .anyMatch(c -> vi.equals(c.get(di)) && vj.equals(c.get(dj)));
                        assertThat(covered)
                                .as("Pair (%s=%s, %s=%s) not covered by any combination", di, vi, dj, vj)
                                .isTrue();
                    }
                }
            }
        }
    }

    private static void writeMinimalTargets(Path root) throws IOException {
        Files.createDirectories(root);
        writeArtifact(root, "universal.md");
        writeArtifact(root, "java.md", "lang.java.21");
        writeArtifact(root, "spring.md", "web.spring.boot");
        writeArtifact(root, "db.md", "data.database.postgres");
    }

    private static void writeArtifact(Path root, String name, String... capIds) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        String capList = capIds.length == 0
                ? "[]"
                : "\n" + String.join("\n", Arrays.stream(capIds).map(c -> "  - " + c).toList());
        Files.writeString(file, "---\nname: " + name.replace(".md", "")
                + "\nrequires-capabilities:" + capList + "\n---\n# Content\n");
    }
}
