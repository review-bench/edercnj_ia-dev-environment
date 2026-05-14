package dev.iadevkit.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "generate",
        description = "Copies Claude Code resources (.claude/) to the target directory",
        mixinStandardHelpOptions = true)
public class GenerateCommand implements Callable<Integer> {

    static final String CLAUDE_MD = "CLAUDE.md";
    static final String RESOURCE_ROOT = "claude";

    @Spec CommandLine.Model.CommandSpec spec;

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
        long startTimeMs = System.currentTimeMillis();
        Path target = outputDir.toAbsolutePath().normalize();

        if (!isWithinAllowedScope(target)) {
            spec.commandLine()
                    .getErr()
                    .println("Error: output directory is not valid: " + outputDir);
            return 2;
        }

        Map<String, Integer> counts = new TreeMap<>();
        PrintWriter out = spec.commandLine().getOut();

        copyResourceTree(RESOURCE_ROOT, target.resolve(".claude"), counts, out);
        copyRootClaudeMd(target, counts, out);

        printSummary(counts, System.currentTimeMillis() - startTimeMs, out);
        return 0;
    }

    private boolean isWithinAllowedScope(Path target) {
        try {
            Path canonical = target.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
            return !canonical.toString().contains("..");
        } catch (IOException e) {
            // Path does not exist yet — validate syntactically
            return !target.toString().contains("..");
        }
    }

    private void copyResourceTree(
            String resourceRoot, Path targetDir, Map<String, Integer> counts, PrintWriter out)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource(resourceRoot);
        if (url == null)
            throw new IllegalStateException("Bundled resource not found: " + resourceRoot);
        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                walkAndCopy(fs.getPath("/" + resourceRoot), targetDir, counts, out);
            }
        } else {
            walkAndCopy(Path.of(uri), targetDir, counts, out);
        }
    }

    private void walkAndCopy(
            Path source, Path targetDir, Map<String, Integer> counts, PrintWriter out)
            throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                if (Files.isDirectory(src)) continue;
                String relative = source.relativize(src).toString();
                Path dest = targetDir.resolve(relative);
                String displayPath = targetDir.getFileName() + "/" + relative;
                if (!force && Files.exists(dest)) {
                    if (verbose) out.println("  skip   " + displayPath);
                    continue;
                }
                if (!dryRun) {
                    Files.createDirectories(dest.getParent());
                    try (InputStream in = Files.newInputStream(src)) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                if (verbose) out.println("  " + (dryRun ? "would copy " : "copy   ") + displayPath);
                counts.merge(categorize(relative), 1, Integer::sum);
            }
        }
    }

    private void copyRootClaudeMd(Path targetDir, Map<String, Integer> counts, PrintWriter out)
            throws IOException {
        URL url = getClass().getClassLoader().getResource(CLAUDE_MD);
        if (url == null) return;
        Path dest = targetDir.resolve(CLAUDE_MD);
        if (!force && Files.exists(dest)) {
            if (verbose) out.println("  skip   " + CLAUDE_MD);
            return;
        }
        if (!dryRun) {
            try (InputStream in = url.openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (verbose) out.println("  " + (dryRun ? "would copy " : "copy   ") + CLAUDE_MD);
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

    private void printSummary(Map<String, Integer> counts, long elapsedMs, PrintWriter out) {
        String action = dryRun ? "Dry Run" : "Success";
        out.printf("%nPipeline: %s (%dms)%n%n", action, elapsedMs);
        String sep = "  " + "─".repeat(22) + "  " + "─".repeat(5);
        out.printf("  %-22s  %5s%n", "Category", "Count");
        out.println(sep);
        int total = 0;
        for (Map.Entry<String, Integer> categoryEntry : counts.entrySet()) {
            out.printf("  %-22s  %5d%n", categoryEntry.getKey(), categoryEntry.getValue());
            total += categoryEntry.getValue();
        }
        out.println(sep);
        out.printf("  %-22s  %5d%n", "Total", total);
        out.flush();
    }
}
