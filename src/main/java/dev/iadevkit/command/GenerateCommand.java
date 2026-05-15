package dev.iadevkit.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "generate",
        description = "Copies Claude Code resources (.claude/) to the target directory",
        mixinStandardHelpOptions = true)
public class GenerateCommand implements Callable<Integer> {

    @Option(
            names = {"-o", "--output"},
            description = "Target project directory (default: current directory)",
            defaultValue = ".")
    private Path outputDir;

    @Option(
            names = {"-f", "--force"},
            description = "Overwrite existing files")
    private boolean force;

    @Option(
            names = {"--dry-run"},
            description = "Simulate without writing any files")
    private boolean dryRun;

    @Option(
            names = {"-v", "--verbose"},
            description = "List each copied file")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        Path target = outputDir.toAbsolutePath().normalize();
        Map<String, Integer> counts = new TreeMap<>();

        copyResourceTree("claude", target.resolve(".claude"), counts);
        copyClaudioMd(target, counts);

        printSummary(counts, System.currentTimeMillis() - start);
        return 0;
    }

    private void copyResourceTree(String resourceRoot, Path targetDir, Map<String, Integer> counts)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource(resourceRoot);
        if (url == null)
            throw new IllegalStateException("Bundled resource not found: " + resourceRoot);
        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                walkAndCopy(fs.getPath("/" + resourceRoot), targetDir, counts);
            }
        } else {
            walkAndCopy(Path.of(uri), targetDir, counts);
        }
    }

    private void walkAndCopy(Path source, Path targetDir, Map<String, Integer> counts)
            throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                if (Files.isDirectory(src)) continue;
                String relative = source.relativize(src).toString();
                Path dest = targetDir.resolve(relative);
                if (!force && Files.exists(dest)) {
                    if (verbose) System.out.println("  skip   " + dest);
                    continue;
                }
                if (!dryRun) {
                    Files.createDirectories(dest.getParent());
                    try (InputStream in = Files.newInputStream(src)) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                if (verbose) System.out.println("  " + (dryRun ? "would copy " : "copy   ") + dest);
                counts.merge(categorize(relative), 1, Integer::sum);
            }
        }
    }

    private void copyClaudioMd(Path targetDir, Map<String, Integer> counts) throws IOException {
        URL url = getClass().getClassLoader().getResource("CLAUDE.md");
        if (url == null) return;
        Path dest = targetDir.resolve("CLAUDE.md");
        if (!force && Files.exists(dest)) return;
        if (!dryRun) {
            try (InputStream in = url.openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (verbose) System.out.println("  " + (dryRun ? "would copy " : "copy   ") + dest);
        counts.merge("Root Files", 1, Integer::sum);
    }

    static String categorize(String relative) {
        String first =
                relative.contains("/") ? relative.substring(0, relative.indexOf('/')) : relative;
        return switch (first) {
            case "agents" -> "Agents";
            case "hooks" -> "Hooks";
            case "knowledge" -> "Knowledge";
            case "rules" -> "Rules";
            case "scripts" -> "Scripts";
            case "skills" -> "Skills";
            case "templates" -> "Templates";
            case "settings.json" -> "Settings";
            default -> "Other";
        };
    }

    private void printSummary(Map<String, Integer> counts, long elapsedMs) {
        String action = dryRun ? "Dry Run" : "Success";
        System.out.printf("%nPipeline: %s (%dms)%n%n", action, elapsedMs);
        String sep = "  " + "─".repeat(22) + "  " + "─".repeat(5);
        System.out.printf("  %-22s  %5s%n", "Category", "Count");
        System.out.println(sep);
        int total = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.printf("  %-22s  %5d%n", e.getKey(), e.getValue());
            total += e.getValue();
        }
        System.out.println(sep);
        System.out.printf("  %-22s  %5d%n", "Total", total);
    }
}
