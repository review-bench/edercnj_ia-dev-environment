package dev.iadev.application.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EpicMarkdownParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\n"})
    void parse_nullOrBlankContent_returnsEmptyMap(String content) {
        assertThat(EpicMarkdownParser.parse(content)).isEmpty();
    }

    @Test
    void parse_singleH2Section_returnsSectionWithBody() {
        String content = "## Overview\nFirst line\nSecond line\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections).containsOnlyKeys("Overview");
        assertThat(sections.get("Overview")).containsExactly("First line", "Second line");
    }

    @Test
    void parse_singleH3Section_returnsSectionWithBody() {
        String content = "### Decision Rationale\nDecision text\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections).containsOnlyKeys("Decision Rationale");
        assertThat(sections.get("Decision Rationale")).containsExactly("Decision text");
    }

    @Test
    void parse_multipleSections_returnsAllSections() {
        String content =
                """
                ## 1. Visão & Problema
                Problem description
                ## 2. Hipótese
                Hypothesis text
                ## 8. Decision Rationale
                Decision A
                """;

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections)
                .containsOnlyKeys("1. Visão & Problema", "2. Hipótese", "8. Decision Rationale");
        assertThat(sections.get("1. Visão & Problema")).containsExactly("Problem description");
        assertThat(sections.get("2. Hipótese")).containsExactly("Hypothesis text");
        assertThat(sections.get("8. Decision Rationale")).containsExactly("Decision A");
    }

    @Test
    void parse_trailingBlanksPerSection_trimmed() {
        String content = "## Section\nLine 1\n\n\n## Next\nContent\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections.get("Section")).containsExactly("Line 1");
    }

    @Test
    void parse_bodyLinesBeforeFirstHeader_ignored() {
        String content = "# Title\nPreamble\n## Section\nBody\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections).containsOnlyKeys("Section");
    }

    @Test
    void parse_preservesInsertionOrder() {
        String content = "## B\nB body\n## A\nA body\n## C\nC body\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections.keySet()).containsExactly("B", "A", "C");
    }

    @Test
    void parse_emptyBody_returnsEmptyList() {
        String content = "## Section\n## Next\nContent\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections.get("Section")).isEmpty();
    }

    @Test
    void parse_deterministic_sameInputSameOutput() {
        String content = "## A\nLine 1\nLine 2\n## B\nLine 3\n";

        Map<String, List<String>> first = EpicMarkdownParser.parse(content);
        Map<String, List<String>> second = EpicMarkdownParser.parse(content);

        assertThat(first).isEqualTo(second);
        assertThat(first.keySet().toArray()).isEqualTo(second.keySet().toArray());
    }

    @Test
    void parse_sectionHeaderStripsLeadingHashes() {
        String content = "##   Padded Header  \nBody\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections).containsOnlyKeys("Padded Header");
    }

    @Test
    void parse_resultMapIsUnmodifiable() {
        String content = "## A\nBody\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> sections.put("new", List.of()));
    }

    @Test
    void parse_bodyListIsUnmodifiable() {
        String content = "## A\nBody\n";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> sections.get("A").add("extra"));
    }

    @Test
    void parse_lastSectionWithoutTrailingNewline_captured() {
        String content = "## Final\nLast line";

        Map<String, List<String>> sections = EpicMarkdownParser.parse(content);

        assertThat(sections).containsOnlyKeys("Final");
        assertThat(sections.get("Final")).containsExactly("Last line");
    }
}
