package dev.iadev.application.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryIndexWriterTest {

    // ── upsertInContent (pure, no I/O) ──────────────────────────────────────

    @Test
    void upsertInContent_newEntry_appended() {
        String content = "entries:\n";

        String result = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");

        assertThat(result).contains("- epic-id: EPIC-0067");
        assertThat(result).contains("indexable: true");
        assertThat(result).contains("archived: false");
        assertThat(result).contains("last-updated: \"2026-05-01\"");
        assertThat(result).contains("superseded-by: null");
    }

    @Test
    void upsertInContent_existingEntry_updatesLastUpdated() {
        String content = "entries:\n"
                + "  - epic-id: EPIC-0067\n"
                + "    indexable: false\n"
                + "    archived: true\n"
                + "    last-updated: \"2026-01-01\"\n"
                + "    superseded-by: null\n";

        String result = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");

        assertThat(result).contains("last-updated: \"2026-05-01\"");
    }

    @Test
    void upsertInContent_existingEntry_preservesIndexableFlag() {
        String content = "entries:\n"
                + "  - epic-id: EPIC-0067\n"
                + "    indexable: false\n"
                + "    archived: false\n"
                + "    last-updated: \"2026-01-01\"\n";

        String result = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");

        assertThat(result).contains("indexable: false");
    }

    @Test
    void upsertInContent_existingEntry_preservesArchivedFlag() {
        String content = "entries:\n"
                + "  - epic-id: EPIC-0067\n"
                + "    indexable: true\n"
                + "    archived: true\n"
                + "    last-updated: \"2026-01-01\"\n";

        String result = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");

        assertThat(result).contains("archived: true");
    }

    @Test
    void upsertInContent_multipleEntries_onlyTargetUpdated() {
        String content = "entries:\n"
                + "  - epic-id: EPIC-0060\n"
                + "    indexable: true\n"
                + "    archived: false\n"
                + "    last-updated: \"2026-01-01\"\n"
                + "  - epic-id: EPIC-0067\n"
                + "    indexable: true\n"
                + "    archived: false\n"
                + "    last-updated: \"2026-01-01\"\n";

        String result = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");

        int idx0060 = result.indexOf("EPIC-0060");
        int lastUpdatedFor0060 = result.indexOf("last-updated:", idx0060);
        assertThat(result.substring(lastUpdatedFor0060)).startsWith("last-updated: \"2026-01-01\"");

        int idx0067 = result.indexOf("EPIC-0067");
        int lastUpdatedFor0067 = result.indexOf("last-updated:", idx0067);
        assertThat(result.substring(lastUpdatedFor0067)).startsWith("last-updated: \"2026-05-01\"");
    }

    @Test
    void upsertInContent_emptyFile_createsValidEntry() {
        String result = MemoryIndexWriter.upsertInContent("entries:\n", "EPIC-0010", "2026-05-01");

        assertThat(result).startsWith("entries:\n");
        assertThat(result).contains("- epic-id: EPIC-0010");
    }

    @Test
    void upsertInContent_idempotent_sameInputSameOutput() {
        String content = "entries:\n";

        String first = MemoryIndexWriter.upsertInContent(content, "EPIC-0067", "2026-05-01");
        String second = MemoryIndexWriter.upsertInContent(first, "EPIC-0067", "2026-05-01");

        assertThat(first).isEqualTo(second);
    }

    // ── upsert (filesystem) ─────────────────────────────────────────────────

    @Test
    void upsert_fileNotExists_createsFileWithEntry(@TempDir Path dir) throws IOException {
        Path index = dir.resolve("_index.yaml");

        MemoryIndexWriter.upsert(index, "EPIC-0067", "2026-05-01");

        assertThat(Files.exists(index)).isTrue();
        String written = Files.readString(index);
        assertThat(written).contains("- epic-id: EPIC-0067");
    }

    @Test
    void upsert_fileExists_updatesEntry(@TempDir Path dir) throws IOException {
        Path index = dir.resolve("_index.yaml");
        Files.writeString(index,
                "entries:\n"
                + "  - epic-id: EPIC-0067\n"
                + "    indexable: true\n"
                + "    archived: false\n"
                + "    last-updated: \"2026-01-01\"\n");

        MemoryIndexWriter.upsert(index, "EPIC-0067", "2026-05-10");

        assertThat(Files.readString(index)).contains("last-updated: \"2026-05-10\"");
    }
}
