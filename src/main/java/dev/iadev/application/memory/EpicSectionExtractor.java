package dev.iadev.application.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extracts structured content from parsed epic sections for memory summary generation.
 *
 * <p>All extraction methods are deterministic: same input map → same output. Methods never
 * introduce variance; they apply fixed rules against named sections.
 */
public final class EpicSectionExtractor {

    static final String PLACEHOLDER_NO_DECISIONS = "<no formal decisions recorded — extracted from prose>";
    static final String OUTCOME_PENDING = "outcome: pending";

    private static final Pattern DECISION_LINE = Pattern.compile("^\\*\\*Decisão:\\*\\*.*");
    private static final Pattern MOTIVO_LINE = Pattern.compile("^\\*\\*Motivo:\\*\\*.*");
    private static final Pattern CONSEQUENCIA_LINE = Pattern.compile("^\\*\\*Consequência:\\*\\*.*");
    private static final Pattern ALTERNATIVE_LINE = Pattern.compile("^\\*\\*Alternativa descartada:\\*\\*.*");
    private static final Pattern HYPOTHESIS_LINE = Pattern.compile("^(>\\s*)?[Ss]e .*(então|entao).*");
    private static final Pattern PATTERN_KEYWORD = Pattern.compile("(?iu)padrão|pattern|convenção");
    private static final Pattern ANTIPATTERN_KEYWORD = Pattern.compile("(?iu)anti-padrão|antipadrão|rejeitado|evitar");
    private static final int MAX_WHY_LINES = 5;

    private EpicSectionExtractor() {}

    /**
     * Extracts the first paragraph of the problem/vision section (≤5 lines).
     *
     * @param sections parsed epic sections
     * @return up to 5 non-blank lines from the first matching header
     */
    public static List<String> extractWhy(Map<String, List<String>> sections) {
        List<String> body = findSection(sections, "1. Visão & Problema", "Contexto", "1.");
        List<String> result = new ArrayList<>();
        for (String line : body) {
            if (result.size() >= MAX_WHY_LINES) break;
            if (!line.isBlank()) result.add(line);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Extracts the hypothesis line from section 3 (v2) or prose. Appends {@code outcome: pending}
     * when no completion report outcome is available.
     */
    public static List<String> extractHypothesis(Map<String, List<String>> sections) {
        List<String> body = findSection(sections, "3. Hipótese & OKRs", "Hipótese", "3.");
        List<String> result = new ArrayList<>();
        for (String line : body) {
            if (HYPOTHESIS_LINE.matcher(line.strip()).matches()) {
                result.add(line.strip());
                break;
            }
        }
        result.add(OUTCOME_PENDING);
        return Collections.unmodifiableList(result);
    }

    /**
     * Extracts decision blocks (Decisão/Motivo/Consequência lines).
     *
     * @param sections parsed sections
     * @param allowLegacyFallback when true, return placeholder instead of empty list
     */
    public static List<String> extractDecisions(
            Map<String, List<String>> sections, boolean allowLegacyFallback) {
        List<String> body = findSection(sections, "8. Decision Rationale", "6.", "Decision Rationale");
        List<String> result = new ArrayList<>();
        for (String line : body) {
            String s = line.strip();
            if (DECISION_LINE.matcher(s).matches()
                    || MOTIVO_LINE.matcher(s).matches()
                    || CONSEQUENCIA_LINE.matcher(s).matches()) {
                result.add(s);
            }
        }
        if (result.isEmpty() && allowLegacyFallback) {
            return List.of(PLACEHOLDER_NO_DECISIONS);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Extracts rejected alternatives lines.
     */
    public static List<String> extractAlternatives(Map<String, List<String>> sections) {
        List<String> body = findSection(sections, "8. Decision Rationale", "6.", "Decision Rationale");
        List<String> result = new ArrayList<>();
        for (String line : body) {
            if (ALTERNATIVE_LINE.matcher(line.strip()).matches()) {
                result.add(line.strip());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Scans Decision Rationale and all sections for pattern-keyword lines; deduplicates.
     */
    public static List<String> extractPatterns(Map<String, List<String>> sections) {
        return extractByKeyword(sections, PATTERN_KEYWORD);
    }

    /**
     * Scans Decision Rationale and all sections for anti-pattern-keyword lines; deduplicates.
     */
    public static List<String> extractAntiPatterns(Map<String, List<String>> sections) {
        return extractByKeyword(sections, ANTIPATTERN_KEYWORD);
    }

    private static List<String> extractByKeyword(Map<String, List<String>> sections, Pattern kw) {
        List<String> result = new ArrayList<>();
        for (List<String> body : sections.values()) {
            for (String line : body) {
                String s = line.strip();
                if (!s.isBlank() && kw.matcher(s).find() && !result.contains(s)) {
                    result.add(s);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> findSection(Map<String, List<String>> sections, String... candidates) {
        for (String candidate : candidates) {
            for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
                if (entry.getKey().startsWith(candidate)) {
                    return entry.getValue();
                }
            }
        }
        return List.of();
    }
}
