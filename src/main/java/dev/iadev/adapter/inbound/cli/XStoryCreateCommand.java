package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.feature.CreateStoriesFromFeatureResult;
import dev.iadev.application.feature.CreateStoriesFromFeatureUseCase;
import dev.iadev.application.feature.FeatureEpicSourceLoader;
import dev.iadev.application.feature.FeatureMarkdownParser;
import dev.iadev.application.feature.FeatureToStoryDecompositionUseCase;
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
        name = "x-story-create",
        mixinStandardHelpOptions = true,
        description = "Create 1-N Story artifacts from an existing Feature markdown with epic linkage.")
public class XStoryCreateCommand implements Callable<Integer> {

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
            names = {"--epic-id"},
            required = true,
            description = "Epic identifier (EPIC-NNNN or NNNN).")
    String epicId;

    @Option(
            names = {"--capability-file"},
            description = "Capability markdown file used to load inherited RNFs.")
    String capabilityFilePath;

    @Option(
            names = {"--product-file"},
            description = "Product markdown file used to extend RNF inheritance.")
    String productFilePath;

    @Option(
            names = {"--output-dir"},
            description = "Output directory for generated story artifacts (default: ai/epics).")
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
            String normalizedEpicId = normalizeEpicId(epicId);
            Path outputDir = outputDirPath == null ? Path.of("ai", "epics") : Path.of(outputDirPath);
            if (dryRun) {
                out.println("Validation OK [dry-run]; feature=" + featureFile + ", epicId=" + normalizedEpicId);
                return EXIT_SUCCESS;
            }
            CreateStoriesFromFeatureResult result = new CreateStoriesFromFeatureUseCase(
                    new FeatureMarkdownParser(),
                    new FeatureEpicSourceLoader(),
                    new FeatureToStoryDecompositionUseCase())
                    .execute(normalizedEpicId, featureFile, capabilityFile, productFile, outputDir);
            out.println("Stories created: " + result.storyFiles().size());
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

    private static String normalizeEpicId(String value) {
        String normalized = value == null ? "" : value.replace("EPIC-", "");
        if (!normalized.matches("\\d{4}")) {
            throw new IllegalArgumentException("epic-id must be EPIC-NNNN or NNNN");
        }
        return normalized;
    }
}
