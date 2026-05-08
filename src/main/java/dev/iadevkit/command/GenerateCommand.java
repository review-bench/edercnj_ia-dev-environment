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
        Path target = outputDir.toAbsolutePath().normalize();
        int copied = copyResourceTree("claude", target.resolve(".claude"));
        copyClaudioMd(target);
        System.out.printf(
                "ia-dev-kit: %d files %s to %s%n",
                copied, dryRun ? "would be copied" : "copied", target);
        return 0;
    }

    private int copyResourceTree(String resourceRoot, Path targetDir)
            throws IOException, URISyntaxException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceRoot);
        if (resourceUrl == null) {
            throw new IllegalStateException("Bundled resource not found: " + resourceRoot);
        }

        URI uri = resourceUrl.toURI();
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                return walkAndCopy(fs.getPath("/" + resourceRoot), targetDir);
            }
        }
        return walkAndCopy(Path.of(uri), targetDir);
    }

    private int walkAndCopy(Path source, Path targetDir) throws IOException {
        int count = 0;
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                if (Files.isDirectory(src)) {
                    continue;
                }
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
                if (verbose)
                    System.out.println("  " + (dryRun ? "would copy " : "copy   ") + dest);
                count++;
            }
        }
        return count;
    }

    private void copyClaudioMd(Path targetDir) throws IOException {
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
    }
}
