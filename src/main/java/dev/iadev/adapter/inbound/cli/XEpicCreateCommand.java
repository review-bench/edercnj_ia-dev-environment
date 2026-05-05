package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.feature.CreateEpicFromFeatureResult;
import dev.iadev.application.feature.CreateEpicFromFeatureUseCase;
import dev.iadev.application.feature.FeatureEpicSourceLoader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-epic-create",
        mixinStandardHelpOptions = true,
        description = "Create an Epic artifact from a Feature with source lineage and inherited RNFs.")
public class XEpicCreateCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec
    CommandSpec spec;

    @Option(
            names = {"--from-feature"},
            required = true,
            description = "Feature markdown file path or feature id.")
    String fromFeature;

    @Option(
            names = {"--capability-file"},
            description = "Capability markdown file used to load inherited RNFs.")
    String capabilityFilePath;

    @Option(
            names = {"--product-file"},
            description = "Product markdown file used to extend RNF inheritance.")
    String productFilePath;

    @Option(
            names = {"--epic-id"},
            description = "Epic identifier (4 digits). Default: 0001.")
    String epicId = "0001";

    @Option(
            names = {"--output-dir"},
            description = "Output directory for generated epic artifacts (default: ai/epics).")
    String outputDirPath;

    @Option(
            names = {"--dry-run"},
            description = "Validate and resolve inputs without writing files.")
    boolean dryRun;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        try {
            Path featureFile = resolveFeature(fromFeature);
            Path capabilityFile = resolveOptional(capabilityFilePath);
            Path productFile = resolveOptional(productFilePath);
            Path outputDir = outputDirPath == null ? Path.of("ai", "epics") : Path.of(outputDirPath);
            if (dryRun) {
                out.println("Validation OK [dry-run]; source-feature=" + featureFile);
                return EXIT_SUCCESS;
            }
            CreateEpicFromFeatureResult result = new CreateEpicFromFeatureUseCase(new FeatureEpicSourceLoader())
                    .execute(epicId, featureFile, capabilityFile, productFile, outputDir);
            out.println("Epic created: " + result.epicFile() + " (inheritedRnfs=" + result.inheritedRnfCount() + ")");
            return EXIT_SUCCESS;
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return EXIT_VALIDATION;
        } catch (IOException e) {
            out.println("Error: " + e.getMessage());
            return EXIT_EXECUTION;
        }
    }

    private static Path resolveFeature(String reference) throws IOException {
        Path direct = Path.of(reference);
        if (Files.exists(direct)) {
            return direct;
        }
        try (Stream<Path> files = Files.walk(Path.of("ai"))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> path.getFileName().toString().contains(reference))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("feature not found: " + reference));
        }
    }

    private static Path resolveOptional(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("file not found: " + path);
        }
        return path;
    }
}
