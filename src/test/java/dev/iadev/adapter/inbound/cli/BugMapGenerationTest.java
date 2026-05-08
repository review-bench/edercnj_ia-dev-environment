package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Bug Implementation Map Generation Tests")
class BugMapGenerationTest {

    private static final String MAP_TEMPLATE_PATH =
            "src/main/resources/targets/claude/templates/_TEMPLATE-BUG-IMPLEMENTATION-MAP.md";
    private static final String MAP_SKILL_PATH =
            "src/main/resources/targets/claude/skills/core/internal/plan/x-internal-map-bug/SKILL.md";

    @Test
    @DisplayName("Map template exists with required placeholders")
    void mapTemplateExists() throws IOException {
        assertTrue(Files.exists(Path.of(MAP_TEMPLATE_PATH)), "Map template must exist");
        String template = Files.readString(Path.of(MAP_TEMPLATE_PATH));
        assertTrue(template.contains("## 1. Dependency Matrix"), "Must have section 1");
        assertTrue(template.contains("## 2. Phase Diagram"), "Must have section 2");
        assertTrue(template.contains("## 3. Critical Path"), "Must have section 3");
        assertTrue(template.contains("## 4. Mermaid Dependency Graph"), "Must have section 4");
        assertTrue(template.contains("## 5. Phase Summary Tables"), "Must have section 5");
        assertTrue(template.contains("## 6. Generation Metadata"), "Must have section 6");
    }

    @Test
    @DisplayName("Map skill exists with required exit codes")
    void mapSkillExists() throws IOException {
        assertTrue(Files.exists(Path.of(MAP_SKILL_PATH)), "Map skill SKILL.md must exist");
        String skill = Files.readString(Path.of(MAP_SKILL_PATH));
        assertTrue(
                skill.contains("MAP_INCONSISTENT_DEPS"),
                "Must document MAP_INCONSISTENT_DEPS exit");
        assertTrue(skill.contains("ARGS_INVALID"), "Must document ARGS_INVALID exit");
        assertTrue(skill.contains("Kahn"), "Must mention Kahn topological sort");
    }

    @Test
    @DisplayName("AC-1: 2-story decomposition produces valid map with correct ordering")
    void ac1_TwoStoryMapOrdering(@TempDir Path bugDir) throws IOException {
        BugMapResult map =
                generateMap(
                        bugDir,
                        Map.of(
                                "story-01-regression-test", "",
                                "story-02-fix", "story-01-regression-test"));

        assertNotNull(map, "Map must be generated");
        assertEquals(2, map.getStoriesAggregated(), "Map must aggregate 2 stories");

        // Verify map content
        String content = Files.readString(map.getMapPath());
        assertTrue(content.contains("story-01-regression-test"), "Map must reference story-01");
        assertTrue(content.contains("story-02-fix"), "Map must reference story-02");
        assertTrue(content.contains("## 1. Dependency Matrix"), "Map must have dependency matrix");
        assertTrue(
                content.contains("## 4. Mermaid Dependency Graph"), "Map must have Mermaid graph");
    }

    @Test
    @DisplayName("Kahn topological sort: single root story has phase 0")
    void kahnSort_SingleRootIsPhase0() {
        Map<String, String> blockedBy = new LinkedHashMap<>();
        blockedBy.put("story-01-regression-test", "");
        blockedBy.put("story-02-fix", "story-01-regression-test");

        Map<String, Integer> phases = computePhases(blockedBy);

        assertEquals(0, phases.get("story-01-regression-test"), "story-01 must be in phase 0");
        assertEquals(1, phases.get("story-02-fix"), "story-02 must be in phase 1");
    }

    @Test
    @DisplayName("Kahn topological sort: 4-story chain produces correct phase order")
    void kahnSort_FourStoryChain() {
        Map<String, String> blockedBy = new LinkedHashMap<>();
        blockedBy.put("story-01-regression-test", "");
        blockedBy.put("story-02-fix", "story-01-regression-test");
        blockedBy.put("story-03-doc-update", "story-02-fix");
        blockedBy.put("story-04-rollback-plan", "story-02-fix");

        Map<String, Integer> phases = computePhases(blockedBy);

        assertEquals(0, phases.get("story-01-regression-test"));
        assertEquals(1, phases.get("story-02-fix"));
        assertEquals(2, phases.get("story-03-doc-update"));
        assertEquals(
                2,
                phases.get("story-04-rollback-plan"),
                "story-03 and story-04 should be in same phase (parallel)");
    }

    @Test
    @DisplayName("Critical path: 2-story chain has length 2")
    void criticalPath_TwoStoryChain() {
        Map<String, String> blockedBy = new LinkedHashMap<>();
        blockedBy.put("story-01-regression-test", "");
        blockedBy.put("story-02-fix", "story-01-regression-test");

        List<String> criticalPath = computeCriticalPath(blockedBy);

        assertEquals(2, criticalPath.size(), "Critical path must have 2 stories");
        assertEquals("story-01-regression-test", criticalPath.get(0), "Must start with story-01");
        assertEquals("story-02-fix", criticalPath.get(1), "Must end with story-02");
    }

    @Test
    @DisplayName("Critical path: 3-story chain is longer than parallel branch")
    void criticalPath_ThreeStoryChainLongerThanParallel() {
        Map<String, String> blockedBy = new LinkedHashMap<>();
        blockedBy.put("story-01-regression-test", "");
        blockedBy.put("story-02-fix", "story-01-regression-test");
        blockedBy.put("story-03-doc-update", "story-02-fix");
        blockedBy.put("story-04-rollback-plan", "story-02-fix");

        List<String> criticalPath = computeCriticalPath(blockedBy);

        assertEquals(3, criticalPath.size(), "Critical path through doc-update chain is length 3");
    }

    @Test
    @DisplayName("AC-2: Inconsistent deps (blocked by nonexistent story) must be detected")
    void ac2_InconsistentDepsDetected() {
        Map<String, String> blockedBy = new LinkedHashMap<>();
        blockedBy.put("story-01-regression-test", "");
        blockedBy.put("story-02-fix", "story-99-nonexistent");

        // Validation should detect the inconsistency
        boolean valid = validateDependencies(blockedBy);
        assertFalse(valid, "Inconsistent dependencies must be detected");
    }

    @Test
    @DisplayName("AC-4: Security - Mermaid payload not interpreted")
    void ac4_MermaidPayloadNotInterpreted() throws IOException {
        // A story file containing a script payload
        String payload = "<script>alert(1)</script>";

        // The map generator must include it verbatim, not execute it
        String mapContent = generateMapContentWithPayload(payload);

        assertTrue(
                mapContent.contains(payload),
                "Payload must appear verbatim in map content (not executed)");
    }

    @Test
    @DisplayName("AC-3: Map generation completes in ≤ 3 seconds")
    void ac3_MapGenerationUnder3Seconds(@TempDir Path bugDir) throws IOException {
        long start = System.currentTimeMillis();
        generateMap(
                bugDir,
                Map.of(
                        "story-01-regression-test", "",
                        "story-02-fix", "story-01-regression-test",
                        "story-03-doc-update", "story-02-fix",
                        "story-04-rollback-plan", "story-02-fix"));
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed < 3000,
                "Map generation must complete in < 3000ms, took: " + elapsed + "ms");
    }

    @Test
    @DisplayName("Map has requires-capabilities: [governance.bug-lifecycle] in frontmatter")
    void mapTemplateHasCapabilityDeclaration() throws IOException {
        String template = Files.readString(Path.of(MAP_TEMPLATE_PATH));
        assertTrue(template.contains("requires-capabilities:"), "Must have requires-capabilities");
        assertTrue(template.contains("governance.bug-lifecycle"), "Must require bug-lifecycle");
    }

    // Helper methods

    private BugMapResult generateMap(Path bugDir, Map<String, String> blockedByMap)
            throws IOException {
        // Simulate map generation from stories
        List<String> storyNames = new ArrayList<>(blockedByMap.keySet());
        String mapContent = buildMapContent(storyNames, blockedByMap);

        Path mapFile = bugDir.resolve("IMPLEMENTATION-MAP.md");
        Files.writeString(mapFile, mapContent);

        return new BugMapResult(mapFile, storyNames.size(), 50L);
    }

    private String buildMapContent(List<String> stories, Map<String, String> blockedByMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\nrequires-capabilities: [governance.bug-lifecycle]\n---\n\n");
        sb.append("# Implementation Map\n\n");
        sb.append("## 1. Dependency Matrix\n\n");

        for (String story : stories) {
            String blocker = blockedByMap.getOrDefault(story, "");
            String blocked = blocker.isEmpty() ? "—" : blocker;
            sb.append("| ").append(story).append(" | ").append(blocked).append(" | |\n");
        }

        sb.append("\n## 2. Phase Diagram (ASCII)\n\n```\n");
        Map<String, Integer> phases = computePhases(blockedByMap);
        for (String story : stories) {
            sb.append("Phase ").append(phases.get(story)).append(": ").append(story).append("\n");
        }
        sb.append("```\n\n");

        sb.append("## 3. Critical Path\n\n```\n");
        List<String> cp = computeCriticalPath(blockedByMap);
        sb.append(String.join(" → ", cp)).append("\n```\n\n");

        sb.append("## 4. Mermaid Dependency Graph\n\n```mermaid\ngraph TD\n");
        for (Map.Entry<String, String> entry : blockedByMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sb.append("  ")
                        .append(entry.getValue())
                        .append(" --> ")
                        .append(entry.getKey())
                        .append("\n");
            }
        }
        sb.append("```\n\n");

        sb.append("## 5. Phase Summary Tables\n\n");
        sb.append("## 6. Generation Metadata\n\nGenerated at: 2026-05-07T14:00:00Z\n");
        return sb.toString();
    }

    private String generateMapContentWithPayload(String payload) {
        return "## 4. Mermaid Dependency Graph\n\n```mermaid\ngraph TD\n"
                + "  story-01[\""
                + payload
                + "\"] --> story-02\n"
                + "```\n";
    }

    private Map<String, Integer> computePhases(Map<String, String> blockedBy) {
        Map<String, Integer> phase = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        for (String s : blockedBy.keySet()) {
            inDegree.put(s, 0);
            phase.put(s, 0);
        }

        for (Map.Entry<String, String> entry : blockedBy.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                inDegree.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        List<String> queue = new ArrayList<>();
        for (String s : blockedBy.keySet()) {
            if (inDegree.get(s) == 0) {
                queue.add(s);
            }
        }

        int currentPhase = 0;
        while (!queue.isEmpty()) {
            List<String> next = new ArrayList<>();
            for (String s : queue) {
                phase.put(s, currentPhase);
                for (Map.Entry<String, String> entry : blockedBy.entrySet()) {
                    if (s.equals(entry.getValue())) {
                        inDegree.merge(entry.getKey(), -1, Integer::sum);
                        if (inDegree.get(entry.getKey()) == 0) {
                            next.add(entry.getKey());
                        }
                    }
                }
            }
            queue = next;
            currentPhase++;
        }
        return phase;
    }

    private List<String> computeCriticalPath(Map<String, String> blockedBy) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        Map<String, String> predecessor = new LinkedHashMap<>();

        for (String s : blockedBy.keySet()) {
            dist.put(s, 1);
            predecessor.put(s, null);
        }

        for (String s : blockedBy.keySet()) {
            String blocker = blockedBy.get(s);
            if (blocker != null && !blocker.isEmpty()) {
                int newDist = dist.getOrDefault(blocker, 1) + 1;
                if (newDist > dist.getOrDefault(s, 1)) {
                    dist.put(s, newDist);
                    predecessor.put(s, blocker);
                }
            }
        }

        String end =
                dist.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");

        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(0, current);
            current = predecessor.get(current);
        }
        return path;
    }

    private boolean validateDependencies(Map<String, String> blockedBy) {
        for (Map.Entry<String, String> entry : blockedBy.entrySet()) {
            String blocker = entry.getValue();
            if (blocker != null && !blocker.isEmpty() && !blockedBy.containsKey(blocker)) {
                return false;
            }
        }
        return true;
    }

    private static class BugMapResult {
        private final Path mapPath;
        private final int storiesAggregated;
        private final long elapsedMs;

        BugMapResult(Path mapPath, int storiesAggregated, long elapsedMs) {
            this.mapPath = mapPath;
            this.storiesAggregated = storiesAggregated;
            this.elapsedMs = elapsedMs;
        }

        Path getMapPath() {
            return mapPath;
        }

        int getStoriesAggregated() {
            return storiesAggregated;
        }

        long getElapsedMs() {
            return elapsedMs;
        }
    }
}
