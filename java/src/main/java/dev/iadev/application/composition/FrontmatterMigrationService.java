package dev.iadev.application.composition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Batch migration service: adds {@code requires-capabilities: []} to artifacts lacking it (v2→v3.0).
 *
 * <p>Used by stories 0203-0214 of EPIC-0064 to migrate ~182 artifacts in one pass (RULE-008).
 * Stack-specific capabilities should be refined manually or via {@code x-frontmatter-migrate}.
 */
public final class FrontmatterMigrationService {

    private static final String FRONT_DELIM = "---";
    private static final String REQUIRES_FIELD = "requires-capabilities";

    public record MigrationResult(int processed, int migrated, int skipped, List<String> failures) {
        public static MigrationResult empty() {
            return new MigrationResult(0, 0, 0, List.of());
        }
    }

    public MigrationResult migrateDirectory(Path dir, String glob) {
        if (!Files.isDirectory(dir)) {
            return new MigrationResult(0, 0, 0, List.of("directory not found: " + dir));
        }
        List<String> failures = new ArrayList<>();
        int[] counts = {0, 0, 0};
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> matchesGlob(p, glob))
                    .sorted()
                    .forEach(p -> {
                        counts[0]++;
                        try {
                            if (migrateFile(p)) counts[1]++;
                            else counts[2]++;
                        } catch (IOException e) {
                            failures.add(p + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            failures.add("error walking dir: " + e.getMessage());
        }
        return new MigrationResult(counts[0], counts[1], counts[2], failures);
    }

    public boolean migrateFile(Path file) throws IOException {
        String content = Files.readString(file);
        String stripped = content.stripLeading();
        if (!stripped.startsWith(FRONT_DELIM)) return false;

        int firstNewline = stripped.indexOf('\n');
        if (firstNewline < 0) return false;
        int endFm = stripped.indexOf("\n" + FRONT_DELIM, firstNewline);
        if (endFm < 0) return false;

        String frontmatter = stripped.substring(firstNewline + 1, endFm);
        if (frontmatter.contains(REQUIRES_FIELD)) return false;

        String migratedFm = frontmatter.stripTrailing() + "\n" + REQUIRES_FIELD + ": []\n";
        String rest = stripped.substring(endFm);
        String result = FRONT_DELIM + "\n" + migratedFm + rest;
        Files.writeString(file, result);
        return true;
    }

    private boolean matchesGlob(Path path, String glob) {
        String name = path.getFileName().toString();
        return switch (glob) {
            case "*.md" -> name.endsWith(".md") && !name.startsWith("_");
            case "SKILL.md" -> name.equals("SKILL.md");
            case "*.sh" -> name.endsWith(".sh");
            default -> name.endsWith(".md");
        };
    }
}
