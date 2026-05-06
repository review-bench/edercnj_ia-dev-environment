package dev.iadev.application.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Upserts an entry in {@code ai/memory/_index.yaml}.
 *
 * <p>Preserves {@code indexable} and {@code archived} flags for existing entries. New entries get
 * {@code indexable: true}, {@code archived: false}.
 *
 * <p>This class does not use file locking (the SKILL.md flock is a shell-level concern); locking is
 * handled by the {@code x-internal-epic-summary} skill at orchestration time.
 */
public final class MemoryIndexWriter {

    private static final Pattern EPIC_ID_LINE = Pattern.compile("^(\\s*)- epic-id:\\s*(\\S+)");
    private static final Pattern ENTRIES_INLINE_EMPTY = Pattern.compile("entries:\\s*\\[\\s*\\]");

    private MemoryIndexWriter() {}

    /**
     * Upserts the given {@code epicId} entry in {@code indexFile}.
     *
     * <p>If the file does not exist it is created with a minimal YAML structure. If the entry
     * already exists, its {@code indexable} and {@code archived} flags are preserved; only {@code
     * last-updated} is refreshed.
     *
     * @param indexFile path to {@code _index.yaml}
     * @param epicId e.g. {@code "EPIC-0067"}
     * @param slug e.g. {@code "review-yaml-frontmatter"}
     * @param created ISO-8601 date string for the {@code created} field (new entries only)
     * @param lastUpdated ISO-8601 date string
     * @throws IOException when the file cannot be read or written
     */
    public static void upsert(
            Path indexFile, String epicId, String slug, String created, String lastUpdated)
            throws IOException {
        String existing =
                indexFile.toFile().exists()
                        ? Files.readString(indexFile, StandardCharsets.UTF_8)
                        : "entries:\n";

        String updated = upsertInContent(existing, epicId, slug, created, lastUpdated);
        Files.writeString(
                indexFile,
                updated,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Pure function: applies the upsert logic to raw YAML content and returns the updated string.
     * Exposed for unit testing without filesystem I/O.
     *
     * <p>Normalizes an inline empty list ({@code entries: []}) to block-sequence form ({@code
     * entries:}) before appending, preserving any {@code schemaVersion} header.
     */
    static String upsertInContent(
            String content, String epicId, String slug, String created, String lastUpdated) {
        String normalized = ENTRIES_INLINE_EMPTY.matcher(content).replaceFirst("entries:");
        List<String> lines = new ArrayList<>(List.of(normalized.split("\n", -1)));

        int entryStart = findEntryStart(lines, epicId);
        if (entryStart >= 0) {
            return updateEntry(lines, entryStart, lastUpdated);
        }
        return appendEntry(normalized, epicId, slug, created, lastUpdated);
    }

    private static int findEntryStart(List<String> lines, String epicId) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = EPIC_ID_LINE.matcher(lines.get(i));
            if (m.matches() && epicId.equals(m.group(2))) {
                return i;
            }
        }
        return -1;
    }

    private static String updateEntry(List<String> lines, int entryStart, String lastUpdated) {
        int entryEnd = findEntryEnd(lines, entryStart);
        for (int i = entryStart; i <= entryEnd; i++) {
            String line = lines.get(i);
            if (line.contains("last-updated:")) {
                String indent = leadingSpaces(line);
                lines.set(i, indent + "last-updated: \"" + lastUpdated + "\"");
            }
        }
        return String.join("\n", lines);
    }

    private static int findEntryEnd(List<String> lines, int start) {
        String baseIndent = leadingSpaces(lines.get(start));
        for (int i = start + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            if (!line.startsWith(baseIndent + " ")) return i - 1;
        }
        return lines.size() - 1;
    }

    private static String appendEntry(
            String content, String epicId, String slug, String created, String lastUpdated) {
        String summaryPath = "epic-" + epicId.replace("EPIC-", "").toLowerCase() + "-summary.md";
        String entry =
                "  - epic-id: "
                        + epicId
                        + "\n"
                        + "    slug: "
                        + slug
                        + "\n"
                        + "    summary-path: "
                        + summaryPath
                        + "\n"
                        + "    summary-version: \"1.0\"\n"
                        + "    indexable: true\n"
                        + "    archived: false\n"
                        + "    superseded-by: null\n"
                        + "    created: \""
                        + created
                        + "\"\n"
                        + "    last-updated: \""
                        + lastUpdated
                        + "\"\n";
        if (!content.endsWith("\n")) {
            return content + "\n" + entry;
        }
        return content + entry;
    }

    private static String leadingSpaces(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return line.substring(0, i);
    }
}
