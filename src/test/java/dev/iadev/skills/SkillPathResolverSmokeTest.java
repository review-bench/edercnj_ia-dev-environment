package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EPIC-0060 / story-0060-0004 — CI gate that prevents regressions of hardcoded {@code plans/epic-N}
 * paths in SKILL.md files.
 *
 * <p>The test scans every SKILL.md under {@code src/main/resources/targets/claude/skills/} and
 * reports any skill containing a hardcoded {@code plans/epic-NNNN} reference outside the
 * user-facing {@code ## Triggers} or {@code ## Examples} sections. The baseline file {@code
 * governance/baselines/skill-pathresolver-baseline.txt} grandfathers skills that already had the
 * violation when EPIC-0060 was introduced.
 *
 * <p>A NEW skill (or a previously-clean skill that introduces a hardcoded path) MUST cause this
 * test to fail.
 */
@DisplayName("SkillPathResolverSmokeTest — EPIC-0060 hardcoded path gate")
class SkillPathResolverSmokeTest {

    private static final Path REPO_ROOT = Paths.get(".").toAbsolutePath().normalize();
    private static final Path SKILLS_ROOT =
            REPO_ROOT.resolve("src/main/resources/targets/claude/skills");
    private static final Path BASELINE =
            REPO_ROOT.resolve("governance/baselines/skill-pathresolver-baseline.txt");

    private static final Pattern HARDCODED = Pattern.compile("plans/epic-[0-9]");

    @Test
    @DisplayName("baseline file exists and is parseable")
    void baseline_existsAndParseable() throws IOException {
        // Post-EPIC-0062 (story-0062-0006): all 14 grandfathered skills migrated to
        // PathResolver, so baseline is intentionally empty (only the explanatory
        // comment remains). The file MUST still exist as the contract surface of
        // the audit, but emptiness is a valid (and now expected) state.
        assertThat(BASELINE).exists().isRegularFile();
        Set<String> entries = loadBaseline();
        assertThat(entries).isNotNull();
    }

    @Test
    @DisplayName("no skill outside the baseline contains hardcoded plans/epic-N path")
    void noNewViolations() throws IOException {
        Set<String> baseline = loadBaseline();
        List<String> violators;
        try (Stream<Path> stream = Files.walk(SKILLS_ROOT)) {
            violators =
                    stream.filter(p -> p.getFileName().toString().equals("SKILL.md"))
                            .filter(this::containsHardcodedPath)
                            .map(this::skillIdFromPath)
                            .filter(id -> !baseline.contains(id))
                            .sorted()
                            .toList();
        }

        assertThat(violators)
                .as(
                        "New skills with hardcoded plans/epic-N references "
                                + "(NOT in baseline). Either use PathResolver "
                                + "placeholders or add an explicit baseline "
                                + "entry with reviewer approval.")
                .isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Set<String> loadBaseline() throws IOException {
        try (Stream<String> lines = Files.lines(BASELINE)) {
            return lines.map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private boolean containsHardcodedPath(Path skillMd) {
        try {
            return HARDCODED.matcher(Files.readString(skillMd)).find();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String skillIdFromPath(Path skillMd) {
        Path relative = SKILLS_ROOT.relativize(skillMd.getParent());
        return relative.toString().replace('\\', '/');
    }
}
