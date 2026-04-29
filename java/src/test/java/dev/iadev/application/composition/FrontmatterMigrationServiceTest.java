package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FrontmatterMigrationService")
class FrontmatterMigrationServiceTest {

    private final FrontmatterMigrationService service = new FrontmatterMigrationService();

    @Nested
    @DisplayName("migrateFile()")
    class MigrateFile {

        @Test
        @DisplayName("adds requires-capabilities to file missing it")
        void addsRequiresCapabilities(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("skill.md");
            Files.writeString(file, "---\nname: test\nmodel: sonnet\n---\n# Body\n");
            boolean migrated = service.migrateFile(file);
            assertThat(migrated).isTrue();
            assertThat(Files.readString(file)).contains("requires-capabilities: []");
        }

        @Test
        @DisplayName("skips file already having requires-capabilities")
        void skipsAlreadyMigrated(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("skill.md");
            Files.writeString(file, "---\nname: test\nrequires-capabilities: [data.database.postgres]\n---\n");
            boolean migrated = service.migrateFile(file);
            assertThat(migrated).isFalse();
            String content = Files.readString(file);
            assertThat(content).contains("data.database.postgres");
        }

        @Test
        @DisplayName("returns false for file without frontmatter")
        void skipsNoFrontmatter(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("readme.md");
            Files.writeString(file, "# Just a readme\nNo frontmatter.");
            assertThat(service.migrateFile(file)).isFalse();
        }

        @Test
        @DisplayName("file with only opening delimiter and no closing returns false")
        void noClosingDelimiterReturnsFalse(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("bad.md");
            Files.writeString(file, "---\nname: x\nno closing");
            assertThat(service.migrateFile(file)).isFalse();
        }

        @Test
        @DisplayName("frontmatter with no newline after opening delimiter returns false")
        void noNewlineReturnsFalse(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("no-nl.md");
            // Single line with just --- (stripped = "---" with no newline following)
            Files.writeString(file, "regular content without frontmatter");
            assertThat(service.migrateFile(file)).isFalse();
        }

        @Test
        @DisplayName("body content preserved after migration")
        void bodyPreserved(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("skill.md");
            Files.writeString(file, "---\nname: x-test\n---\n## Phase 1\nSome body content.\n");
            service.migrateFile(file);
            String content = Files.readString(file);
            assertThat(content).contains("## Phase 1");
            assertThat(content).contains("Some body content.");
        }
    }

    @Nested
    @DisplayName("MigrationResult.empty()")
    class Empty {

        @Test
        @DisplayName("empty() returns zero counts")
        void emptyHasZeroCounts() {
            var result = FrontmatterMigrationService.MigrationResult.empty();
            assertThat(result.processed()).isZero();
            assertThat(result.migrated()).isZero();
            assertThat(result.skipped()).isZero();
            assertThat(result.failures()).isEmpty();
        }
    }

    @Nested
    @DisplayName("glob matching")
    class GlobMatching {

        @Test
        @DisplayName("SKILL.md glob matches SKILL.md files")
        void skillMdGlobMatches(@TempDir Path tmp) throws IOException {
            Path dir = tmp.resolve("skills");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("SKILL.md"), "---\nname: x\n---\n# Content\n");
            Files.writeString(dir.resolve("other.md"), "---\nname: y\n---\n# Content\n");
            var result = service.migrateDirectory(tmp, "SKILL.md");
            assertThat(result.processed()).isEqualTo(1);
        }

        @Test
        @DisplayName("unknown glob defaults to *.md matching")
        void unknownGlobDefaultsToMd(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("skill.md"), "---\nname: x\n---\n# Content\n");
            var result = service.migrateDirectory(tmp, "unknown-glob");
            assertThat(result.processed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("migrateDirectory()")
    class MigrateDirectory {

        @Test
        @DisplayName("missing directory returns failure")
        void missingDirectoryReturnsFailure(@TempDir Path tmp) {
            var result = service.migrateDirectory(tmp.resolve("nonexistent"), "SKILL.md");
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().get(0)).contains("directory not found");
        }

        @Test
        @DisplayName("migrates all matching files in directory")
        void migratesMatchingFiles(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("a.md"), "---\nname: a\n---\n# Content\n");
            Files.writeString(tmp.resolve("b.md"), "---\nname: b\nrequires-capabilities: []\n---\n");
            var result = service.migrateDirectory(tmp, "*.md");
            assertThat(result.migrated()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.processed()).isEqualTo(2);
        }
    }
}
