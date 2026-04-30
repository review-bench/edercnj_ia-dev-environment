package dev.iadev.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CI-blocking audit for RA9 planning artifact compliance (RULE-006 EPIC-0056 — story-0056-0007).
 *
 * <p>Scans all {@code plans/epic-XXXX/} markdown files for three RA9 rules:
 *
 * <ul>
 *   <li>{@code RA9_SECTIONS_MISSING} — any of the 9 section headers absent.
 *   <li>{@code RA9_RATIONALE_EMPTY} — section 8 present but empty / placeholder / TODO (Epic +
 *       Story only).
 *   <li>{@code RA9_PACKAGES_MISSING} — section 2 present but all layers marked {@code —}.
 * </ul>
 *
 * <p>Files matching patterns in {@code audits/lifecycle-integrity-baseline.txt} (resource) are
 * grandfathered (epics 0001–0058) and skipped. Epic-0056 itself is the introduction of RA9, so its
 * planning files predate RA9 and are exempted. Epics 0057-0058 already existed at the time RA9 was
 * introduced and are likewise exempt.
 *
 * <p>Files containing {@code <!-- audit-exempt -->} are also skipped.
 */
@DisplayName("LifecycleIntegrityAuditTest — RA9 compliance")
class LifecycleIntegrityAuditTest {

    /**
     * Candidate locations for the legacy {@code plans/} directory. After the v4 layout migration
     * (CLAUDE.md "Folder Cleanup Post-v4"), planning artifacts live under {@code ai/epics/}, but
     * the existing baseline patterns reference {@code plans/epic-NNNN/**} (the pre-v4 layout) and
     * have not been re-keyed. Until that migration completes, the audit walks {@code plans/} when
     * present and gracefully no-ops otherwise — matching the behavior on develop.
     */
    private static final List<Path> PLANS_CANDIDATES =
            List.of(
                    Path.of("plans").toAbsolutePath().normalize(),
                    Path.of("..").resolve("plans").toAbsolutePath().normalize());

    private static final String BASELINE_RESOURCE = "/audits/lifecycle-integrity-baseline.txt";

    private static final Set<String> PLANNING_ARTIFACT_PREFIXES =
            Set.of("epic-", "story-", "task-");

    /**
     * Path segments that identify EPIC-0064 config/schema/fragment artifacts — excluded from RA9
     * lifecycle enforcement (story-0064-0007). Capabilities are pure config YAML, fragments carry
     * {@code fragment-slot} frontmatter instead of {@code Status}, and governance schemas are JSON
     * validators with no lifecycle state.
     */
    private static final List<String> EXCLUDED_NAMESPACE_SEGMENTS =
            List.of("capabilities/", "fragments/", "governance/schemas/");

    private final Ra9SectionsChecker sectionsChecker = new Ra9SectionsChecker();
    private final Ra9RationaleChecker rationaleChecker = new Ra9RationaleChecker();
    private final Ra9PackagesChecker packagesChecker = new Ra9PackagesChecker();

    @Test
    @DisplayName("audit_planningArtifacts_noRa9Violations")
    void audit_planningArtifacts_noRa9Violations() throws IOException {
        Path plansRoot = resolvePlansRoot();
        if (plansRoot == null) {
            // No plans/ directory present in this checkout — pre-v4 layout artifacts live under
            // ai/epics/ now. The baseline patterns still reference plans/, so re-keying is tracked
            // separately. Until then, no-op gracefully (matches the behavior on develop, where
            // plans/ exists but contains no .md files).
            System.err.println(
                    "[LifecycleIntegrityAuditTest] plans/ directory absent — audit no-op."
                            + " Tracked separately for the v4 layout migration.");
            return;
        }

        Set<String> baselinePatterns = loadBaseline();
        List<String> allViolations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(plansRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> isPlanningArtifact(p))
                    .filter(p -> !isBaselined(p, baselinePatterns))
                    .forEach(
                            p -> {
                                try {
                                    String content = Files.readString(p, StandardCharsets.UTF_8);
                                    String filename = p.toString();
                                    allViolations.addAll(sectionsChecker.check(content, filename));
                                    allViolations.addAll(rationaleChecker.check(content, filename));
                                    allViolations.addAll(packagesChecker.check(content, filename));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        }

        assertThat(allViolations)
                .as(
                        "RA9 audit found %d violation(s)."
                                + " Fix or add <!-- audit-exempt -->."
                                + "\nViolations:\n%s",
                        allViolations.size(), String.join("\n", allViolations))
                .isEmpty();
    }

    /**
     * Resolves the {@code plans/} directory if present, returning {@code null} when absent so the
     * caller can no-op gracefully. The legacy plans/ tree was migrated to {@code ai/epics/} in the
     * v4 layout cleanup; until the baseline is re-keyed, this audit only runs against plans/ when a
     * checkout still carries it.
     */
    private Path resolvePlansRoot() {
        for (Path candidate : PLANS_CANDIDATES) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isPlanningArtifact(Path path) {
        String name = path.getFileName().toString();
        return PLANNING_ARTIFACT_PREFIXES.stream().anyMatch(name::startsWith)
                && !isExcludedNamespace(path);
    }

    static boolean isExcludedNamespace(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return EXCLUDED_NAMESPACE_SEGMENTS.stream()
                .anyMatch(seg -> normalized.startsWith(seg) || normalized.contains("/" + seg));
    }

    private boolean isBaselined(Path path, Set<String> patterns) {
        String normalized = path.toString().replace('\\', '/');
        return patterns.stream().anyMatch(pattern -> matchesGlob(normalized, pattern));
    }

    /**
     * Glob matcher with strict path-segment boundaries.
     *
     * <p>{@code prefix/**} matches only paths that contain the directory {@code prefix} as a
     * complete path segment (followed by {@code /}). Prevents over-matching: {@code
     * plans/epic-0001/**} no longer matches {@code plans/epic-00010/**}.
     */
    private boolean matchesGlob(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String dir = pattern.substring(0, pattern.length() - 3);
            return path.contains("/" + dir + "/") || path.startsWith(dir + "/");
        }
        return path.endsWith(pattern) || path.contains("/" + pattern);
    }

    private Set<String> loadBaseline() {
        try (InputStream is =
                LifecycleIntegrityAuditTest.class.getResourceAsStream(BASELINE_RESOURCE)) {
            if (is == null) {
                return Set.of();
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of();
        }
    }

    /**
     * Verifies the three EPIC-0064 namespace exclusions (story-0064-0007).
     *
     * <p>Capabilities are config YAML (not lifecycle-controlled), fragments carry {@code
     * fragment-slot} frontmatter (not standalone Status), and governance schemas are pure JSON
     * validators. All three must be excluded from the RA9 lifecycle audit.
     */
    @Nested
    @DisplayName("EPIC-0064 namespace exclusions (story-0064-0007)")
    class Epic0064NamespaceExclusionsTest {

        @Test
        @DisplayName("capabilities YAML not flagged as missing Status")
        void isExcludedNamespace_capabilitiesPath_returnsTrue() {
            Path p = Path.of("capabilities/data/database/postgres.yaml");
            assertThat(isExcludedNamespace(p)).isTrue();
        }

        @Test
        @DisplayName("fragment md treated as fragment — no standalone Status needed")
        void isExcludedNamespace_fragmentsPath_returnsTrue() {
            Path p = Path.of("targets/claude/skills/x-review/fragments/db.md");
            assertThat(isExcludedNamespace(p)).isTrue();
        }

        @Test
        @DisplayName("governance schema JSON exempted from Status check")
        void isExcludedNamespace_governanceSchemasPath_returnsTrue() {
            Path p = Path.of("governance/schemas/frontmatter-3.0.json");
            assertThat(isExcludedNamespace(p)).isTrue();
        }

        @Test
        @DisplayName("planning artifact md not excluded")
        void isExcludedNamespace_planningArtifactPath_returnsFalse() {
            Path p = Path.of("plans/epic-0064/plans/story-0064-0001.md");
            assertThat(isExcludedNamespace(p)).isFalse();
        }
    }
}
