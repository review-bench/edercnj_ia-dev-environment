package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.product.CreateProductOrchestrationUseCase;
import dev.iadev.application.product.CreateProductResult;
import dev.iadev.domain.capability.CapabilityStubFactory;
import dev.iadev.domain.ideation.IdeationSection;
import dev.iadev.domain.ideation.IdeationTemplate;
import dev.iadev.domain.ideation.IdeationToProductTransformer;
import dev.iadev.domain.ideation.IdeationValidator;
import dev.iadev.domain.product.RNFRootValidator;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "x-create-product",
        mixinStandardHelpOptions = true,
        description =
                "Transform an ideation file into a Product artifact with RNF roots and C1 capability stub.")
public class XCreateProductCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec CommandSpec spec;

    @Option(
            names = {"--ideation-file"},
            required = true,
            description = "Path to the ideation markdown file.")
    String ideationFilePath;

    @Option(
            names = {"--output-dir"},
            description =
                    "Output directory for generated product artifacts (default: ai/products/).")
    String outputDirPath;

    @Option(
            names = {"--product-id"},
            description =
                    "Override product identifier (format: product-NNNN, max 15 chars). Auto-generated if omitted.")
    String productId;

    @Option(
            names = {"--dry-run"},
            description = "Validate inputs without writing any files.")
    boolean dryRun;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();

        try {
            XCreateProductArgumentParser.validateProductId(productId);
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return EXIT_VALIDATION;
        }

        var request =
                XCreateProductArgumentParser.parse(
                        ideationFilePath, outputDirPath, productId, dryRun);

        if (!Files.exists(request.ideationFile())) {
            out.println("Error: ideation-file not found: " + request.ideationFile());
            return EXIT_VALIDATION;
        }

        if (dryRun) {
            out.println("Validation OK [dry-run]; would create product at: " + request.outputDir());
            return EXIT_SUCCESS;
        }

        IdeationTemplate ideation = parseIdeation(request.ideationFile());
        String resolvedProductId = request.productId().orElse("product-0001");

        var useCase =
                new CreateProductOrchestrationUseCase(
                        new IdeationValidator(),
                        new IdeationToProductTransformer(),
                        new CapabilityStubFactory(),
                        new RNFRootValidator());

        CreateProductResult result = useCase.execute(resolvedProductId, ideation);

        if (!result.successful()) {
            result.validationErrors().forEach(error -> out.println("Error: " + error));
            return EXIT_VALIDATION;
        }

        out.println("Product created: " + result.product().name() + " [" + resolvedProductId + "]");
        return EXIT_SUCCESS;
    }

    private static IdeationTemplate parseIdeation(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String title = "";
        Map<IdeationSection, String> sections = new EnumMap<>(IdeationSection.class);
        IdeationSection currentSection = null;
        StringBuilder sectionBody = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("# ") && title.isEmpty()) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                if (currentSection != null) {
                    sections.put(currentSection, sectionBody.toString().trim());
                }
                currentSection = parseSectionHeading(line);
                sectionBody = new StringBuilder();
            } else if (currentSection != null) {
                sectionBody.append(line).append("\n");
            }
        }

        if (currentSection != null) {
            sections.put(currentSection, sectionBody.toString().trim());
        }

        return IdeationTemplate.builder().title(title).sections(sections).build();
    }

    private static IdeationSection parseSectionHeading(String line) {
        String heading = line.substring(3).trim();
        for (IdeationSection section : IdeationSection.values()) {
            if (heading.startsWith(section.number() + ".")
                    || heading.startsWith(section.number() + " ")) {
                return section;
            }
        }
        return null;
    }
}
