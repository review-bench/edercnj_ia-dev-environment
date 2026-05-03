package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RetroSeedSmokeIT — ai/memory/ retro-seed quality rubric (story-0075-0006)")
class RetroSeedSmokeIT {

    private static final Path MEMORY_DIR = Path.of("ai", "memory");
    private static final Path INDEX_FILE = MEMORY_DIR.resolve("_index.yaml");
    private static final int MAX_LINES = 200;

    private static final List<String> REQUIRED_FRONTMATTER_FIELDS = List.of(
            "epic-id:",
            "slug:",
            "summary-version:",
            "created:",
            "last-updated:",
            "indexable:",
            "archived:",
            "superseded-by:",
            "tags:",
            "capabilities-affected:",
            "rules-affected:",
            "adrs-referenced:",
            "patterns-introduced:",
            "antipatterns-rejected:",
            "dependencies-of:",
            "dependencies-for:"
    );

    private static final List<String> REQUIRED_SECTIONS = List.of(
            "## Why this epic existed",
            "## Hypothesis tested",
            "## Decisions taken (with why)",
            "## Alternatives rejected (with why)",
            "## Reusable patterns produced",
            "## Anti-patterns observed",
            "## Links"
    );

    static Stream<Path> summaryFiles() throws IOException {
        assertThat(MEMORY_DIR).as("ai/memory/ directory must exist").isDirectory();
        return Files.list(MEMORY_DIR)
                .filter(p -> p.getFileName().toString().matches("epic-\\d{4}-summary\\.md"))
                .sorted();
    }

    @ParameterizedTest(name = "C1 frontmatter — {0}")
    @MethodSource("summaryFiles")
    void validateFrontmatter(Path summaryFile) throws IOException {
        String content = Files.readString(summaryFile, StandardCharsets.UTF_8);
        assertThat(content).as("%s must start with YAML frontmatter", summaryFile)
                .startsWith("---");

        for (String field : REQUIRED_FRONTMATTER_FIELDS) {
            assertThat(content)
                    .as("%s must contain frontmatter field '%s'", summaryFile.getFileName(), field)
                    .contains(field);
        }
    }

    @ParameterizedTest(name = "C2 sections — {0}")
    @MethodSource("summaryFiles")
    void validateSections(Path summaryFile) throws IOException {
        String content = Files.readString(summaryFile, StandardCharsets.UTF_8);

        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("%s must contain section '%s'", summaryFile.getFileName(), section)
                    .contains(section);
        }
    }

    @ParameterizedTest(name = "C3 line cap — {0}")
    @MethodSource("summaryFiles")
    void validateLineCap(Path summaryFile) throws IOException {
        List<String> lines = Files.readAllLines(summaryFile, StandardCharsets.UTF_8);
        assertThat(lines)
                .as("%s must not exceed %d lines (got %d)", summaryFile.getFileName(), MAX_LINES, lines.size())
                .hasSizeLessThanOrEqualTo(MAX_LINES);
    }

    @ParameterizedTest(name = "C5 hypothesis non-empty — {0}")
    @MethodSource("summaryFiles")
    void validateHypothesisNonEmpty(Path summaryFile) throws IOException {
        String content = Files.readString(summaryFile, StandardCharsets.UTF_8);
        int hypothesisIdx = content.indexOf("## Hypothesis tested");
        assertThat(hypothesisIdx).as("%s must contain ## Hypothesis tested", summaryFile.getFileName())
                .isGreaterThanOrEqualTo(0);

        int nextSectionIdx = content.indexOf("## ", hypothesisIdx + 1);
        String hypothesisBody = nextSectionIdx > 0
                ? content.substring(hypothesisIdx + "## Hypothesis tested".length(), nextSectionIdx)
                : content.substring(hypothesisIdx + "## Hypothesis tested".length());

        assertThat(hypothesisBody.trim())
                .as("%s ## Hypothesis tested section must not be empty", summaryFile.getFileName())
                .isNotEmpty();
    }

    @Test
    @DisplayName("C4 — every summary file has an _index.yaml entry")
    void validateIndexConsistency() throws IOException {
        assertThat(INDEX_FILE).as("_index.yaml must exist").isRegularFile();
        String indexContent = Files.readString(INDEX_FILE, StandardCharsets.UTF_8);

        try (Stream<Path> files = Files.list(MEMORY_DIR)) {
            files.filter(p -> p.getFileName().toString().matches("epic-\\d{4}-summary\\.md"))
                    .sorted()
                    .forEach(summaryFile -> {
                        String filename = summaryFile.getFileName().toString();
                        assertThat(indexContent)
                                .as("_index.yaml must have entry for %s", filename)
                                .contains("summary-path: " + filename);
                    });
        }
    }

    @Test
    @DisplayName("C4 reverse — every _index.yaml entry has a corresponding file")
    void validateIndexEntriesHaveFiles() throws IOException {
        assertThat(INDEX_FILE).as("_index.yaml must exist").isRegularFile();
        String indexContent = Files.readString(INDEX_FILE, StandardCharsets.UTF_8);

        for (String line : indexContent.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("summary-path:")) {
                String filename = trimmed.replace("summary-path:", "").trim();
                Path summaryFile = MEMORY_DIR.resolve(filename);
                assertThat(summaryFile)
                        .as("File referenced in _index.yaml must exist: %s", filename)
                        .isRegularFile();
            }
        }
    }

    @Test
    @DisplayName("C6 — superseded-by references point to existing summaries")
    void validateSupersededConsistency() throws IOException {
        try (Stream<Path> files = Files.list(MEMORY_DIR)) {
            files.filter(p -> p.getFileName().toString().matches("epic-\\d{4}-summary\\.md"))
                    .sorted()
                    .forEach(summaryFile -> {
                        try {
                            String content = Files.readString(summaryFile, StandardCharsets.UTF_8);
                            // Extract superseded-by value
                            for (String line : content.lines().toList()) {
                                if (line.trim().startsWith("superseded-by:") && !line.contains("null")) {
                                    String ref = line.replace("superseded-by:", "").trim();
                                    // ref is EPIC-XXXX format
                                    if (ref.matches("EPIC-\\d{4}")) {
                                        String refNum = ref.replace("EPIC-", "").toLowerCase();
                                        Path refFile = MEMORY_DIR.resolve("epic-" + refNum + "-summary.md");
                                        assertThat(refFile)
                                                .as("%s superseded-by %s but %s not found",
                                                        summaryFile.getFileName(), ref, refFile.getFileName())
                                                .isRegularFile();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
