package dev.iadev.application.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EpicSectionExtractorTest {

    // ── extractWhy ──────────────────────────────────────────────────────────

    @Test
    void extractWhy_v2Section_returnsFirstParagraph() {
        Map<String, List<String>> sections =
                Map.of("1. Visão & Problema", List.of("Line 1", "Line 2", "Line 3"));

        List<String> result = EpicSectionExtractor.extractWhy(sections);

        assertThat(result).containsExactly("Line 1", "Line 2", "Line 3");
    }

    @Test
    void extractWhy_fallsBackToContextoHeader() {
        Map<String, List<String>> sections = Map.of("Contexto", List.of("Context line"));

        List<String> result = EpicSectionExtractor.extractWhy(sections);

        assertThat(result).containsExactly("Context line");
    }

    @Test
    void extractWhy_capsAtFiveNonBlankLines() {
        Map<String, List<String>> sections =
                Map.of("1. Visão & Problema", List.of("L1", "L2", "L3", "L4", "L5", "L6", "L7"));

        List<String> result = EpicSectionExtractor.extractWhy(sections);

        assertThat(result).hasSize(5).containsExactly("L1", "L2", "L3", "L4", "L5");
    }

    @Test
    void extractWhy_skipsBlankLines_countNonBlankOnly() {
        Map<String, List<String>> sections =
                Map.of(
                        "1. Visão & Problema",
                        List.of("L1", "", "L2", "", "L3", "", "L4", "L5", "L6"));

        List<String> result = EpicSectionExtractor.extractWhy(sections);

        assertThat(result).hasSize(5).containsExactly("L1", "L2", "L3", "L4", "L5");
    }

    @Test
    void extractWhy_missingSections_returnsEmpty() {
        Map<String, List<String>> sections = Map.of("Other", List.of("irrelevant"));

        assertThat(EpicSectionExtractor.extractWhy(sections)).isEmpty();
    }

    @Test
    void extractWhy_resultIsUnmodifiable() {
        Map<String, List<String>> sections = Map.of("1. Visão & Problema", List.of("line"));

        List<String> result = EpicSectionExtractor.extractWhy(sections);

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> result.add("x"));
    }

    // ── extractHypothesis ───────────────────────────────────────────────────

    @Test
    void extractHypothesis_v2Section_extractsHypothesisLine() {
        Map<String, List<String>> sections =
                Map.of("3. Hipótese & OKRs", List.of("Some preamble", "Se X então Y", "More text"));

        List<String> result = EpicSectionExtractor.extractHypothesis(sections);

        assertThat(result).contains("Se X então Y");
    }

    @Test
    void extractHypothesis_alwaysAppendsOutcomePending() {
        Map<String, List<String>> sections = Map.of("3. Hipótese & OKRs", List.of("Se X então Z"));

        List<String> result = EpicSectionExtractor.extractHypothesis(sections);

        assertThat(result).last().isEqualTo(EpicSectionExtractor.OUTCOME_PENDING);
    }

    @Test
    void extractHypothesis_noHypothesisLine_returnsOnlyOutcomePending() {
        Map<String, List<String>> sections =
                Map.of("3. Hipótese & OKRs", List.of("No hypothesis here"));

        List<String> result = EpicSectionExtractor.extractHypothesis(sections);

        assertThat(result).containsExactly(EpicSectionExtractor.OUTCOME_PENDING);
    }

    @Test
    void extractHypothesis_quotedHypothesisLine_matched() {
        Map<String, List<String>> sections =
                Map.of("3. Hipótese & OKRs", List.of("> Se implementarmos X então Y"));

        List<String> result = EpicSectionExtractor.extractHypothesis(sections);

        assertThat(result).contains("> Se implementarmos X então Y");
    }

    // ── extractDecisions ────────────────────────────────────────────────────

    @Test
    void extractDecisions_v2Section_extractsDecisionLines() {
        Map<String, List<String>> sections =
                Map.of(
                        "8. Decision Rationale",
                        List.of(
                                "**Decisão:** Use hexagonal", "**Motivo:** Testability",
                                "**Consequência:** More files", "Prose not extracted"));

        List<String> result = EpicSectionExtractor.extractDecisions(sections, false);

        assertThat(result)
                .containsExactly(
                        "**Decisão:** Use hexagonal",
                        "**Motivo:** Testability",
                        "**Consequência:** More files");
    }

    @Test
    void extractDecisions_noDecisionRationale_allowLegacyFallback_returnsPlaceholder() {
        Map<String, List<String>> sections = Map.of("Other", List.of("text"));

        List<String> result = EpicSectionExtractor.extractDecisions(sections, true);

        assertThat(result).containsExactly(EpicSectionExtractor.PLACEHOLDER_NO_DECISIONS);
    }

    @Test
    void extractDecisions_noDecisionRationale_noFallback_returnsEmpty() {
        Map<String, List<String>> sections = Map.of("Other", List.of("text"));

        List<String> result = EpicSectionExtractor.extractDecisions(sections, false);

        assertThat(result).isEmpty();
    }

    @Test
    void extractDecisions_resultIsUnmodifiable() {
        Map<String, List<String>> sections =
                Map.of("8. Decision Rationale", List.of("**Decisão:** X"));

        List<String> result = EpicSectionExtractor.extractDecisions(sections, false);

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> result.add("extra"));
    }

    // ── extractAlternatives ─────────────────────────────────────────────────

    @Test
    void extractAlternatives_v2Section_extractsAlternativeLines() {
        Map<String, List<String>> sections =
                Map.of(
                        "8. Decision Rationale",
                        List.of(
                                "**Alternativa descartada:** Option B",
                                "Prose ignored",
                                "**Decisão:** Option A"));

        List<String> result = EpicSectionExtractor.extractAlternatives(sections);

        assertThat(result).containsExactly("**Alternativa descartada:** Option B");
    }

    @Test
    void extractAlternatives_noAlternatives_returnsEmpty() {
        Map<String, List<String>> sections =
                Map.of("8. Decision Rationale", List.of("**Decisão:** X"));

        assertThat(EpicSectionExtractor.extractAlternatives(sections)).isEmpty();
    }

    // ── extractPatterns ─────────────────────────────────────────────────────

    @Test
    void extractPatterns_findsPatternKeywordLines() {
        Map<String, List<String>> sections =
                Map.of(
                        "8. Decision Rationale",
                        List.of("Use the padrão of hexagonal", "unrelated line"));

        List<String> result = EpicSectionExtractor.extractPatterns(sections);

        assertThat(result).containsExactly("Use the padrão of hexagonal");
    }

    @Test
    void extractPatterns_englishPatternKeyword_found() {
        Map<String, List<String>> sections = Map.of("Patterns", List.of("Apply pattern of CQRS"));

        List<String> result = EpicSectionExtractor.extractPatterns(sections);

        assertThat(result).containsExactly("Apply pattern of CQRS");
    }

    @Test
    void extractPatterns_deduplicatesAcrossSections() {
        String duplicate = "Use the pattern of DDD";
        Map<String, List<String>> sections =
                Map.of(
                        "Sec A", List.of(duplicate),
                        "Sec B", List.of(duplicate));

        List<String> result = EpicSectionExtractor.extractPatterns(sections);

        assertThat(result).hasSize(1).containsExactly(duplicate);
    }

    @Test
    void extractPatterns_caseInsensitiveMatch() {
        Map<String, List<String>> sections =
                Map.of(
                        "X",
                        List.of("use PADRÃO for this", "use PATTERN there", "use CONVENÇÃO here"));

        List<String> result = EpicSectionExtractor.extractPatterns(sections);

        assertThat(result).hasSize(3);
    }

    // ── extractAntiPatterns ─────────────────────────────────────────────────

    @Test
    void extractAntiPatterns_findsAntiPatternKeywordLines() {
        Map<String, List<String>> sections =
                Map.of(
                        "X",
                        List.of(
                                "This approach is rejeitado",
                                "normal line",
                                "evitar coupling here",
                                "use anti-padrão never"));

        List<String> result = EpicSectionExtractor.extractAntiPatterns(sections);

        assertThat(result)
                .containsExactly(
                        "This approach is rejeitado",
                        "evitar coupling here",
                        "use anti-padrão never");
    }

    @Test
    void extractAntiPatterns_deduplicatesAcrossSections() {
        String dup = "evitar god classes";
        Map<String, List<String>> sections =
                Map.of(
                        "A", List.of(dup),
                        "B", List.of(dup));

        assertThat(EpicSectionExtractor.extractAntiPatterns(sections)).hasSize(1);
    }

    // ── determinism ─────────────────────────────────────────────────────────

    @Test
    void extractDecisions_deterministic_sameInputSameOutput() {
        Map<String, List<String>> sections =
                Map.of(
                        "8. Decision Rationale",
                        List.of("**Decisão:** A", "**Motivo:** B", "**Consequência:** C"));

        List<String> first = EpicSectionExtractor.extractDecisions(sections, false);
        List<String> second = EpicSectionExtractor.extractDecisions(sections, false);

        assertThat(first).isEqualTo(second);
    }
}
