package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — validates structural invariants for EPIC-0078 story-0078-0004
 * (Extract concluded epics history from CLAUDE.md).
 *
 * <p>Verifies that: (a) CLAUDE.md ≤200 lines; (b) docs/epics-history.md exists
 * with ≥10 "Concluded" blocks; (c) CLAUDE.md contains a link to epics-history.md;
 * (d) CLAUDE.md contains no "Concluded — " blockquote lines; (e) extracted content
 * represents a reduction of ≥3000 token-equivalents (bytes/4 heuristic).
 */
@DisplayName("Epic0078Story0004SmokeIT — Extract concluded epics to docs/epics-history.md")
class Epic0078Story0004SmokeIT {

    private static final Path CLAUDE_MD = Path.of("CLAUDE.md");
    private static final Path EPICS_HISTORY = Path.of("docs", "epics-history.md");
    private static final int MAX_CLAUDE_MD_LINES = 200;
    private static final int MIN_CONCLUDED_BLOCKS = 10;
    private static final int REQUIRED_TOKEN_REDUCTION = 3000;

    /** Regex matching a "Concluded — EPIC-XXXX" blockquote header line. */
    private static final Pattern CONCLUDED_HEADER =
            Pattern.compile("^>?\\s*\\*\\*Concluded\\s+—\\s+(EPIC-\\d{4}|Folder)");

    @Test
    @DisplayName("scenario1_claudeMd_atMost200Lines")
    void scenario1_claudeMd_atMost200Lines() throws IOException {
        List<String> lines = Files.readAllLines(CLAUDE_MD, StandardCharsets.UTF_8);
        assertThat(lines.size())
                .as("CLAUDE.md must be ≤%d lines (current: %d)", MAX_CLAUDE_MD_LINES, lines.size())
                .isLessThanOrEqualTo(MAX_CLAUDE_MD_LINES);
    }

    @Test
    @DisplayName("scenario2_epicsHistory_existsWithMinConcludedBlocks")
    void scenario2_epicsHistory_existsWithMinConcludedBlocks() throws IOException {
        assertThat(EPICS_HISTORY)
                .as("docs/epics-history.md must exist")
                .exists();

        List<String> lines = Files.readAllLines(EPICS_HISTORY, StandardCharsets.UTF_8);
        long blockCount =
                lines.stream().filter(l -> CONCLUDED_HEADER.matcher(l).find()).count();

        assertThat(blockCount)
                .as(
                        "docs/epics-history.md must contain ≥%d Concluded blocks (found: %d)",
                        MIN_CONCLUDED_BLOCKS, blockCount)
                .isGreaterThanOrEqualTo(MIN_CONCLUDED_BLOCKS);
    }

    @Test
    @DisplayName("scenario3_claudeMd_containsLinkToEpicsHistory")
    void scenario3_claudeMd_containsLinkToEpicsHistory() throws IOException {
        String content = Files.readString(CLAUDE_MD, StandardCharsets.UTF_8);
        assertThat(content)
                .as("CLAUDE.md must contain a link to docs/epics-history.md")
                .contains("docs/epics-history.md");
    }

    @Test
    @DisplayName("scenario4_claudeMd_containsNoConcludedBlocks")
    void scenario4_claudeMd_containsNoConcludedBlocks() throws IOException {
        List<String> lines = Files.readAllLines(CLAUDE_MD, StandardCharsets.UTF_8);
        List<String> concludedLines =
                lines.stream().filter(l -> CONCLUDED_HEADER.matcher(l).find()).toList();

        assertThat(concludedLines)
                .as(
                        "CLAUDE.md must contain no 'Concluded — ' blockquote headers; found: %s",
                        concludedLines)
                .isEmpty();
    }

    @Test
    @DisplayName("scenario5_extractedContent_representsMeaningfulTokenReduction")
    void scenario5_extractedContent_representsMeaningfulTokenReduction() throws IOException {
        // Token reduction is estimated via bytes/4 heuristic on extracted content.
        // docs/epics-history.md holds the extracted blocks; its byte size approximates reduction.
        long extractedBytes = Files.size(EPICS_HISTORY);
        long estimatedTokensExtracted = extractedBytes / 4;

        assertThat(estimatedTokensExtracted)
                .as(
                        "Extracted content (%d bytes) must represent ≥%d token-equivalents "
                                + "(bytes/4 heuristic); estimated: %d",
                        extractedBytes, REQUIRED_TOKEN_REDUCTION, estimatedTokensExtracted)
                .isGreaterThanOrEqualTo(REQUIRED_TOKEN_REDUCTION);
    }
}
