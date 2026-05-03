package dev.iadev.application.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles {@link MemoryFrontmatter} by grepping deterministic signals from an epic's raw markdown
 * content.
 *
 * <p>Tags and capabilities-affected are runtime-only concerns (they require access to story files
 * and the tags catalog at LLM turn time); this assembler leaves them as empty lists so the {@code
 * x-internal-epic-summary} skill can inject them.
 *
 * <p>All extraction is deterministic: same input → same output.
 */
public final class MemoryFrontmatterAssembler {

    private static final String SUMMARY_VERSION = "1.0";

    private static final Pattern RULE_PATTERN = Pattern.compile("Rule (\\d+)");
    private static final Pattern ADR_PATTERN = Pattern.compile("ADR-(\\d{4})");
    private static final Pattern DEPENDS_ON_PATTERN =
            Pattern.compile("Depende de:\\s*\\[?EPIC-(\\d{4})");
    private static final Pattern BLOCKS_PATTERN = Pattern.compile("Blocks:\\s*\\[?EPIC-(\\d{4})");

    private MemoryFrontmatterAssembler() {}

    /**
     * Assembles the frontmatter for an epic memory summary.
     *
     * @param epicId e.g. {@code "EPIC-0067"}
     * @param slug e.g. {@code "review-yaml-frontmatter"}
     * @param epicContent raw markdown of the epic document
     * @param patternsIntroduced already-extracted pattern lines (from {@link EpicSectionExtractor})
     * @param antipatternsRejected already-extracted anti-pattern lines
     * @param date ISO-8601 date string for {@code created} and {@code last-updated}
     * @return assembled frontmatter (tags + capabilities-affected are empty — runtime-only)
     */
    public static MemoryFrontmatter assemble(
            String epicId,
            String slug,
            String epicContent,
            List<String> patternsIntroduced,
            List<String> antipatternsRejected,
            String date) {

        List<String> rulesAffected = extractRules(epicContent);
        List<String> adrsReferenced = extractAdrs(epicContent);
        List<String> dependenciesOf = extractEpicRefs(epicContent, DEPENDS_ON_PATTERN);
        List<String> dependenciesFor = extractEpicRefs(epicContent, BLOCKS_PATTERN);

        return new MemoryFrontmatter(
                epicId,
                slug,
                SUMMARY_VERSION,
                date,
                date,
                true,
                false,
                null,
                List.of(),
                List.of(),
                rulesAffected,
                adrsReferenced,
                Collections.unmodifiableList(new ArrayList<>(patternsIntroduced)),
                Collections.unmodifiableList(new ArrayList<>(antipatternsRejected)),
                dependenciesOf,
                dependenciesFor);
    }

    private static List<String> extractRules(String content) {
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = RULE_PATTERN.matcher(content);
        while (m.find()) {
            seen.add("Rule " + m.group(1));
        }
        return Collections.unmodifiableList(new ArrayList<>(seen));
    }

    private static List<String> extractAdrs(String content) {
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = ADR_PATTERN.matcher(content);
        while (m.find()) {
            seen.add("ADR-" + m.group(1));
        }
        return Collections.unmodifiableList(new ArrayList<>(seen));
    }

    private static List<String> extractEpicRefs(String content, Pattern pattern) {
        Set<String> seen = new LinkedHashSet<>();
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            seen.add("EPIC-" + m.group(1));
        }
        return Collections.unmodifiableList(new ArrayList<>(seen));
    }
}
