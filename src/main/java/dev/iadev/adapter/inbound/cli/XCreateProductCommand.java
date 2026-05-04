package dev.iadev.adapter.inbound.cli;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "x-create-product",
        mixinStandardHelpOptions = true,
        description = "Transform an ideation file into a Product artifact with RNF roots and C1 capability stub.")
public class XCreateProductCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_VALIDATION = 1;
    static final int EXIT_EXECUTION = 2;

    @Spec
    CommandSpec spec;

    @Option(
            names = {"--ideation-file"},
            required = true,
            description = "Path to the ideation markdown file.")
    String ideationFilePath;

    @Option(
            names = {"--output-dir"},
            description = "Output directory for generated product artifacts (default: ai/products/).")
    String outputDirPath;

    @Option(
            names = {"--product-id"},
            description = "Override product identifier (format: product-NNNN, max 15 chars). Auto-generated if omitted.")
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

        var request = XCreateProductArgumentParser.parse(ideationFilePath, outputDirPath, productId, dryRun);

        if (!Files.exists(request.ideationFile())) {
            out.println("Error: ideation-file not found: " + request.ideationFile());
            return EXIT_VALIDATION;
        }

        if (dryRun) {
            out.println("Validation OK [dry-run]; would create product at: " + request.outputDir());
            return EXIT_SUCCESS;
        }

        out.println("Product creation initiated for: " + request.ideationFile());
        return EXIT_SUCCESS;
    }
}
