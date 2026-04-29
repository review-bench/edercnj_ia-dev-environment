package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ArtifactScanner")
class ArtifactScannerTest {

    private final ArtifactScanner scanner = new ArtifactScanner();

    @Nested
    @DisplayName("scan()")
    class ScanMethod {

        @Test
        @DisplayName("empty directory returns empty list")
        void emptyDirectory(@TempDir Path dir) throws IOException {
            assertThat(scanner.scan(dir)).isEmpty();
        }

        @Test
        @DisplayName("file without frontmatter not returned")
        void noFrontmatterSkipped(@TempDir Path dir) throws IOException {
            Files.writeString(dir.resolve("no-fm.md"), "# Just content\nNo frontmatter.");
            assertThat(scanner.scan(dir)).isEmpty();
        }

        @Test
        @DisplayName("file starting with _ is skipped")
        void underscoreFilesSkipped(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("_TEMPLATE-test.md"),
                    "---\nname: template\nrequires-capabilities: []\n---\n");
            assertThat(scanner.scan(dir)).isEmpty();
        }

        @Test
        @DisplayName("valid frontmatter with requires-capabilities scanned")
        void validFrontmatterScanned(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("skill.md"),
                    "---\nname: test-skill\nrequires-capabilities:\n  - data.database.postgres\n---\n# Content\n");
            List<ArtifactScanner.ScannedArtifact> result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities())
                    .containsExactly("data.database.postgres");
        }

        @Test
        @DisplayName("inline list requires-capabilities parsed correctly")
        void inlineListParsed(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("skill.md"),
                    "---\nname: test\nrequires-capabilities: [data.database.postgres, data.cache.redis]\n---\n");
            List<ArtifactScanner.ScannedArtifact> result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities())
                    .containsExactly("data.database.postgres", "data.cache.redis");
        }

        @Test
        @DisplayName("empty requires-capabilities returns empty list")
        void emptyRequiresCapabilities(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("universal.md"),
                    "---\nname: universal\nrequires-capabilities: []\n---\n");
            List<ArtifactScanner.ScannedArtifact> result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities()).isEmpty();
        }

        @Test
        @DisplayName("file with frontmatter missing closing --- returns empty")
        void unclosedFrontmatterSkipped(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("unclosed.md"), "---\nname: test\nrequires-capabilities: []");
            assertThat(scanner.scan(dir)).isEmpty();
        }

        @Test
        @DisplayName("requires-capabilities with other field following stops list correctly")
        void requiresCapabilitiesFollowedByOtherField(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("multi2.md"),
                    "---\nname: x\nrequires-capabilities:\n  - data.database.postgres\nmodel: sonnet\n---\n");
            var result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities())
                    .containsExactly("data.database.postgres");
        }

        @Test
        @DisplayName("multiline requires-capabilities list parsed")
        void multilineRequiresCapabilities(@TempDir Path dir) throws IOException {
            Files.writeString(
                    dir.resolve("multi.md"),
                    "---\nname: x\nrequires-capabilities:\n  - data.database.postgres\n  - data.cache.redis\n---\n");
            var result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities())
                    .containsExactly("data.database.postgres", "data.cache.redis");
        }

        @Test
        @DisplayName("non-.md file in directory is skipped")
        void nonMdFileSkipped(@TempDir Path dir) throws IOException {
            Files.writeString(dir.resolve("script.sh"), "#!/bin/bash\necho hello");
            assertThat(scanner.scan(dir)).isEmpty();
        }

        @Test
        @DisplayName("frontmatter without requires-capabilities field returns universal (empty list)")
        void frontmatterMissingRequiresCapabilities(@TempDir Path dir) throws IOException {
            Files.writeString(dir.resolve("norequires.md"), "---\nname: x\nmodel: sonnet\n---\n# Content\n");
            var result = scanner.scan(dir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).requiredCapabilities()).isEmpty();
        }

        @Test
        @DisplayName("results sorted alphabetically by relative path (RULE-004)")
        void sortedByPath(@TempDir Path dir) throws IOException {
            Path sub = dir.resolve("z-skills");
            Files.createDirectories(sub);
            Files.writeString(
                    sub.resolve("b-skill.md"), "---\nname: b\nrequires-capabilities: []\n---\n");
            Files.writeString(
                    dir.resolve("a-skill.md"), "---\nname: a\nrequires-capabilities: []\n---\n");
            List<ArtifactScanner.ScannedArtifact> result = scanner.scan(dir);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).relativePath()).startsWith("a-skill");
        }
    }
}
